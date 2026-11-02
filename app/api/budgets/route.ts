import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { Budget } from "@/models/Budget";
import { verifyAuth } from "@/lib/verifyAuth";
import { DEBIT_CATEGORIES } from "@/lib/categories";

/** Monthly category budgets: GET all, PUT {category, limit} to set, DELETE ?category= to remove. */

export async function GET(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    await connectMongo();
    const rows = await Budget.find({ userId: uid }).sort({ category: 1 }).lean<{ category: string; limit: number }[]>();
    return NextResponse.json({ budgets: rows.map((b) => ({ category: b.category, limit: b.limit })) });
}

export async function PUT(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    const { category, limit } = await req.json().catch(() => ({}));
    if (!(DEBIT_CATEGORIES as readonly string[]).includes(category)) {
        return NextResponse.json({ error: "Pick a spending category." }, { status: 400 });
    }
    const value = Math.round(Number(limit));
    if (!(value >= 1 && value <= 100_000_000)) {
        return NextResponse.json({ error: "Enter a monthly limit greater than 0." }, { status: 400 });
    }
    await connectMongo();
    await Budget.updateOne({ userId: uid, category }, { $set: { limit: value } }, { upsert: true });
    return NextResponse.json({ budget: { category, limit: value } });
}

export async function DELETE(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    const category = new URL(req.url).searchParams.get("category");
    if (!category) return NextResponse.json({ error: "category is required" }, { status: 400 });
    await connectMongo();
    await Budget.deleteOne({ userId: uid, category });
    return NextResponse.json({ success: true });
}
