import { NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { User } from "@/models/User";

// Counted on every request, never prerendered at build time.
export const dynamic = "force-dynamic";

/**
 * Public landing-page stats. Returns only an aggregate count — no names,
 * emails or any other per-user data — so it needs no authentication.
 */
export async function GET() {
    try {
        await connectMongo();
        // Every account created through /api/users/sync (website or Android app); the admin is not a user.
        const users = await User.countDocuments({ role: { $ne: "admin" } });
        return NextResponse.json(
            { users },
            { headers: { "Cache-Control": "public, s-maxage=30, stale-while-revalidate=60" } }
        );
    } catch (err) {
        console.error("[stats] count failed:", err);
        return NextResponse.json({ error: "Stats unavailable" }, { status: 503 });
    }
}
