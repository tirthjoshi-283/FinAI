package com.example.fin_ai.insights

import com.example.fin_ai.data.BudgetDto
import com.example.fin_ai.data.TransactionDto
import com.example.fin_ai.ui.util.Fmt
import com.example.fin_ai.ui.util.isDebit
import com.example.fin_ai.ui.util.monthAgo
import com.example.fin_ai.ui.util.thisMonth
import java.util.Calendar

/** How one category is doing against its monthly budget. */
data class BudgetStatus(
    val category: String,
    val limit: Double,
    val spent: Double,
) {
    val fraction: Double get() = if (limit > 0) spent / limit else 0.0
    val remaining: Double get() = limit - spent
    val level: Level get() = when {
        fraction >= 1.0 -> Level.OVER
        fraction >= 0.8 -> Level.WARNING
        else -> Level.OK
    }

    enum class Level { OK, WARNING, OVER }
}

object BudgetMath {

    fun statuses(budgets: List<BudgetDto>, transactions: List<TransactionDto>): List<BudgetStatus> {
        val month = transactions.thisMonth().filter { it.isDebit }
        return budgets.map { b -> BudgetStatus(b.category, b.limit, month.filter { it.category == b.category }.sumOf { it.amount }) }
            .sortedByDescending { it.fraction }
    }

    fun daysLeftInMonth(): Int {
        val c = Calendar.getInstance()
        return c.getActualMaximum(Calendar.DAY_OF_MONTH) - c.get(Calendar.DAY_OF_MONTH) + 1
    }

    /** Average monthly spend per category over the last 3 complete months, rounded up to ₹100. */
    fun suggestions(transactions: List<TransactionDto>): Map<String, Double> {
        val history = (1..3).flatMap { transactions.monthAgo(it) }.filter { it.isDebit }
        return history.groupBy { it.category }
            .mapValues { (_, l) -> kotlin.math.ceil(l.sumOf { it.amount } / 3 / 100) * 100 }
            .filterValues { it > 0 }
    }

    /** "yyyyMM" — budget alerts fire at most once per category, threshold and month. */
    fun monthKey(): String = Fmt.format(System.currentTimeMillis(), "yyyyMM")
}
