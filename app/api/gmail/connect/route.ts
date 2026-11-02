/**
 * GET /api/gmail/connect
 *
 * Generates the Google OAuth2 consent URL and redirects the user to it.
 * The user must be authenticated (Bearer token = Firebase ID token).
 */
import { NextRequest, NextResponse } from "next/server";
import { getAuthUrl } from "@/lib/gmail";
import { verifyAuth } from "@/lib/verifyAuth";

export async function GET(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

    try {
        // Dynamically build the redirect URI from the current request's origin.
        // This works in both localhost dev and Render production without any
        // extra env variables.
        let origin = req.nextUrl.origin;
        if (req.headers.get("x-forwarded-proto") === "https" || origin.includes("onrender.com")) {
            origin = origin.replace("http://", "https://");
        }
        const redirectUri = `${origin}/api/gmail/callback`;

        const authUrl = getAuthUrl(redirectUri);
        return NextResponse.json({ authUrl });
    } catch (e) {
        const err = e as Error;
        return NextResponse.json({ error: err.message }, { status: 500 });
    }
}
