import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { Transaction } from "@/models/Transaction";
import { verifyAuth } from "@/lib/verifyAuth";
import { CREDIT_CATEGORIES, DEBIT_CATEGORIES } from "@/lib/categories";

export async function DELETE(
    req: NextRequest,
    { params }: { params: { id: string } }
) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

    try {
        const { id } = params;
        if (!id) return NextResponse.json({ error: "Missing ID" }, { status: 400 });

        await connectMongo();

        // Ensure the transaction belongs to the user
        const tx = await Transaction.findOneAndDelete({ _id: id, userId: uid });

        if (!tx) {
            return NextResponse.json({ error: "Transaction not found or not owned by user" }, { status: 404 });
        }

        return NextResponse.json({ success: true, id: id });
    } catch (e) {
        const err = e as Error;
        return NextResponse.json({ error: err.message }, { status: 500 });
    }
}

// Editable fields: category (e.g. correcting an AI-scanned bill), title, and a personal note.
export async function PATCH(
    req: NextRequest,
    { params }: { params: { id: string } }
) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

    try {
        const { category, title, note } = await req.json();
        const set: Record<string, string> = {};
        const unset: Record<string, 1> = {};

        if (category !== undefined) {
            const allowed: readonly string[] = [...DEBIT_CATEGORIES, ...CREDIT_CATEGORIES];
            if (typeof category !== "string" || !allowed.includes(category)) {
                return NextResponse.json({ error: "Invalid category" }, { status: 400 });
            }
            set.category = category;
        }
        if (title !== undefined) {
            if (typeof title !== "string" || !title.trim()) {
                return NextResponse.json({ error: "Title can't be empty" }, { status: 400 });
            }
            set.title = title.trim().slice(0, 80);
        }
        if (note !== undefined) {
            if (note !== null && typeof note !== "string") {
                return NextResponse.json({ error: "Invalid note" }, { status: 400 });
            }
            const clean = (note ?? "").trim().slice(0, 300);
            if (clean) set.note = clean;
            else unset.note = 1;
        }
        if (!Object.keys(set).length && !Object.keys(unset).length) {
            return NextResponse.json({ error: "Nothing to update" }, { status: 400 });
        }

        await connectMongo();
        const tx = await Transaction.findOneAndUpdate(
            { _id: params.id, userId: uid },
            { ...(Object.keys(set).length ? { $set: set } : {}), ...(Object.keys(unset).length ? { $unset: unset } : {}) },
            { new: true }
        );
        if (!tx) {
            return NextResponse.json({ error: "Transaction not found or not owned by user" }, { status: 404 });
        }

        return NextResponse.json({ success: true, id: params.id, category: tx.category, title: tx.title, note: tx.note ?? null });
    } catch (e) {
        const err = e as Error;
        return NextResponse.json({ error: err.message }, { status: 500 });
    }
}
