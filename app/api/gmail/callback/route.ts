/**
 * GET /api/gmail/callback
 *
 * OAuth2 redirect handler. Google sends `code` and `state` here after user consents.
 * - Exchanges code for access + refresh tokens.
 * - Encrypts and stores tokens in MongoDB.
 * - Redirects back to the dashboard transactions page.
 *
 * NOTE: `state` param carries the Firebase UID, set by the client when initiating the flow.
 */
import { NextRequest, NextResponse } from "next/server";
import { getOAuthClient, saveTokens } from "@/lib/gmail";

export async function GET(req: NextRequest) {
    const { searchParams } = new URL(req.url);
    const code = searchParams.get("code");
    const uid = searchParams.get("state"); // Firebase UID passed as state

    if (!code || !uid) {
        return NextResponse.redirect(
            new URL("/dashboard/transactions?gmailError=missing_params", req.url)
        );
    }

    try {
        // Reconstruct the EXACT same redirect URI that was used in /api/gmail/connect.
        // Google requires redirect_uri to match at token exchange time.
        let origin = req.nextUrl.origin;
        if (req.headers.get("x-forwarded-proto") === "https" || origin.includes("onrender.com")) {
            origin = origin.replace("http://", "https://");
        }
        const redirectUri = `${origin}/api/gmail/callback`;

        const oauth2Client = getOAuthClient(redirectUri);
        const { tokens } = await oauth2Client.getToken(code);

        if (!tokens.access_token || !tokens.refresh_token) {
            return NextResponse.redirect(
                new URL("/dashboard/transactions?gmailError=no_refresh_token", req.url)
            );
        }

        const expiry = tokens.expiry_date ? new Date(tokens.expiry_date) : null;

        await saveTokens(
            uid,
            tokens.access_token,
            tokens.refresh_token,
            expiry
        );

        return NextResponse.redirect(
            new URL("/dashboard/transactions?gmailConnected=true", req.url)
        );
    } catch (e) {
        const err = e as Error;
        console.error("Gmail OAuth callback error:", err.message);
        return NextResponse.redirect(
            new URL(
                `/dashboard/transactions?gmailError=${encodeURIComponent(err.message)}`,
                req.url
            )
        );
    }
}
