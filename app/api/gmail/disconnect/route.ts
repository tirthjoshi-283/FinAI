/**
 * DELETE /api/gmail/disconnect
 *
 * Disconnects Gmail for the current user:
 * - Revokes the access token at Google's server.
 * - Clears all gmailSync data from MongoDB.
 *
 * Auth: Bearer <firebase_id_token>
 */
import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { User } from "@/models/User";
import { getOAuthClient, getAuthClientForUser } from "@/lib/gmail";
import { verifyAuth } from "@/lib/verifyAuth";

export async function DELETE(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

    try {
        await connectMongo();

        // Best-effort: try to revoke token at Google
        try {
            const auth = await getAuthClientForUser(uid);
            const credentials = auth.credentials;
            if (credentials.access_token) {
                const oauth2Client = getOAuthClient();
                await oauth2Client.revokeToken(credentials.access_token);
            }
        } catch {
            // If revocation fails (expired token, etc.), still proceed to clear from DB
        }

        // Clear all gmailSync data from MongoDB
        await User.findOneAndUpdate(
            { uid },
            {
                $set: {
                    "gmailSync.connected": false,
                    "gmailSync.accessToken": "",
                    "gmailSync.refreshToken": "",
                    "gmailSync.tokenExpiry": null,
                    "gmailSync.lastSyncAt": null,
                    "gmailSync.syncedMessageIds": [],
                },
            }
        );

        return NextResponse.json({ success: true, message: "Gmail disconnected successfully." });
    } catch (e) {
        const err = e as Error;
        return NextResponse.json({ error: err.message }, { status: 500 });
    }
}
