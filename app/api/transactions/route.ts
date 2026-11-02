import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { Transaction } from "@/models/Transaction";
import { verifyAuth } from "@/lib/verifyAuth";

export async function GET(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

    try {
        await connectMongo();
        const transactions = await Transaction.find({ userId: uid }).sort({ date: -1 }).lean();

        // Map _id to id — include Gmail UPI metadata so the frontend can use them in charts/badges
        type TransactionContext = {
            _id: { toString: () => string };
            amount: number;
            type: string;
            category: string;
            title: string;
            date: Date;
            source?: string;
            bankName?: string;
            merchant?: string;
            upiRef?: string;
            accountLast4?: string;
            note?: string;
        };
        const mapped = transactions.map((t: TransactionContext) => ({
            id: t._id.toString(),
            amount: t.amount,
            type: t.type,
            category: t.category,
            title: t.title,
            date: t.date,
            source: t.source ?? "manual",
            bankName: t.bankName,
            merchant: t.merchant,
            upiRef: t.upiRef,
            accountLast4: t.accountLast4,
            note: t.note,
        }));

        return NextResponse.json(
            { transactions: mapped },
            {
                headers: {
                    // Private (per-user) data: browser may cache briefly but must revalidate
                    "Cache-Control": "private, max-age=0, must-revalidate",
                },
            }
        );
    } catch (e) {
        const err = e as Error;
        return NextResponse.json({ error: err.message }, { status: 500 });
    }
}

export async function POST(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

    try {
        const body = await req.json();
        const { amount, type, category, title, date } = body;

        if (!amount || !type || !category || !title || !date) {
            return NextResponse.json({ error: "Missing required fields" }, { status: 400 });
        }

        await connectMongo();
        const tx = await Transaction.create({
            userId: uid,
            amount,
            type,
            category,
            title,
            date: new Date(date),
        });

        return NextResponse.json({
            transaction: {
                id: tx._id.toString(),
                amount: tx.amount,
                type: tx.type,
                category: tx.category,
                title: tx.title,
                date: tx.date,
            }
        }, { status: 201 });
    } catch (e) {
        const err = e as Error;
        return NextResponse.json({ error: err.message }, { status: 500 });
    }
}
