package com.example.fin_ai.ui.util

import com.example.fin_ai.data.TransactionDto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Shared money / date helpers for the ledger and every insight screen. */
object Fmt {

    private val isoParser = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    }

    /** API dates are ISO-8601 UTC ("2026-10-07T14:30:00.000Z"). */
    fun millis(iso: String): Long =
        runCatching { isoParser.get()!!.parse(iso.take(19))!!.time }.getOrDefault(0L)

    /** ₹1,23,456 in the Indian grouping style; decimals only when they matter. */
    fun rupees(amount: Double, decimals: Boolean = false): String {
        val abs = kotlin.math.abs(amount)
        val whole = abs.toLong()
        val s = whole.toString()
        val grouped = if (s.length <= 3) s else {
            val head = s.dropLast(3)
            head.reversed().chunked(2).joinToString(",").reversed() + "," + s.takeLast(3)
        }
        val frac = if (decimals || abs != whole.toDouble()) ".%02d".format(((abs - whole) * 100).toLong()) else ""
        return (if (amount < 0) "-₹" else "₹") + grouped + frac
    }

    fun signed(tx: TransactionDto): String = (if (tx.isCredit) "+" else "-") + rupees(tx.amount)

    fun dateTime(iso: String): String = format(millis(iso), "dd MMM yyyy, hh:mm a")

    fun dayMonth(iso: String): String = format(millis(iso), "dd MMM")

    fun format(millis: Long, pattern: String): String = SimpleDateFormat(pattern, Locale.getDefault()).format(millis)

    /** "Today", "Yesterday" or "Tue, 07 Oct" — used as ledger section headers. */
    fun dayHeader(iso: String): String {
        val t = millis(iso)
        val today = startOfDay(System.currentTimeMillis())
        return when {
            t >= today -> "Today"
            t >= today - DAY -> "Yesterday"
            else -> format(t, "EEE, dd MMM yyyy")
        }
    }

    const val DAY = 24L * 60 * 60 * 1000

    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Start of the month [monthsAgo] months before the current one (0 = this month). */
    fun startOfMonth(monthsAgo: Int = 0): Long = Calendar.getInstance().apply {
        add(Calendar.MONTH, -monthsAgo)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun monthName(monthsAgo: Int = 0, pattern: String = "MMMM"): String = format(startOfMonth(monthsAgo), pattern)
}

/** Convenience accessors used across screens. */
val TransactionDto.millis: Long get() = Fmt.millis(date)
val TransactionDto.isDebit: Boolean get() = !isCredit

fun List<TransactionDto>.inRange(from: Long, until: Long = Long.MAX_VALUE) = filter { it.millis in from until until }
fun List<TransactionDto>.thisMonth() = inRange(Fmt.startOfMonth(0))
fun List<TransactionDto>.monthAgo(n: Int) = inRange(Fmt.startOfMonth(n), Fmt.startOfMonth(n - 1))
fun List<TransactionDto>.spend() = filter { it.isDebit }.sumOf { it.amount }
fun List<TransactionDto>.income() = filter { it.isCredit }.sumOf { it.amount }

/** A stable display name for "who": merchant if known, otherwise the title. */
val TransactionDto.payee: String get() = merchant?.takeIf { it.isNotBlank() } ?: title
