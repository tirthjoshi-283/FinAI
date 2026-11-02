import { NextRequest } from "next/server";
import { getAdminAuth } from "@/lib/firebaseAdmin";

export interface VerifiedCaller {
    uid: string;
    email: string;
}

/**
 * Resolves the verified caller from a "Bearer <idToken>" header.
 * Returns null when the token is absent, malformed, expired or forged.
 */
export async function verifyCaller(req: NextRequest): Promise<VerifiedCaller | null> {
    const authHeader = req.headers.get("authorization");
    if (!authHeader?.startsWith("Bearer ")) return null;

    const idToken = authHeader.slice("Bearer ".length).trim();
    if (!idToken) return null;

    try {
        const decoded = await getAdminAuth().verifyIdToken(idToken);
        return { uid: decoded.uid, email: (decoded.email ?? "").toLowerCase() };
    } catch (err) {
        console.error("[verifyAuth] token rejected:", (err as Error).message);
        return null;
    }
}

/**
 * Resolves the caller's Firebase UID from a "Bearer <idToken>" header.
 * Returns null when the token is absent, malformed, expired or forged.
 */
export async function verifyAuth(req: NextRequest): Promise<string | null> {
    return (await verifyCaller(req))?.uid ?? null;
}

/**
 * True when the request carries a valid ID token belonging to the configured
 * admin account. Used so trusted clients (e.g. the Android app) can reach admin
 * endpoints with the signed-in user's own token instead of a shared secret that
 * would have to be embedded in the client.
 */
export async function isAdminRequest(req: NextRequest): Promise<boolean> {
    const caller = await verifyCaller(req);
    if (!caller?.email) return false;

    const adminEmail = process.env.ADMIN_EMAIL?.toLowerCase();
    return Boolean(adminEmail) && caller.email === adminEmail;
}
