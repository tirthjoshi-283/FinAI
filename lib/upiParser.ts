/**
 * lib/upiParser.ts
 *
 * Parses UPI transaction alert emails from Indian banks.
 * Strategy:
 *   1. Try bank-specific regex patterns (fast, reliable).
 *   2. If no match → fallback to Groq AI structured extraction.
 *
 * Supported Banks / Apps:
 *   HDFC, ICICI, SBI, Axis, Kotak, PNB, IndusInd, Yes Bank, IDFC First,
 *   Paytm, PhonePe, Google Pay, Amazon Pay, Airtel Payments Bank, Federal Bank.
 */

import { groqChat } from "./groq";
import type { RawEmailData } from "@/lib/gmail";

// ─── Output interface ─────────────────────────────────────────────────────────

export interface ParsedUPITransaction {
    type: "credit" | "debit";
    amount: number;
    merchant: string;         // payee/payer name, empty string if unknown
    bankName: string;
    upiRef: string;           // UTR / reference number, empty string if unknown
    date: Date;
    category: string;         // auto-categorized
    title: string;            // human-readable title for the transaction
    rawSubject: string;
}

// ─── Merchant → Category map ──────────────────────────────────────────────────

const MERCHANT_CATEGORIES: [RegExp, string][] = [
    [/swiggy|zomato|dominos|pizza|mcdonald|kfc|burger|dunkin|blinkit|zepto/i, "Food & Dining"],
    [/amazon|flipkart|meesho|myntra|nykaa|ajio|snapdeal|shopsy/i, "Shopping"],
    [/uber|ola|rapido|porter|yulu|bounce|blumart/i, "Transport"],
    [/irctc|railways|make.?my.?trip|goibibo|redbus|cleartrip|easemytrip|indigo|spicejet|airindia/i, "Travel"],
    [/netflix|hotstar|prime.?video|zee5|sonyliv|jiocinema|bookmyshow|pvr|inox/i, "Entertainment"],
    [/electricity|water|gas|airtel|jio|bsnl|vodafone|vi |idea|tata.?sky|dish.?tv|d2h|broadband|internet/i, "Bills & Utilities"],
    [/hospital|clinic|pharmacy|apollo|medplus|1mg|netmeds|practo|healthkart/i, "Healthcare"],
    [/school|college|university|tuition|coursera|udemy|byju|unacademy|vedantu/i, "Education"],
    [/rent|pg |hostel|society|maintenance/i, "Housing"],
    [/salary|stipend|incentive|bonus|freelance|payment.received|credited.by.employer/i, "Income"],
    [/mutual.fund|sip|lic|insurance|premium|policy/i, "Investments"],
    [/fuel|petrol|diesel|hp.?petrol|indian.?oil|bharat.?petroleum/i, "Fuel"],
    [/gym|fitness|yoga|cult.?fit/i, "Fitness"],
];

function categorizeByMerchant(merchant: string, title: string): string {
    const text = `${merchant} ${title}`.toLowerCase();
    for (const [pattern, category] of MERCHANT_CATEGORIES) {
        if (pattern.test(text)) return category;
    }
    return "UPI Transfer";
}

// ─── Regex patterns per bank ──────────────────────────────────────────────────

interface BankPattern {
    bankName: string;
    /** Match against email subject */
    subjectTest: RegExp;
    /** Extract debit/credit, amount, merchant, upiRef from combined subject+body */
    parse: (subject: string, body: string) => Partial<ParsedUPITransaction> | null;
}

const AMOUNT_PATTERNS = [
    /(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)/i,
    /([\d,]+(?:\.\d{1,2})?)\s*(?:INR|Rs\.?|₹)/i,
];

function extractAmount(text: string): number | null {
    for (const p of AMOUNT_PATTERNS) {
        const m = text.match(p);
        if (m) {
            const num = parseFloat(m[1].replace(/,/g, ""));
            if (!isNaN(num) && num > 0) return num;
        }
    }
    return null;
}

function extractUpiRef(text: string): string {
    const patterns = [
        /(?:UPI\s*Ref\.?\s*(?:No\.?)?|UTR\s*(?:No\.?)?|Reference\s*No\.?|Txn\s*ID|Transaction\s*ID)[:\s#]*([A-Z0-9]{10,25})/i,
        /\b([0-9]{12,25})\b/,
    ];
    for (const p of patterns) {
        const m = text.match(p);
        if (m) return m[1];
    }
    return "";
}

function extractMerchant(text: string, type: "credit" | "debit"): string {
    // "to <merchant>" for debits, "from <sender>" for credits
    const debitPatterns = [
        /(?:paid|sent|transferred|debited)\s+to\s+([A-Za-z0-9 .&'_-]{2,40}?)(?:\s*via|\s*on|\s*at|\s*for|\s*\.|$)/i,
        /(?:to\s+VPA|to\s+UPI\s+ID)[:\s]+([A-Za-z0-9@._-]{3,50})/i,
        /Merchant\s*:\s*([A-Za-z0-9 .&'_-]{2,40})/i,
    ];
    const creditPatterns = [
        /(?:received|credited)\s+from\s+([A-Za-z0-9 .&'_-]{2,40}?)(?:\s*via|\s*on|\s*at|\s*\.|$)/i,
        /(?:from\s+VPA|from\s+UPI\s+ID)[:\s]+([A-Za-z0-9@._-]{3,50})/i,
        /Sender\s*:\s*([A-Za-z0-9 .&'_-]{2,40})/i,
    ];
    const patterns = type === "debit" ? debitPatterns : creditPatterns;
    for (const p of patterns) {
        const m = text.match(p);
        if (m) return m[1].trim();
    }
    return "";
}

const BANK_PATTERNS: BankPattern[] = [
    // ── HDFC Bank ──────────────────────────────────────────────────────────────
    {
        bankName: "HDFC Bank",
        subjectTest: /hdfc|alert|UPI/i,
        parse(subject, body) {
            const text = `${subject} ${body}`;
            const amount = extractAmount(text);
            if (!amount) return null;

            const isDebit = /debited|paid|sent/i.test(text);
            const isCredit = /credited|received/i.test(text);
            if (!isDebit && !isCredit) return null;

            const type: "credit" | "debit" = isDebit ? "debit" : "credit";
            const merchant = extractMerchant(text, type);
            const upiRef = extractUpiRef(text);

            return { type, amount, merchant, upiRef, bankName: "HDFC Bank" };
        },
    },
    // ── ICICI Bank ─────────────────────────────────────────────────────────────
    {
        bankName: "ICICI Bank",
        subjectTest: /icici|UPI|transaction/i,
        parse(subject, body) {
            const text = `${subject} ${body}`;
            const amount = extractAmount(text);
            if (!amount) return null;

            const isDebit = /debited|paid|sent/i.test(text);
            const isCredit = /credited|received/i.test(text);
            if (!isDebit && !isCredit) return null;

            const type: "credit" | "debit" = isDebit ? "debit" : "credit";
            const merchant = extractMerchant(text, type);
            const upiRef = extractUpiRef(text);

            return { type, amount, merchant, upiRef, bankName: "ICICI Bank" };
        },
    },
    // ── SBI ────────────────────────────────────────────────────────────────────
    {
        bankName: "SBI",
        subjectTest: /sbi|state bank|UPI/i,
        parse(subject, body) {
            const text = `${subject} ${body}`;
            const amount = extractAmount(text);
            if (!amount) return null;

            const isDebit = /debited|paid|sent/i.test(text);
            const isCredit = /credited|received/i.test(text);
            if (!isDebit && !isCredit) return null;

            const type: "credit" | "debit" = isDebit ? "debit" : "credit";
            const merchant = extractMerchant(text, type);
            const upiRef = extractUpiRef(text);

            return { type, amount, merchant, upiRef, bankName: "SBI" };
        },
    },
    // ── Axis Bank ──────────────────────────────────────────────────────────────
    {
        bankName: "Axis Bank",
        subjectTest: /axis|UPI|transaction/i,
        parse(subject, body) {
            const text = `${subject} ${body}`;
            const amount = extractAmount(text);
            if (!amount) return null;

            const isDebit = /debited|paid|sent/i.test(text);
            const isCredit = /credited|received/i.test(text);
            if (!isDebit && !isCredit) return null;

            const type: "credit" | "debit" = isDebit ? "debit" : "credit";
            const merchant = extractMerchant(text, type);
            const upiRef = extractUpiRef(text);

            return { type, amount, merchant, upiRef, bankName: "Axis Bank" };
        },
    },
    // ── Kotak Bank ─────────────────────────────────────────────────────────────
    {
        bankName: "Kotak Bank",
        subjectTest: /kotak|UPI|transaction/i,
        parse(subject, body) {
            const text = `${subject} ${body}`;
            const amount = extractAmount(text);
            if (!amount) return null;

            const isDebit = /debited|paid|sent/i.test(text);
            const isCredit = /credited|received/i.test(text);
            if (!isDebit && !isCredit) return null;

            const type: "credit" | "debit" = isDebit ? "debit" : "credit";
            const merchant = extractMerchant(text, type);
            const upiRef = extractUpiRef(text);

            return { type, amount, merchant, upiRef, bankName: "Kotak Bank" };
        },
    },
    // ── Generic / Paytm / PhonePe / GPay (works across all) ───────────────────
    {
        bankName: "UPI",
        subjectTest: /paytm|phonepe|google pay|gpay|amazon pay|upi|payment|transfer/i,
        parse(subject, body) {
            const text = `${subject} ${body}`;
            const amount = extractAmount(text);
            if (!amount) return null;

            const isDebit = /debited|paid|sent|debit|outgoing/i.test(text);
            const isCredit = /credited|received|credit|incoming/i.test(text);
            if (!isDebit && !isCredit) return null;

            const type: "credit" | "debit" = isDebit ? "debit" : "credit";
            const merchant = extractMerchant(text, type);
            const upiRef = extractUpiRef(text);

            let bankName = "UPI";
            if (/paytm/i.test(text)) bankName = "Paytm";
            else if (/phonepe/i.test(text)) bankName = "PhonePe";
            else if (/google.?pay|gpay/i.test(text)) bankName = "Google Pay";
            else if (/amazon.?pay/i.test(text)) bankName = "Amazon Pay";

            return { type, amount, merchant, upiRef, bankName };
        },
    },
];

// ─── Groq AI Fallback ─────────────────────────────────────────────────────────

const AI_SYSTEM_PROMPT = `You are a financial data extraction assistant specializing in Indian UPI bank transaction alert emails.

Extract transaction information from the email text and respond ONLY with valid JSON (no markdown, no explanation).

Return this exact JSON structure:
{
  "type": "credit" | "debit",
  "amount": <number, e.g. 1500.00>,
  "merchant": "<payee/payer name or empty string>",
  "bankName": "<bank name or UPI app name>",
  "upiRef": "<UTR/reference number or empty string>"
}

Rules:
- "debit" = money went OUT of the account (paid, sent, debited)
- "credit" = money came IN to the account (received, credited)
- amount must be a positive number (no currency symbols)
- If you cannot determine type or amount, return null instead of JSON
- NEVER include markdown code blocks in response`;

async function parseWithAI(
    subject: string,
    body: string
): Promise<Partial<ParsedUPITransaction> | null> {
    try {
        if (!process.env.GROQ_API_KEY) return null;
        const truncatedBody = body.slice(0, 1500); // Keep prompt concise

        const rawText = await groqChat(
            [
                { role: "system", content: AI_SYSTEM_PROMPT },
                { role: "user", content: `Email Subject: ${subject}

Email Body:
${truncatedBody}` },
            ],
            { temperature: 0 },
            "UPIParser"
        );
        const cleaned = rawText.replace(/```json|```/g, "").trim();

        if (cleaned === "null" || !cleaned) return null;

        const parsed = JSON.parse(cleaned);
        if (!parsed.type || !parsed.amount) return null;

        return {
            type: parsed.type as "credit" | "debit",
            amount: parseFloat(parsed.amount),
            merchant: parsed.merchant ?? "",
            bankName: parsed.bankName ?? "UPI",
            upiRef: parsed.upiRef ?? "",
        };
    } catch {
        return null; // Silently fail — don't block sync for a single email
    }
}

// ─── Main Parser ──────────────────────────────────────────────────────────────

/**
 * Parses a single raw UPI email into a structured transaction.
 * @returns ParsedUPITransaction or null if the email is not a UPI alert
 */
export async function parseUpiEmail(
    email: RawEmailData
): Promise<ParsedUPITransaction | null> {
    const { subject, body, internalDate } = email;
    const combinedText = `${subject} ${body}`;

    // Quick sanity check — must look like a transaction email
    const isTransactionEmail = /(?:debited|credited|paid|received|transfer|UPI|transaction)/i.test(combinedText);
    if (!isTransactionEmail) return null;

    // Try regex patterns first
    let partial: Partial<ParsedUPITransaction> | null = null;
    for (const pattern of BANK_PATTERNS) {
        if (pattern.subjectTest.test(subject) || pattern.subjectTest.test(body)) {
            partial = pattern.parse(subject, body);
            if (partial) break;
        }
    }

    // Fallback to Groq AI if regex didn't work
    if (!partial) {
        partial = await parseWithAI(subject, body);
    }

    if (!partial || !partial.type || !partial.amount) return null;

    const merchant = partial.merchant ?? "";
    const type = partial.type;

    // Build a human-readable title
    let title: string;
    if (merchant) {
        title = type === "debit" ? `Paid to ${merchant}` : `Received from ${merchant}`;
    } else {
        title = type === "debit" ? "UPI Debit" : "UPI Credit";
    }

    const category = categorizeByMerchant(merchant, title);

    return {
        type,
        amount: partial.amount,
        merchant,
        bankName: partial.bankName ?? "UPI",
        upiRef: partial.upiRef ?? "",
        date: internalDate,
        category,
        title,
        rawSubject: subject,
    };
}
