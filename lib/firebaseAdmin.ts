import { cert, getApp, getApps, initializeApp, App } from "firebase-admin/app";
import { getAuth, Auth } from "firebase-admin/auth";

// verifyIdToken() only needs the project ID: it checks tokens against Google's public
// certificates. Service-account credentials are optional and only used if present AND valid,
// so a missing or badly pasted private key can never break the build or login checks.
function buildApp(): App {
    if (getApps().length > 0) return getApp();

    const projectId = process.env.FIREBASE_ADMIN_PROJECT_ID || process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID;
    if (!projectId) {
        throw new Error("Firebase project ID missing. Set NEXT_PUBLIC_FIREBASE_PROJECT_ID (or FIREBASE_ADMIN_PROJECT_ID).");
    }

    const clientEmail = process.env.FIREBASE_ADMIN_CLIENT_EMAIL;
    // Env files store the PEM key with literal "\n" sequences; restore real newlines and strip stray quotes.
    const privateKey = process.env.FIREBASE_ADMIN_PRIVATE_KEY
        ?.replace(/\\n/g, "\n")
        .replace(/^["']|["']$/g, "")
        .trim();

    if (clientEmail && privateKey) {
        try {
            return initializeApp({ projectId, credential: cert({ projectId, clientEmail, privateKey }) });
        } catch (err) {
            console.warn("[firebaseAdmin] Ignoring invalid service-account credentials:", (err as Error).message);
        }
    }
    return initializeApp({ projectId });
}

let auth: Auth | null = null;

/** Lazily initialised so importing this module never fails at build time. */
export function getAdminAuth(): Auth {
    if (!auth) auth = getAuth(buildApp());
    return auth;
}
