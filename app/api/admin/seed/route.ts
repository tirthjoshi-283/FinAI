import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { User } from "@/models/User";
import { encrypt } from "@/lib/encryption";
import crypto from "crypto";

export async function POST(req: NextRequest) {
    const adminSecret = req.headers.get("x-admin-secret");
    if (adminSecret !== process.env.ADMIN_SECRET) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    try {
        await connectMongo();

        const adminEmail = process.env.ADMIN_EMAIL || "admin@finai.com";
        const adminPassword = process.env.ADMIN_PASSWORD || "FinAI@Admin2024";

        const existing = await User.findOne({ email: adminEmail.toLowerCase() });
        if (existing) {
            return NextResponse.json({ message: "Admin already exists", uid: existing.uid });
        }

        const uid = "admin_" + crypto.randomBytes(8).toString("hex");

        await User.create({
            uid,
            name: "Admin",
            email: adminEmail.toLowerCase(),
            phone: "",
            provider: "email",
            role: "admin",
            encryptedPassword: encrypt(adminPassword),
            loginHistory: [],
        });

        return NextResponse.json({ success: true, uid, email: adminEmail });
    } catch (err) {
        const e = err as Error;
        console.error("[admin/seed]", e);
        return NextResponse.json({ error: e.message }, { status: 500 });
    }
}
