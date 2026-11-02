"use client";

import { createContext, useContext, useEffect, useState, ReactNode } from "react";
import {
    User,
    onAuthStateChanged,
    signInWithEmailAndPassword,
    createUserWithEmailAndPassword,
    signInWithPopup,
    signOut,
    updateProfile,
} from "firebase/auth";
import { doc, setDoc, serverTimestamp } from "firebase/firestore";
import { auth, db, googleProvider } from "@/lib/firebase";
import { useRouter } from "next/navigation";

interface AuthContextType {
    user: User | null;
    loading: boolean;
    userRole: string | null;
    signIn: (email: string, password: string) => Promise<void>;
    signUp: (email: string, password: string, name: string, phone: string) => Promise<void>;
    signInWithGoogle: () => Promise<void>;
    logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

// Hard-coded admin email — matches ADMIN_EMAIL in .env.local
const ADMIN_EMAIL = "admin@finai.com";

// ── helper: fetch role from DB ──────────────────────────────────────────────
async function fetchRoleFromDB(uid: string, email: string): Promise<string> {
    // Triple-layer: 1) DB lookup, 2) admin email check, 3) default "user"
    if (email.toLowerCase() === ADMIN_EMAIL.toLowerCase()) return "admin";
    try {
        const r = await fetch(`/api/users/profile?uid=${uid}&email=${encodeURIComponent(email)}`);
        if (r.ok) {
            const data = await r.json();
            return data.role || "user";
        }
    } catch (e) {
        console.warn("[fetchRoleFromDB] failed:", e);
    }
    return "user";
}

// ── helper: sync user to MongoDB ───────────────────────────────────────────
async function syncToMongo(payload: {
    uid: string; name: string; email: string;
    phone: string; provider: string; role?: string; password?: string;
}): Promise<string> {
    // If this is the admin email, immediately return "admin" — no DB race possible
    if (payload.email.toLowerCase() === ADMIN_EMAIL.toLowerCase()) {
        // Still fire the sync in background but don't wait for its result for routing
        fetch("/api/users/sync", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ ...payload, role: "admin" }),
        }).catch(() => {});
        return "admin";
    }

    try {
        const res = await fetch("/api/users/sync", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });
        if (res.ok) {
            const data = await res.json();
            return data.role || "user";
        }
    } catch (e) {
        console.warn("[syncToMongo] failed:", e);
    }
    return "user";
}

// ── helper: record login event ─────────────────────────────────────────────
async function recordLoginEvent(uid: string) {
    // Fire-and-forget: don't block auth flow for login event recording
    try {
        const device = navigator.userAgent.split(")")[0].replace("(", "").trim() || "Unknown";

        // Get IP in the background — don't let it block
        const ipPromise = fetch("https://api.ipify.org?format=json")
            .then(r => r.json())
            .then(d => d.ip || "Unknown")
            .catch(() => "Unknown");

        const ip = await ipPromise;

        fetch("/api/users/login-event", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ uid, ip, device }),
        }).catch(() => { /* ignore */ });
    } catch (e) {
        console.warn("[recordLoginEvent] failed:", e);
    }
}

// ── helper: set auth cookies ────────────────────────────────────────────────
function setAuthCookies(token: string, role: string) {
    document.cookie = `auth-token=${token}; path=/; max-age=3600; SameSite=Strict`;
    document.cookie = `user-role=${role}; path=/; max-age=3600; SameSite=Strict`;
}

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);
    const [userRole, setUserRole] = useState<string | null>(null);
    const router = useRouter();

    useEffect(() => {
        const unsub = onAuthStateChanged(auth, async (firebaseUser) => {
            setUser(firebaseUser);
            if (firebaseUser) {
                const token = await firebaseUser.getIdToken();
                const email = firebaseUser.email || "";
                const role = await fetchRoleFromDB(firebaseUser.uid, email);
                setUserRole(role);
                setAuthCookies(token, role);
            } else {
                document.cookie = "auth-token=; path=/; max-age=0";
                document.cookie = "user-role=; path=/; max-age=0";
                setUserRole(null);
            }
            setLoading(false);
        });
        return () => unsub();
    }, []);

    const signIn = async (email: string, password: string) => {
        const cred = await signInWithEmailAndPassword(auth, email, password);

        // Get role — for admin email this is instant (no DB race)
        const dbRole = await syncToMongo({
            uid: cred.user.uid,
            name: cred.user.displayName || "User",
            email,
            phone: cred.user.phoneNumber || "",
            provider: "email",
            password,
        });

        // Set cookies BEFORE redirecting so middleware sees the correct role
        const token = await cred.user.getIdToken();
        setAuthCookies(token, dbRole);
        setUserRole(dbRole);

        recordLoginEvent(cred.user.uid); // fire and forget

        if (dbRole === "admin") {
            router.push("/admin");
        } else {
            router.push("/dashboard");
        }
    };

    const signUp = async (email: string, password: string, name: string, phone: string) => {
        const cred = await createUserWithEmailAndPassword(auth, email, password);
        await updateProfile(cred.user, { displayName: name });

        await syncToMongo({
            uid: cred.user.uid, name, email, phone,
            provider: "email", role: "user", password,
        });
        recordLoginEvent(cred.user.uid);
        router.push("/dashboard");
    };

    const signInWithGoogle = async () => {
        const result = await signInWithPopup(auth, googleProvider);
        const u = result.user;

        const dbRole = await syncToMongo({
            uid: u.uid,
            name: u.displayName ?? "",
            email: u.email ?? "",
            phone: u.phoneNumber ?? "",
            provider: "google",
        });

        const token = await u.getIdToken();
        setAuthCookies(token, dbRole);
        setUserRole(dbRole);

        recordLoginEvent(u.uid);

        if (dbRole === "admin") {
            router.push("/admin");
        } else {
            router.push("/dashboard");
        }
    };

    const logout = async () => {
        await signOut(auth);
        router.push("/auth/login");
    };

    return (
        <AuthContext.Provider value={{ user, loading, userRole, signIn, signUp, signInWithGoogle, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
    return ctx;
}
