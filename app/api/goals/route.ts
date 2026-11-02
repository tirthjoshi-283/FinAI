import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { Goal } from "@/models/Goal";
import { verifyAuth } from "@/lib/verifyAuth";
import { cleanEmoji, goalToDto, parseAmount, parseDeadline } from "@/lib/goals";

export async function GET(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

    try {
        await connectMongo();
        const goals = await Goal.find({ userId: uid }).sort({ createdAt: -1 }).lean();

        return NextResponse.json(
            { goals: goals.map((g) => goalToDto(g as never)) },
            {
                headers: {
                    "Cache-Control": "private, max-age=0, must-revalidate",
                },
            }
        );
    } catch (e) {
        const err = e as Error;
        return NextResponse.json({ error: err.message }, { status: 500 });
    }
}

/** POST {title, targetAmount, emoji?, deadline?} */
export async function POST(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

    try {
        const body = await req.json();
        const title = typeof body.title === "string" ? body.title.trim().slice(0, 60) : "";
        const targetAmount = parseAmount(body.targetAmount);
        const deadline = parseDeadline(body.deadline);

        if (!title || targetAmount === null) {
            return NextResponse.json({ error: "Give the goal a name and a target greater than 0." }, { status: 400 });
        }
        if (deadline === "invalid") {
            return NextResponse.json({ error: "Pick a target date in the future." }, { status: 400 });
        }

        await connectMongo();
        const newGoal = await Goal.create({
            userId: uid,
            title,
            targetAmount,
            currentAmount: 0,
            emoji: cleanEmoji(body.emoji),
            deadline: deadline ?? undefined,
        });

        return NextResponse.json({ goal: goalToDto(newGoal) }, { status: 201 });
    } catch (e) {
        const err = e as Error;
        return NextResponse.json({ error: err.message }, { status: 500 });
    }
}
