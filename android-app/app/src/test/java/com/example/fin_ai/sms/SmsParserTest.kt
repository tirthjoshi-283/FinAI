package com.example.fin_ai.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {

    private val now = 1_791_000_000_000L

    private fun genuine(sender: String, body: String): ParsedSms {
        val v = SmsParser.analyse(sender, body, now)
        assertTrue("Expected Genuine but was $v", v is SmsVerdict.Genuine)
        return (v as SmsVerdict.Genuine).tx
    }

    private fun suspicious(sender: String, body: String): String {
        val v = SmsParser.analyse(sender, body, now)
        assertTrue("Expected Suspicious but was $v", v is SmsVerdict.Suspicious)
        return (v as SmsVerdict.Suspicious).reason
    }

    private fun ignored(sender: String, body: String) {
        val v = SmsParser.analyse(sender, body, now)
        assertEquals(SmsVerdict.NotTransaction, v)
    }

    // ---- Genuine alerts -------------------------------------------------------

    @Test fun hdfcUpiDebit() {
        val tx = genuine(
            "VM-HDFCBK-S",
            "Sent Rs.250.00\nFrom HDFC Bank A/C *4321\nTo SWIGGY\nOn 07/10/26\nRef 627812345678\nNot You?\nCall 18002586161/SMS BLOCK UPI to 7308080808"
        )
        assertEquals("debit", tx.type)
        assertEquals(250.0, tx.amount, 0.001)
        assertEquals("HDFC Bank", tx.bankName)
        assertEquals("SWIGGY", tx.merchant)
        assertEquals("627812345678", tx.upiRef)
        assertEquals("4321", tx.accountLast4)
    }

    @Test fun hdfcUpiCredit() {
        val tx = genuine(
            "AD-HDFCBK",
            "Credit Alert!\nRs.15000.00 credited to HDFC Bank A/c XX4321 on 07-10-26 from VPA rahul.sharma@okaxis (UPI 627899998888)"
        )
        assertEquals("credit", tx.type)
        assertEquals(15000.0, tx.amount, 0.001)
        assertEquals("rahul.sharma", tx.merchant)
        assertEquals("627899998888", tx.upiRef)
    }

    @Test fun iciciDebitMentioningCreditedPayee() {
        val tx = genuine(
            "JM-ICICIT-S",
            "ICICI Bank Acct XX123 debited for Rs 1,299.00 on 07-Oct-26; AMAZON PAY credited. UPI:627811112222. Call 18002662 for dispute. SMS BLOCK 123 to 9215676766."
        )
        assertEquals("debit", tx.type) // "debited" comes first: it's the user's account
        assertEquals(1299.0, tx.amount, 0.001)
        assertEquals("AMAZON PAY", tx.merchant)
        assertEquals("ICICI Bank", tx.bankName)
    }

    @Test fun sbiUpiDebitWithoutCurrencySymbol() {
        val tx = genuine(
            "BZ-SBIUPI-S",
            "Dear UPI user A/C X9876 debited by 450.0 on date 07Oct26 trf to ZOMATO Refno 627833334444. If not u? call 1800111109. -SBI"
        )
        assertEquals("debit", tx.type)
        assertEquals(450.0, tx.amount, 0.001)
        assertEquals("ZOMATO", tx.merchant)
        assertEquals("9876", tx.accountLast4)
        assertEquals("SBI", tx.bankName)
    }

    @Test fun sbiSalaryCreditIgnoresBalance() {
        val tx = genuine(
            "VK-SBIINB",
            "Dear Customer, Your A/C XXXXX9876 has been credited with Rs 85,000.00 on 01Oct26 by NEFT from ACME TECHNOLOGIES PVT LTD. Avl Bal Rs 1,02,345.50 -SBI"
        )
        assertEquals("credit", tx.type)
        assertEquals(85000.0, tx.amount, 0.001)
        assertEquals("ACME TECHNOLOGIES PVT LTD", tx.merchant)
    }

    @Test fun axisCardSpendIgnoresLimit() {
        val tx = genuine(
            "AX-AXISBK",
            "Spent INR 2,499 Axis Bank Card no. XX5678 07-10-26 14:22:11 IST MYNTRA Avl Limit: INR 47,501"
        )
        assertEquals("debit", tx.type)
        assertEquals(2499.0, tx.amount, 0.001)
        assertEquals("5678", tx.accountLast4)
    }

    @Test fun kotakWithOfficialLinkIsAccepted() {
        val tx = genuine(
            "AX-KOTAKB-S",
            "Sent Rs.120.00 from Kotak Bank AC X1111 to paytmqr281005050101@paytm on 07-10-26.UPI Ref 627855556666. Not you, https://kotak.com/fraud"
        )
        assertEquals(120.0, tx.amount, 0.001)
        assertEquals("627855556666", tx.upiRef)
    }

    @Test fun sameSmsAlwaysHashesTheSame() {
        val body = "Sent Rs.250.00 From HDFC Bank A/C *4321 To SWIGGY On 07/10/26 Ref 627812345678"
        // Operator prefix / suffix differ between deliveries; the hash must not.
        assertEquals(genuine("VM-HDFCBK-S", body).hash, genuine("AD-HDFCBK", body).hash)
        assertNotEquals(genuine("VM-HDFCBK", body).hash, genuine("VM-HDFCBK", body.replace("250", "260")).hash)
    }

    // ---- Fakes -----------------------------------------------------------------

    @Test fun bankAlertFromPhoneNumberIsFake() {
        val reason = suspicious(
            "+919812345678",
            "Dear SBI customer, your A/c XX1234 credited Rs 49,999. To claim refund click http://sbi-refund.xyz"
        )
        assertTrue(reason.contains("phone number"))
    }

    @Test fun kycThreatFromRealLookingHeaderIsFake() {
        suspicious("VM-HDFCBK", "Rs 5000 debited from A/c XX1234. If not done by you update KYC at hdfc-secure.in")
    }

    @Test fun nonBankLinkIsFake() {
        val reason = suspicious("VM-ICICIB", "ICICI Bank Acct XX123 debited for Rs 9,999.00. Dispute at icici-help.top/claim")
        assertTrue(reason.contains("icici-help.top"))
    }

    @Test fun promotionalHeaderIsFake() {
        suspicious("VM-HDFCBK-P", "Sent Rs.250.00 From HDFC Bank A/C *4321 To SWIGGY On 07/10/26 Ref 627812345678")
    }

    @Test fun senderBankMismatchIsFake() {
        val reason = suspicious("VM-ICICIB", "SBI: Your A/c X1234 debited Rs 500 on 07Oct26 trf to ABC Refno 627800001111")
        assertTrue(reason.contains("SBI"))
    }

    @Test fun bankThemedScamFromNumberIsFakeEvenWithoutAmount() {
        suspicious("9876543210", "Dear customer your SBI account will be blocked today. Update PAN immediately.")
    }

    // ---- Unknown registered header --------------------------------------------

    @Test fun unknownHeaderNeedsReviewUntilTrusted() {
        val body = "Your A/c XX2222 debited Rs 300.00 on 07-10-26 to CHAIPOINT. UPI Ref 627877778888 -XYZ Bank"
        val first = SmsParser.analyse("VM-XYZBNK", body, now)
        assertTrue(first is SmsVerdict.Unverified)
        val trusted = SmsParser.analyse("VM-XYZBNK", body, now, trustedHeaders = setOf("XYZBNK"))
        assertTrue(trusted is SmsVerdict.Genuine)
    }

    // ---- Not transactions ------------------------------------------------------

    @Test fun otpIsIgnored() = ignored("VM-HDFCBK", "123456 is OTP for txn of Rs 500.00 at AMAZON on HDFC Bank card XX1234. Valid for 5 mins.")

    @Test fun failedTxnIsIgnored() = ignored("VM-ICICIB", "Txn of Rs 500.00 on ICICI Bank Card XX123 at FLIPKART has failed due to incorrect PIN.")

    @Test fun dueReminderIsIgnored() = ignored("VM-SBICRD", "Your SBI Card bill of Rs 5,000.00 is due on 15-Oct-26. Pay now to avoid charges.")

    @Test fun futureDebitIsIgnored() = ignored("VM-HDFCBK", "Rs 999.00 will be debited from A/c XX4321 on 10-10-26 towards NETFLIX mandate.")

    @Test fun promoIsIgnored() = ignored("VM-AXISBK", "Get a loan of up to Rs 5,00,000 instantly! Pre-approved offer for you.")
}
