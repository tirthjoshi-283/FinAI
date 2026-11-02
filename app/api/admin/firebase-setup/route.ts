import { NextRequest, NextResponse } from "next/server";

export async function POST(req: NextRequest) {
    const adminSecret = req.headers.get("x-admin-secret");
    if (adminSecret !== process.env.ADMIN_SECRET) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const adminEmail = process.env.ADMIN_EMAIL || "admin@finai.com";
    const adminPassword = process.env.ADMIN_PASSWORD || "FinAI@Admin2024";
    const apiKey = process.env.NEXT_PUBLIC_FIREBASE_API_KEY!;

    try {
        let uid = "";
        let idToken = "";

        // Step 1: Create user via Firebase REST API
        const createRes = await fetch(
            `https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${apiKey}`,
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    email: adminEmail,
                    password: adminPassword,
                    returnSecureToken: true,
                }),
            }
        );
        const createData = await createRes.json();

        if (createData.error) {
            // If already exists, log in to get the UID and ID Token
            if (createData.error.message === "EMAIL_EXISTS") {
                const loginRes = await fetch(
                    `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${apiKey}`,
                    {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({
                            email: adminEmail,
                            password: adminPassword,
                            returnSecureToken: true,
                        }),
                    }
                );
                const loginData = await loginRes.json();
                if (loginData.error) {
                    return NextResponse.json({ error: loginData.error.message }, { status: 400 });
                }
                uid = loginData.localId;
                idToken = loginData.idToken;
            } else {
                return NextResponse.json({ error: createData.error.message }, { status: 400 });
            }
        } else {
            uid = createData.localId;
            idToken = createData.idToken;
        }

        // Step 2: Write admin doc to Firestore REST API (run for both new and existing)
        const projectId = process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID!;
        await fetch(
            `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/users/${uid}?updateMask.fieldPaths=role&updateMask.fieldPaths=name`,
            {
                method: "PATCH",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${idToken}`,
                },
                body: JSON.stringify({
                    fields: {
                        role: { stringValue: "admin" },
                        name: { stringValue: "Admin" },
                    },
                }),
            }
        );

        // Also ensure full document exists using a separate PATCH without updateMask
        // to avoid wiping out other fields if they exist, but setting defaults if they don't
        await fetch(
            `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/users/${uid}?updateMask.fieldPaths=role&updateMask.fieldPaths=email&updateMask.fieldPaths=uid`,
            {
                method: "PATCH",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${idToken}`,
                },
                body: JSON.stringify({
                    fields: {
                        uid: { stringValue: uid },
                        email: { stringValue: adminEmail },
                        role: { stringValue: "admin" },
                    },
                }),
            }
        );

        return NextResponse.json({ success: true, uid, email: adminEmail });
    } catch (err) {
        const e = err as Error;
        console.error("[firebase-setup]", e);
        return NextResponse.json({ error: e.message }, { status: 500 });
    }
}
