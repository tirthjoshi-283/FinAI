import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { User } from "@/models/User";

export async function GET(req: NextRequest) {
    try {
        const uid = req.nextUrl.searchParams.get("uid");
        const email = req.nextUrl.searchParams.get("email");

        if (!uid && !email) return NextResponse.json({ error: "Missing uid or email" }, { status: 400 });

        await connectMongo();

        let user;
        if (email) {
            user = await User.findOne({ email: email.toLowerCase() }).select("role name email");
        }
        if (!user && uid) {
            user = await User.findOne({ uid }).select("role name email");
        }

        if (!user) return NextResponse.json({ error: "User not found" }, { status: 404 });

        return NextResponse.json({ role: user.role, name: user.name, email: user.email });
    } catch (e: any /* eslint-disable-line @typescript-eslint/no-explicit-any */) {
        return NextResponse.json({ error: e.message }, { status: 500 });
    }
}
