import { NextRequest, NextResponse } from "next/server";
import { extractText, getDocumentProxy } from "unpdf";
import { connectMongo } from "@/lib/mongodb";
import { Transaction } from "@/models/Transaction";
import { verifyAuth } from "@/lib/verifyAuth";
import { groqChat, GroqError, GroqMessage, GROQ_VISION_MODEL } from "@/lib/groq";
import { DEBIT_CATEGORIES, DEBIT_CATEGORY_GUIDE } from "@/lib/categories";

export const runtime = "nodejs";

const MAX_FILE_BYTES = 4 * 1024 * 1024; // Groq caps base64 images at 4 MB
const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];
const PDF_TYPE = "application/pdf";

const SYSTEM_PROMPT = `You are a bill and receipt reader for an Indian personal finance app.
Read the document (restaurant bill, shopping invoice, tax receipt, utility bill, ticket, pharmacy bill, etc.) and return ONLY a JSON object:
{
  "isBill": true | false,           // false if this is not a bill/receipt/invoice/payment proof
  "merchant": "<shop / hotel / company / authority name>",
  "title": "<short description, max 40 chars, e.g. 'Dinner at Hotel Sagar' or 'Nike shoes'>",
  "amount": <final amount paid as a number, i.e. GRAND TOTAL / NET PAYABLE including taxes; no currency symbol>,
  "date": "<bill date as YYYY-MM-DD, or empty string if not visible>",
  "category": "<one of: ${DEBIT_CATEGORIES.join(", ")}>",
  "summary": "<one sentence listing the main items purchased>"
}

Category rules:
${DEBIT_CATEGORY_GUIDE}

Rules:
- amount must be the total actually paid, not a subtotal or a single line item.
- Indian bills often write dates as DD/MM/YYYY; convert correctly.
- If you cannot find a total amount, set "isBill" to false.`;

type Extracted = {
    isBill?: boolean;
    merchant?: string;
    title?: string;
    amount?: number | string;
    date?: string;
    category?: string;
    summary?: string;
};

async function readPdfText(bytes: Uint8Array): Promise<string> {
    const pdf = await getDocumentProxy(bytes);
    const { text } = await extractText(pdf, { mergePages: true });
    return text.replace(/\s+\n/g, "\n").trim();
}

function parseBillDate(value: string | undefined): Date {
    const now = new Date();
    if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return now;
    const d = new Date(`${value}T12:00:00`);
    // Reject unreadable dates and obvious misreads (future, or older than 5 years)
    if (isNaN(d.getTime()) || d > now || now.getFullYear() - d.getFullYear() > 5) return now;
    return d;
}

export async function POST(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

    if (!process.env.GROQ_API_KEY) {
        return NextResponse.json({ error: "GROQ_API_KEY is not configured." }, { status: 500 });
    }

    let file: File | null = null;
    try {
        const form = await req.formData();
        const value = form.get("file");
        if (value instanceof File) file = value;
    } catch {
        /* handled below */
    }
    if (!file) return NextResponse.json({ error: "No file uploaded." }, { status: 400 });

    const isImage = IMAGE_TYPES.includes(file.type);
    const isPdf = file.type === PDF_TYPE;
    if (!isImage && !isPdf) {
        return NextResponse.json(
            { error: "Unsupported file type. Upload a JPG, PNG, WEBP image or a PDF." },
            { status: 415 }
        );
    }
    if (file.size > MAX_FILE_BYTES) {
        return NextResponse.json({ error: "File is too large (max 4 MB)." }, { status: 413 });
    }

    try {
        const bytes = new Uint8Array(await file.arrayBuffer());
        let messages: GroqMessage[];
        let model: string | undefined;

        if (isImage) {
            const dataUrl = `data:${file.type};base64,${Buffer.from(bytes).toString("base64")}`;
            model = GROQ_VISION_MODEL;
            messages = [
                { role: "system", content: SYSTEM_PROMPT },
                {
                    role: "user",
                    content: [
                        { type: "text", text: "Extract this bill." },
                        { type: "image_url", image_url: { url: dataUrl } },
                    ],
                },
            ];
        } else {
            const text = await readPdfText(bytes);
            if (text.length < 20) {
                return NextResponse.json(
                    { error: "This PDF looks like a scanned image with no readable text. Upload a photo or screenshot of it instead." },
                    { status: 422 }
                );
            }
            messages = [
                { role: "system", content: SYSTEM_PROMPT },
                { role: "user", content: `Bill text (extracted from PDF "${file.name}"):\n\n${text.slice(0, 12000)}` },
            ];
        }

        const raw = await groqChat(messages, { json: true, temperature: 0.1, model }, "BillScan");
        const data = JSON.parse(raw.replace(/```json|```/g, "").trim()) as Extracted;

        const amount = Math.round(parseFloat(String(data.amount ?? "").replace(/[^0-9.]/g, "")) * 100) / 100;
        if (data.isBill === false || !amount || amount <= 0) {
            return NextResponse.json(
                { error: "Couldn't find a bill total in this file. Try a clearer photo, or add it manually." },
                { status: 422 }
            );
        }

        const category = (DEBIT_CATEGORIES as readonly string[]).includes(data.category ?? "")
            ? data.category!
            : "Miscellaneous";
        const merchant = (data.merchant ?? "").trim().slice(0, 80);
        const title = ((data.title ?? "").trim() || merchant || file.name).slice(0, 60);

        await connectMongo();
        const tx = await Transaction.create({
            userId: uid,
            type: "debit",
            amount,
            category,
            title,
            merchant: merchant || undefined,
            date: parseBillDate(data.date),
            source: "bill_scan",
        });

        return NextResponse.json(
            {
                transaction: {
                    id: tx._id.toString(),
                    amount: tx.amount,
                    type: tx.type,
                    category: tx.category,
                    title: tx.title,
                    date: tx.date,
                    source: tx.source,
                    merchant: tx.merchant,
                },
                summary: (data.summary ?? "").slice(0, 200),
            },
            { status: 201 }
        );
    } catch (e) {
        const err = e as Error;
        console.error("[BillScan] failed:", err.message.substring(0, 300));
        if (err instanceof GroqError && err.status === 429) {
            return NextResponse.json({ error: "AI is busy (rate limit). Wait a minute and try again." }, { status: 429 });
        }
        return NextResponse.json(
            { error: `Couldn't process this file: ${err.message.substring(0, 160)}` },
            { status: 500 }
        );
    }
}
