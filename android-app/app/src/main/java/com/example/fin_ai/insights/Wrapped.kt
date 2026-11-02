package com.example.fin_ai.insights

import com.example.fin_ai.data.TransactionDto
import com.example.fin_ai.ui.util.Fmt
import com.example.fin_ai.ui.util.income
import com.example.fin_ai.ui.util.isDebit
import com.example.fin_ai.ui.util.monthAgo
import com.example.fin_ai.ui.util.payee
import com.example.fin_ai.ui.util.spend

/** One story card: a big headline number, a line of context, and a gradient theme. */
data class WrappedCard(val kicker: String, val headline: String, val detail: String, val emoji: String, val theme: Int)

object Wrapped {

    fun cards(all: List<TransactionDto>, monthsAgo: Int): List<WrappedCard> {
        val month = all.monthAgo(monthsAgo)
        val prev = all.monthAgo(monthsAgo + 1)
        val name = Fmt.monthName(monthsAgo)
        if (month.isEmpty()) {
            return listOf(WrappedCard("$name Wrapped", "Nothing yet", "Add or capture some transactions and come back!", "🌱", 0))
        }
        val debits = month.filter { it.isDebit }
        val spent = month.spend()
        val income = month.income()
        val out = mutableListOf<WrappedCard>()

        out += WrappedCard("Your money story", "$name\nWrapped", "${month.size} transactions tracked. Swipe to relive your month →", "✨", 0)

        val prevSpent = prev.spend()
        out += WrappedCard(
            "You spent", Fmt.rupees(spent),
            if (prevSpent > 0) {
                val pct = ((spent - prevSpent) / prevSpent * 100).toInt()
                if (pct <= 0) "That's ${-pct}% less than last month. Nicely done! 👏" else "That's $pct% more than last month."
            } else "across ${debits.size} payments.",
            "💸", 1
        )

        val byCat = debits.groupBy { it.category }.mapValues { (_, l) -> l.sumOf { it.amount } }
        byCat.maxByOrNull { it.value }?.let { (cat, sum) ->
            out += WrappedCard(
                "Your top category", cat,
                "${(sum * 100 / spent).toInt()}% of everything you spent — ${Fmt.rupees(sum)}.",
                categoryEmoji(cat), 2
            )
        }

        debits.groupBy { it.payee }.maxByOrNull { it.value.size }?.takeIf { it.value.size >= 2 }?.let { (p, l) ->
            out += WrappedCard("Your favourite place", p, "You paid them ${l.size} times, ${Fmt.rupees(l.sumOf { it.amount })} in total.", "📍", 3)
        }

        debits.maxByOrNull { it.amount }?.let { big ->
            out += WrappedCard("Biggest splurge", Fmt.rupees(big.amount), "${big.title} on ${Fmt.dayMonth(big.date)}.", "🛍️", 4)
        }

        val streak = Achievements.bestNoSpendStreak(month)
        if (income > 0) {
            val rate = ((income - spent) / income * 100).toInt()
            out += WrappedCard(
                "You saved",
                if (rate >= 0) "$rate%" else Fmt.rupees(spent - income),
                if (rate >= 0) "of what you earned — ${Fmt.rupees(income - spent)} kept." + (if (streak >= 3) " Plus a $streak-day no-spend streak!" else "")
                else "more than you earned this month. Next month's a fresh start!",
                if (rate >= 20) "🐷" else if (rate >= 0) "💰" else "📉", 5
            )
        }

        val (persona, emoji, why) = persona(byCat, spent, income)
        out += WrappedCard("Your money personality", persona, why, emoji, 6)
        return out
    }

    private fun persona(byCat: Map<String, Double>, spent: Double, income: Double): Triple<String, String, String> {
        val rate = if (income > 0) (income - spent) / income else 0.0
        if (rate >= 0.3) return Triple("The Super Saver", "🐷", "You kept over 30% of your income. Future you says thanks!")
        val (top, sum) = byCat.maxByOrNull { it.value } ?: return Triple("The Balanced One", "⚖️", "Nothing dominated your spending.")
        val share = sum / spent
        if (share < 0.35) return Triple("The Balanced One", "⚖️", "No single category took over your month.")
        return when (top) {
            "Food" -> Triple("The Foodie", "🍕", "Good food is your love language — ${(share * 100).toInt()}% went on eating.")
            "Shopping" -> Triple("The Shopaholic", "🛍️", "Cart full, heart full. ${(share * 100).toInt()}% went on shopping.")
            "Travel" -> Triple("The Globetrotter", "✈️", "Always on the move — travel led your spending.")
            "Entertainment" -> Triple("The Entertainer", "🎬", "Movies, shows and fun took the spotlight.")
            "Education" -> Triple("The Scholar", "🎓", "Investing in yourself — education topped the list.")
            "Health" -> Triple("The Wellness Warrior", "💪", "Health first, always.")
            "Rent", "Utilities", "Taxes" -> Triple("The Responsible Adult", "🏠", "Bills and essentials came first this month.")
            else -> Triple("The Explorer", "🧭", "$top led your spending this month.")
        }
    }

    private fun categoryEmoji(c: String) = when (c) {
        "Food" -> "🍔"; "Shopping" -> "🛒"; "Travel" -> "✈️"; "Utilities" -> "💡"; "Health" -> "🏥"
        "Education" -> "📚"; "Entertainment" -> "🎬"; "Rent" -> "🏠"; "Taxes" -> "🧾"; "Jewellery" -> "💍"
        else -> "📦"
    }
}
