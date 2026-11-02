package com.example.fin_ai.insights

import com.example.fin_ai.data.TransactionDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SubscriptionDetectorTest {

    private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    private val now = iso.parse("2026-10-10T10:00:00.000Z")!!.time
    private var n = 0

    private fun tx(date: String, amount: Double, merchant: String, category: String = "Entertainment") = TransactionDto(
        id = "t${n++}", amount = amount, type = "debit", category = category,
        title = "Paid to $merchant", date = "${date}T09:00:00.000Z", merchant = merchant,
    )

    @Test fun monthlyNetflixDetectedWithNextDueDate() {
        val subs = SubscriptionDetector.detect(
            listOf(tx("2026-08-05", 649.0, "NETFLIX.COM"), tx("2026-09-05", 649.0, "Netflix.com"), tx("2026-10-05", 649.0, "netflix.com")),
            now
        )
        assertEquals(1, subs.size)
        val s = subs.single()
        assertEquals(Subscription.Frequency.MONTHLY, s.frequency)
        assertEquals(649.0, s.amount, 0.01)
        assertEquals(3, s.occurrences)
        assertTrue("next due ~4 Nov, was ${s.daysUntilDue(now)} days", s.daysUntilDue(now) in 24..27)
    }

    @Test fun knownServiceNeedsOnlyTwoPayments() {
        val subs = SubscriptionDetector.detect(listOf(tx("2026-09-12", 299.0, "airtel"), tx("2026-10-12", 299.0, "AIRTEL")), now)
        assertEquals(1, subs.size)
    }

    @Test fun irregularShoppingIsNotASubscription() {
        val subs = SubscriptionDetector.detect(
            listOf(
                tx("2026-08-02", 1200.0, "AMAZON", "Shopping"),
                tx("2026-08-19", 340.0, "AMAZON", "Shopping"),
                tx("2026-09-30", 2999.0, "AMAZON", "Shopping"),
            ),
            now
        )
        assertTrue(subs.isEmpty())
    }

    @Test fun cancelledSubscriptionIsDropped() {
        // Last paid in June: two monthly cycles missed by October.
        val subs = SubscriptionDetector.detect(
            listOf(tx("2026-04-01", 119.0, "SPOTIFY"), tx("2026-05-01", 119.0, "SPOTIFY"), tx("2026-06-01", 119.0, "SPOTIFY")),
            now
        )
        assertTrue(subs.isEmpty())
    }

    @Test fun weeklyAndMonthlyCostNormalised() {
        val subs = SubscriptionDetector.detect(
            listOf(tx("2026-09-19", 100.0, "MILKBASKET", "Food"), tx("2026-09-26", 100.0, "MILKBASKET", "Food"), tx("2026-10-03", 100.0, "MILKBASKET", "Food")),
            now
        )
        assertEquals(Subscription.Frequency.WEEKLY, subs.single().frequency)
        assertEquals(428.57, subs.single().monthlyCost, 0.1)
    }
}
