import { NextRequest, NextResponse } from "next/server";
import { connectMongo } from "@/lib/mongodb";
import { User } from "@/models/User";

export async function POST(req: NextRequest) {
    try {
        const { uid, ip, device } = await req.json();

        if (!uid) {
            return NextResponse.json({ error: "Missing uid" }, { status: 400 });
        }

        // ── Geo-lookup with a hard 2-second timeout ─────────────────────────
        // Previously this could hang for 6s+ if ip-api.com was slow/unreachable.
        let city = "Unknown";
        let country = "Unknown";
        try {
            const controller = new AbortController();
            const geoTimeout = setTimeout(() => controller.abort(), 2000); // 2s max
            const geoRes = await fetch(
                `http://ip-api.com/json/${ip}?fields=city,country,status`,
                { signal: controller.signal }
            );
            clearTimeout(geoTimeout);
            const geoData = await geoRes.json();
            if (geoData.status === "success") {
                city = geoData.city || "Unknown";
                country = geoData.country || "Unknown";
            }
        } catch {
            // Geo lookup timed out or failed — keep defaults, don't block response
        }

        const loginEvent = {
            timestamp: new Date(),
            ip: ip || "Unknown",
            city,
            country,
            device: device || "Unknown",
        };

        // ── Fire-and-forget DB write ─────────────────────────────────────────
        // We respond immediately and let MongoDB write happen asynchronously.
        // { w: 0 } means we don't wait for write acknowledgement.
        connectMongo().then(() => {
            User.findOneAndUpdate(
                { uid },
                {
                    $push: {
                        loginHistory: {
                            $each: [loginEvent],
                            $slice: -50,
                        },
                    },
                },
                { w: 0 } as Parameters<typeof User.findOneAndUpdate>[2]
            ).catch((e: Error) => console.error("[login-event] db write:", e.message));
        }).catch((e: Error) => console.error("[login-event] mongo conn:", e.message));

        // Respond instantly — client doesn't need to wait for the DB write
        return NextResponse.json({ success: true, location: { city, country } });
    } catch (err) {
        const e = err as Error;
        console.error("[users/login-event]", e);
        return NextResponse.json({ error: e.message }, { status: 500 });
    }
}
