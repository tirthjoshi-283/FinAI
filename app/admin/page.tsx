"use client";

import React, { useEffect, useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";
import {
    Shield, Users, LogOut, Loader2, Crown, Mail,
    Globe, Clock, Smartphone, ChevronDown, ChevronUp,
    TrendingUp, Lock, RefreshCw, Database
} from "lucide-react";

interface LoginEvent {
    timestamp: string;
    ip: string;
    city: string;
    country: string;
    device: string;
}

interface MongoUser {
    uid: string;
    name: string;
    email: string;
    phone: string;
    provider: "email" | "google";
    role: "user" | "admin";
    encryptedPassword: string;
    loginHistory: LoginEvent[];
    transactions: { count: number; total: number };
    createdAt: string;
    updatedAt: string;
}

export default function AdminPage() {
    const { user, userRole, logout, loading } = useAuth();
    const router = useRouter();
    const [users, setUsers] = useState<MongoUser[]>([]);
    const [fetching, setFetching] = useState(true);
    const [expandedUid, setExpandedUid] = useState<string | null>(null);
    const [error, setError] = useState("");

    const fetchUsers = async () => {
        setFetching(true);
        setError("");
        try {
            const res = await fetch("/api/admin/users", {
                headers: { "x-admin-secret": process.env.NEXT_PUBLIC_ADMIN_SECRET || "finai_admin_secret_x9k2m7p4" },
            });
            if (!res.ok) throw new Error("Failed to fetch users");
            const data = await res.json();
            setUsers(data.users || []);
        } catch (err) {
            const e = err as Error;
            setError(e.message || "Failed to load users");
        } finally {
            setFetching(false);
        }
    };

    useEffect(() => {
        if (!loading) {
            if (!user) { router.push("/auth/login"); return; }
            if (userRole !== "admin") { router.push("/dashboard"); return; }
            fetchUsers();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [user, userRole, loading]);

    const fmtDate = (ts: string) => {
        if (!ts) return "—";
        return new Date(ts).toLocaleDateString("en-IN", {
            day: "numeric", month: "short", year: "numeric",
            hour: "2-digit", minute: "2-digit",
        });
    };

    const lastLogin = (u: MongoUser) => {
        if (!u.loginHistory?.length) return "Never";
        const last = u.loginHistory[u.loginHistory.length - 1];
        return fmtDate(last.timestamp);
    };

    const lastLocation = (u: MongoUser) => {
        if (!u.loginHistory?.length) return "—";
        const last = u.loginHistory[u.loginHistory.length - 1];
        if (!last.city && !last.country) return "—";
        return [last.city, last.country].filter(Boolean).join(", ");
    };

    const maskPassword = (enc: string) => {
        if (!enc) return "—";
        return enc.substring(0, 20) + "…[AES-256]";
    };

    if (loading || fetching) {
        return (
            <div className="flex flex-col items-center justify-center min-h-screen gap-3">
                <Loader2 className="w-8 h-8 text-orange-500 animate-spin" />
                <span className="text-zinc-500 text-sm">Loading admin data…</span>
            </div>
        );
    }

    const admins = users.filter(u => u.role === "admin");
    const googleUsers = users.filter(u => u.provider === "google");
    const activeToday = users.filter(u => {
        if (!u.loginHistory?.length) return false;
        const last = new Date(u.loginHistory[u.loginHistory.length - 1].timestamp);
        return Date.now() - last.getTime() < 86400000;
    });

    return (
        <div className="min-h-screen bg-[#080808] text-white">
            {/* ── Top Bar ── */}
            <div className="sticky top-0 z-20 bg-[#080808]/80 backdrop-blur-xl border-b border-white/[0.06] px-6 lg:px-10 py-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-xl bg-orange-500/20 border border-orange-500/30 flex items-center justify-center">
                        <Shield className="w-4 h-4 text-orange-500" />
                    </div>
                    <div>
                        <h1 className="text-lg font-bold text-white leading-none">FinAI Admin</h1>
                        <p className="text-zinc-600 text-xs mt-0.5">User Management Console</p>
                    </div>
                </div>
                <div className="flex items-center gap-3">
                    <button
                        onClick={fetchUsers}
                        className="flex items-center gap-2 px-3 py-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-zinc-400 hover:text-white text-xs font-medium transition-all"
                    >
                        <RefreshCw className="w-3.5 h-3.5" /> Refresh
                    </button>
                    <button
                        onClick={logout}
                        className="flex items-center gap-2 px-3 py-2 rounded-xl bg-red-500/10 hover:bg-red-500/20 border border-red-500/20 text-red-400 text-xs font-medium transition-all"
                    >
                        <LogOut className="w-3.5 h-3.5" /> Logout
                    </button>
                </div>
            </div>

            <div className="px-6 lg:px-10 py-8 max-w-[1600px] mx-auto">
                {/* ── Error ── */}
                {error && (
                    <div className="mb-6 px-4 py-3 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 text-sm">
                        ⚠ {error} — Make sure your MongoDB URI is set in .env.local
                    </div>
                )}

                {/* ── Stats Grid ── */}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
                    {[
                        { label: "Total Users", value: users.length, icon: Users, color: "orange" },
                        { label: "Admins", value: admins.length, icon: Crown, color: "yellow" },
                        { label: "Google Sign-Ins", value: googleUsers.length, icon: Globe, color: "blue" },
                        { label: "Active Today", value: activeToday.length, icon: TrendingUp, color: "green" },
                    ].map((s, i) => (
                        <div
                            key={i}
                            className="relative overflow-hidden bg-white/[0.025] border border-white/[0.07] rounded-2xl p-5 hover:border-white/15 transition-colors"
                        >
                            <div className={`w-9 h-9 rounded-xl flex items-center justify-center mb-3 ${s.color === "orange" ? "bg-orange-500/15 border border-orange-500/25" :
                                s.color === "yellow" ? "bg-yellow-500/15 border border-yellow-500/25" :
                                    s.color === "blue" ? "bg-blue-500/15 border border-blue-500/25" :
                                        "bg-green-500/15 border border-green-500/25"
                                }`}>
                                <s.icon className={`w-4 h-4 ${s.color === "orange" ? "text-orange-400" :
                                    s.color === "yellow" ? "text-yellow-400" :
                                        s.color === "blue" ? "text-blue-400" :
                                            "text-green-400"
                                    }`} />
                            </div>
                            <p className="text-3xl font-bold text-white">{s.value}</p>
                            <p className="text-zinc-500 text-xs mt-1">{s.label}</p>
                        </div>
                    ))}
                </div>

                {/* ── Users Table ── */}
                <div className="bg-white/[0.025] border border-white/[0.07] rounded-2xl overflow-hidden">
                    <div className="px-6 py-4 border-b border-white/[0.07] flex items-center justify-between">
                        <div className="flex items-center gap-2">
                            <Database className="w-4 h-4 text-orange-500" />
                            <h2 className="font-semibold text-white text-sm">All Registered Users</h2>
                            <span className="ml-2 px-2 py-0.5 rounded-full bg-orange-500/15 text-orange-400 text-xs font-medium border border-orange-500/20">
                                {users.length}
                            </span>
                        </div>
                    </div>

                    <div className="overflow-x-auto">
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="border-b border-white/[0.05]">
                                    {["User", "Provider", "Phone", "Last Login", "Location", "Logins", "Transactions", "Encrypted Password", "Role", "Details"].map(h => (
                                        <th key={h} className="px-5 py-3.5 text-left text-[11px] font-semibold text-zinc-600 uppercase tracking-wider whitespace-nowrap">
                                            {h}
                                        </th>
                                    ))}
                                </tr>
                            </thead>
                            <tbody>
                                {users.map((u) => (
                                    <React.Fragment key={u.uid}>
                                        <tr
                                            className="border-b border-white/[0.04] hover:bg-white/[0.02] transition-colors"
                                        >
                                            {/* User */}
                                            <td className="px-5 py-4">
                                                <div className="flex items-center gap-3">
                                                    <div className="w-8 h-8 rounded-full bg-orange-500/20 border border-orange-500/30 flex items-center justify-center text-orange-400 text-xs font-bold flex-shrink-0">
                                                        {(u.name || u.email || "?")[0].toUpperCase()}
                                                    </div>
                                                    <div className="min-w-0">
                                                        <p className="text-white font-medium text-sm truncate max-w-[140px]">{u.name || "—"}</p>
                                                        <p className="text-zinc-500 text-xs truncate max-w-[140px]">
                                                            {u.email ? (() => {
                                                                const [local, domain] = u.email.split("@");
                                                                if (!domain || local.length <= 3) return u.email;
                                                                const start = local.substring(0, 3);
                                                                const end = local.slice(-3);
                                                                return `${start}***${end}@${domain}`;
                                                            })() : "—"}
                                                        </p>
                                                    </div>
                                                </div>
                                            </td>

                                            {/* Provider */}
                                            <td className="px-5 py-4">
                                                <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border ${u.provider === "google"
                                                    ? "bg-blue-500/10 text-blue-400 border-blue-500/20"
                                                    : "bg-white/5 text-zinc-400 border-white/10"
                                                    }`}>
                                                    {u.provider === "google" ? (
                                                        <svg className="w-3 h-3" viewBox="0 0 24 24">
                                                            <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" />
                                                            <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
                                                            <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
                                                            <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
                                                        </svg>
                                                    ) : <Mail className="w-3 h-3" />}
                                                    {u.provider}
                                                </span>
                                            </td>

                                            {/* Phone */}
                                            <td className="px-5 py-4 text-zinc-400 whitespace-nowrap text-xs">{u.phone || "—"}</td>

                                            {/* Last Login */}
                                            <td className="px-5 py-4 whitespace-nowrap">
                                                <div className="flex items-center gap-1.5 text-zinc-400 text-xs">
                                                    <Clock className="w-3 h-3 text-zinc-600 flex-shrink-0" />
                                                    {lastLogin(u)}
                                                </div>
                                            </td>

                                            {/* Location */}
                                            <td className="px-5 py-4 whitespace-nowrap">
                                                <div className="flex items-center gap-1.5 text-zinc-400 text-xs">
                                                    <Globe className="w-3 h-3 text-zinc-600 flex-shrink-0" />
                                                    {lastLocation(u)}
                                                </div>
                                            </td>

                                            {/* Login Count */}
                                            <td className="px-5 py-4">
                                                <span className="inline-flex items-center justify-center w-7 h-7 rounded-lg bg-white/5 border border-white/10 text-white text-xs font-bold">
                                                    {u.loginHistory?.length || 0}
                                                </span>
                                            </td>

                                            {/* Transactions */}
                                            <td className="px-5 py-4">
                                                <div className="text-xs">
                                                    <p className="text-white font-medium">{u.transactions?.count || 0} txns</p>
                                                    <p className="text-zinc-500">₹{(u.transactions?.total || 0).toLocaleString("en-IN")}</p>
                                                </div>
                                            </td>

                                            {/* Encrypted Password */}
                                            <td className="px-5 py-4">
                                                <div className="flex items-center gap-1.5">
                                                    <Lock className="w-3 h-3 text-zinc-600 flex-shrink-0" />
                                                    <span className="text-zinc-600 font-mono text-[10px] max-w-[120px] truncate">
                                                        {u.provider === "google" ? "Google Auth" : maskPassword(u.encryptedPassword)}
                                                    </span>
                                                </div>
                                            </td>

                                            {/* Role */}
                                            <td className="px-5 py-4">
                                                <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium border ${u.role === "admin"
                                                    ? "bg-orange-500/15 text-orange-400 border-orange-500/25"
                                                    : "bg-white/5 text-zinc-500 border-white/10"
                                                    }`}>
                                                    {u.role === "admin" && <Crown className="w-3 h-3 mr-1" />}
                                                    {u.role}
                                                </span>
                                            </td>

                                            {/* Expand */}
                                            <td className="px-5 py-4">
                                                <button
                                                    onClick={() => setExpandedUid(expandedUid === u.uid ? null : u.uid)}
                                                    className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-white/5 hover:bg-orange-500/10 hover:text-orange-400 hover:border-orange-500/30 border border-white/10 text-zinc-400 text-xs font-medium transition-all"
                                                >
                                                    {expandedUid === u.uid ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
                                                    History
                                                </button>
                                            </td>
                                        </tr>

                                        {/* ── Expanded Login History ── */}
                                        {expandedUid === u.uid && (
                                            <tr key={`${u.uid}-hist`} className="bg-orange-500/[0.03] border-b border-orange-500/10">
                                                <td colSpan={10} className="px-6 py-4">
                                                    <p className="text-xs font-semibold text-orange-400 uppercase tracking-wider mb-3">
                                                        Login History ({u.loginHistory?.length || 0} sessions)
                                                    </p>
                                                    {!u.loginHistory?.length ? (
                                                        <p className="text-zinc-600 text-xs">No login history recorded.</p>
                                                    ) : (
                                                        <div className="space-y-2 max-h-48 overflow-y-auto pr-2">
                                                            {[...u.loginHistory].reverse().map((ev, idx) => (
                                                                <div key={idx} className="flex flex-wrap items-center gap-4 px-4 py-2.5 rounded-xl bg-white/[0.03] border border-white/[0.05] text-xs">
                                                                    <span className="flex items-center gap-1.5 text-zinc-400">
                                                                        <Clock className="w-3 h-3 text-zinc-600" />
                                                                        {fmtDate(ev.timestamp)}
                                                                    </span>
                                                                    <span className="flex items-center gap-1.5 text-zinc-400">
                                                                        <Globe className="w-3 h-3 text-zinc-600" />
                                                                        {[ev.city, ev.country].filter(Boolean).join(", ") || "Unknown"}
                                                                    </span>
                                                                    <span className="flex items-center gap-1.5 text-zinc-500 font-mono">
                                                                        IP: {ev.ip || "—"}
                                                                    </span>
                                                                    <span className="flex items-center gap-1.5 text-zinc-500">
                                                                        <Smartphone className="w-3 h-3 text-zinc-600" />
                                                                        <span className="max-w-[200px] truncate">{ev.device || "—"}</span>
                                                                    </span>
                                                                </div>
                                                            ))}
                                                        </div>
                                                    )}
                                                </td>
                                            </tr>
                                        )}
                                    </React.Fragment>
                                ))}
                            </tbody>
                        </table>

                        {users.length === 0 && !error && (
                            <div className="text-center py-16">
                                <Database className="w-10 h-10 text-zinc-700 mx-auto mb-3" />
                                <p className="text-zinc-600 text-sm">No users in MongoDB yet.</p>
                                <p className="text-zinc-700 text-xs mt-1">Users will appear here after they register or log in.</p>
                            </div>
                        )}
                    </div>
                </div>

                {/* ── Footer ── */}
                <p className="text-center text-zinc-700 text-xs mt-8">
                    FinAI Admin Console · MongoDB · AES-256 Encrypted · Restricted Access
                </p>
            </div>
        </div>
    );
}
