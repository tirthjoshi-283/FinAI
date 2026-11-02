package com.example.fin_ai.sms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.fin_ai.R
import com.example.fin_ai.data.ApiClient
import com.example.fin_ai.data.SmsImportRequest
import com.example.fin_ai.data.SmsImportResponse
import com.example.fin_ai.data.SmsTransactionDto
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** Result of running a set of messages through [SmsParser]. */
data class SmsScanSummary(val genuine: Int, val review: Int, val blocked: Int)

object SmsCapture {

    val REQUIRED_PERMISSIONS = buildList {
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun hasSmsPermissions(context: Context): Boolean = listOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        .all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    /** Sorts messages into upload queue / review list / blocked log. Never uploads SMS text. */
    fun process(context: Context, messages: List<Triple<String, String, Long>>): SmsScanSummary {
        val store = SmsStore(context)
        val trusted = store.trustedHeaders
        val genuine = mutableListOf<ParsedSms>()
        var review = 0
        var blocked = 0
        for ((sender, body, time) in messages) {
            when (val v = SmsParser.analyse(sender, body, time, trusted)) {
                is SmsVerdict.Genuine -> genuine += v.tx
                is SmsVerdict.Unverified -> {
                    store.addReview(ReviewSms(v.header, SmsStore.preview(body), v.tx))
                    review++
                }
                is SmsVerdict.Suspicious -> {
                    store.addBlocked(BlockedSms(sender, SmsStore.preview(body), v.reason, time))
                    blocked++
                }
                SmsVerdict.NotTransaction -> Unit
            }
        }
        store.enqueue(genuine)
        return SmsScanSummary(genuine.size, review, blocked)
    }

    /** Uploads everything queued. Safe to call repeatedly; the server de-duplicates. */
    suspend fun uploadPending(context: Context): SmsImportResponse? {
        val store = SmsStore(context)
        val pending = store.pending()
        // Only the account that owns SMS capture on this phone may receive its messages.
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (pending.isEmpty() || uid == null || uid != store.ownerUid) return null
        var total = SmsImportResponse()
        for (chunk in pending.chunked(100)) {
            val res = ApiClient.api.importSms(SmsImportRequest(chunk.map { it.toDto() }))
            store.removeUploaded(chunk.map { it.hash }.toSet())
            total = total.copy(
                imported = total.imported + res.imported,
                duplicates = total.duplicates + res.duplicates,
                invalid = total.invalid + res.invalid,
                otherAccount = total.otherAccount + res.otherAccount,
                transactions = total.transactions + res.transactions,
            )
        }
        return total
    }

    fun scheduleUpload(context: Context) {
        val request = OneTimeWorkRequestBuilder<SmsUploadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("sms-upload", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    /** Reads the SMS inbox from the last [days] days and processes it. Needs READ_SMS. */
    fun readInbox(context: Context, days: Int): List<Triple<String, String, Long>> {
        val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        val out = mutableListOf<Triple<String, String, Long>>()
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.DATE} >= ?",
            arrayOf(since.toString()),
            "${Telephony.Sms.DATE} DESC"
        )?.use { c ->
            while (c.moveToNext()) {
                val address = c.getString(0) ?: continue
                val body = c.getString(1) ?: continue
                out += Triple(address, body, c.getLong(2))
            }
        }
        return out
    }

    // ---- Notifications ----

    private const val CHANNEL_CAPTURED = "sms_captured"
    private const val CHANNEL_ALERTS = "sms_fraud_alerts"

    fun notifyCaptured(context: Context, tx: ParsedSms) {
        val verb = if (tx.type == "debit") "debited" else "credited"
        val who = tx.merchant?.let { if (tx.type == "debit") " to $it" else " from $it" }.orEmpty()
        notify(
            context, CHANNEL_CAPTURED, "Transactions captured", NotificationManager.IMPORTANCE_LOW,
            tx.hash.hashCode(), "₹%,.2f %s%s".format(tx.amount, verb, who), "Added to FinAI from your ${tx.bankName} SMS."
        )
    }

    fun notifyBlocked(context: Context, sender: String, reason: String) {
        notify(
            context, CHANNEL_ALERTS, "Fake bank SMS alerts", NotificationManager.IMPORTANCE_HIGH,
            (sender + reason).hashCode(), "⚠️ Possible fake bank SMS from $sender", reason
        )
    }

    private fun notify(
        context: Context, channelId: String, channelName: String, importance: Int,
        id: Int, title: String, text: String,
    ) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(channelId, channelName, importance))
        }
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = launch?.let {
            android.app.PendingIntent.getActivity(context, 0, it, android.app.PendingIntent.FLAG_IMMUTABLE)
        }
        val n = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(id, n) } // SecurityException if POST_NOTIFICATIONS was revoked
    }

    private fun ParsedSms.toDto() = SmsTransactionDto(
        hash = hash,
        type = type,
        amount = amount,
        date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(timestampMillis)),
        bankName = bankName,
        merchant = merchant,
        upiRef = upiRef,
        accountLast4 = accountLast4,
    )
}

/** Uploads queued SMS transactions; retried with backoff until it succeeds. */
class SmsUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        // Wait for the owning account to sign in again; never hand its SMS to someone else.
        if (uid == null || uid != SmsStore(applicationContext).ownerUid) return Result.retry()
        val res = SmsCapture.uploadPending(applicationContext)
        if (res != null && res.imported > 0) {
            // Keep the widget and budget alerts current even when the app is closed.
            runCatching {
                val txs = com.example.fin_ai.data.ApiClient.api.getTransactions().transactions
                val budgets = com.example.fin_ai.data.BudgetRepository.cached(applicationContext)
                com.example.fin_ai.insights.BudgetAlerts.check(applicationContext, txs, budgets)
                com.example.fin_ai.widget.WidgetData.update(applicationContext, txs, budgets)
            }
        }
        Result.success()
    }.getOrElse { if (runAttemptCount < 8) Result.retry() else Result.failure() }
}

/** Receives new SMS in real time (works even when the app is closed). */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val store = SmsStore(context)
        if (!store.enabled || store.ownerUid == null) return

        // Long alerts arrive in several parts: stitch them back together per sender.
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val messages = parts
            .groupBy { it.originatingAddress.orEmpty() }
            .map { (sender, msgs) -> Triple(sender, msgs.joinToString("") { it.messageBody.orEmpty() }, msgs.first().timestampMillis) }

        val trusted = store.trustedHeaders
        for ((sender, body, time) in messages) {
            when (val v = SmsParser.analyse(sender, body, time, trusted)) {
                is SmsVerdict.Genuine -> SmsCapture.notifyCaptured(context, v.tx)
                is SmsVerdict.Suspicious -> SmsCapture.notifyBlocked(context, sender, v.reason)
                else -> Unit
            }
        }
        val summary = SmsCapture.process(context, messages)
        if (summary.genuine > 0) SmsCapture.scheduleUpload(context)
    }
}
