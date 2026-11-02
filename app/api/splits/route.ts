import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { Split } from "@/models/Split";
import { Transaction } from "@/models/Transaction";
import { verifyAuth } from "@/lib/verifyAuth";
import { cleanPeople, splitToDto } from "@/lib/splits";

/** GET all splits; POST {transactionId, people:[{name, amount}]} creates or replaces the split for a debit. */

export async function GET(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    await connectMongo();
    const rows = await Split.find({ userId: uid }).sort({ createdAt: -1 }).lean();
    return NextResponse.json({ splits: rows.map((r) => splitToDto(r as never)) });
}

export async function POST(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    const body = await req.json().catch(() => ({}));
    await connectMongo();

    const tx = await Transaction.findOne({ _id: body.transactionId, userId: uid, type: "debit" })
        .select({ title: 1, amount: 1 })
        .lean<{ _id: { toString(): string }; title: string; amount: number }>()
        .catch(() => null);
    if (!tx) return NextResponse.json({ error: "Only your own expenses can be split." }, { status: 404 });

    const people = cleanPeople(body.people, tx.amount);
    if (typeof people === "string") return NextResponse.json({ error: people }, { status: 400 });

    const split = await Split.findOneAndUpdate(
        { userId: uid, transactionId: tx._id.toString() },
        { $set: { title: tx.title, total: tx.amount, people } },
        { upsert: true, new: true }
    ).lean();
    return NextResponse.json({ split: splitToDto(split as never) }, { status: 201 });
}
