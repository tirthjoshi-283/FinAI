package com.example.fin_ai.sms

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** A message that failed an authenticity check, kept so the user can see what was blocked. */
data class BlockedSms(val sender: String, val preview: String, val reason: String, val receivedAt: Long)

/** A plausible alert from a registered header we don't know; waits for the user to trust or dismiss. */
data class ReviewSms(val header: String, val preview: String, val tx: ParsedSms)

/**
 * Small on-device store for SMS auto-capture. Raw SMS text is never uploaded; only
 * short previews of blocked/review items are kept here, on the phone, for the user.
 */
class SmsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("finai_sms", Context.MODE_PRIVATE)
    private val gson = Gson()

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    /**
     * Firebase UID of the account that owns SMS capture on this phone. Only that account
     * imports; other accounts signed in on the same phone see capture as unavailable.
     */
    var ownerUid: String?
        get() = prefs.getString(KEY_OWNER, null)
        set(value) = prefs.edit { if (value == null) remove(KEY_OWNER) else putString(KEY_OWNER, value) }

    /** Number linked on the server for [ownerUid], cached for display. */
    var linkedPhone: String?
        get() = prefs.getString(KEY_PHONE, null)
        set(value) = prefs.edit { if (value == null) remove(KEY_PHONE) else putString(KEY_PHONE, value) }

    /** Hands SMS capture to another account: nothing captured for the old owner is kept. */
    fun resetOwnership() = synchronized(LOCK) {
        prefs.edit {
            remove(KEY_OWNER)
            remove(KEY_PHONE)
            putBoolean(KEY_ENABLED, false)
            putString(KEY_QUEUE, "[]")
            putString(KEY_REVIEW, "[]")
        }
    }

    var lastImportAt: Long
        get() = prefs.getLong(KEY_LAST_IMPORT, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_IMPORT, value) }

    val trustedHeaders: Set<String>
        get() = prefs.getStringSet(KEY_TRUSTED, emptySet()).orEmpty()

    fun trust(header: String) = synchronized(LOCK) {
        prefs.edit { putStringSet(KEY_TRUSTED, trustedHeaders + header.uppercase()) }
    }

    // ---- Upload queue (survives process death until the worker confirms upload) ----

    fun enqueue(items: List<ParsedSms>) = synchronized(LOCK) {
        if (items.isEmpty()) return
        val merged = (pending() + items).distinctBy { it.hash }
        write(KEY_QUEUE, merged)
    }

    fun pending(): List<ParsedSms> = read(KEY_QUEUE, object : TypeToken<List<ParsedSms>>() {})

    fun removeUploaded(hashes: Set<String>) = synchronized(LOCK) {
        write(KEY_QUEUE, pending().filterNot { it.hash in hashes })
    }

    // ---- Blocked (suspicious) log ----

    fun blocked(): List<BlockedSms> = read(KEY_BLOCKED, object : TypeToken<List<BlockedSms>>() {})

    fun addBlocked(item: BlockedSms) = synchronized(LOCK) {
        val list = (listOf(item) + blocked())
            .distinctBy { it.sender + it.preview }
            .take(MAX_LOG)
        write(KEY_BLOCKED, list)
    }

    fun clearBlocked() = synchronized(LOCK) { write(KEY_BLOCKED, emptyList<BlockedSms>()) }

    // ---- Needs-review list ----

    fun review(): List<ReviewSms> = read(KEY_REVIEW, object : TypeToken<List<ReviewSms>>() {})

    fun addReview(item: ReviewSms) = synchronized(LOCK) {
        write(KEY_REVIEW, (listOf(item) + review()).distinctBy { it.tx.hash }.take(MAX_LOG))
    }

    /** Removes and returns every review item from [header]. */
    fun takeReview(header: String): List<ReviewSms> = synchronized(LOCK) {
        val (taken, kept) = review().partition { it.header.equals(header, ignoreCase = true) }
        write(KEY_REVIEW, kept)
        taken
    }

    private fun <T> read(key: String, type: TypeToken<List<T>>): List<T> =
        prefs.getString(key, null)?.let { runCatching { gson.fromJson<List<T>>(it, type.type) }.getOrNull() }.orEmpty()

    private fun write(key: String, value: List<Any>) = prefs.edit { putString(key, gson.toJson(value)) }

    companion object {
        private val LOCK = Any()
        private const val MAX_LOG = 30
        private const val KEY_ENABLED = "enabled"
        private const val KEY_OWNER = "owner_uid"
        private const val KEY_PHONE = "linked_phone"
        private const val KEY_LAST_IMPORT = "last_import"
        private const val KEY_TRUSTED = "trusted_headers"
        private const val KEY_QUEUE = "upload_queue"
        private const val KEY_BLOCKED = "blocked"
        private const val KEY_REVIEW = "review"

        fun preview(body: String): String = body.replace(Regex("\\s+"), " ").trim().take(140)
    }
}
