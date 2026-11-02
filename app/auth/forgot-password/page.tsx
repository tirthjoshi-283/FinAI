"use client";

import { useState } from "react";
import Link from "next/link";
import { Mail, Phone, Loader2, KeyRound, Eye, EyeOff, X } from "lucide-react";

export default function ForgotPasswordPage() {
    const [email, setEmail] = useState("");
    const [phone, setPhone] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [popup, setPopup] = useState<{ name: string; password: string } | null>(null);
    const [showPass, setShowPass] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError("");
        setLoading(true);
        try {
            const res = await fetch("/api/users/forgot-password", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, phone }),
            });
            const data = await res.json();
            if (!res.ok) {
                setError(data.error || "Something went wrong.");
            } else {
                setPopup({ name: data.name, password: data.password });
            }
        } catch {
            setError("Network error. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex items-center justify-center min-h-screen px-4">
            {/* ── Forgot Password Card ── */}
            <div className="w-full max-w-md">
                <div className="bg-black/60 backdrop-blur-xl border border-white/10 rounded-3xl p-8 shadow-[0_0_60px_rgba(249,115,22,0.08)]">
                    <div className="mb-8 text-center">
                        <div className="w-14 h-14 rounded-2xl bg-orange-500/15 border border-orange-500/25 flex items-center justify-center mx-auto mb-4">
                            <KeyRound className="w-7 h-7 text-orange-400" />
                        </div>
                        <h1 className="text-3xl font-bold text-white tracking-tight mb-2">
                            Forgot Password
                        </h1>
                        <p className="text-zinc-400 text-sm">
                            Enter your registered email &amp; phone to retrieve your password.
                        </p>
                    </div>

                    {error && (
                        <div className="mb-6 px-4 py-3 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 text-sm text-center">
                            {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div className="relative">
                            <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" />
                            <input
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="Registered email address"
                                required
                                className="w-full bg-white/5 border border-white/10 rounded-xl pl-11 pr-4 py-3 text-white placeholder:text-zinc-600 text-sm outline-none focus:border-orange-500/50 transition-all"
                            />
                        </div>

                        <div className="relative">
                            <Phone className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" />
                            <input
                                type="tel"
                                value={phone}
                                onChange={(e) => setPhone(e.target.value)}
                                placeholder="Registered phone number"
                                className="w-full bg-white/5 border border-white/10 rounded-xl pl-11 pr-4 py-3 text-white placeholder:text-zinc-600 text-sm outline-none focus:border-orange-500/50 transition-all"
                            />
                        </div>

                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full py-3 rounded-xl bg-orange-500 hover:bg-orange-400 text-white font-semibold text-sm transition-all shadow-[0_0_20px_rgba(249,115,22,0.3)] hover:shadow-[0_0_30px_rgba(249,115,22,0.5)] flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed"
                        >
                            {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
                            Retrieve My Password
                        </button>
                    </form>

                    <p className="text-center text-zinc-500 text-sm mt-6">
                        Remember it?{" "}
                        <Link href="/auth/login" className="text-orange-400 hover:text-orange-300 font-medium transition-colors">
                            Sign In
                        </Link>
                    </p>
                </div>
            </div>

            {/* ── Password Popup Modal ── */}
            {popup && (
                <div
                    className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm px-4"
                    onClick={() => setPopup(null)}
                >
                    <div
                        className="relative w-full max-w-sm bg-[#0d0d0d] border border-orange-500/30 rounded-3xl p-8 shadow-[0_0_80px_rgba(249,115,22,0.2)] animate-in fade-in zoom-in-95"
                        onClick={(e) => e.stopPropagation()}
                    >
                        {/* Close */}
                        <button
                            onClick={() => setPopup(null)}
                            className="absolute top-4 right-4 text-zinc-500 hover:text-white transition-colors"
                        >
                            <X className="w-5 h-5" />
                        </button>

                        {/* Icon */}
                        <div className="w-14 h-14 rounded-2xl bg-orange-500/15 border border-orange-500/25 flex items-center justify-center mx-auto mb-5">
                            <KeyRound className="w-6 h-6 text-orange-400" />
                        </div>

                        <div className="text-center mb-6">
                            <h2 className="text-xl font-bold text-white mb-1">Password Found!</h2>
                            <p className="text-zinc-500 text-sm">
                                Hi <span className="text-orange-400 font-medium">{popup.name || "there"}</span>, here is your password.
                            </p>
                        </div>

                        {/* Password display */}
                        <div className="relative">
                            <div className="w-full bg-white/5 border border-orange-500/30 rounded-xl px-4 py-3 text-white font-mono text-sm tracking-wide pr-12 break-all">
                                {showPass ? popup.password : "•".repeat(popup.password.length)}
                            </div>
                            <button
                                onClick={() => setShowPass(!showPass)}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-orange-400 transition-colors"
                            >
                                {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                            </button>
                        </div>

                        <p className="text-center text-zinc-600 text-xs mt-4">
                            Keep this safe. This popup will close when you click outside.
                        </p>

                        <Link
                            href="/auth/login"
                            onClick={() => setPopup(null)}
                            className="mt-5 w-full flex items-center justify-center py-3 rounded-xl bg-orange-500 hover:bg-orange-400 text-white font-semibold text-sm transition-all shadow-[0_0_20px_rgba(249,115,22,0.3)]"
                        >
                            Go to Sign In
                        </Link>
                    </div>
                </div>
            )}
        </div>
    );
}
