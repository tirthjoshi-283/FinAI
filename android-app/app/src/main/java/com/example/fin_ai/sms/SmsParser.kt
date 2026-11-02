package com.example.fin_ai.sms

import java.security.MessageDigest

/** A bank transaction extracted from an SMS. Only these fields ever leave the phone. */
data class ParsedSms(
    val hash: String,
    val type: String, // "credit" | "debit"
    val amount: Double,
    val bankName: String,
    val merchant: String?,
    val upiRef: String?,
    val accountLast4: String?,
    val timestampMillis: Long,
)

sealed interface SmsVerdict {
    /** From a known bank's registered header and passed every content check. */
    data class Genuine(val tx: ParsedSms) : SmsVerdict

    /** Looks like a real alert from a registered (DLT) header we don't know; user can trust it. */
    data class Unverified(val tx: ParsedSms, val header: String, val reason: String) : SmsVerdict

    /** Looks like a bank alert but fails an authenticity check: likely fake or phishing. */
    data class Suspicious(val reason: String) : SmsVerdict

    /** Not a completed money movement (OTP, promo, reminder, failed txn, ...). */
    data object NotTransaction : SmsVerdict
}

/**
 * Parses Indian bank SMS alerts and decides whether they are genuine.
 *
 * Authenticity layers, strongest first:
 *  1. Sender must be a DLT-registered alphanumeric header, not a phone number.
 *  2. Header must belong to a known bank (or one the user explicitly trusted);
 *     promotional ("-P") headers are never accepted.
 *  3. No phishing markers: KYC/PAN-update or "account blocked" threats, prizes,
 *     links to anything but official bank domains, app-install prompts.
 *  4. A message must not claim to be from a different bank than its sender.
 *  5. Must read as a completed transaction: amount + debit/credit verb +
 *     masked account or UPI/UTR reference. OTPs, reminders, mandates and failed
 *     transactions are ignored.
 */
object SmsParser {

    private val DLT_SENDER = Regex("^(?:[A-Z]{2}-)?([A-Z0-9]{6})(?:-([STPG]))?$", RegexOption.IGNORE_CASE)
    private val PHONE_SENDER = Regex("^\\+?[0-9][0-9 -]{5,16}$")

    private val AMOUNT = Regex("(?:rs\\.?|inr|₹)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    private val AMOUNT_AFTER_VERB = Regex(
        "(?:debited|credited|deducted)\\s+(?:by|with|for)\\s+(?:rs\\.?|inr|₹)?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE
    )
    private val DEBIT_WORDS = Regex(
        "\\b(debited|debit of|spent|paid|sent|withdrawn|withdrawal|purchase|deducted|txn of|transferred to|trf to)\\b",
        RegexOption.IGNORE_CASE
    )
    private val CREDIT_WORDS = Regex("\\b(credited|received|deposited|refunded|refund of)\\b", RegexOption.IGNORE_CASE)
    private val ACCOUNT = Regex(
        "(?:a/?c|acct|account|card)\\s*(?:no\\.?)?\\s*(?:ending\\s*(?:with|in)?\\s*)?[:\\-]?\\s*(?:[x*]+\\s*)([0-9]{3,6})\\b|(?:card|a/?c|acct|account)\\s+ending\\s*(?:with|in)?\\s*([0-9]{3,6})\\b",
        RegexOption.IGNORE_CASE
    )
    private val REFERENCE = Regex(
        "(?:upi\\s*ref(?:erence)?(?:\\s*no)?|upi|ref(?:erence)?(?:\\s*no)?|refno|utr(?:\\s*no)?|rrn|imps\\s*ref|txn\\s*id|transaction\\s*id)[\\s:.#-]*([A-Za-z0-9]{6,22})",
        RegexOption.IGNORE_CASE
    )

    private val NOT_A_TXN = Regex(
        "\\b(otp|one[- ]time password|verification code)\\b|will be debited|will be credited|is due|due on|due date|" +
            "requested (?:money|rs|inr|₹)|collect request|has requested|mandate|autopay (?:set|registered|scheduled)|" +
            "failed|declined|unsuccessful|could not be processed|insufficient|reversal initiated|" +
            "pre-?approved|eligible for|get a loan|offer|cashback upto|apply now",
        RegexOption.IGNORE_CASE
    )
    private val PHISHING = Regex(
        "\\bkyc\\b|update (?:your )?(?:pan|kyc|aadhaar)|(?:pan|aadhaar|kyc).{0,25}(?:expire|update|link|suspend)|" +
            "(?:will be|has been|is) (?:blocked|suspended|deactivated|frozen)|" +
            "\\b(?:won|winner|lottery|prize|lucky draw|claim (?:now|your))\\b|reward points.{0,25}(?:expir|redeem)|" +
            "\\.apk\\b|download (?:the )?app|whatsapp|telegram|share (?:your )?(?:otp|pin|cvv)",
        RegexOption.IGNORE_CASE
    )
    private val BANKISH = Regex("\\b(bank|a/?c|account|upi|card|sbi|hdfc|icici|axis|kotak)\\b", RegexOption.IGNORE_CASE)
    private val LINK = Regex("(?:https?://)?((?:[a-z0-9-]+\\.)+([a-z]{2,10}))(?=[/\\s)\\],;:!?]|\\.(?:\\s|$)|$)", RegexOption.IGNORE_CASE)
    private val LINK_TLDS = setOf(
        "com", "in", "net", "org", "co", "io", "xyz", "info", "link", "me", "ly", "app", "site", "online",
        "top", "live", "club", "cc", "tk", "ml", "ga", "cf", "gq", "ru", "cn", "biz", "us", "uk", "sbi",
        "bank", "gov", "edu", "ai", "shop", "store", "click", "buzz", "icu", "vip", "win", "page", "dev",
    )
    private val OFFICIAL_DOMAINS = listOf(
        "bank.in", "hdfcbank.com", "hdfcbk.io", "icicibank.com", "icici.co", "sbi.co.in", "onlinesbi.sbi",
        "sbi.bank.in", "sbicard.com", "axisbank.com", "axis.bank.in", "kotak.com", "pnbindia.in",
        "bankofbaroda.in", "canarabank.com", "unionbankofindia.co.in", "indusind.com", "yesbank.in",
        "idfcfirstbank.com", "idbibank.in", "federalbank.co.in", "rblbank.com", "paytmbank.com",
        "airtel.in", "npci.org.in", "cybercrime.gov.in", "rbi.org.in",
    )

    private val DEBIT_MERCHANT = listOf(
        Regex(";\\s*([A-Za-z0-9 .&'@_-]{2,40}?)\\s+credited", RegexOption.IGNORE_CASE),
        Regex("\\b(?:trf|transfer(?:red)?) to\\s+([A-Za-z0-9 .&'@_-]{2,40}?)\\s*(?:\\s(?:refno|ref|on|upi|via)\\b|\\(|\\.\\s|$)", RegexOption.IGNORE_CASE),
        Regex("\\bto\\s+(?:vpa\\s+)?([A-Za-z0-9 .&'@_-]{2,40}?)\\s*(?:\\s(?:on|ref|refno|upi|via|using)\\b|\\(|\\.\\s|$)", RegexOption.IGNORE_CASE),
        Regex("\\bat\\s+([A-Za-z0-9 .&'@_*-]{2,40}?)\\s*(?:\\s(?:on|via|ref|using|txn)\\b|\\(|\\.\\s|$)", RegexOption.IGNORE_CASE),
        Regex("\\binfo[:\\s]+([A-Za-z0-9 /.&_-]{2,40}?)(?:\\.\\s|\\s(?:avl|avbl|bal)\\b|$)", RegexOption.IGNORE_CASE),
    )
    private val CREDIT_MERCHANT = listOf(
        Regex("\\bfrom\\s+(?:vpa\\s+)?([A-Za-z0-9 .&'@_-]{2,40}?)\\s*(?:\\s(?:on|ref|refno|upi|via|info)\\b|\\(|\\.\\s|$)", RegexOption.IGNORE_CASE),
        Regex("\\bby\\s+([A-Za-z][A-Za-z0-9 .&'@_-]{1,40}?)\\s*(?:\\s(?:on|ref|refno|upi|via)\\b|\\(|\\.\\s|$)", RegexOption.IGNORE_CASE),
        Regex("\\binfo[:\\s]+([A-Za-z0-9 /.&_-]{2,40}?)(?:\\.\\s|\\s(?:avl|avbl|bal)\\b|$)", RegexOption.IGNORE_CASE),
    )
    private val NOT_A_MERCHANT = Regex(
        "^(?:your|you|a/?c|acct|account|card|the|bank|upi|vpa|rs|inr|date|ref)\\b|a/?c\\s*[x*]|[x*]{2,}\\d|^[0-9 .-]+$",
        RegexOption.IGNORE_CASE
    )

    /**
     * @param sender   the SMS originating address, e.g. "VM-HDFCBK-S" or "+919876543210"
     * @param trustedHeaders extra 6-char headers the user explicitly trusted
     */
    fun analyse(sender: String, body: String, timestampMillis: Long, trustedHeaders: Set<String> = emptySet()): SmsVerdict {
        val text = body.replace(Regex("\\s+"), " ").trim()
        val dlt = DLT_SENDER.matchEntire(sender.trim())
        val core = parseCore(text, sender, timestampMillis)
            ?: return if (dlt == null && PHISHING.containsMatchIn(text) && BANKISH.containsMatchIn(text)) {
                // Not a transaction, but a bank-themed scam from a personal number ("A/c will be blocked, update KYC").
                SmsVerdict.Suspicious("Bank-themed scam pattern from a non-bank sender ($sender).")
            } else {
                SmsVerdict.NotTransaction
            }

        // Layer 1: ordinary numbers / emails can't be banks.
        if (dlt == null) {
            return if (PHONE_SENDER.matches(sender.trim())) {
                SmsVerdict.Suspicious("Bank-style message sent from a regular phone number ($sender). Banks never send alerts from personal numbers.")
            } else {
                SmsVerdict.Suspicious("Bank-style message from an unregistered sender ($sender).")
            }
        }
        val header = dlt.groupValues[1].uppercase()
        val suffix = dlt.groupValues[2].uppercase()

        // Layer 2a: promotional route can't carry transaction alerts.
        if (suffix == "P") return SmsVerdict.Suspicious("Transaction alert sent on a promotional (-P) header $sender.")

        // Layer 3: phishing markers and non-bank links.
        PHISHING.find(text)?.let { return SmsVerdict.Suspicious("Contains a phishing pattern: \"${it.value}\".") }
        badLink(text)?.let { return SmsVerdict.Suspicious("Contains a link to a non-bank website ($it).") }

        val bank = BankSenders.bankFor(header)
        if (bank != null) {
            // Layer 4: sender and claimed bank must agree.
            claimedOtherBank(text, bank)?.let {
                return SmsVerdict.Suspicious("Sent from ${bank.name}'s header but claims to be from $it.")
            }
            return SmsVerdict.Genuine(core.copy(bankName = bank.name))
        }
        // Layer 2b: registered but unknown header — let the user decide.
        val named = if (header in trustedHeaders) header else null
        return if (named != null) {
            SmsVerdict.Genuine(core.copy(bankName = header))
        } else {
            SmsVerdict.Unverified(core.copy(bankName = header), header, "Sender $header is not on the known-bank list.")
        }
    }

    /** Layer 5: extracts the transaction, or null if this isn't a completed money movement. */
    private fun parseCore(text: String, sender: String, timestampMillis: Long): ParsedSms? {
        if (NOT_A_TXN.containsMatchIn(text)) return null

        val debitAt = DEBIT_WORDS.find(text)?.range?.first
        val creditAt = CREDIT_WORDS.find(text)?.range?.first
        val type = when {
            debitAt == null && creditAt == null -> return null
            creditAt == null -> "debit"
            debitAt == null -> "credit"
            // The first verb describes the user's own account ("A/c debited ...; X credited").
            else -> if (debitAt < creditAt) "debit" else "credit"
        }

        val amount = findAmount(text) ?: return null
        val account = ACCOUNT.find(text)?.let { m -> (m.groupValues[1].ifEmpty { m.groupValues[2] }).takeLast(4) }
        val ref = REFERENCE.findAll(text).map { it.groupValues[1] }.firstOrNull { r -> r.count(Char::isDigit) >= 6 }
        if (account == null && ref == null && !Regex("\\b(bank|upi|imps|neft|rtgs)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            return null
        }

        return ParsedSms(
            hash = sha256(DLT_SENDER.matchEntire(sender.trim())?.groupValues?.get(1)?.uppercase().orEmpty() + "|" + text),
            type = type,
            amount = amount,
            bankName = "",
            merchant = findMerchant(text, type),
            upiRef = ref,
            accountLast4 = account,
            timestampMillis = timestampMillis,
        )
    }

    private fun findAmount(text: String): Double? {
        val candidates = AMOUNT.findAll(text).filterNot { m ->
            // Skip balances: "Avl Bal Rs 12,000"
            val before = text.substring(maxOf(0, m.range.first - 14), m.range.first).lowercase()
            "bal" in before || "limit" in before
        }.map { it.groupValues[1] } + AMOUNT_AFTER_VERB.findAll(text).map { it.groupValues[1] }
        return candidates.firstNotNullOfOrNull { it.replace(",", "").toDoubleOrNull()?.takeIf { v -> v > 0 } }
    }

    private fun findMerchant(text: String, type: String): String? {
        val patterns = if (type == "debit") DEBIT_MERCHANT else CREDIT_MERCHANT
        for (p in patterns) {
            for (m in p.findAll(text)) {
                var name = m.groupValues[1].trim().trimEnd('.', '-', ' ')
                if (name.length < 2 || NOT_A_MERCHANT.containsMatchIn(name)) continue
                // "swiggy@icici" -> "swiggy"; keep the VPA if the handle part is just a number.
                if ('@' in name) {
                    val local = name.substringBefore('@')
                    if (local.any(Char::isLetter) && local.count(Char::isDigit) < 6) name = local
                }
                return name.take(40)
            }
        }
        return null
    }

    private fun badLink(text: String): String? =
        LINK.findAll(text)
            // A real link starts a word ("A/c.Info" or "26.UPI" are not links) and ends in a real TLD.
            .filter { m -> m.range.first == 0 || text[m.range.first - 1] in " \t(:\"'" }
            .filter { m -> m.groupValues[2].lowercase() in LINK_TLDS }
            .map { it.groupValues[1].lowercase() }
            .firstOrNull { host -> OFFICIAL_DOMAINS.none { host == it || host.endsWith(".$it") } }

    /** Name of another bank the message claims to be from, judged from its opening words. */
    private fun claimedOtherBank(text: String, own: BankSenders.Bank): String? {
        val opening = text.take(45).lowercase()
        if (own.keywords.any { it in opening }) return null
        return BankSenders.banks.firstOrNull { other ->
            other != own && other.keywords.any { kw -> Regex("\\b${Regex.escape(kw)}\\b").containsMatchIn(opening) }
        }?.name
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
