"use client";

import { useState, useEffect, useRef } from "react";
import { Sparkles, Bot, Send, TrendingUp, AlertTriangle, Lightbulb, Wallet, Loader2, Clock, RefreshCw } from "lucide-react";
import { cn } from "@/lib/utils";
import { useDashboard } from "@/components/DashboardProvider";
import { useAuth } from "@/context/AuthContext";
import ReactMarkdown from "react-markdown";
import dynamic from "next/dynamic";

const AIVisualizer = dynamic(
    () => import("@/components/dashboard/AIVisualizer").then(mod => mod.AIVisualizer),
    { 
        ssr: false, 
        loading: () => (
            <div className="w-full h-full min-h-[250px] flex items-center justify-center bg-surface-container-low/50 animate-pulse rounded-[1.5rem]">
                <Loader2 className="w-8 h-8 text-primary animate-spin opacity-50" />
            </div>
        )
    }
);

type Insight = {
    id: number;
    type: "warning" | "opportunity" | "tip";
    title: string;
    desc: string;
    color: string;
    bg: string;
    border: string;
};

const CACHE_KEY = "finai_insights_cache";
const CACHE_TTL_MS = 24 * 60 * 60 * 1000;

function getCachedInsights() {
    try {
        const raw = localStorage.getItem(CACHE_KEY);
        if (!raw) return null;
        const { data, timestamp } = JSON.parse(raw);
        if (Date.now() - timestamp > CACHE_TTL_MS) { localStorage.removeItem(CACHE_KEY); return null; }
        return data;
    } catch { return null; }
}

function setCachedInsights(data: object) {
    try { localStorage.setItem(CACHE_KEY, JSON.stringify({ data, timestamp: Date.now() })); } catch { }
}

function clearInsightsCache() {
    try { localStorage.removeItem(CACHE_KEY); } catch { }
}

export default function AISuggestionsPage() {
    const { transactions, loadingTransactions } = useDashboard();
    const { user } = useAuth();

    // The AI routes read the signed-in user's own transactions server-side.
    const aiFetch = async (url: string, body: object) => {
        const token = await user?.getIdToken();
        return fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
            body: JSON.stringify(body),
        });
    };
    const [message, setMessage] = useState("");
    const [insights, setInsights] = useState<Insight[]>([]);
    const [healthScore, setHealthScore] = useState<number | null>(null);
    const [healthText, setHealthText] = useState("");
    const [loadingInsights, setLoadingInsights] = useState(false);
    const [insightError, setInsightError] = useState("");
    const [retryCountdown, setRetryCountdown] = useState(0);
    const [chatHistory, setChatHistory] = useState<{ role: "ai" | "user"; content: string }[]>([
        { role: "ai", content: "Hello! I'm your embedded financial AI powered by Groq. I have full read access to your transactions matrix. Want me to analyze your latest spending trends or suggest a budget?" }
    ]);
    const [chatLoading, setChatLoading] = useState(false);
    const chatEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (retryCountdown <= 0) return;
        const timer = setInterval(() => setRetryCountdown(p => { if (p <= 1) { clearInterval(timer); return 0; } return p - 1; }), 1000);
        return () => clearInterval(timer);
    }, [retryCountdown]);

    useEffect(() => { chatEndRef.current?.scrollIntoView({ behavior: "smooth" }); }, [chatHistory, chatLoading]);

    const fetchInsights = async (forceRefresh = false) => {
        if (loadingTransactions || transactions.length === 0 || loadingInsights) return;
        if (!forceRefresh) {
            const cached = getCachedInsights();
            if (cached) { setInsights(cached.insights || []); setHealthScore(cached.healthScore || 0); setHealthText(cached.insightText || ""); return; }
        }
        setLoadingInsights(true);
        setInsightError("");
        try {
            const res = await aiFetch("/api/ai/insights", {});
            if (!res.ok) {
                const errData = await res.json();
                const retryMatch = errData.error?.match(/retry[^0-9]*(\d+(\.\d+)?)\s*s/i);
                if (retryMatch) { setRetryCountdown(Math.ceil(parseFloat(retryMatch[1]))); throw new Error(`Rate limit reached. Please wait ${Math.ceil(parseFloat(retryMatch[1]))} seconds before retrying.`); }
                throw new Error(errData.error || "Failed to fetch");
            }
            const { data } = await res.json();
            setInsights(data.insights || []);
            setHealthScore(data.healthScore || 0);
            setHealthText(data.insightText || "");
            setCachedInsights(data);
        } catch (error: unknown) {
            const err = error as Error;
            setInsightError(err.message || "The AI encountered an error while parsing your financial matrix.");
        } finally {
            setLoadingInsights(false);
        }
    };

    useEffect(() => {
        if (loadingTransactions || transactions.length === 0 || insights.length > 0 || loadingInsights) return;
        fetchInsights(false);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [transactions, loadingTransactions]);

    const handleSendMessage = async (e?: React.FormEvent) => {
        if (e) e.preventDefault();
        const trimMsg = message.trim();
        if (!trimMsg || chatLoading) return;
        setChatHistory(prev => [...prev, { role: "user", content: trimMsg }]);
        setMessage("");
        setChatLoading(true);
        try {
            const res = await aiFetch("/api/chat", { message: trimMsg });
            if (!res.ok) throw new Error("Failed to get chat response");
            const data = await res.json();
            setChatHistory(prev => [...prev, { role: "ai", content: data.reply }]);
        } catch {
            setChatHistory(prev => [...prev, { role: "ai", content: "I encountered an error processing that request." }]);
        } finally {
            setChatLoading(false);
        }
    };

    const getIcon = (type: string) => {
        if (type === "warning") return <AlertTriangle className="w-6 h-6 text-error" />;
        if (type === "opportunity") return <TrendingUp className="w-6 h-6 text-primary" />;
        return <Lightbulb className="w-6 h-6 text-yellow-500" />;
    };

    return (
        <div className="w-full max-w-7xl mx-auto space-y-8 relative z-10 pb-20">
            {/* Header */}
            <header className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-on-surface tracking-tighter flex items-center gap-3">
                        <Sparkles className="w-8 h-8 text-primary" /> AI Financial Advisor
                    </h1>
                    <p className="text-on-surface-variant mt-0.5">Personalized insights and intelligent chatbot workspace.</p>
                </div>
                <div className="px-4 py-2 rounded-full text-sm font-semibold flex items-center gap-2 bg-primary-container text-on-primary-container border border-primary/20 shadow-sm animate-in fade-in zoom-in duration-500">
                    <div className="w-2 h-2 rounded-full animate-pulse bg-primary" />
                    Groq AI Active
                </div>
            </header>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                {/* Left: Insights + Health */}
                <div className="lg:col-span-7 space-y-6">
                    {/* AI Visualizer Dedicated Block */}
                    <div className="p-6 h-[250px] bg-surface-container-lowest ghost-border ambient-shadow rounded-[1.5rem] flex items-center justify-center">
                        <AIVisualizer />
                    </div>

                    {/* Smart Insights */}
                    <div className="p-6 min-h-[400px] bg-surface-container-lowest ghost-border ambient-shadow rounded-[1.5rem]">
                        <div className="flex items-center justify-between mb-6">
                            <h2 className="text-xl font-bold text-on-surface flex items-center gap-2">
                                <Lightbulb className="w-5 h-5 text-yellow-500" /> Smart Insights
                            </h2>
                            {insights.length > 0 && (
                                <button onClick={() => { clearInsightsCache(); fetchInsights(true); }}
                                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs transition-all bg-surface-container hover:bg-surface-variant text-on-surface-variant hover:text-on-surface">
                                    <RefreshCw className="w-3.5 h-3.5" /> Refresh
                                </button>
                            )}
                        </div>

                        {loadingTransactions || loadingInsights ? (
                            <div className="flex flex-col items-center justify-center p-12 text-on-surface-variant gap-4">
                                <Loader2 className="w-10 h-10 animate-spin text-primary/50" />
                                <p>Artificial Intelligence is crunching your data...</p>
                            </div>
                        ) : insightError ? (
                            <div className="p-12 text-center text-error bg-error-container border border-error/20 rounded-2xl">
                                <AlertTriangle className="w-8 h-8 mx-auto mb-3 opacity-80" />
                                <p className="text-sm leading-relaxed">{insightError}</p>
                                {retryCountdown > 0 ? (
                                    <div className="mt-4 flex items-center justify-center gap-2 text-on-error-container/70 text-sm">
                                        <Clock className="w-4 h-4 animate-pulse" />
                                        <span>Retry available in <span className="font-bold text-on-error-container">{retryCountdown}s</span></span>
                                    </div>
                                ) : (
                                    <button onClick={() => { clearInsightsCache(); fetchInsights(true); }}
                                        className="mt-4 px-4 py-2 rounded-lg text-sm transition-colors text-error hover:bg-error/10 border border-error/20">
                                        Retry Analysis
                                    </button>
                                )}
                            </div>
                        ) : insights.length > 0 ? (
                            <div className="space-y-4">
                                {insights.map(insight => (
                                    <div key={insight.id} className={cn("p-5 rounded-2xl border transition-all hover:bg-surface-variant/20 hover:shadow-sm", insight.bg, insight.border)}>
                                        <div className="flex items-start gap-4">
                                            <div className={cn("p-3 rounded-xl bg-surface-container shrink-0", insight.color)}>
                                                {getIcon(insight.type)}
                                            </div>
                                            <div>
                                                <h3 className="text-on-surface font-bold text-lg">{insight.title}</h3>
                                                <p className="text-on-surface-variant text-sm mt-1 leading-relaxed">{insight.desc}</p>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <div className="p-12 text-center text-on-surface-variant rounded-2xl border border-dashed border-outline-variant/50">
                                No transactions found to analyze. Add some transactions to surface insights!
                            </div>
                        )}
                    </div>

                    {/* Health Score */}
                    <div className="p-6 bg-surface-container-lowest ghost-border ambient-shadow rounded-[1.5rem]">
                        <h2 className="text-xl font-bold text-on-surface mb-4 flex items-center gap-2">
                            <Wallet className="w-5 h-5 text-primary" /> Portfolio Health Score
                        </h2>
                        <div className="flex flex-col sm:flex-row items-center gap-6 relative">
                            <div className="w-24 h-24 rounded-full flex items-center justify-center shrink-0 border-[4px] border-surface-container relative z-10 bg-surface-container-lowest"
                                style={{ borderTopColor: "var(--primary)" }}>
                                <span className="text-3xl font-bold text-on-surface">
                                    {healthScore !== null ? healthScore : "--"}
                                    <span className="text-lg text-on-surface-variant font-normal">/100</span>
                                </span>
                            </div>
                            <div className="text-on-surface-variant text-sm leading-relaxed relative z-10">
                                {healthText ? healthText : "Loading scoring algorithms..."}
                            </div>
                        </div>
                    </div>
                </div>

                {/* Right: Chatbot */}
                <div className="lg:col-span-5 h-[600px] lg:h-[800px] flex flex-col">
                    <div className="flex flex-col flex-1 overflow-hidden bg-surface-container-lowest ghost-border shadow-ambient rounded-[1.5rem]">
                        {/* Chat Header */}
                        <div className="p-4 flex items-center gap-3 shrink-0 bg-surface-variant/30 border-b border-outline-variant/30">
                            <div className="w-10 h-10 rounded-full flex items-center justify-center bg-primary-container border border-primary/30 relative shadow-sm">
                                <Bot className="w-5 h-5 text-primary" />
                                <div className="absolute bottom-0 right-0 w-2.5 h-2.5 border-2 rounded-full border-surface-container-lowest bg-[#15B886]" />
                            </div>
                            <div>
                                <h3 className="text-on-surface font-bold">FinAI Assistant</h3>
                                <p className="text-xs text-on-surface-variant">Always active • Deep Context</p>
                            </div>
                        </div>

                        {/* Chat History */}
                        <div className="flex-1 p-4 overflow-y-auto space-y-4">
                            <div className="flex justify-center">
                                <span className="text-xs text-on-surface-variant px-3 py-1 rounded-full bg-surface-container-low border border-outline-variant/30">Conversation started</span>
                            </div>
                            {chatHistory.map((msg, i) => (
                                <div key={i} className={cn("flex gap-3", msg.role === "user" ? "flex-row-reverse" : "")}>
                                    <div className="w-8 h-8 rounded-full flex items-center justify-center border shrink-0 shadow-sm"
                                        style={msg.role === "ai"
                                            ? { background: "var(--primary-container)", borderColor: "var(--primary)" }
                                            : { background: "var(--surface-container-high)", borderColor: "var(--outline)" }}>
                                        {msg.role === "ai" ? <Bot className="w-4 h-4 text-primary" /> : <span className="text-[10px] font-bold text-on-surface">ME</span>}
                                    </div>
                                    <div className="rounded-2xl p-3 max-w-[85%] text-sm shadow-sm"
                                        style={msg.role === "ai"
                                            ? { background: "var(--surface-container-low)", border: "1px solid var(--outline-variant)", borderTopLeftRadius: "4px", color: "var(--on-surface)" }
                                            : { background: "var(--primary)", border: "1px solid var(--primary)", borderTopRightRadius: "4px", color: "var(--on-primary)" }}>
                                        {msg.role === "ai" ? (
                                            <div className="space-y-2">
                                                    <ReactMarkdown components={{
                                                    strong: ({ ...props }: React.ComponentPropsWithoutRef<'strong'>) => <strong className="font-bold" {...props} />,
                                                    ul: ({ ...props }: React.ComponentPropsWithoutRef<'ul'>) => <ul className="list-disc pl-4 space-y-1 my-2" {...props} />,
                                                    ol: ({ ...props }: React.ComponentPropsWithoutRef<'ol'>) => <ol className="list-decimal pl-4 space-y-1 my-2" {...props} />,
                                                    p: ({ ...props }: React.ComponentPropsWithoutRef<'p'>) => <p className="leading-relaxed" {...props} />,
                                                }}>
                                                    {msg.content}
                                                </ReactMarkdown>
                                            </div>
                                        ) : msg.content}
                                    </div>
                                </div>
                            ))}
                            {chatLoading && (
                                <div className="flex gap-3">
                                    <div className="w-8 h-8 rounded-full flex items-center justify-center border bg-primary-container border-primary shrink-0">
                                        <Bot className="w-4 h-4 text-primary" />
                                    </div>
                                    <div className="rounded-2xl rounded-tl-none p-4 max-w-[85%] flex items-center gap-1.5 bg-surface-container-low border border-outline-variant text-on-surface">
                                        {[0, 150, 300].map(d => (
                                            <div key={d} className="w-1.5 h-1.5 rounded-full animate-bounce bg-primary"
                                                style={{ animationDelay: `${d}ms` }} />
                                        ))}
                                    </div>
                                </div>
                            )}
                            <div ref={chatEndRef} />
                        </div>

                        {/* Input */}
                        <div className="p-4 shrink-0 bg-surface-container-lowest border-t border-outline-variant/30">
                            <form className="relative" onSubmit={handleSendMessage}>
                                <input
                                    type="text"
                                    value={message}
                                    onChange={e => setMessage(e.target.value)}
                                    placeholder="Message FinAI..."
                                    className="w-full rounded-xl py-3 pl-4 pr-12 text-sm text-on-surface bg-surface-container-low hover:bg-surface-container border border-transparent outline-none transition-all placeholder:text-outline focus:border-primary/40 focus:shadow-[inset_0_0_8px_rgba(0,108,73,0.05)]"
                                    disabled={chatLoading}
                                />
                                <button type="submit" disabled={message.trim().length === 0 || chatLoading}
                                    className="absolute right-2 top-1/2 -translate-y-1/2 p-2 rounded-lg transition-all"
                                    style={message.trim().length > 0
                                        ? { background: "var(--primary)", boxShadow: "0 0 12px rgba(0,108,73,0.3)" }
                                        : { background: "transparent", opacity: 0.4 }}>
                                    <Send className={cn("w-4 h-4", message.trim().length > 0 ? "text-on-primary" : "text-on-surface-variant")} />
                                </button>
                            </form>
                            <p className="text-[10px] text-on-surface-variant text-center mt-2 flex items-center justify-center gap-1">
                                <Sparkles className="w-3 h-3 text-primary/70" /> AI can make mistakes. Verify critical actions.
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
