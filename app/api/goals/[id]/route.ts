import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { Goal } from "@/models/Goal";
import { verifyAuth } from "@/lib/verifyAuth";
import { cleanEmoji, goalToDto, parseAmount, parseDeadline } from "@/lib/goals";

/**
 * PATCH any of:
 *  - amountToAdd: add (positive) or withdraw (negative) funds; the balance never goes below 0
 *  - title, targetAmount, emoji, deadline (null clears it)
 */
export async function PATCH(
    req: NextRequest,
    { params }: { params: { id: string } }
) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

    try {
        const { id } = params;
        const body = await req.json();
        if (!id) return NextResponse.json({ error: "Missing ID" }, { status: 400 });

        await connectMongo();
        const goal = await Goal.findOne({ _id: id, userId: uid });
        if (!goal) {
            return NextResponse.json({ error: "Goal not found or not owned by user" }, { status: 404 });
        }

        let changed = false;
        if (body.amountToAdd !== undefined) {
            const raw = Number(body.amountToAdd);
            const amount = parseAmount(Math.abs(raw));
            if (amount === null) return NextResponse.json({ error: "Enter a valid amount." }, { status: 400 });
            goal.currentAmount = Math.max(0, Math.round((goal.currentAmount + Math.sign(raw) * amount) * 100) / 100);
            changed = true;
        }
        if (body.title !== undefined) {
            const title = typeof body.title === "string" ? body.title.trim().slice(0, 60) : "";
            if (!title) return NextResponse.json({ error: "Title can't be empty." }, { status: 400 });
            goal.title = title;
            changed = true;
        }
        if (body.targetAmount !== undefined) {
            const target = parseAmount(body.targetAmount);
            if (target === null) return NextResponse.json({ error: "Enter a target greater than 0." }, { status: 400 });
            goal.targetAmount = target;
            changed = true;
        }
        if (body.emoji !== undefined) {
            goal.emoji = cleanEmoji(body.emoji);
            changed = true;
        }
        const deadline = parseDeadline(body.deadline);
        if (deadline === "invalid") return NextResponse.json({ error: "Pick a target date in the future." }, { status: 400 });
        if (deadline !== undefined) {
            goal.deadline = deadline ?? undefined;
            changed = true;
        }
        if (!changed) return NextResponse.json({ error: "Nothing to update" }, { status: 400 });

        await goal.save();
        return NextResponse.json({ goal: goalToDto(goal) });
    } catch (e) {
        const err = e as Error;
        return NextResponse.json({ error: err.message }, { status: 500 });
    }
}

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

        const goal = await Goal.findOneAndDelete({ _id: id, userId: uid });

        if (!goal) {
            return NextResponse.json({ error: "Goal not found or not owned by user" }, { status: 404 });
        }

        return NextResponse.json({ success: true, id: id });
    } catch (e) {
        const err = e as Error;
        return NextResponse.json({ error: err.message }, { status: 500 });
    }
}
