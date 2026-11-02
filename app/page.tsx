"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import {
  ArrowRight, BarChart3, Globe, Shield, Zap, Command, Layers, Box, Hexagon,
  Triangle, UserPlus, TrendingUp, ChevronDown, Check, Menu, X, Activity,
  Network, Brain, Lock, LineChart,
} from "lucide-react";
import { AmbientBackground } from "@/components/ui/AmbientBackground";
import { ScrollReveal } from "@/components/ui/ScrollReveal";

/* ============================================================
   CONSTANTS
   ============================================================ */

const LOGOS = [
  { icon: Command,   label: "Quantum" },
  { icon: Layers,    label: "Stacked" },
  { icon: Box,       label: "Blockade" },
  { icon: Hexagon,   label: "Nexus" },
  { icon: Triangle,  label: "Apex" },
  { icon: Activity,  label: "Pulse" },
  { icon: Network,   label: "Linkage" },
  { icon: Brain,     label: "NeuralX" },
];

const FEATURES = [
  {
    icon: Shield,
    title: "OSINT Market Intel",
    desc: "Leverage Open Source Intelligence to predict market shifts before they happen, aggregating millions of data points instantly.",
    accent: "#7C3AED",
    span: "col-span-12 md:col-span-12 lg:col-span-5",
    featured: true,
  },
  {
    icon: BarChart3,
    title: "Smart Ledgers",
    desc: "Automated portfolio rebalancing and hyper-accurate expense tracking powered by neural networks.",
    accent: "#22D3EE",
    span: "col-span-12 md:col-span-6 lg:col-span-4",
    featured: false,
  },
  {
    icon: Globe,
    title: "Global Assets",
    desc: "Track equities and crypto seamlessly worldwide with millisecond refresh rates.",
    accent: "#9F67FF",
    span: "col-span-12 md:col-span-6 lg:col-span-3",
    featured: false,
  },
  {
    icon: Lock,
    title: "Enterprise Security",
    desc: "Bank-grade encryption, 2FA, and zero-knowledge architecture across all endpoints.",
    accent: "#22D3EE",
    span: "col-span-12 md:col-span-6 lg:col-span-3",
    featured: false,
  },
  {
    icon: LineChart,
    title: "Predictive Analytics",
    desc: "Forward-looking probability models trained on decades of market microstructure data.",
    accent: "#7C3AED",
    span: "col-span-12 md:col-span-6 lg:col-span-4",
    featured: false,
  },
  {
    icon: Brain,
    title: "Neural Autopilot",
    desc: "Set your strategy once. Our reinforcement-learning engine continuously optimises allocation in real-time.",
    accent: "#9F67FF",
    span: "col-span-12 md:col-span-12 lg:col-span-5",
    featured: true,
  },
];

const STEPS = [
  { step: "01", title: "Connect Accounts",  desc: "Securely link brokerages and crypto wallets via shielded API gateways.", icon: UserPlus },
  { step: "02", title: "Set Parameters",    desc: "Define risk tolerance and let our AI build your ideal allocation strategy.", icon: Zap },
  { step: "03", title: "Activate Autopilot", desc: "FinAI monitors markets 24/7 and rebalances automatically to lock in profits.", icon: TrendingUp },
];

const TESTIMONIALS = [
  {
    quote: "FinAI completely revolutionized how I manage my clients' offshore accounts. The OSINT intel is bordering on precognitive.",
    name: "Sarah Jenkins", role: "Hedge Fund Manager",
    avatar: "SJ",
  },
  {
    quote: "The automated rebalancing saves me 15 hours a week. It literally pays for itself inside the first day of every month.",
    name: "Marcus Thorne", role: "Day Trader",
    avatar: "MT",
  },
  {
    quote: "I've never seen a platform synthesize global events into actionable trading data this quickly. Best UI in the game.",
    name: "Elena Rodriguez", role: "Crypto Analyst",
    avatar: "ER",
  },
];

const STATS = [
  { value: "$2.4B+",  label: "Assets Managed" },
  { value: "99.98%", label: "Uptime SLA" },
  { value: "180ms",  label: "Avg. Execution" },
  { value: "40K+",   label: "Active Investors" },
];

/* ============================================================
   SUB-COMPONENTS
   ============================================================ */

/** Animated typing-cursor terminal display for the benefits section */
function TerminalPanel() {
  const lines = [
    { text: "MARKET SURGE DETECTED — NASDAQ: +2.3%", color: "#22D3EE" },
    { text: "AI signal: BUY — AAPL 200x confidence", color: "#9F67FF" },
    { text: "Rebalancing portfolio…  ████████ 100%",  color: "#22D3EE" },
    { text: "Trade executed in 147ms ✓",               color: "#4ADE80" },
    { text: "P&L delta +$4,812 locked",                color: "#4ADE80" },
  ];
  const [visible, setVisible] = useState(0);

  useEffect(() => {
    if (visible >= lines.length) return;
    const t = setTimeout(() => setVisible(v => v + 1), 900);
    return () => clearTimeout(t);
  }, [visible, lines.length]);

  // Restart loop
  useEffect(() => {
    if (visible === lines.length) {
      const t = setTimeout(() => setVisible(0), 3000);
      return () => clearTimeout(t);
    }
  }, [visible, lines.length]);

  const bars = [80, 40, 60, 95, 30, 70, 85];

  return (
    <div
      className="glass-panel rounded-2xl border overflow-hidden"
      style={{ borderColor: "rgba(124,58,237,0.25)" }}
    >
      {/* Title bar */}
      <div className="flex items-center gap-2 px-5 py-3 border-b border-white/5 bg-white/[0.02]">
        <span className="w-3 h-3 rounded-full bg-red-500/80" />
        <span className="w-3 h-3 rounded-full bg-yellow-500/80" />
        <span className="w-3 h-3 rounded-full bg-green-500/80" />
        <span className="ml-3 text-xs text-zinc-500 font-mono">finai — live-feed</span>
        <span className="ml-auto px-2 py-0.5 rounded-full bg-[#22D3EE]/10 text-[#22D3EE] text-[10px] font-semibold">● LIVE</span>
      </div>

      {/* Chart bars */}
      <div className="px-5 pt-4 pb-2 flex items-end gap-1.5 h-32">
        {bars.map((h, i) => (
          <div key={i} className="flex-1 flex flex-col justify-end">
            <div
              className="w-full rounded-t-sm transition-all duration-700"
              style={{
                height: `${h}%`,
                background: i % 2 === 0
                  ? "linear-gradient(to top, #7C3AED, #9F67FF)"
                  : "linear-gradient(to top, #0891B2, #22D3EE)",
                opacity: 0.85,
                animation: `barGrow 1s ease-out ${i * 0.12}s both`,
                "--bar-h": `${h}%`,
              } as React.CSSProperties}
            />
          </div>
        ))}
      </div>

      {/* Terminal log */}
      <div className="px-5 pb-5 pt-2 space-y-1 font-mono text-xs min-h-[130px]">
        {lines.slice(0, visible).map((line, i) => (
          <div key={i} className="flex items-start gap-2">
            <span className="text-zinc-600 select-none">›</span>
            <span style={{ color: line.color }}>{line.text}</span>
          </div>
        ))}
        {/* Blinking cursor */}
        <span
          className="inline-block w-2 h-3.5 bg-[#9F67FF] align-middle"
          style={{ animation: "blink 1s step-end infinite" }}
        />
      </div>
    </div>
  );
}

/* ============================================================
   MAIN PAGE
   ============================================================ */

export default function Home() {
  const [navScrolled, setNavScrolled] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const heroRef = useRef<HTMLDivElement>(null);

  // Detect scroll for nav style
  useEffect(() => {
    const onScroll = () => setNavScrolled(window.scrollY > 40);
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <div
      className="min-h-screen relative"
      style={{ background: "linear-gradient(160deg, #05050F 0%, #0A0818 50%, #050510 100%)" }}
    >
      <AmbientBackground />

      {/* ── NAVIGATION ── */}
      <nav
        className="fixed top-0 left-0 right-0 z-50 flex items-center justify-between px-6 py-3 transition-all duration-500"
        style={
          navScrolled
            ? {
                background: "rgba(5,5,15,0.85)",
                backdropFilter: "blur(20px)",
                borderBottom: "1px solid rgba(124,58,237,0.15)",
                boxShadow: "0 4px 40px rgba(0,0,0,0.4)",
              }
            : { background: "transparent" }
        }
      >
        {/* Logo */}
        <Link href="/" className="flex items-center gap-2.5 group">
          <div className="relative w-8 h-8">
            <div
              className="absolute inset-0 rounded-lg"
              style={{ background: "linear-gradient(135deg, #7C3AED, #22D3EE)", opacity: 0.9 }}
            />
            <Zap className="absolute inset-0 m-auto w-4 h-4 text-white" />
          </div>
          <span className="text-white font-bold text-xl tracking-tight">
            Fin<span style={{ color: "#9F67FF" }}>AI</span>
          </span>
        </Link>

        {/* Desktop nav links */}
        <div className="hidden md:flex items-center gap-8 text-sm font-medium text-zinc-400">
          {["Features", "Benefits", "Pricing", "About"].map(item => (
            <a
              key={item}
              href={`#${item.toLowerCase()}`}
              className="hover:text-white transition-colors relative group"
            >
              {item}
              <span
                className="absolute -bottom-1 left-0 w-0 h-px group-hover:w-full transition-all duration-300"
                style={{ background: "linear-gradient(90deg, #7C3AED, #22D3EE)" }}
              />
            </a>
          ))}
        </div>

        {/* CTA + mobile toggle */}
        <div className="flex items-center gap-3">
          <Link
            href="/auth/login"
            className="hidden sm:block text-sm text-zinc-400 hover:text-white transition-colors"
          >
            Sign In
          </Link>
          <Link
            href="/auth/register"
            className="px-5 py-2.5 rounded-full text-white text-sm font-semibold transition-all duration-300 hover:scale-105"
            style={{
              background: "linear-gradient(135deg, #7C3AED, #6025C0)",
              boxShadow: "0 0 20px rgba(124,58,237,0.35)",
            }}
          >
            Get Started
          </Link>
          <button
            className="md:hidden text-zinc-400 hover:text-white"
            onClick={() => setMobileOpen(o => !o)}
            aria-label="Toggle menu"
          >
            {mobileOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
          </button>
        </div>

        {/* Mobile menu */}
        {mobileOpen && (
          <div
            className="absolute top-full left-0 right-0 mt-1 mx-4 rounded-2xl p-6 space-y-4"
            style={{ background: "rgba(13,13,26,0.97)", border: "1px solid rgba(124,58,237,0.2)" }}
          >
            {["Features", "Benefits", "Pricing", "About"].map(item => (
              <a
                key={item}
                href={`#${item.toLowerCase()}`}
                className="block text-zinc-300 hover:text-white transition-colors py-1"
                onClick={() => setMobileOpen(false)}
              >
                {item}
              </a>
            ))}
          </div>
        )}
      </nav>

      {/* ── HERO SECTION ── */}
      <main
        ref={heroRef}
        id="home"
        className="relative z-10 min-h-screen flex flex-col items-center justify-center text-center px-6 pt-24 pb-16"
      >
        {/* Animated badge */}
        <div
          className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full mb-8 text-xs font-semibold uppercase tracking-wider"
          style={{
            background: "rgba(124,58,237,0.12)",
            border: "1px solid rgba(124,58,237,0.4)",
            color: "#9F67FF",
            animation: "fadeSlideUp 0.8s cubic-bezier(0.22,1,0.36,1) 0.1s both",
          }}
        >
          <span
            className="w-2 h-2 rounded-full"
            style={{ background: "#9F67FF", animation: "pulseRing 2s ease-out infinite" }}
          />
          AI-Powered Financial Intelligence
        </div>

        {/* Hero heading */}
        <h1
          className="text-5xl md:text-7xl lg:text-8xl font-bold text-white tracking-tighter max-w-5xl leading-[1.05] mb-6"
          style={{ animation: "fadeSlideUp 0.8s cubic-bezier(0.22,1,0.36,1) 0.25s both" }}
        >
          Empowering Your<br />
          Investments with{" "}
          <span className="gradient-text">AI Technology</span>
        </h1>

        {/* Sub copy */}
        <p
          className="text-zinc-400 text-lg md:text-xl max-w-2xl mb-10 leading-relaxed"
          style={{ animation: "fadeSlideUp 0.8s cubic-bezier(0.22,1,0.36,1) 0.4s both" }}
        >
          Transform your wealth with AI-driven insights, OSINT market intelligence,
          and automated risk management — all from one highly-secure dashboard.
        </p>

        {/* CTA Buttons */}
        <div
          className="flex flex-col sm:flex-row items-center gap-4 w-full justify-center"
          style={{ animation: "fadeSlideUp 0.8s cubic-bezier(0.22,1,0.36,1) 0.55s both" }}
        >
          <Link
            href="/auth/register"
            className="flex items-center gap-2 px-8 py-4 rounded-full text-white font-semibold text-sm transition-all hover:scale-105 w-full sm:w-auto justify-center"
            style={{
              background: "linear-gradient(135deg, #7C3AED 0%, #22D3EE 100%)",
              boxShadow: "0 0 30px rgba(124,58,237,0.4), 0 0 80px rgba(124,58,237,0.15)",
            }}
          >
            Explore Dashboard <ArrowRight className="w-4 h-4" />
          </Link>
          <Link
            href="#features"
            className="flex items-center gap-2 px-8 py-4 rounded-full text-zinc-300 hover:text-white font-semibold text-sm border transition-all hover:border-violet-500/50 w-full sm:w-auto justify-center"
            style={{ borderColor: "rgba(255,255,255,0.12)", background: "rgba(255,255,255,0.04)" }}
          >
            See Features
          </Link>
        </div>

        {/* Stats row */}
        <div
          className="mt-16 grid grid-cols-2 md:grid-cols-4 gap-6 max-w-3xl w-full"
          style={{ animation: "fadeSlideUp 0.8s cubic-bezier(0.22,1,0.36,1) 0.7s both" }}
        >
          {STATS.map((s, i) => (
            <div key={i} className="text-center">
              <div
                className="text-3xl font-bold mb-1 gradient-text"
              >
                {s.value}
              </div>
              <div className="text-zinc-500 text-xs uppercase tracking-wider">{s.label}</div>
            </div>
          ))}
        </div>

        {/* Scroll indicator */}
        <div
          className="absolute bottom-8 left-1/2 -translate-x-1/2 flex flex-col items-center gap-1 text-zinc-600"
          style={{ animation: "bounceDown 1.6s ease-in-out infinite" }}
        >
          <span className="text-[10px] uppercase tracking-widest">Scroll</span>
          <ChevronDown className="w-4 h-4" />
        </div>
      </main>

      {/* ── TRUSTED BY — Infinite Marquee ── */}
      <section className="py-12 relative z-10 overflow-hidden border-y" style={{ borderColor: "rgba(255,255,255,0.05)", background: "rgba(255,255,255,0.015)" }}>
        <p className="text-center text-xs font-semibold text-zinc-600 uppercase tracking-widest mb-8">
          Trusted by innovative financial teams worldwide
        </p>
        <div className="relative overflow-hidden">
          {/* Fade edges */}
          <div className="absolute left-0 top-0 bottom-0 w-32 z-10 pointer-events-none"
            style={{ background: "linear-gradient(to right, #05050F, transparent)" }} />
          <div className="absolute right-0 top-0 bottom-0 w-32 z-10 pointer-events-none"
            style={{ background: "linear-gradient(to left, #05050F, transparent)" }} />

          {/* Duplicated for seamless loop */}
          <div className="marquee-track">
            {[...LOGOS, ...LOGOS].map((Logo, i) => (
              <div
                key={i}
                className="flex items-center gap-2.5 mx-8 py-2 px-5 rounded-full opacity-40 hover:opacity-80 transition-opacity duration-300 whitespace-nowrap"
                style={{ border: "1px solid rgba(255,255,255,0.08)", background: "rgba(255,255,255,0.02) " }}
              >
                <Logo.icon className="w-5 h-5 text-zinc-300" />
                <span className="text-zinc-300 font-semibold text-sm">{Logo.label}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── FEATURES BENTO ── */}
      <section id="features" className="py-28 px-6 max-w-7xl mx-auto relative z-10">
        <ScrollReveal>
          <div className="text-center mb-16">
            <div
              className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold uppercase tracking-wider mb-4"
              style={{ background: "rgba(34,211,238,0.08)", border: "1px solid rgba(34,211,238,0.25)", color: "#22D3EE" }}
            >
              Platform Capabilities
            </div>
            <h2 className="text-4xl md:text-5xl font-bold text-white mb-4 tracking-tighter">
              Smarter Investing <span className="gradient-text">Starts Here</span>
            </h2>
            <p className="text-zinc-400 max-w-2xl mx-auto">
              Maximize returns, minimize effort — a fully automated investment ecosystem
              that saves you time, worry, and guesswork.
            </p>
          </div>
        </ScrollReveal>

        <div className="bento-grid">
          {FEATURES.map((f, i) => (
            <ScrollReveal
              key={i}
              className={f.span}
              delay={i * 100}
            >
              <div
                className="h-full p-8 rounded-3xl neon-hover-glow group cursor-default transition-transform duration-300 hover:-translate-y-1 flex flex-col justify-between"
                style={{
                  background: "rgba(13,13,26,0.7)",
                  backdropFilter: "blur(16px)",
                  border: `1px solid rgba(255,255,255,0.07)`,
                }}
              >
                {/* Icon */}
                <div>
                  <div
                    className="w-12 h-12 rounded-xl flex items-center justify-center mb-6 transition-all duration-300 group-hover:scale-110"
                    style={{
                      background: `${f.accent}18`,
                      border: `1px solid ${f.accent}30`,
                    }}
                  >
                    <f.icon className="w-6 h-6" style={{ color: f.accent }} />
                  </div>
                  <h3
                    className="text-xl font-bold text-white mb-2"
                    style={f.featured ? { fontSize: "1.4rem" } : {}}
                  >
                    {f.title}
                  </h3>
                  <p className="text-zinc-400 leading-relaxed">{f.desc}</p>
                </div>

                {/* Featured accent bar */}
                {f.featured && (
                  <div
                    className="mt-6 h-px w-full rounded-full"
                    style={{ background: `linear-gradient(90deg, ${f.accent}, transparent)` }}
                  />
                )}
              </div>
            </ScrollReveal>
          ))}
        </div>
      </section>

      {/* ── BENEFITS ── */}
      <section id="benefits" className="py-28 px-6 max-w-7xl mx-auto relative z-10">
        <div className="flex flex-col lg:flex-row gap-16 items-center">
          <ScrollReveal className="lg:w-1/2 space-y-8">
            <div
              className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold uppercase tracking-wider"
              style={{ background: "rgba(124,58,237,0.1)", border: "1px solid rgba(124,58,237,0.35)", color: "#9F67FF" }}
            >
              <Zap className="w-3.5 h-3.5" /> Real-time Execution
            </div>
            <h2 className="text-4xl md:text-5xl font-bold text-white tracking-tighter leading-tight">
              Actionable Intelligence<br />
              <span className="gradient-text">at the Speed of Light</span>
            </h2>
            <p className="text-zinc-400 text-lg leading-relaxed">
              We process massive amounts of open-source market data and social sentiment
              to deliver real-time trading advantages. Never miss a breakout.
            </p>
            <ul className="space-y-4">
              {[
                "Millisecond trade execution API",
                "Dark pool tracking & volume analysis",
                "Automated risk & stop-loss adjustments",
                "Cross-asset correlation heatmaps",
              ].map((item, i) => (
                <li key={i} className="flex items-center gap-3 text-white font-medium">
                  <div
                    className="w-6 h-6 rounded-full flex items-center justify-center flex-shrink-0"
                    style={{ background: "rgba(34,211,238,0.12)", border: "1px solid rgba(34,211,238,0.3)" }}
                  >
                    <Check className="w-3.5 h-3.5" style={{ color: "#22D3EE" }} />
                  </div>
                  {item}
                </li>
              ))}
            </ul>
            <Link
              href="/coming-soon"
              className="inline-flex items-center gap-2 px-6 py-3 rounded-full text-white font-semibold text-sm transition-all hover:scale-105"
              style={{
                background: "linear-gradient(135deg, #7C3AED, #22D3EE)",
                boxShadow: "0 0 20px rgba(124,58,237,0.3)",
              }}
            >
              Start Free Trial <ArrowRight className="w-4 h-4" />
            </Link>
          </ScrollReveal>

          <ScrollReveal className="lg:w-1/2 w-full" delay={200}>
            <TerminalPanel />
          </ScrollReveal>
        </div>
      </section>

      {/* ── HOW IT WORKS ── */}
      <section className="py-28 px-6 max-w-7xl mx-auto relative z-10">
        <ScrollReveal>
          <div className="text-center mb-16">
            <div
              className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold uppercase tracking-wider mb-4"
              style={{ background: "rgba(124,58,237,0.08)", border: "1px solid rgba(124,58,237,0.25)", color: "#9F67FF" }}
            >
              Getting Started
            </div>
            <h2 className="text-4xl md:text-5xl font-bold text-white mb-4 tracking-tighter">
              Automate Your Wealth in{" "}
              <span className="gradient-text">3 Steps</span>
            </h2>
            <p className="text-zinc-400 max-w-2xl mx-auto">
              From account creation to first executed trade in under 5 minutes.
            </p>
          </div>
        </ScrollReveal>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 relative">
          {/* Connector line */}
          <div
            className="hidden md:block absolute top-14 left-0 right-0 h-px z-0"
            style={{ background: "linear-gradient(90deg, transparent, rgba(124,58,237,0.3), rgba(34,211,238,0.3), transparent)" }}
          />

          {STEPS.map((item, i) => (
            <ScrollReveal key={i} delay={i * 150}>
              <div
                className="relative z-10 p-8 rounded-3xl flex flex-col items-center text-center neon-hover-glow transition-transform duration-300 hover:-translate-y-2"
                style={{
                  background: "rgba(13,13,26,0.7)",
                  backdropFilter: "blur(16px)",
                  border: "1px solid rgba(255,255,255,0.07)",
                }}
              >
                {/* Pulsing icon ring */}
                <div className="relative mb-6">
                  <div
                    className="absolute inset-0 rounded-2xl opacity-0 group-hover:opacity-100"
                    style={{ background: "rgba(124,58,237,0.15)", animation: "pulseRing 2s ease-out infinite" }}
                  />
                  <div
                    className="w-16 h-16 rounded-2xl flex items-center justify-center"
                    style={{
                      background: "linear-gradient(135deg, rgba(124,58,237,0.2), rgba(34,211,238,0.1))",
                      border: "1px solid rgba(124,58,237,0.35)",
                      boxShadow: "0 0 20px rgba(124,58,237,0.2)",
                    }}
                  >
                    <item.icon className="w-8 h-8" style={{ color: "#9F67FF" }} />
                  </div>
                </div>

                <div
                  className="text-2xl font-bold mb-2 font-mono"
                  style={{ color: "#7C3AED" }}
                >
                  {item.step}
                </div>
                <h3 className="text-xl font-bold text-white mb-3">{item.title}</h3>
                <p className="text-zinc-400 leading-relaxed">{item.desc}</p>
              </div>
            </ScrollReveal>
          ))}
        </div>
      </section>

      {/* ── TESTIMONIALS ── */}
      <section className="py-28 px-6 max-w-7xl mx-auto relative z-10">
        <ScrollReveal>
          <div className="text-center mb-16">
            <h2 className="text-4xl md:text-5xl font-bold text-white mb-4 tracking-tighter">
              Trusted by <span className="gradient-text">Elite Investors</span>
            </h2>
            <p className="text-zinc-400 max-w-2xl mx-auto">
              See how financial professionals are transforming their portfolios with FinAI.
            </p>
          </div>
        </ScrollReveal>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {TESTIMONIALS.map((t, i) => (
            <ScrollReveal key={i} delay={i * 120}>
              <div
                className="h-full p-8 rounded-3xl neon-hover-glow flex flex-col justify-between"
                style={{
                  background: "rgba(13,13,26,0.7)",
                  backdropFilter: "blur(16px)",
                  border: "1px solid rgba(255,255,255,0.07)",
                  borderLeft: "3px solid #7C3AED",
                }}
              >
                {/* Stars */}
                <div>
                  <div className="flex gap-1 mb-5">
                    {[1,2,3,4,5].map(s => (
                      <span key={s} style={{ color: "#9F67FF" }}>★</span>
                    ))}
                  </div>
                  <p className="text-zinc-300 text-base leading-relaxed mb-6 flex-1">
                    &quot;{t.quote}&quot;
                  </p>
                </div>

                {/* Author */}
                <div
                  className="flex items-center gap-4 pt-6"
                  style={{ borderTop: "1px solid rgba(255,255,255,0.07)" }}
                >
                  <div
                    className="w-11 h-11 rounded-full flex items-center justify-center text-white text-sm font-bold flex-shrink-0"
                    style={{ background: "linear-gradient(135deg, #7C3AED, #22D3EE)" }}
                  >
                    {t.avatar}
                  </div>
                  <div>
                    <div className="text-white font-semibold text-sm">{t.name}</div>
                    <div className="text-zinc-500 text-xs">{t.role}</div>
                  </div>
                </div>
              </div>
            </ScrollReveal>
          ))}
        </div>
      </section>

      {/* ── FINAL CTA ── */}
      <section className="py-28 px-6 max-w-5xl mx-auto relative z-10 text-center">
        <ScrollReveal>
          <div
            className="relative p-14 rounded-[2.5rem] overflow-hidden"
            style={{
              background: "linear-gradient(135deg, rgba(124,58,237,0.15), rgba(34,211,238,0.08))",
              border: "1px solid rgba(124,58,237,0.3)",
              boxShadow: "0 0 80px rgba(124,58,237,0.15), inset 0 0 80px rgba(34,211,238,0.03)",
            }}
          >
            {/* Background mesh */}
            <div
              className="absolute inset-0 opacity-[0.04] pointer-events-none"
              style={{
                backgroundImage: `
                  linear-gradient(rgba(255,255,255,0.5) 1px, transparent 1px),
                  linear-gradient(90deg, rgba(255,255,255,0.5) 1px, transparent 1px)
                `,
                backgroundSize: "40px 40px",
              }}
            />

            <div className="relative z-10">
              <div
                className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold uppercase tracking-wider mb-6"
                style={{ background: "rgba(124,58,237,0.15)", border: "1px solid rgba(124,58,237,0.4)", color: "#9F67FF" }}
              >
                No credit card required
              </div>
              <h2 className="text-4xl md:text-5xl font-bold text-white mb-5 tracking-tighter">
                Ready to Scale Your Wealth?
              </h2>
              <p className="text-zinc-300 text-lg mb-10 max-w-xl mx-auto leading-relaxed">
                Join thousands of investors using FinAI to automate portfolio growth
                and dramatically reduce risk — starting today.
              </p>
              <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
                <Link
                  href="/auth/register"
                  className="px-9 py-4 rounded-full text-white font-semibold text-sm transition-all hover:scale-105 flex items-center gap-2"
                  style={{
                    background: "linear-gradient(135deg, #7C3AED 0%, #22D3EE 100%)",
                    boxShadow: "0 0 30px rgba(124,58,237,0.45)",
                  }}
                >
                  Get Started Now <ArrowRight className="w-4 h-4" />
                </Link>
                <Link
                  href="/auth/login"
                  className="px-9 py-4 rounded-full text-zinc-300 hover:text-white font-semibold text-sm border transition-all hover:border-violet-500/50"
                  style={{ borderColor: "rgba(255,255,255,0.12)", background: "rgba(255,255,255,0.04)" }}
                >
                  Sign In
                </Link>
              </div>
            </div>
          </div>
        </ScrollReveal>
      </section>

      {/* ── FOOTER ── */}
      <footer
        className="relative z-10 border-t py-10 px-6"
        style={{ borderColor: "rgba(255,255,255,0.06)", background: "rgba(0,0,0,0.3)" }}
      >
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-6">
          {/* Brand */}
          <div className="flex items-center gap-2.5">
            <div
              className="w-7 h-7 rounded-lg flex items-center justify-center"
              style={{ background: "linear-gradient(135deg, #7C3AED, #22D3EE)" }}
            >
              <Zap className="w-3.5 h-3.5 text-white" />
            </div>
            <span className="text-white font-bold">Fin<span style={{ color: "#9F67FF" }}>AI</span></span>
          </div>

          {/* Nav links */}
          <div className="flex items-center gap-6 text-sm text-zinc-500">
            {["Privacy", "Terms", "Security", "Contact"].map(l => (
              <a key={l} href="#" className="hover:text-zinc-300 transition-colors">{l}</a>
            ))}
          </div>

          <p className="text-zinc-600 text-xs">
            © {new Date().getFullYear()} FinAI Inc. All rights reserved.
          </p>
        </div>
      </footer>
    </div>
  );
}
