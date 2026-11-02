"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import {
    ArrowLeft, Zap, TrendingUp, Shield, Brain, Activity,
    Smartphone, Chrome, Globe, Star, ArrowRight, Sparkles,
} from "lucide-react";

/* ── mini animated counter hook ── */
function useCounter(target: number, duration = 2000) {
    const [val, setVal] = useState(0);
    useEffect(() => {
        let start = 0;
        const step = target / (duration / 16);
        const timer = setInterval(() => {
            start += step;
            if (start >= target) { setVal(target); clearInterval(timer); }
            else setVal(Math.floor(start));
        }, 16);
        return () => clearInterval(timer);
    }, [target, duration]);
    return val;
}

/* ── floating orb canvas ── */
function ParticleCanvas() {
    const canvasRef = useRef<HTMLCanvasElement>(null);
    useEffect(() => {
        const canvas = canvasRef.current!;
        const ctx = canvas.getContext("2d")!;
        let raf: number;
        const resize = () => { canvas.width = canvas.offsetWidth; canvas.height = canvas.offsetHeight; };
        resize();
        window.addEventListener("resize", resize);

        const orbs = Array.from({ length: 28 }, () => ({
            x: Math.random() * canvas.width, y: Math.random() * canvas.height,
            r: Math.random() * 2 + 0.5,
            vx: (Math.random() - 0.5) * 0.4, vy: (Math.random() - 0.5) * 0.4,
            color: Math.random() > 0.5 ? "124,58,237" : "34,211,238",
            opacity: Math.random() * 0.5 + 0.2,
        }));

        const draw = () => {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            orbs.forEach(o => {
                o.x += o.vx; o.y += o.vy;
                if (o.x < 0 || o.x > canvas.width)  o.vx *= -1;
                if (o.y < 0 || o.y > canvas.height) o.vy *= -1;
                ctx.beginPath();
                ctx.arc(o.x, o.y, o.r, 0, Math.PI * 2);
                ctx.fillStyle = `rgba(${o.color},${o.opacity})`;
                ctx.fill();
            });
            // draw thin connecting lines between close orbs
            for (let i = 0; i < orbs.length; i++) {
                for (let j = i + 1; j < orbs.length; j++) {
                    const dx = orbs[i].x - orbs[j].x, dy = orbs[i].y - orbs[j].y;
                    const dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < 120) {
                        ctx.beginPath();
                        ctx.moveTo(orbs[i].x, orbs[i].y);
                        ctx.lineTo(orbs[j].x, orbs[j].y);
                        ctx.strokeStyle = `rgba(124,58,237,${0.12 * (1 - dist / 120)})`;
                        ctx.lineWidth = 0.5;
                        ctx.stroke();
                    }
                }
            }
            raf = requestAnimationFrame(draw);
        };
        draw();
        return () => { cancelAnimationFrame(raf); window.removeEventListener("resize", resize); };
    }, []);
    return <canvas ref={canvasRef} className="absolute inset-0 w-full h-full pointer-events-none" />;
}

/* ── circular progress ring ── */
function ProgressRing({ pct }: { pct: number }) {
    const r = 54, circ = 2 * Math.PI * r;
    return (
        <svg width="130" height="130" className="rotate-[-90deg]">
            <circle cx="65" cy="65" r={r} fill="none" stroke="rgba(124,58,237,0.12)" strokeWidth="8" />
            <circle cx="65" cy="65" r={r} fill="none"
                stroke="url(#ringGrad)" strokeWidth="8"
                strokeDasharray={circ}
                strokeDashoffset={circ * (1 - pct / 100)}
                strokeLinecap="round"
                style={{ transition: "stroke-dashoffset 2s cubic-bezier(0.22,1,0.36,1)" }}
            />
            <defs>
                <linearGradient id="ringGrad" x1="0%" y1="0%" x2="100%" y2="0%">
                    <stop offset="0%"   stopColor="#7C3AED" />
                    <stop offset="100%" stopColor="#22D3EE" />
                </linearGradient>
            </defs>
        </svg>
    );
}

const FEATURES = [
    { icon: Brain,      label: "AI Trade Signals",     desc: "Real-time buy/sell signals powered by pattern recognition" },
    { icon: Smartphone, label: "One-Click Execution",  desc: "Execute trades instantly from any device, anywhere" },
    { icon: Shield,     label: "Risk Guardian",         desc: "Automatic stop-loss and position sizing recommendations" },
    { icon: Activity,   label: "Live P&L Tracking",   desc: "Watch your gains accumulate in real time on the extension" },
    { icon: Chrome,     label: "Browser Extension",    desc: "Works natively in Chrome, Edge and Brave — no new app needed" },
    { icon: Globe,      label: "Multi-Exchange",       desc: "NSE, BSE, Crypto — all from a single unified interface" },
];

const STATS = [
    { target: 40000, suffix: "+", label: "Beta Waitlist" },
    { target: 98,    suffix: "%", label: "Backtested Accuracy" },
    { target: 180,   suffix: "ms", label: "Avg. Execution" },
];

export default function ComingSoonPage() {
    const [progress] = useState(72); // dev progress %
    const [hovered, setHovered] = useState<number | null>(null);

    const c1 = useCounter(STATS[0].target);
    const c2 = useCounter(STATS[1].target);
    const c3 = useCounter(STATS[2].target);
    const counters = [c1, c2, c3];

    return (
        <div
            className="min-h-screen relative overflow-hidden flex flex-col"
            style={{ background: "linear-gradient(160deg, #05050F 0%, #0A0818 55%, #050510 100%)" }}
        >
            <ParticleCanvas />

            {/* Ambient glows */}
            <div className="absolute top-0 left-1/3 w-[600px] h-[600px] rounded-full pointer-events-none"
                style={{ background: "radial-gradient(circle, rgba(124,58,237,0.12) 0%, transparent 70%)" }} />
            <div className="absolute bottom-0 right-1/4 w-[500px] h-[500px] rounded-full pointer-events-none"
                style={{ background: "radial-gradient(circle, rgba(34,211,238,0.08) 0%, transparent 70%)" }} />

            {/* ── NAV ── */}
            <nav className="relative z-10 flex items-center justify-between px-6 py-5">
                <Link href="/" className="flex items-center gap-2 group">
                    <div className="w-8 h-8 rounded-lg flex items-center justify-center"
                        style={{ background: "linear-gradient(135deg, #7C3AED, #22D3EE)" }}>
                        <Zap className="w-4 h-4 text-white" />
                    </div>
                    <span className="text-white font-bold text-xl tracking-tight">
                        Fin<span style={{ color: "#9F67FF" }}>AI</span>
                    </span>
                </Link>
                <Link href="/"
                    className="flex items-center gap-2 text-sm text-zinc-400 hover:text-white transition-colors group">
                    <ArrowLeft className="w-4 h-4 group-hover:-translate-x-0.5 transition-transform" />
                    Back to Home
                </Link>
            </nav>

            {/* ── MAIN ── */}
            <main className="relative z-10 flex-1 flex flex-col items-center justify-center px-6 py-16 text-center">

                {/* Badge */}
                <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full mb-8 text-xs font-semibold uppercase tracking-widest"
                    style={{
                        background: "rgba(124,58,237,0.12)",
                        border: "1px solid rgba(124,58,237,0.4)",
                        color: "#9F67FF",
                        animation: "fadeSlideUp 0.7s cubic-bezier(0.22,1,0.36,1) 0.1s both",
                    }}>
                    <span className="w-2 h-2 rounded-full bg-[#9F67FF]"
                        style={{ animation: "pulseRing 2s ease-out infinite" }} />
                    Trading Extension — In Development
                </div>

                {/* Heading */}
                <h1 className="text-5xl md:text-7xl font-bold text-white tracking-tighter max-w-4xl leading-[1.05] mb-6"
                    style={{ animation: "fadeSlideUp 0.7s cubic-bezier(0.22,1,0.36,1) 0.2s both" }}>
                    Something Powerful<br />
                    Is <span style={{
                        background: "linear-gradient(135deg, #7C3AED, #22D3EE)",
                        WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent"
                    }}>Coming Soon</span>
                </h1>

                {/* Motivational message */}
                <p className="text-zinc-300 text-lg md:text-xl max-w-2xl leading-relaxed mb-4"
                    style={{ animation: "fadeSlideUp 0.7s cubic-bezier(0.22,1,0.36,1) 0.3s both" }}>
                    We&apos;re building the <span className="text-[#22D3EE] font-semibold">FinAI Trading Extension</span> — a browser-native AI co-pilot
                    that lives right alongside your broker. Execute trades, monitor risk, and receive
                    AI-generated signals without ever switching tabs.
                </p>
                <p className="text-zinc-500 text-base max-w-xl leading-relaxed mb-12"
                    style={{ animation: "fadeSlideUp 0.7s cubic-bezier(0.22,1,0.36,1) 0.38s both" }}>
                    🚀 Your investments — smarter, faster, and always on. Stay tuned.
                </p>

                {/* Progress ring + stats */}
                <div className="flex flex-col md:flex-row items-center gap-10 mb-16"
                    style={{ animation: "fadeSlideUp 0.7s cubic-bezier(0.22,1,0.36,1) 0.45s both" }}>

                    {/* Ring */}
                    <div className="relative flex items-center justify-center">
                        <ProgressRing pct={progress} />
                        <div className="absolute inset-0 flex flex-col items-center justify-center">
                            <span className="text-3xl font-extrabold text-white">{progress}%</span>
                            <span className="text-[10px] uppercase tracking-widest text-zinc-500">Complete</span>
                        </div>
                    </div>

                    {/* Divider */}
                    <div className="hidden md:block w-px h-24 bg-white/5" />

                    {/* Stats */}
                    <div className="flex gap-10">
                        {STATS.map((s, i) => (
                            <div key={i} className="text-center">
                                <div className="text-3xl font-extrabold mb-1"
                                    style={{ background: "linear-gradient(135deg, #9F67FF, #22D3EE)", WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent" }}>
                                    {counters[i].toLocaleString("en-IN")}{s.suffix}
                                </div>
                                <div className="text-xs uppercase tracking-widest text-zinc-500">{s.label}</div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Feature grid */}
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 max-w-5xl w-full mb-14"
                    style={{ animation: "fadeSlideUp 0.7s cubic-bezier(0.22,1,0.36,1) 0.55s both" }}>
                    {FEATURES.map((f, i) => (
                        <div key={i}
                            onMouseEnter={() => setHovered(i)}
                            onMouseLeave={() => setHovered(null)}
                            className="relative text-left p-5 rounded-2xl cursor-default transition-all duration-300"
                            style={{
                                background: hovered === i ? "rgba(124,58,237,0.1)" : "rgba(255,255,255,0.025)",
                                border: hovered === i ? "1px solid rgba(124,58,237,0.4)" : "1px solid rgba(255,255,255,0.06)",
                                boxShadow: hovered === i ? "0 0 30px rgba(124,58,237,0.15)" : "none",
                                transform: hovered === i ? "translateY(-3px)" : "none",
                            }}>
                            <div className="w-10 h-10 rounded-xl flex items-center justify-center mb-3"
                                style={{
                                    background: hovered === i ? "rgba(124,58,237,0.2)" : "rgba(124,58,237,0.08)",
                                    border: "1px solid rgba(124,58,237,0.25)",
                                }}>
                                <f.icon className="w-5 h-5" style={{ color: "#9F67FF" }} />
                            </div>
                            <div className="text-white font-semibold text-sm mb-1">{f.label}</div>
                            <div className="text-zinc-500 text-xs leading-relaxed">{f.desc}</div>

                            {/* Corner sparkle on hover */}
                            {hovered === i && (
                                <Sparkles className="absolute top-3 right-3 w-4 h-4 text-[#22D3EE] opacity-70"
                                    style={{ animation: "pulseRing 1.5s ease-out infinite" }} />
                            )}
                        </div>
                    ))}
                </div>

                {/* Motivation callout */}
                <div className="max-w-2xl w-full mb-12 p-6 rounded-2xl text-left"
                    style={{
                        background: "linear-gradient(135deg, rgba(124,58,237,0.12), rgba(34,211,238,0.06))",
                        border: "1px solid rgba(124,58,237,0.25)",
                        animation: "fadeSlideUp 0.7s cubic-bezier(0.22,1,0.36,1) 0.65s both",
                    }}>
                    <div className="flex items-start gap-4">
                        <div className="w-10 h-10 rounded-xl flex-shrink-0 flex items-center justify-center mt-0.5"
                            style={{ background: "rgba(34,211,238,0.1)", border: "1px solid rgba(34,211,238,0.3)" }}>
                            <TrendingUp className="w-5 h-5" style={{ color: "#22D3EE" }} />
                        </div>
                        <div>
                            <div className="text-white font-semibold mb-1 flex items-center gap-2">
                                <Star className="w-4 h-4 text-[#9F67FF]" /> A message from our team
                            </div>
                            <p className="text-zinc-400 text-sm leading-relaxed">
                                The <span className="text-[#22D3EE] font-medium">FinAI Trading Extension</span> is actively being developed by our team of AI engineers.
                                We&apos;re making it incredibly easy to manage your investments right from your fingertips —
                                no complex platforms, no jargon. Just <span className="text-white font-medium">one click, powered by AI</span>.
                                Trust the process. We&apos;re almost there.
                            </p>
                        </div>
                    </div>
                </div>

                {/* CTAs */}
                <div className="flex flex-col sm:flex-row items-center gap-4"
                    style={{ animation: "fadeSlideUp 0.7s cubic-bezier(0.22,1,0.36,1) 0.72s both" }}>
                    <Link href="/auth/register"
                        className="flex items-center gap-2 px-8 py-3.5 rounded-full text-white font-semibold text-sm transition-all hover:scale-105"
                        style={{
                            background: "linear-gradient(135deg, #7C3AED, #22D3EE)",
                            boxShadow: "0 0 28px rgba(124,58,237,0.4)",
                        }}>
                        Get Early Access <ArrowRight className="w-4 h-4" />
                    </Link>
                    <Link href="/"
                        className="px-8 py-3.5 rounded-full text-zinc-400 hover:text-white font-semibold text-sm border transition-all hover:border-violet-500/40"
                        style={{ borderColor: "rgba(255,255,255,0.1)", background: "rgba(255,255,255,0.03)" }}>
                        Back to Homepage
                    </Link>
                </div>
            </main>

            {/* ── FOOTER NOTE ── */}
            <footer className="relative z-10 text-center py-6 text-zinc-700 text-xs">
                © {new Date().getFullYear()} FinAI Inc. — The extension is in active development. Expected release: Late 2026.
            </footer>

            {/* Page-level animations */}
            <style>{`
                @keyframes fadeSlideUp {
                    from { opacity: 0; transform: translateY(24px); }
                    to   { opacity: 1; transform: translateY(0); }
                }
                @keyframes pulseRing {
                    0%    { transform: scale(1);   opacity: 0.8; }
                    70%, 100% { transform: scale(1.5); opacity: 0; }
                }
            `}</style>
        </div>
    );
}
