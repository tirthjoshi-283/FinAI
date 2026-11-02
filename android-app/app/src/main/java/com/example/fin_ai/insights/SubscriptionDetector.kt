package com.example.fin_ai.insights

import com.example.fin_ai.data.TransactionDto
import com.example.fin_ai.ui.util.Fmt
import com.example.fin_ai.ui.util.isDebit
import com.example.fin_ai.ui.util.payee
import kotlin.math.abs
import kotlin.math.roundToInt

/** A payment that repeats on a schedule (Netflix, rent, EMI, mobile recharge, ...). */
data class Subscription(
    val name: String,
    val category: String,
    val amount: Double,          // typical (median) amount
    val frequency: Frequency,
    val occurrences: Int,
    val lastPaid: Long,
    val nextDue: Long,
    val lastTransactionId: String,
) {
    /** Cost normalised to one month, for the "you pay ₹X/month in subscriptions" total. */
    val monthlyCost: Double get() = amount * 30.0 / frequency.days

    fun daysUntilDue(now: Long = System.currentTimeMillis()): Int = ((nextDue - Fmt.startOfDay(now)) / Fmt.DAY).toInt()

    enum class Frequency(val label: String, val days: Int, val range: IntRange) {
        WEEKLY("Weekly", 7, 6..8),
        MONTHLY("Monthly", 30, 26..35),
        QUARTERLY("Every 3 months", 91, 84..98),
        YEARLY("Yearly", 365, 350..380),
    }
}

object SubscriptionDetector {

    /** Payees that are recurring by nature: accept them with just two payments. */
    private val KNOWN = Regex(
        "netflix|spotify|prime|hotstar|disney|youtube|jiocinema|zee5|sonyliv|apple|google|icloud|" +
            "airtel|jio|vodafone|\\bvi\\b|bsnl|broadband|act fibernet|tata ?play|dth|electricity|gas|" +
            "rent|emi|loan|insurance|lic|sip|mutual|gym|cult",
        RegexOption.IGNORE_CASE
    )

    fun detect(transactions: List<TransactionDto>, now: Long = System.currentTimeMillis()): List<Subscription> {
        return transactions.filter { it.isDebit }
            .groupBy { normalise(it.payee) }
            .filterKeys { it.isNotBlank() }
            .mapNotNull { (_, list) -> analyse(list.sortedBy { Fmt.millis(it.date) }, now) }
            .sortedBy { it.nextDue }
    }

    private fun analyse(list: List<TransactionDto>, now: Long): Subscription? {
        if (list.size < 2) return null
        val name = list.last().payee
        val known = KNOWN.containsMatchIn(name)
        if (list.size < 3 && !known) return null

        // Amounts must be steady: every payment within 15% of the median (subscriptions don't swing).
        val median = list.map { it.amount }.sorted().let { it[it.size / 2] }
        if (list.any { abs(it.amount - median) > median * 0.15 }) return null

        val times = list.map { Fmt.millis(it.date) }
        val gaps = times.zipWithNext { a, b -> ((b - a) / Fmt.DAY).toInt() }.filter { it > 0 }
        if (gaps.isEmpty()) return null
        val gap = gaps.sorted()[gaps.size / 2]
        val freq = Subscription.Frequency.entries.firstOrNull { gap in it.range } ?: return null
        // Most gaps must agree with the cadence, so one random repeat purchase doesn't qualify.
        if (gaps.count { it in freq.range } * 3 < gaps.size * 2) return null

        val last = times.last()
        // Stale: missed two cycles in a row, so it was probably cancelled.
        if (now - last > freq.days * 2L * Fmt.DAY) return null

        return Subscription(
            name = name,
            category = list.last().category,
            amount = (median * 100).roundToInt() / 100.0,
            frequency = freq,
            occurrences = list.size,
            lastPaid = last,
            nextDue = Fmt.startOfDay(last + gap.toLong() * Fmt.DAY),
            lastTransactionId = list.last().id,
        )
    }

    /** "Paid to NETFLIX.COM" / "netflix@icici" -> "netflix" so variants group together. */
    internal fun normalise(payee: String): String = payee.lowercase()
        .removePrefix("paid to ").removePrefix("received from ")
        .substringBefore('@')
        .replace(Regex("\\.(com|in|co|net)\\b"), "")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
