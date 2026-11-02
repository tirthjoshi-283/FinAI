"use client";

import { useState } from "react";
import { useAuth } from "@/context/AuthContext";
import Link from "next/link";
import { Eye, EyeOff, Mail, Lock, User, Phone, Loader2, Zap } from "lucide-react";

export default function RegisterPage() {
    const { signUp, signInWithGoogle } = useAuth();
    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [phone, setPhone] = useState("");
    const [password, setPassword] = useState("");
    const [showPass, setShowPass] = useState(false);
    const [loading, setLoading] = useState(false);
    const [googleLoading, setGoogleLoading] = useState(false);
    const [error, setError] = useState("");

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError("");
        if (password.length < 6) { setError("Password must be at least 6 characters."); return; }
        setLoading(true);
        try {
            await signUp(email, password, name, phone);
        } catch (error: unknown) {
            const err = error as Error & { code?: string };
            setError(friendlyError(err.code || ""));
        } finally {
            setLoading(false);
        }
    };

    const handleGoogle = async () => {
        setError("");
        setGoogleLoading(true);
        try {
            await signInWithGoogle();
        } catch (error: unknown) {
            const err = error as Error & { code?: string };
            setError(friendlyError(err.code || ""));
        } finally {
            setGoogleLoading(false);
        }
    };

    const inputStyle = {
        background: "rgba(255,255,255,0.04)",
        border: "1px solid rgba(255,255,255,0.09)",
    };
    const focusInput = (e: React.FocusEvent<HTMLInputElement>) => {
        e.currentTarget.style.border = "1px solid rgba(124,58,237,0.55)";
        e.currentTarget.style.boxShadow = "0 0 0 3px rgba(124,58,237,0.10)";
    };
    const blurInput = (e: React.FocusEvent<HTMLInputElement>) => {
        e.currentTarget.style.border = "1px solid rgba(255,255,255,0.09)";
        e.currentTarget.style.boxShadow = "none";
    };

    return (
        <div className="flex items-center justify-center min-h-screen px-4 py-16">
            <div className="w-full max-w-md">
                <div
                    className="rounded-3xl p-8"
                    style={{
                        background: "rgba(13,13,26,0.75)",
                        backdropFilter: "blur(24px)",
                        border: "1px solid rgba(124,58,237,0.20)",
                        boxShadow: "0 0 60px rgba(124,58,237,0.10), 0 0 120px rgba(34,211,238,0.04)",
                    }}
                >
                    {/* Header */}
                    <div className="mb-8 text-center">
                        <div className="w-14 h-14 rounded-2xl flex items-center justify-center mx-auto mb-4"
                            style={{ background: "linear-gradient(135deg, #7C3AED, #22D3EE)", boxShadow: "0 0 24px rgba(124,58,237,0.35)" }}>
                            <Zap className="w-7 h-7 text-white" />
                        </div>
                        <h1 className="text-3xl font-bold text-white tracking-tight mb-2">Create account</h1>
                        <p className="text-zinc-400 text-sm">Start your FinAI journey today</p>
                    </div>

                    {error && (
                        <div className="mb-6 px-4 py-3 rounded-xl text-red-400 text-sm text-center"
                            style={{ background: "rgba(239,68,68,0.08)", border: "1px solid rgba(239,68,68,0.25)" }}>
                            {error}
                        </div>
                    )}

                    {/* Google Button */}
                    <button
                        onClick={handleGoogle}
                        disabled={googleLoading}
                        className="w-full flex items-center justify-center gap-3 px-4 py-3 rounded-xl text-white text-sm font-medium transition-all mb-6 disabled:opacity-60"
                        style={{ background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.10)" }}
                        onMouseEnter={e => (e.currentTarget.style.background = "rgba(255,255,255,0.09)")}
                        onMouseLeave={e => (e.currentTarget.style.background = "rgba(255,255,255,0.05)")}
                    >
                        {googleLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : (
                            <svg className="w-4 h-4" viewBox="0 0 24 24">
                                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" />
                                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
                                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
                                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
                            </svg>
                        )}
                        Continue with Google
                    </button>

                    <div className="flex items-center gap-3 mb-6">
                        <div className="flex-1 h-px" style={{ background: "rgba(255,255,255,0.08)" }} />
                        <span className="text-zinc-600 text-xs font-medium">OR</span>
                        <div className="flex-1 h-px" style={{ background: "rgba(255,255,255,0.08)" }} />
                    </div>

                    <form onSubmit={handleSubmit} className="space-y-4">
                        {[
                            { icon: User, type: "text", value: name, setter: setName, placeholder: "Full name", required: true },
                            { icon: Mail, type: "email", value: email, setter: setEmail, placeholder: "Email address", required: true },
                            { icon: Phone, type: "tel", value: phone, setter: setPhone, placeholder: "Phone number (optional)", required: false },
                        ].map(({ icon: Icon, type, value, setter, placeholder, required }) => (
                            <div key={placeholder} className="relative">
                                <Icon className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" />
                                <input
                                    type={type}
                                    value={value}
                                    onChange={e => setter(e.target.value)}
                                    placeholder={placeholder}
                                    required={required}
                                    className="w-full rounded-xl pl-11 pr-4 py-3 text-white placeholder:text-zinc-600 text-sm outline-none transition-all"
                                    style={inputStyle}
                                    onFocus={focusInput}
                                    onBlur={blurInput}
                                />
                            </div>
                        ))}

                        <div className="relative">
                            <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" />
                            <input
                                type={showPass ? "text" : "password"}
                                value={password}
                                onChange={e => setPassword(e.target.value)}
                                placeholder="Password (min 6 characters)"
                                required
                                className="w-full rounded-xl pl-11 pr-11 py-3 text-white placeholder:text-zinc-600 text-sm outline-none transition-all"
                                style={inputStyle}
                                onFocus={focusInput}
                                onBlur={blurInput}
                            />
                            <button type="button" onClick={() => setShowPass(!showPass)}
                                className="absolute right-4 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-zinc-300 transition-colors">
                                {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                            </button>
                        </div>

                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full py-3 rounded-xl text-white font-semibold text-sm transition-all flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed hover:scale-[1.02]"
                            style={{
                                background: "linear-gradient(135deg, #7C3AED 0%, #22D3EE 100%)",
                                boxShadow: "0 0 24px rgba(124,58,237,0.40)",
                            }}
                        >
                            {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
                            Create Account
                        </button>
                    </form>

                    <p className="text-center text-zinc-500 text-sm mt-6">
                        Already have an account?{" "}
                        <Link href="/auth/login" className="font-medium transition-colors" style={{ color: "#9F67FF" }}
                            onMouseEnter={e => (e.currentTarget.style.color = "#22D3EE")}
                            onMouseLeave={e => (e.currentTarget.style.color = "#9F67FF")}>
                            Sign in
                        </Link>
                    </p>
                </div>
            </div>
        </div>
    );
}

function friendlyError(code: string): string {
    switch (code) {
        case "auth/email-already-in-use": return "This email is already registered.";
        case "auth/invalid-email": return "Please enter a valid email address.";
        case "auth/weak-password": return "Password must be at least 6 characters.";
        default: return "Something went wrong. Please try again.";
    }
}
