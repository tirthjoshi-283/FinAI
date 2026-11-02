import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { User } from "@/models/User";
import { decrypt } from "@/lib/encryption";

export async function POST(req: NextRequest) {
    try {
        const { email, phone } = await req.json();

        if (!email) {
            return NextResponse.json({ error: "Email is required" }, { status: 400 });
        }

        await connectMongo();

        const user = await User.findOne({ email: email.toLowerCase() });

        if (!user) {
            return NextResponse.json(
                { error: "No account registered with this email." },
                { status: 404 }
            );
        }

        // Validate phone if provided and user has one stored
        if (user.phone && phone) {
            const normalizedStored = user.phone.replace(/\D/g, "");
            const normalizedInput = (phone as string).replace(/\D/g, "");
            if (normalizedStored !== normalizedInput) {
                return NextResponse.json(
                    { error: "The details you provided do not match our records." },
                    { status: 401 }
                );
            }
        }

        // Google users don't have a stored password
        if (user.provider === "google") {
            return NextResponse.json(
                { error: "This account uses Google Sign-In. No password is stored." },
                { status: 400 }
            );
        }

        const plainPassword = decrypt(user.encryptedPassword);

        if (!plainPassword) {
            return NextResponse.json(
                { error: "Could not retrieve password. Please contact support." },
                { status: 500 }
            );
        }

        return NextResponse.json({
            success: true,
            name: user.name,
            password: plainPassword,
        });
    } catch (err) {
        const e = err as Error;
        console.error("[forgot-password]", e);
        return NextResponse.json({ error: e.message }, { status: 500 });
    }
}
