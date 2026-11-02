package com.example.fin_ai.sms

/**
 * Registered DLT sender headers of Indian banks.
 *
 * Since TRAI's DLT rules, commercial SMS can only come from a registered 6-character
 * header (shown as e.g. "VM-HDFCBK", "AD-ICICIT-S"; the 2-letter prefix is the telecom
 * operator/circle, the optional suffix the message class). A fraudster texting from an
 * ordinary mobile number cannot use these headers, which is what makes them the
 * primary authenticity signal.
 *
 * Headers not listed here are not rejected outright: a transactional-looking message
 * from an unknown registered header is held for the user to review and trust.
 */
object BankSenders {

    data class Bank(val name: String, val headers: Set<String>, val keywords: List<String>)

    val banks: List<Bank> = listOf(
        Bank("HDFC Bank", setOf("HDFCBK", "HDFCBN", "HDFCCC"), listOf("hdfc")),
        Bank("ICICI Bank", setOf("ICICIB", "ICICIT", "ICICIO", "ICICIC"), listOf("icici")),
        Bank("SBI", setOf("SBIINB", "SBIUPI", "SBIPSG", "SBIBNK", "SBMSMS", "ATMSBI", "CBSSBI", "SBIOTP", "SBICRD", "SBICRM"), listOf("sbi", "state bank")),
        Bank("Axis Bank", setOf("AXISBK", "AXISMR", "AXISCC"), listOf("axis")),
        Bank("Kotak Bank", setOf("KOTAKB", "KOTAKM", "KMBANK"), listOf("kotak")),
        Bank("Punjab National Bank", setOf("PNBSMS", "PUNBNK", "PNBBNK"), listOf("pnb", "punjab national")),
        Bank("Bank of Baroda", setOf("BOBTXN", "BOBSMS", "BARODA", "BOBCRD"), listOf("bank of baroda", "bob")),
        Bank("Canara Bank", setOf("CANBNK", "CNRBNK"), listOf("canara")),
        Bank("Union Bank of India", setOf("UNIONB", "UBININ"), listOf("union bank")),
        Bank("Bank of India", setOf("BOIIND", "BOISMS"), listOf("bank of india")),
        Bank("IndusInd Bank", setOf("INDUSB", "INDUSL"), listOf("indusind")),
        Bank("Yes Bank", setOf("YESBNK", "YESBKA"), listOf("yes bank")),
        Bank("IDFC First Bank", setOf("IDFCFB", "IDFCBK"), listOf("idfc")),
        Bank("IDBI Bank", setOf("IDBIBK", "IDBIBN"), listOf("idbi")),
        Bank("Federal Bank", setOf("FEDBNK", "FEDFIB"), listOf("federal")),
        Bank("Indian Bank", setOf("INDBNK", "INDBMK"), listOf("indian bank")),
        Bank("Central Bank of India", setOf("CENTBK", "CBOI"), listOf("central bank")),
        Bank("Indian Overseas Bank", setOf("IOBCHN", "IOBBNK"), listOf("indian overseas", "iob")),
        Bank("AU Small Finance Bank", setOf("AUBANK", "AUBKSF"), listOf("au bank", "au small")),
        Bank("RBL Bank", setOf("RBLBNK", "RBLCRD"), listOf("rbl")),
        Bank("Paytm Payments Bank", setOf("PAYTMB", "PYTMBK"), listOf("paytm")),
        Bank("Airtel Payments Bank", setOf("AIRBNK", "AIRPBK"), listOf("airtel payments")),
        Bank("HSBC", setOf("HSBCIN", "HSBCBK"), listOf("hsbc")),
        Bank("Standard Chartered", setOf("SCBANK", "STANCB"), listOf("standard chartered")),
        Bank("American Express", setOf("AMEXIN", "AMEXCC"), listOf("american express", "amex")),
    )

    private val byHeader: Map<String, Bank> = banks.flatMap { b -> b.headers.map { it to b } }.toMap()

    fun bankFor(header: String): Bank? = byHeader[header.uppercase()]
}
