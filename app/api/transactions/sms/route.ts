import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { Transaction } from "@/models/Transaction";
import { User } from "@/models/User";
import { verifyAuth } from "@/lib/verifyAuth";
import { groqChat } from "@/lib/groq";
import { CREDIT_CATEGORIES, DEBIT_CATEGORIES, DEBIT_CATEGORY_GUIDE } from "@/lib/categories";

/**
 * Batch import of bank-SMS transactions captured by the Android app.
 *
 * The phone does the parsing and the genuineness checks (DLT sender header,
 * known-bank allowlist, phishing heuristics) and uploads only the extracted
 * fields — never the SMS text itself. This route validates, de-duplicates and
 * categorises them.
 */

const MAX_BATCH = 100;
const MAX_AMOUNT = 10_000_000; // ₹1 crore: anything larger is almost certainly a mis-parse

type IncomingSms = {
    hash: string;
    type: "credit" | "debit";
    amount: number;
    date: string;
    bankName: string;
    merchant?: string;
    upiRef?: string;
    accountLast4?: string;
};

// Fast keyword rules first; the AI only sees merchants these don't recognise.
const DEBIT_RULES: [RegExp, string][] = [
    [/swiggy|zomato|domino|pizza|mcdonald|kfc|burger|starbucks|cafe|restaurant|hotel|dhaba|blinkit|zepto|bigbasket|dmart|grofers|instamart|bakery|sweets/i, "Food"],
    [/amazon|flipkart|meesho|myntra|nykaa|ajio|snapdeal|shopsy|decathlon|reliance\s*trends|lifestyle|croma|vijay\s*sales/i, "Shopping"],
    [/tanishq|kalyan|malabar|jewel/i, "Jewellery"],
    [/uber|ola|rapido|irctc|redbus|makemytrip|goibibo|cleartrip|indigo|air\s*india|spicejet|fastag|petrol|fuel|hpcl|bpcl|indian\s*oil|iocl|metro/i, "Travel"],
    [/electricity|bescom|msedcl|tata\s*power|adani\s*(electric|energy)|torrent|water|gas|indane|bharat\s*gas|airtel|jio|bsnl|vodafone|\bvi\b|broadband|dth|tata\s*play|recharge/i, "Utilities"],
    [/hospital|clinic|pharma|apollo|medplus|1mg|netmeds|pharmeasy|diagnostic|lab|practo/i, "Health"],
    [/school|college|university|tuition|coursera|udemy|byju|unacademy|vedantu|fees/i, "Education"],
    [/netflix|hotstar|prime\s*video|spotify|zee5|sonyliv|jiocinema|bookmyshow|pvr|inox|steam|playstation/i, "Entertainment"],
    [/\brent\b|nobroker|maintenance|society|\bpg\b|hostel/i, "Rent"],
    [/income\s*tax|gst|tds|challan|municipal|property\s*tax|e-?pay\s*tax/i, "Taxes"],
];
const CREDIT_RULES: [RegExp, string][] = [
    [/salary|sal\b|payroll|stipend/i, "Salary"],
    [/refund|reversal|reversed|cashback|chargeback/i, "Refunds"],
    [/dividend|interest|mutual\s*fund|redemption|zerodha|groww|upstox/i, "Investments"],
];

function ruleCategory(tx: IncomingSms): string | null {
    const text = `${tx.merchant ?? ""}`;
    for (const [re, cat] of tx.type === "debit" ? DEBIT_RULES : CREDIT_RULES) {
        if (re.test(text)) return cat;
    }
    return null;
}

/** One AI call for all debits the rules couldn't place. Falls back silently. */
async function aiCategories(items: { i: number; merchant: string }[]): Promise<Map<number, string>> {
    const result = new Map<number, string>();
    if (items.length === 0 || !process.env.GROQ_API_KEY) return result;
    try {
        const raw = await groqChat(
            [
                {
                    role: "system",
                    content: `Assign each Indian bank-transaction payee to one spending category.
Categories: ${DEBIT_CATEGORIES.join(", ")}
${DEBIT_CATEGORY_GUIDE}
Person names, phone numbers or UPI IDs of individuals -> "Miscellaneous".
Return ONLY JSON: {"categories": [{"i": <number>, "category": "<category>"}]}`,
                },
                { role: "user", content: JSON.stringify(items) },
            ],
            { json: true, temperature: 0 },
            "SmsCategorise"
        );
        const parsed = JSON.parse(raw) as { categories?: { i: number; category: string }[] };
        for (const c of parsed.categories ?? []) {
            if ((DEBIT_CATEGORIES as readonly string[]).includes(c.category)) result.set(c.i, c.category);
        }
    } catch (e) {
        console.warn("[SmsImport] AI categorisation skipped:", (e as Error).message.substring(0, 160));
    }
    return result;
}

function clean(value: unknown, max: number): string | undefined {
    if (typeof value !== "string") return undefined;
    const v = value.replace(/\s+/g, " ").trim().slice(0, max);
    return v || undefined;
}

function validate(raw: unknown): IncomingSms | null {
    if (!raw || typeof raw !== "object") return null;
    const r = raw as Record<string, unknown>;
    const amount = typeof r.amount === "number" ? Math.round(r.amount * 100) / 100 : NaN;
    const date = typeof r.date === "string" ? new Date(r.date) : null;
    const hash = clean(r.hash, 128);
    const bankName = clean(r.bankName, 60);
    if (!hash || !bankName || (r.type !== "credit" && r.type !== "debit")) return null;
    if (!(amount > 0 && amount <= MAX_AMOUNT)) return null;
    // Reject unparseable dates and anything in the future (allowing for clock skew).
    if (!date || isNaN(date.getTime()) || date.getTime() > Date.now() + 10 * 60 * 1000) return null;
    return {
        hash,
        type: r.type,
        amount,
        date: date.toISOString(),
        bankName,
        merchant: clean(r.merchant, 80),
        upiRef: clean(r.upiRef, 40)?.replace(/[^A-Za-z0-9]/g, ""),
        accountLast4: clean(r.accountLast4, 6)?.replace(/\D/g, ""),
    };
}

export async function POST(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

    let body: { transactions?: unknown };
    try {
        body = await req.json();
    } catch {
        return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
    }
    if (!Array.isArray(body.transactions)) {
        return NextResponse.json({ error: "transactions must be an array" }, { status: 400 });
    }
    if (body.transactions.length > MAX_BATCH) {
        return NextResponse.json({ error: `At most ${MAX_BATCH} transactions per request` }, { status: 413 });
    }

    const valid = body.transactions.map(validate).filter((t): t is IncomingSms => t !== null);
    const invalid = body.transactions.length - valid.length;

    try {
        await connectMongo();

        // SMS capture is tied to one mobile number per account (see /api/users/sms-phone).
        const owner = await User.findOne({ uid }).select({ smsPhone: 1 }).lean<{ smsPhone?: string }>();
        if (!owner?.smsPhone) {
            return NextResponse.json(
                { error: "Link your mobile number before importing bank SMS.", code: "PHONE_NOT_LINKED" },
                { status: 403 }
            );
        }

        // Duplicate = same SMS already imported, or same UPI/UTR reference from any source
        // (e.g. the Gmail UPI sync already captured this payment).
        const hashes = valid.map((t) => t.hash);
        const refs = valid.map((t) => t.upiRef).filter((r): r is string => Boolean(r));
        const existing = await Transaction.find({
            userId: uid,
            $or: [{ smsHash: { $in: hashes } }, ...(refs.length ? [{ upiRef: { $in: refs } }] : [])],
        })
            .select({ smsHash: 1, upiRef: 1 })
            .lean<{ smsHash?: string; upiRef?: string }[]>();
        const seenHashes = new Set(existing.map((e) => e.smsHash).filter(Boolean));
        const seenRefs = new Set(existing.map((e) => e.upiRef).filter(Boolean));

        // An SMS sits on exactly one person's phone: if another account already imported it,
        // this account is a second login on that same phone and must not get a copy.
        // (UPI refs are NOT checked across accounts — payer and payee share the same UTR.)
        const elsewhere = new Set(
            (await Transaction.find({ smsHash: { $in: hashes }, userId: { $ne: uid } })
                .select({ smsHash: 1 })
                .lean<{ smsHash?: string }[]>()).map((e) => e.smsHash)
        );

        const fresh: IncomingSms[] = [];
        let otherAccount = 0;
        for (const t of valid) {
            if (elsewhere.has(t.hash)) {
                otherAccount++;
                continue;
            }
            if (seenHashes.has(t.hash) || (t.upiRef && seenRefs.has(t.upiRef))) continue;
            seenHashes.add(t.hash); // also dedups within this batch
            if (t.upiRef) seenRefs.add(t.upiRef);
            fresh.push(t);
        }

        const categories = fresh.map(ruleCategory);
        const unknownDebits = fresh
            .map((t, i) => ({ t, i }))
            .filter(({ t, i }) => !categories[i] && t.type === "debit" && t.merchant)
            .map(({ t, i }) => ({ i, merchant: t.merchant! }));
        const ai = await aiCategories(unknownDebits);

        const docs = fresh.map((t, i) => {
            const category =
                categories[i] ?? ai.get(i) ?? (t.type === "debit" ? "Miscellaneous" : "Other Income");
            const who = t.merchant ? (t.type === "debit" ? `to ${t.merchant}` : `from ${t.merchant}`) : `via ${t.bankName}`;
            return {
                userId: uid,
                type: t.type,
                amount: t.amount,
                category: (t.type === "debit" ? DEBIT_CATEGORIES : CREDIT_CATEGORIES).includes(category as never)
                    ? category
                    : t.type === "debit" ? "Miscellaneous" : "Other Income",
                title: `${t.type === "debit" ? "Paid" : "Received"} ${who}`.slice(0, 60),
                date: new Date(t.date),
                source: "sms" as const,
                merchant: t.merchant,
                bankName: t.bankName,
                upiRef: t.upiRef,
                accountLast4: t.accountLast4,
                smsHash: t.hash,
            };
        });

        const created = docs.length ? await Transaction.insertMany(docs) : [];

        return NextResponse.json({
            imported: created.length,
            duplicates: valid.length - fresh.length - otherAccount,
            otherAccount,
            invalid,
            transactions: created.map((tx) => ({
                id: tx._id.toString(),
                amount: tx.amount,
                type: tx.type,
                category: tx.category,
                title: tx.title,
                date: tx.date,
                source: tx.source,
                merchant: tx.merchant,
                bankName: tx.bankName,
            })),
        });
    } catch (e) {
        console.error("[SmsImport] failed:", (e as Error).message.substring(0, 300));
        return NextResponse.json({ error: "Couldn't import SMS transactions." }, { status: 500 });
    }
}
