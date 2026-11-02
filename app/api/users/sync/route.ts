import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { User } from "@/models/User";
import { encrypt } from "@/lib/encryption";

export async function POST(req: NextRequest) {
    try {
        const { uid, name, email, phone, provider, role, password } = await req.json();

        if (!uid || !email) {
            return NextResponse.json({ error: "Missing required fields" }, { status: 400 });
        }

        await connectMongo();

        const encryptedPassword = provider === "email" ? encrypt(password || "") : "";
        
        const adminEmail = process.env.ADMIN_EMAIL?.toLowerCase() || "admin@finai.com";
        const isSuperAdmin = email.toLowerCase() === adminEmail;

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const updateDoc: any = {
            $setOnInsert: { 
                createdAt: new Date(),
                role: isSuperAdmin ? "admin" : (role || "user")
            },
            $set: {
                uid,
                name: name || "",
                phone: phone || "",
                provider: provider || "email",
                encryptedPassword,
            },
        };

        // If this is the hardcoded super admin, always force their role to "admin" during login
        // to recover from any accidental downgrades.
        if (isSuperAdmin) {
            updateDoc.$set.role = "admin";
        }

        // Upsert — update if exists, insert if not. Search by EMAIL so we catch legacy or seeded accounts.
        const updatedUser = await User.findOneAndUpdate(
            { email: email.toLowerCase() },
            updateDoc,
            { upsert: true, new: true }
        );

        return NextResponse.json({ success: true, role: updatedUser.role });
    } catch (err) {
        const e = err as Error;
        console.error("[users/sync]", e);
        return NextResponse.json({ error: e.message }, { status: 500 });
    }
}
