package com.example.fin_ai.insights

import com.example.fin_ai.data.BudgetDto
import com.example.fin_ai.data.TransactionDto
import com.example.fin_ai.ui.util.Fmt
import com.example.fin_ai.ui.util.income
import com.example.fin_ai.ui.util.inRange
import com.example.fin_ai.ui.util.isDebit
import com.example.fin_ai.ui.util.millis
import com.example.fin_ai.ui.util.monthAgo
import com.example.fin_ai.ui.util.spend
import java.util.Calendar

data class Badge(val id: String, val emoji: String, val title: String, val howTo: String, val unlocked: Boolean)

data class Challenge(val title: String, val detail: String, val progress: Float, val good: Boolean)

data class Level(val number: Int, val name: String, val xp: Int, val xpForNext: Int, val xpIntoLevel: Int)

/** Gamification computed purely from the user's own data — nothing extra is stored. */
object Achievements {

    // ---- Streaks ----

    /**
     * A quiet stretch longer than this means the user wasn't tracking (e.g. one old test
     * entry years ago), not that they were being frugal — so it doesn't count as a streak.
     */
    const val MAX_STREAK_DAYS = 21

    private fun spendDays(txs: List<TransactionDto>): Set<Long> =
        txs.filter { it.isDebit }.map { Fmt.startOfDay(it.millis) }.toSet()

    /** Consecutive days up to today with no money spent (0 if something was spent today). */
    fun currentNoSpendStreak(txs: List<TransactionDto>, now: Long = System.currentTimeMillis()): Int {
        if (txs.isEmpty()) return 0
        val spent = spendDays(txs)
        val first = Fmt.startOfDay(txs.minOf { it.millis })
        var day = Fmt.startOfDay(now)
        var n = 0
        while (day >= first && day !in spent) {
            n++
            day = previousDay(day)
        }
        return if (n > MAX_STREAK_DAYS) 0 else n
    }

    fun bestNoSpendStreak(txs: List<TransactionDto>, now: Long = System.currentTimeMillis()): Int {
        if (txs.isEmpty()) return 0
        val spent = spendDays(txs)
        var day = Fmt.startOfDay(txs.minOf { it.millis })
        val end = Fmt.startOfDay(now)
        var best = 0
        var run = 0
        while (day <= end) {
            if (day in spent) {
                if (run <= MAX_STREAK_DAYS) best = maxOf(best, run)
                run = 0
            } else run++
            day = nextDay(day)
        }
        if (run <= MAX_STREAK_DAYS) best = maxOf(best, run)
        return best
    }

    // ---- Weekly challenges ----

    fun challenges(txs: List<TransactionDto>, now: Long = System.currentTimeMillis()): List<Challenge> {
        val weekStart = startOfWeek(now)
        val thisWeek = txs.inRange(weekStart)
        val lastWeek = txs.inRange(weekStart - 7 * Fmt.DAY, weekStart)
        val out = mutableListOf<Challenge>()

        val lastSpend = lastWeek.spend()
        if (lastSpend > 0) {
            val spent = thisWeek.spend()
            out += Challenge(
                "Beat last week",
                "Spend less than ${Fmt.rupees(lastSpend)} this week · ${Fmt.rupees(spent)} so far",
                (spent / lastSpend).toFloat(),
                good = spent < lastSpend,
            )
        }

        // Top category over the last 4 weeks; target 80% of its weekly average.
        val history = txs.inRange(weekStart - 28 * Fmt.DAY, weekStart).filter { it.isDebit }
        history.groupBy { it.category }.maxByOrNull { (_, l) -> l.sumOf { it.amount } }?.let { (cat, l) ->
            val target = kotlin.math.ceil(l.sumOf { it.amount } / 4 * 0.8 / 50) * 50
            if (target > 0) {
                val spent = thisWeek.filter { it.isDebit && it.category == cat }.sumOf { it.amount }
                out += Challenge(
                    "Cut back on $cat",
                    "Keep $cat under ${Fmt.rupees(target)} this week · ${Fmt.rupees(spent)} so far",
                    (spent / target).toFloat(),
                    good = spent <= target,
                )
            }
        }

        val spentDays = spendDays(thisWeek)
        var day = weekStart
        var noSpend = 0
        while (day <= Fmt.startOfDay(now)) {
            if (day !in spentDays) noSpend++
            day = nextDay(day)
        }
        out += Challenge(
            "3 no-spend days",
            "Go a whole day without spending, 3 times this week · $noSpend/3 done",
            (noSpend / 3f),
            good = noSpend >= 3,
        )
        return out
    }

    // ---- Badges ----

    fun badges(
        txs: List<TransactionDto>,
        budgets: List<BudgetDto>,
        goalsCompleted: Int,
        splits: Int,
        now: Long = System.currentTimeMillis(),
    ): List<Badge> {
        val best = bestNoSpendStreak(txs, now)
        val savedMonth = (1..6).any { m ->
            val l = txs.monthAgo(m)
            val inc = l.income()
            inc > 0 && (inc - l.spend()) / inc >= 0.2
        }
        val lastMonth = txs.monthAgo(1).filter { it.isDebit }
        val budgetBoss = budgets.isNotEmpty() && lastMonth.isNotEmpty() &&
            budgets.all { b -> lastMonth.filter { it.category == b.category }.sumOf { it.amount } <= b.limit }
        return listOf(
            Badge("first", "🌱", "First Step", "Record your first transaction", txs.isNotEmpty()),
            Badge("scan", "🧾", "Bill Buster", "Scan a bill or receipt", txs.any { it.source == "bill_scan" }),
            Badge("sms", "📲", "SMS Autopilot", "Capture a transaction from a bank SMS", txs.any { it.source == "sms" }),
            Badge("streak3", "🔥", "Streak Starter", "3 no-spend days in a row", best >= 3),
            Badge("streak7", "⚡", "Streak Master", "7 no-spend days in a row", best >= 7),
            Badge("saver", "💰", "Super Saver", "Save 20% of your income in a month", savedMonth),
            Badge("planner", "🗂️", "Planner", "Set 3 category budgets", budgets.size >= 3),
            Badge("boss", "👑", "Budget Boss", "Stay within every budget for a month", budgetBoss),
            Badge("goal", "🎯", "Goal Getter", "Complete a savings goal", goalsCompleted > 0),
            Badge("split", "🤝", "Fair Share", "Split a bill with friends", splits > 0),
            Badge("notes", "📝", "Storyteller", "Add notes to 5 transactions", txs.count { !it.note.isNullOrBlank() } >= 5),
            Badge("century", "💯", "Centurion", "Track 100 transactions", txs.size >= 100),
        )
    }

    // ---- XP & levels ----

    private val LEVELS = listOf("Rookie", "Saver", "Planner", "Strategist", "Wealth Wizard", "Finance Legend")
    private const val XP_PER_LEVEL = 300

    fun level(txs: List<TransactionDto>, badges: List<Badge>, now: Long = System.currentTimeMillis()): Level {
        val xp = badges.count { it.unlocked } * 100 + bestNoSpendStreak(txs, now) * 15 + minOf(txs.size, 300) * 2
        val n = xp / XP_PER_LEVEL
        return Level(n + 1, LEVELS[minOf(n, LEVELS.lastIndex)], xp, XP_PER_LEVEL, xp % XP_PER_LEVEL)
    }

    // ---- date helpers ----

    private fun nextDay(day: Long) = Calendar.getInstance().apply { timeInMillis = day; add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
    private fun previousDay(day: Long) = Calendar.getInstance().apply { timeInMillis = day; add(Calendar.DAY_OF_MONTH, -1) }.timeInMillis

    /** Monday 00:00 of the current week. */
    fun startOfWeek(now: Long): Long = Calendar.getInstance().apply {
        timeInMillis = Fmt.startOfDay(now)
        val dow = get(Calendar.DAY_OF_WEEK) // Sunday = 1
        add(Calendar.DAY_OF_MONTH, -((dow + 5) % 7))
    }.timeInMillis
}
