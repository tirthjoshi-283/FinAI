import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { Split } from "@/models/Split";
import { verifyAuth } from "@/lib/verifyAuth";
import { splitToDto } from "@/lib/splits";

/** PATCH {name, settled} marks one friend as paid/unpaid; DELETE removes the split. */

export async function PATCH(req: NextRequest, { params }: { params: { id: string } }) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    const { name, settled } = await req.json().catch(() => ({}));
    if (typeof name !== "string" || typeof settled !== "boolean") {
        return NextResponse.json({ error: "name and settled are required" }, { status: 400 });
    }
    await connectMongo();
    const split = await Split.findOneAndUpdate(
        { _id: params.id, userId: uid, "people.name": name },
        { $set: { "people.$.settled": settled } },
        { new: true }
    ).lean().catch(() => null);
    if (!split) return NextResponse.json({ error: "Split not found" }, { status: 404 });
    return NextResponse.json({ split: splitToDto(split as never) });
}

export async function DELETE(req: NextRequest, { params }: { params: { id: string } }) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    await connectMongo();
    const r = await Split.deleteOne({ _id: params.id, userId: uid }).catch(() => null);
    if (!r?.deletedCount) return NextResponse.json({ error: "Split not found" }, { status: 404 });
    return NextResponse.json({ success: true });
}
