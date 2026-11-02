/**
 * lib/gmail.ts
 *
 * Gmail API client module for UPI email fetching.
 * Handles OAuth2 token refresh, safe email fetching, and body decoding.
 * ONLY reads emails from verified Indian bank/UPI sender addresses.
 */

import { google } from "googleapis";
import { connectMongo } from "@/lib/mongodb";
import { User } from "@/models/User";
import { encrypt, decrypt } from "@/lib/encryption";

// ─── Verified sender whitelist ────────────────────────────────────────────────
// ONLY emails from these exact sender addresses are processed.
// This prevents any accidental reads from unrelated emails.
export const ALLOWED_SENDERS: string[] = [
    // HDFC Bank
    "alerts@hdfcbank.net",
    "alerts@hdfcbank.com",
    "noreply@hdfcbank.com",
    // ICICI Bank
    "alerts@icicibank.com",
    "info@icicibank.com",
    // SBI
    "sbialerts@sbi.co.in",
    "alerts@sbi.co.in",
    // Axis Bank
    "alerts@axisbank.com",
    "noreply@axisbank.com",
    // Kotak Bank
    "alerts@kotak.com",
    "noreply@kotak.com",
    // Bank of Baroda
    "alerts@bankofbaroda.com",
    // Punjab National Bank
    "alerts@pnb.co.in",
    // IndusInd Bank
    "alerts@indusind.com",
    // Yes Bank
    "alerts@yesbank.in",
    // IDFC First Bank
    "alerts@idfcfirstbank.com",
    // Paytm Payments Bank
    "alerts@paytmbank.com",
    "noreply@paytm.com",
    // PhonePe
    "noreply@phonepe.com",
    "alerts@phonepe.com",
    // Google Pay (GPay)
    "noreply@google.com",
    "googleplay-noreply@google.com",
    // Amazon Pay
    "no-reply@amazon.in",
    // Airtel Payments Bank
    "alerts@airtel.com",
    // Federal Bank
    "alerts@federalbank.co.in",
];

// Build the Gmail search query dynamically from the whitelist
// Format: `from:(sender1 OR sender2 OR ...)` — Gmail's official syntax
function buildGmailQuery(): string {
    const senderList = ALLOWED_SENDERS.map((s) => `from:${s}`).join(" OR ");
    return `(${senderList}) -in:spam -in:trash`;
}

// ─── OAuth2 Client ────────────────────────────────────────────────────────────

/**
 * Returns an OAuth2 client.
 * @param redirectUri  - The redirect URI to use (dynamically determined from request host).
 *                       Falls back to GOOGLE_REDIRECT_URI env var if not provided.
 */
export function getOAuthClient(redirectUri?: string) {
    return new google.auth.OAuth2(
        process.env.GOOGLE_CLIENT_ID,
        process.env.GOOGLE_CLIENT_SECRET,
        redirectUri ?? process.env.GOOGLE_REDIRECT_URI
    );
}

/**
 * Generates the Google OAuth consent URL.
 * Requests only gmail.readonly — no write access ever.
 * @param redirectUri - The redirect URI to embed in the auth URL (must match Google Cloud config).
 */
export function getAuthUrl(redirectUri: string): string {
    const oauth2Client = getOAuthClient(redirectUri);
    return oauth2Client.generateAuthUrl({
        access_type: "offline",
        prompt: "consent",          // Always show consent + get refresh_token
        scope: ["https://www.googleapis.com/auth/gmail.readonly"],
    });
}

// ─── Token Management ─────────────────────────────────────────────────────────

/**
 * Saves OAuth tokens for a user (encrypted) after the OAuth callback.
 */
export async function saveTokens(
    uid: string,
    accessToken: string,
    refreshToken: string,
    expiry: Date | null
) {
    await connectMongo();
    await User.findOneAndUpdate(
        { uid },
        {
            $set: {
                "gmailSync.connected": true,
                "gmailSync.accessToken": encrypt(accessToken),
                "gmailSync.refreshToken": encrypt(refreshToken),
                "gmailSync.tokenExpiry": expiry,
            },
        }
    );
}

/**
 * Returns a fresh, valid OAuth2 client for a given user.
 * Automatically refreshes the access token if expired.
 */
export async function getAuthClientForUser(uid: string) {
    await connectMongo();
    const user = await User.findOne({ uid }).lean();
    if (!user || !user.gmailSync?.connected) {
        throw new Error("Gmail not connected for this user.");
    }

    const oauth2Client = getOAuthClient();
    const decryptedRefresh = decrypt(user.gmailSync.refreshToken);
    const decryptedAccess = decrypt(user.gmailSync.accessToken);

    oauth2Client.setCredentials({
        access_token: decryptedAccess,
        refresh_token: decryptedRefresh,
        expiry_date: user.gmailSync.tokenExpiry
            ? new Date(user.gmailSync.tokenExpiry).getTime()
            : undefined,
    });

    // Listen for token refresh and persist the new access token
    oauth2Client.on("tokens", async (tokens) => {
        const updateData: Record<string, string | Date> = {};
        if (tokens.access_token) {
            updateData["gmailSync.accessToken"] = encrypt(tokens.access_token);
        }
        if (tokens.expiry_date) {
            updateData["gmailSync.tokenExpiry"] = new Date(tokens.expiry_date);
        }
        if (Object.keys(updateData).length > 0) {
            await User.findOneAndUpdate({ uid }, { $set: updateData });
        }
    });

    return oauth2Client;
}

// ─── Email Fetching ───────────────────────────────────────────────────────────

export interface RawEmailData {
    messageId: string;
    subject: string;
    from: string;
    body: string;        // Plain text body
    internalDate: Date;
}

/**
 * Fetches UPI-related emails from whitelisted senders only.
 * @param uid - Firebase UID of the user
 * @param maxResults - max emails to fetch per sync (default 50)
 * @param alreadySyncedIds - set of message IDs already in DB (for early skip)
 */
export async function fetchUpiEmails(
    uid: string,
    maxResults = 50,
    alreadySyncedIds: Set<string> = new Set()
): Promise<RawEmailData[]> {
    const auth = await getAuthClientForUser(uid);
    const gmail = google.gmail({ version: "v1", auth });

    const query = buildGmailQuery();

    // Step 1: Get list of matching message IDs
    const listRes = await gmail.users.messages.list({
        userId: "me",
        q: query,
        maxResults,
    });

    const messages = listRes.data.messages ?? [];
    if (messages.length === 0) return [];

    // Step 2: Filter out already-synced messages
    const newMessages = messages.filter(
        (m) => m.id && !alreadySyncedIds.has(m.id)
    );
    if (newMessages.length === 0) return [];

    // Step 3: Fetch full content for each new message (in parallel, batches of 10)
    const results: RawEmailData[] = [];
    const BATCH_SIZE = 10;

    for (let i = 0; i < newMessages.length; i += BATCH_SIZE) {
        const batch = newMessages.slice(i, i + BATCH_SIZE);
        const fetched = await Promise.allSettled(
            batch.map(async (msg) => {
                if (!msg.id) return null;
                const full = await gmail.users.messages.get({
                    userId: "me",
                    id: msg.id,
                    format: "full",
                });

                const headers = full.data.payload?.headers ?? [];
                const subject = headers.find((h) => h.name === "Subject")?.value ?? "";
                const from = headers.find((h) => h.name === "From")?.value ?? "";

                // Security check: verify the sender is actually whitelisted
                // Gmail query filters by sender but we double-check here
                const fromEmail = extractEmail(from);
                if (!ALLOWED_SENDERS.some((s) => s.toLowerCase() === fromEmail.toLowerCase())) {
                    return null; // Skip non-whitelisted senders
                }

                type BodyPayload = { mimeType?: string | null; body?: { data?: string | null } | null; parts?: unknown[] | null };
                const body = extractBody(full.data.payload as BodyPayload | null | undefined);
                const internalDate = full.data.internalDate
                    ? new Date(parseInt(full.data.internalDate))
                    : new Date();

                return {
                    messageId: msg.id,
                    subject,
                    from,
                    body,
                    internalDate,
                } as RawEmailData;
            })
        );

        for (const r of fetched) {
            if (r.status === "fulfilled" && r.value) {
                results.push(r.value);
            }
        }
    }

    return results;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Extracts plain email address from a "Name <email>" string */
function extractEmail(from: string): string {
    const match = from.match(/<([^>]+)>/);
    return match ? match[1].trim() : from.trim();
}

/** Recursively extracts plain-text body from a Gmail message part */
function extractBody(payload: { mimeType?: string | null; body?: { data?: string | null } | null; parts?: unknown[] | null } | null | undefined): string {
    if (!payload) return "";

    // Direct plain-text body
    if (payload.mimeType === "text/plain" && payload.body?.data) {
        return Buffer.from(payload.body.data, "base64url").toString("utf-8");
    }

    // Multipart: recurse through parts
    if (payload.parts) {
        for (const part of payload.parts) {
            const text = extractBody(part as { mimeType?: string | null; body?: { data?: string | null } | null; parts?: unknown[] | null });
            if (text) return text;
        }
    }

    // Fallback: try body data directly
    if (payload.body?.data) {
        return Buffer.from(payload.body.data, "base64url").toString("utf-8");
    }

    return "";
}
