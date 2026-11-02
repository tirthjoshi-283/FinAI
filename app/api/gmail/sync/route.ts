/**
 * POST /api/gmail/sync
 *
 * Fetches new UPI emails from Gmail, parses them, and saves as Transactions.
 * - Fully idempotent: won't create duplicates (gmailMessageId dedup).
 * - Returns summary of synced/skipped/failed emails.
 *
 * Auth: Bearer <firebase_id_token>
 */
import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { User } from "@/models/User";
import { Transaction } from "@/models/Transaction";
import { fetchUpiEmails } from "@/lib/gmail";
import { parseUpiEmail } from "@/lib/upiParser";
import { verifyAuth } from "@/lib/verifyAuth";

export async function POST(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

    try {
        await connectMongo();

        // 1. Fetch user and check Gmail is connected
        const user = await User.findOne({ uid });
        if (!user) {
            return NextResponse.json({ error: "User not found" }, { status: 404 });
        }
        if (!user.gmailSync?.connected) {
            return NextResponse.json({ error: "Gmail not connected" }, { status: 400 });
        }

        const alreadySyncedIds = new Set<string>(user.gmailSync.syncedMessageIds ?? []);

        // 2. Fetch new emails (already-synced filtered inside fetchUpiEmails)
        const emails = await fetchUpiEmails(uid, 50, alreadySyncedIds);

        if (emails.length === 0) {
            return NextResponse.json({
                synced: 0,
                skipped: 0,
                failed: 0,
                message: "No new UPI emails found.",
                transactions: [],
            });
        }

        // 3. Parse each email
        const results = await Promise.allSettled(emails.map((e) => parseUpiEmail(e)));

        const synced: string[] = [];
        const newTransactions = [];
        let failed = 0;
        let skipped = 0;

        for (let i = 0; i < results.length; i++) {
            const result = results[i];
            const email = emails[i];

            if (result.status === "rejected" || !result.value) {
                // Could not parse — skip silently
                skipped++;
                // Still mark as seen so we don't retry forever
                synced.push(email.messageId);
                continue;
            }

            const parsed = result.value;

            // 4. Dedup check at DB level (belt-and-suspenders)
            const exists = await Transaction.exists({
                userId: uid,
                gmailMessageId: email.messageId,
            });

            if (exists) {
                skipped++;
                synced.push(email.messageId);
                continue;
            }

            // 5. Save transaction
            try {
                const tx = await Transaction.create({
                    userId: uid,
                    type: parsed.type,
                    amount: parsed.amount,
                    category: parsed.category,
                    title: parsed.title,
                    date: parsed.date,
                    source: "gmail_upi",
                    gmailMessageId: email.messageId,
                    merchant: parsed.merchant || undefined,
                    upiRef: parsed.upiRef || undefined,
                    bankName: parsed.bankName || undefined,
                });

                newTransactions.push({
                    id: tx._id.toString(),
                    type: tx.type,
                    amount: tx.amount,
                    category: tx.category,
                    title: tx.title,
                    date: tx.date,
                    bankName: parsed.bankName,
                    merchant: parsed.merchant,
                });

                synced.push(email.messageId);
            } catch {
                failed++;
                // Still push messageId to avoid infinite retries on bad records
                synced.push(email.messageId);
            }
        }

        // 6. Update user's syncedMessageIds and lastSyncAt atomically
        if (synced.length > 0) {
            await User.findOneAndUpdate(
                { uid },
                {
                    $addToSet: { "gmailSync.syncedMessageIds": { $each: synced } },
                    $set: { "gmailSync.lastSyncAt": new Date() },
                }
            );
        }

        const actualSynced = newTransactions.length;

        return NextResponse.json({
            synced: actualSynced,
            skipped,
            failed,
            message: actualSynced > 0
                ? `Successfully imported ${actualSynced} new UPI transaction${actualSynced > 1 ? "s" : ""}.`
                : "No new parseable UPI transactions found.",
            transactions: newTransactions,
        });
    } catch (e) {
        const err = e as Error;
        console.error("Gmail sync error:", err.message);
        return NextResponse.json({ error: err.message }, { status: 500 });
    }
}
