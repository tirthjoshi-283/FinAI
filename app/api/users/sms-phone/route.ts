import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { User } from "@/models/User";
import { verifyAuth } from "@/lib/verifyAuth";

/**
 * The mobile number whose bank SMS this account imports. Bank-SMS capture is refused
 * until one is linked, and a number can belong to only one account — so several FinAI
 * accounts signed in on the same phone can't each import the same messages.
 */

/** Accepts 9876543210, 09876543210, 919876543210, +91 98765-43210 → "+919876543210". */
function normaliseIndianMobile(raw: unknown): string | null {
    if (typeof raw !== "string") return null;
    let digits = raw.replace(/[\s()-]/g, "");
    if (digits.startsWith("+91")) digits = digits.slice(3);
    else if (digits.startsWith("91") && digits.length === 12) digits = digits.slice(2);
    else if (digits.startsWith("0") && digits.length === 11) digits = digits.slice(1);
    return /^[6-9]\d{9}$/.test(digits) ? `+91${digits}` : null;
}

export async function GET(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    await connectMongo();
    const user = await User.findOne({ uid }).select({ smsPhone: 1 }).lean<{ smsPhone?: string }>();
    return NextResponse.json({ phone: user?.smsPhone ?? null });
}

export async function PUT(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

    const body = await req.json().catch(() => ({}));
    const phone = normaliseIndianMobile(body.phone);
    if (!phone) {
        return NextResponse.json({ error: "Enter a valid 10-digit Indian mobile number." }, { status: 400 });
    }

    try {
        await connectMongo();
        const taken = await User.exists({ smsPhone: phone, uid: { $ne: uid } });
        if (taken) {
            return NextResponse.json(
                { error: "This number is already linked to another FinAI account. Unlink it there first." },
                { status: 409 }
            );
        }
        const user = await User.findOneAndUpdate({ uid }, { $set: { smsPhone: phone } }, { new: true });
        if (!user) return NextResponse.json({ error: "Account not found. Sign out and sign in again." }, { status: 404 });
        return NextResponse.json({ phone });
    } catch (e) {
        // Unique index race: someone linked the same number a moment ago.
        if ((e as { code?: number }).code === 11000) {
            return NextResponse.json(
                { error: "This number is already linked to another FinAI account." },
                { status: 409 }
            );
        }
        return NextResponse.json({ error: "Couldn't link the number." }, { status: 500 });
    }
}

export async function DELETE(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    await connectMongo();
    await User.updateOne({ uid }, { $unset: { smsPhone: 1 } });
    return NextResponse.json({ phone: null });
}
