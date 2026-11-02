/**
 * GET /api/gmail/status
 *
 * Returns the Gmail sync status for the current user.
 * Used by the UI to show connect/sync state.
 *
 * Auth: Bearer <firebase_id_token>
 */
import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { User } from "@/models/User";
import { verifyAuth } from "@/lib/verifyAuth";

export async function GET(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

    try {
        await connectMongo();
        const user = await User.findOne({ uid }).lean();

        if (!user) {
            return NextResponse.json({ error: "User not found" }, { status: 404 });
        }

        const gmailSync = user.gmailSync;

        return NextResponse.json({
            connected: gmailSync?.connected ?? false,
            lastSyncAt: gmailSync?.lastSyncAt ?? null,
            syncedCount: gmailSync?.syncedMessageIds?.length ?? 0,
        });
    } catch (e) {
        const err = e as Error;
        return NextResponse.json({ error: err.message }, { status: 500 });
    }
}
