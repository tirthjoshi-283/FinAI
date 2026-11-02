package com.example.fin_ai.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StripLatexTest {

    @Test fun realAnswerFromTheChatBecomesReadable() {
        val raw = """
            \[
            \text{Months required} = \frac{₹89 997 778}{₹1 435.36} \approx 62 735 \text{ months}
            \]
        """.trimIndent()
        val out = stripLatex(raw)
        assertFalse("no backslashes left: $out", '\\' in out)
        assertFalse("no braces left: $out", '{' in out || '}' in out)
        assertEquals("Months required = ₹89 997 778 ÷ ₹1 435.36 ≈ 62 735 months", out.trim())
    }

    @Test fun nestedFractionsAndSymbols() {
        assertEquals("a ÷ b × 2 ≤ 5", stripLatex("""\frac{a}{b} \times 2 \le 5""").trim())
    }

    @Test fun plainTextIsUntouched() {
        val text = "You saved **₹5,000** this month — 20% of income."
        assertEquals(text, stripLatex(text))
    }
}
