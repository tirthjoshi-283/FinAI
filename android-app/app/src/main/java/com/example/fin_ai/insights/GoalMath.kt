package com.example.fin_ai.insights

import com.example.fin_ai.data.GoalDto
import com.example.fin_ai.data.TransactionDto
import com.example.fin_ai.ui.util.Fmt
import com.example.fin_ai.ui.util.income
import com.example.fin_ai.ui.util.monthAgo
import com.example.fin_ai.ui.util.spend
import kotlin.math.ceil

/** Forecast for one goal. Nulls mean "not enough information". */
data class GoalProjection(
    val remaining: Double,
    /** Months to finish at the user's recent savings pace. */
    val monthsAtPace: Int?,
    /** ₹ per month needed to finish by the deadline. */
    val neededPerMonth: Double?,
    val daysLeft: Int?,
    /** Pace ≥ needed-per-month (only when there is a deadline and a pace). */
    val onTrack: Boolean?,
)

object GoalMath {

    val MILESTONES = listOf(25, 50, 75, 100)

    private val EMOJI_RULES = listOf(
        Regex("bike|scooter|motorcycle|activa|royal enfield", RegexOption.IGNORE_CASE) to "🏍️",
        Regex("car|suv|vehicle", RegexOption.IGNORE_CASE) to "🚗",
        Regex("house|home|flat|apartment|mansion|villa|plot", RegexOption.IGNORE_CASE) to "🏠",
        Regex("laptop|macbook|computer|pc", RegexOption.IGNORE_CASE) to "💻",
        Regex("phone|iphone|mobile|pixel|samsung", RegexOption.IGNORE_CASE) to "📱",
        Regex("trip|travel|vacation|holiday|goa|europe|tour|flight", RegexOption.IGNORE_CASE) to "✈️",
        Regex("wedding|marriage|ring", RegexOption.IGNORE_CASE) to "💍",
        Regex("emergency|rainy|safety", RegexOption.IGNORE_CASE) to "🛟",
        Regex("study|college|education|course|mba|fees|exam", RegexOption.IGNORE_CASE) to "🎓",
        Regex("gift|birthday|anniversary", RegexOption.IGNORE_CASE) to "🎁",
        Regex("camera|lens", RegexOption.IGNORE_CASE) to "📷",
        Regex("gaming|ps5|xbox|console", RegexOption.IGNORE_CASE) to "🎮",
        Regex("invest|stock|sip|mutual|retire", RegexOption.IGNORE_CASE) to "📈",
        Regex("watch", RegexOption.IGNORE_CASE) to "⌚",
        Regex("gold|jewel", RegexOption.IGNORE_CASE) to "💎",
    )

    fun emojiFor(goal: GoalDto): String = goal.emoji?.takeIf { it.isNotBlank() } ?: emojiFor(goal.title)

    fun emojiFor(title: String): String = EMOJI_RULES.firstOrNull { (re, _) -> re.containsMatchIn(title) }?.second ?: "🎯"

    /** Average (income − spending) per month over the last 3 complete months that had activity. */
    fun monthlySavingsPace(txs: List<TransactionDto>): Double {
        val months = (1..3).map { txs.monthAgo(it) }.filter { it.isNotEmpty() }
        if (months.isEmpty()) return 0.0
        return kotlin.math.round(months.sumOf { it.income() - it.spend() } / months.size).coerceAtLeast(0.0)
    }

    fun project(goal: GoalDto, pace: Double, now: Long = System.currentTimeMillis()): GoalProjection {
        val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
        val monthsAtPace = if (remaining <= 0) 0 else if (pace > 0) ceil(remaining / pace).toInt() else null
        val deadlineMs = goal.deadline?.let { Fmt.millis(it) }?.takeIf { it > 0 }
        val daysLeft = deadlineMs?.let { ((it - Fmt.startOfDay(now)) / Fmt.DAY).toInt() }
        val neededPerMonth = if (daysLeft != null && remaining > 0) remaining / (daysLeft.coerceAtLeast(1) / 30.0).coerceAtLeast(1.0) else null
        val onTrack = if (neededPerMonth != null && pace > 0) pace >= neededPerMonth else if (remaining <= 0) true else null
        return GoalProjection(remaining, monthsAtPace, neededPerMonth, daysLeft, onTrack)
    }

    /** Highest milestone newly reached when moving from [before] to [after] saved, or null. */
    fun milestoneCrossed(target: Double, before: Double, after: Double): Int? {
        if (target <= 0 || after <= before) return null
        val b = before / target * 100
        val a = after / target * 100
        return MILESTONES.lastOrNull { m -> b < m && a >= m }
    }

    /** "1 yr 4 mo", "7 mo", "under a month". */
    fun monthsLabel(months: Int): String = when {
        months <= 0 -> "under a month"
        months < 12 -> "$months mo"
        months % 12 == 0 -> "${months / 12} yr"
        else -> "${months / 12} yr ${months % 12} mo"
    }
}
