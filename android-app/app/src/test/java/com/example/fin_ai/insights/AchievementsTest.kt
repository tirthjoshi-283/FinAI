package com.example.fin_ai.insights

import com.example.fin_ai.data.TransactionDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AchievementsTest {

    private var n = 0
    private val utc = TimeZone.getTimeZone("UTC")

    /** Noon local time on [day] of October 2026, as the API's ISO string. */
    private fun at(day: Int): Long = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, day, 12, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis

    private fun tx(day: Int, amount: Double, type: String = "debit", source: String? = null) = TransactionDto(
        id = "t${n++}", amount = amount, type = type, category = "Food", title = "x",
        date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply { timeZone = utc }.format(at(day)),
        source = source,
    )

    @Test fun currentStreakCountsDaysSinceLastSpend() {
        val txs = listOf(tx(1, 100.0), tx(5, 50.0), tx(6, 20.0))
        assertEquals(4, Achievements.currentNoSpendStreak(txs, now = at(10))) // 7,8,9,10
        assertEquals(0, Achievements.currentNoSpendStreak(txs, now = at(6)))  // spent today
    }

    @Test fun incomeDoesNotBreakAStreak() {
        val txs = listOf(tx(1, 100.0), tx(3, 5000.0, type = "credit"))
        assertEquals(4, Achievements.currentNoSpendStreak(txs, now = at(5)))   // 2,3,4,5
    }

    @Test fun bestStreakFindsLongestGap() {
        val txs = listOf(tx(1, 10.0), tx(9, 10.0), tx(11, 10.0))
        assertEquals(7, Achievements.bestNoSpendStreak(txs, now = at(12)))     // 2..8
    }

    @Test fun badgesUnlockFromRealData() {
        val txs = listOf(tx(1, 10.0, source = "bill_scan"), tx(2, 10.0, source = "sms"))
        val unlocked = Achievements.badges(txs, emptyList(), goalsCompleted = 0, splits = 1, now = at(12))
            .filter { it.unlocked }.map { it.id }.toSet()
        assertTrue(unlocked.containsAll(setOf("first", "scan", "sms", "split", "streak3", "streak7")))
        assertTrue("goal" !in unlocked && "century" !in unlocked)
    }

    @Test fun longInactiveGapIsNotAStreak() {
        // One stray entry years ago, then real activity: the huge gap must not count.
        val old = TransactionDto(id = "old", amount = 10.0, type = "debit", category = "Food", title = "x", date = "2024-01-05T12:00:00.000Z")
        val txs = listOf(old, tx(1, 10.0), tx(5, 10.0))
        assertEquals(3, Achievements.bestNoSpendStreak(txs, now = at(6)))   // 2,3,4
        assertEquals(1, Achievements.currentNoSpendStreak(txs, now = at(6)))
        assertEquals(0, Achievements.currentNoSpendStreak(listOf(old), now = at(6)))
    }

    @Test fun weekStartsOnMonday() {
        // 10 Oct 2026 is a Saturday -> week starts Monday 5 Oct.
        val c = Calendar.getInstance().apply { timeInMillis = Achievements.startOfWeek(at(10)) }
        assertEquals(Calendar.MONDAY, c.get(Calendar.DAY_OF_WEEK))
        assertEquals(5, c.get(Calendar.DAY_OF_MONTH))
    }
}
