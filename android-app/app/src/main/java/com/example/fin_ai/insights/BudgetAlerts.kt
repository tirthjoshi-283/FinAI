package com.example.fin_ai.insights

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.example.fin_ai.R
import com.example.fin_ai.data.BudgetDto
import com.example.fin_ai.data.TransactionDto
import com.example.fin_ai.ui.util.Fmt

/**
 * Notifies when a category crosses 80% and 100% of its monthly budget — each threshold
 * at most once per category per month, so re-checking is always safe.
 */
object BudgetAlerts {

    private const val CHANNEL = "budget_alerts"

    fun check(context: Context, transactions: List<TransactionDto>, budgets: List<BudgetDto>) {
        if (budgets.isEmpty() || transactions.isEmpty()) return
        val prefs = context.applicationContext.getSharedPreferences("finai_budget_alerts", Context.MODE_PRIVATE)
        val month = BudgetMath.monthKey()
        for (s in BudgetMath.statuses(budgets, transactions)) {
            val threshold = when (s.level) {
                BudgetStatus.Level.OVER -> 100
                BudgetStatus.Level.WARNING -> 80
                BudgetStatus.Level.OK -> continue
            }
            val key = "${month}_${s.category}_$threshold"
            if (prefs.getBoolean(key, false)) continue
            prefs.edit { putBoolean(key, true) }
            if (threshold == 100) {
                notify(
                    context, key.hashCode(),
                    "🚨 ${s.category} budget exceeded",
                    "You've spent ${Fmt.rupees(s.spent)} of your ${Fmt.rupees(s.limit)} ${s.category} budget this month."
                )
            } else {
                notify(
                    context, key.hashCode(),
                    "⚠️ ${s.category} budget ${(s.fraction * 100).toInt()}% used",
                    "${Fmt.rupees(s.remaining)} left for ${s.category} with ${BudgetMath.daysLeftInMonth()} days to go."
                )
            }
        }
    }

    private fun notify(context: Context, id: Int, title: String, text: String) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Budget alerts", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val tap = launch?.let { PendingIntent.getActivity(context, 1, it, PendingIntent.FLAG_IMMUTABLE) }
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(id, n) }
    }
}
