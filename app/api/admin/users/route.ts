import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { User } from "@/models/User";
import { Transaction } from "@/models/Transaction";
import { isAdminRequest } from "@/lib/verifyAuth";

export async function GET(req: NextRequest) {
    // Two accepted proofs of admin identity:
    //  1. A verified Firebase ID token belonging to ADMIN_EMAIL. Preferred, and the
    //     only one used by the Android app — a mobile client cannot hold a shared
    //     secret, since anything shipped in the APK can be extracted from it.
    //  2. The x-admin-secret header, kept so the existing web console keeps working.
    const adminSecret = req.headers.get("x-admin-secret");
    const secretMatches =
        Boolean(process.env.ADMIN_SECRET) && adminSecret === process.env.ADMIN_SECRET;

    if (!secretMatches && !(await isAdminRequest(req))) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    try {
        await connectMongo();

        const users = await User.find({}).sort({ createdAt: -1 }).lean();

        // Fetch transaction counts per user
        const txCounts = await Transaction.aggregate([
            { $group: { _id: "$userId", count: { $sum: 1 }, total: { $sum: "$amount" } } }
        ]);
        const txMap: Record<string, { count: number; total: number }> = {};

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        txCounts.forEach((t: any) => { txMap[t._id] = { count: t.count, total: t.total }; });

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const result = users.map((u: any) => ({
            ...u,
            _id: u._id.toString(),
            transactions: txMap[u.uid] || { count: 0, total: 0 },
        }));

        return NextResponse.json({ users: result });
    } catch (err) {
        const e = err as Error;
        console.error("[admin/users]", e);
        return NextResponse.json({ error: e.message }, { status: 500 });
    }
}
