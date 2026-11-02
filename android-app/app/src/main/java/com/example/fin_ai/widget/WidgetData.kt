package com.example.fin_ai.widget

import android.content.Context
import androidx.core.content.edit
import androidx.glance.appwidget.updateAll
import com.example.fin_ai.data.BudgetDto
import com.example.fin_ai.data.TransactionDto
import com.example.fin_ai.ui.util.Fmt
import com.example.fin_ai.ui.util.isDebit
import com.example.fin_ai.ui.util.millis
import com.example.fin_ai.ui.util.thisMonth

/** What the home-screen widget shows, saved on the device so the widget never needs the network. */
data class WidgetSnapshot(
    val todaySpent: Double,
    val monthSpent: Double,
    /** Sum of all monthly budgets (0 when none are set). */
    val budgetTotal: Double,
    /** This month's spend in budgeted categories only. */
    val budgetSpent: Double,
    val lastTitle: String?,
    val lastAmount: String?,
    val lastIsCredit: Boolean,
) {
    val budgetLeft: Double get() = budgetTotal - budgetSpent
}

object WidgetData {

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences("finai_widget", Context.MODE_PRIVATE)

    /** Recompute from the latest data and redraw any placed widgets. */
    suspend fun update(context: Context, txs: List<TransactionDto>, budgets: List<BudgetDto>) {
        val today = Fmt.startOfDay(System.currentTimeMillis())
        val monthDebits = txs.thisMonth().filter { it.isDebit }
        val budgeted = budgets.map { it.category }.toSet()
        val last = txs.maxByOrNull { it.millis }
        prefs(context).edit {
            putFloat(TODAY, txs.filter { it.isDebit && it.millis >= today }.sumOf { it.amount }.toFloat())
            putFloat(MONTH, monthDebits.sumOf { it.amount }.toFloat())
            putFloat(BUDGET_TOTAL, budgets.sumOf { it.limit }.toFloat())
            putFloat(BUDGET_SPENT, monthDebits.filter { it.category in budgeted }.sumOf { it.amount }.toFloat())
            putString(LAST_TITLE, last?.title)
            putString(LAST_AMOUNT, last?.let(Fmt::signed))
            putBoolean(LAST_CREDIT, last?.isCredit == true)
            putLong(UPDATED, System.currentTimeMillis())
        }
        runCatching { FinWidget().updateAll(context) }
    }

    fun read(context: Context): WidgetSnapshot? {
        val p = prefs(context)
        if (!p.contains(UPDATED)) return null
        return WidgetSnapshot(
            todaySpent = p.getFloat(TODAY, 0f).toDouble(),
            monthSpent = p.getFloat(MONTH, 0f).toDouble(),
            budgetTotal = p.getFloat(BUDGET_TOTAL, 0f).toDouble(),
            budgetSpent = p.getFloat(BUDGET_SPENT, 0f).toDouble(),
            lastTitle = p.getString(LAST_TITLE, null),
            lastAmount = p.getString(LAST_AMOUNT, null),
            lastIsCredit = p.getBoolean(LAST_CREDIT, false),
        )
    }

    /** On sign-out: the widget must not keep showing the previous account's money. */
    suspend fun clear(context: Context) {
        prefs(context).edit { clear() }
        runCatching { FinWidget().updateAll(context) }
    }

    private const val TODAY = "today"
    private const val MONTH = "month"
    private const val BUDGET_TOTAL = "budget_total"
    private const val BUDGET_SPENT = "budget_spent"
    private const val LAST_TITLE = "last_title"
    private const val LAST_AMOUNT = "last_amount"
    private const val LAST_CREDIT = "last_credit"
    private const val UPDATED = "updated"
}
