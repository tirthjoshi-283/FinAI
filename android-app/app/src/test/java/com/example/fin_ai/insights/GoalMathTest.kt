package com.example.fin_ai.insights

import com.example.fin_ai.data.GoalDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalMathTest {

    private fun goal(target: Double, saved: Double, deadline: String? = null, title: String = "x") =
        GoalDto(id = "g", title = title, targetAmount = target, currentAmount = saved, deadline = deadline)

    @Test fun emojiPickedFromTitle() {
        assertEquals("🏍️", GoalMath.emojiFor("New bike"))
        assertEquals("🏠", GoalMath.emojiFor("A Big mansion"))
        assertEquals("✈️", GoalMath.emojiFor("Goa trip"))
        assertEquals("🎯", GoalMath.emojiFor("Something else"))
        assertEquals("🐶", GoalMath.emojiFor(goal(1.0, 0.0).copy(emoji = "🐶")))
    }

    @Test fun projectionAtPace() {
        val p = GoalMath.project(goal(100_000.0, 40_000.0), pace = 10_000.0)
        assertEquals(60_000.0, p.remaining, 0.01)
        assertEquals(6, p.monthsAtPace)
        assertNull(p.neededPerMonth) // no deadline
    }

    @Test fun deadlineNeedsMonthlyAmountAndTrackStatus() {
        val now = com.example.fin_ai.ui.util.Fmt.millis("2026-10-10T00:00:00")
        val p = GoalMath.project(goal(60_000.0, 0.0, deadline = "2027-04-08T00:00:00.000Z"), pace = 5_000.0, now = now)
        assertTrue("about 6 months left, was ${p.daysLeft}", p.daysLeft in 178..181)
        assertEquals(10_000.0, p.neededPerMonth!!, 300.0)
        assertEquals(false, p.onTrack)
    }

    @Test fun completedGoalIsOnTrack() {
        val p = GoalMath.project(goal(1000.0, 1200.0), pace = 0.0)
        assertEquals(0, p.monthsAtPace)
        assertEquals(true, p.onTrack)
    }

    @Test fun milestones() {
        assertEquals(50, GoalMath.milestoneCrossed(1000.0, 400.0, 520.0))
        assertEquals(100, GoalMath.milestoneCrossed(1000.0, 400.0, 1000.0))
        assertNull(GoalMath.milestoneCrossed(1000.0, 520.0, 600.0))
        assertNull(GoalMath.milestoneCrossed(1000.0, 600.0, 500.0)) // withdrawal
    }

    @Test fun monthLabels() {
        assertEquals("7 mo", GoalMath.monthsLabel(7))
        assertEquals("1 yr 4 mo", GoalMath.monthsLabel(16))
        assertEquals("2 yr", GoalMath.monthsLabel(24))
    }
}
