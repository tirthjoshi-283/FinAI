"use client";

import { useEffect, useState, useCallback } from "react";
import {
    Newspaper,
    ExternalLink,
    RefreshCw,
    Search,
    TrendingUp,
    Globe,
    Bitcoin,
    Landmark,
    Cpu,
    X,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

interface Article {
    title: string;
    description: string;
    url: string;
    image: string;
    publishedAt: string;
    source: string;
}

const CATEGORIES = [
    { label: "Top Stories", query: "finance", icon: TrendingUp },
    { label: "Markets", query: "stock market", icon: Globe },
    { label: "Crypto", query: "cryptocurrency", icon: Bitcoin },
    { label: "Economy", query: "economy", icon: Landmark },
    { label: "Fintech", query: "fintech", icon: Cpu },
];

function timeAgo(dateString: string) {
    const now = new Date();
    const past = new Date(dateString);
    const diffMs = now.getTime() - past.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 1) return "Just now";
    if (diffMins < 60) return `${diffMins}m ago`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays < 7) return `${diffDays}d ago`;
    return past.toLocaleDateString("en-IN", { month: "short", day: "numeric" });
}

function SkeletonCard() {
    return (
        <div className="animate-pulse rounded-3xl border border-outline-variant/20 bg-surface-container-lowest overflow-hidden">
            <div className="h-44 bg-surface-variant" />
            <div className="p-5 space-y-3">
                <div className="flex items-center gap-2">
                    <div className="h-2.5 bg-surface-variant rounded w-16" />
                    <div className="h-2.5 bg-surface-variant rounded w-12" />
                </div>
                <div className="h-4 bg-surface-variant rounded w-full" />
                <div className="h-4 bg-surface-variant rounded w-4/5" />
                <div className="h-3 bg-surface-variant rounded w-full" />
                <div className="h-3 bg-surface-variant rounded w-2/3" />
            </div>
        </div>
    );
}

export function FinanceNews() {
    const [articles, setArticles] = useState<Article[]>([]);
    const [loading, setLoading] = useState(true);
    const [activeCategory, setActiveCategory] = useState(0);
    const [searchQuery, setSearchQuery] = useState("");
    const [isSearching, setIsSearching] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const fetchNews = useCallback(async (query: string, forceRefresh = false) => {
        setLoading(true);
        try {
            const CACHE_KEY = `finai_news_page_${query}`;
            if (!forceRefresh) {
                const cached = sessionStorage.getItem(CACHE_KEY);
                if (cached) {
                    try {
                        const { data, timestamp } = JSON.parse(cached);
                        if (Date.now() - timestamp < 3600000) {
                            setArticles(data);
                            setLoading(false);
                            return;
                        }
                    } catch {}
                }
            }

            const apiUrl = forceRefresh 
                ? `/api/news?q=${encodeURIComponent(query)}&count=10&forceRefresh=true&t=${Date.now()}`
                : `/api/news?q=${encodeURIComponent(query)}&count=10`;
                
            const res = await fetch(apiUrl);
            if (!res.ok) {
                console.error("News fetch failed:", res.status);
                setArticles([]);
                setLoading(false);
                return;
            }
            const data = await res.json();
            const fetchedArticles = data.articles || [];
            
            // Show toast to explain why it didn't change visually
            if (data.cooldown) {
                setToast("Rate limit protection: You're already viewing the latest news.");
            } else if (forceRefresh) {
                const isIdentical = JSON.stringify(fetchedArticles) === JSON.stringify(articles);
                if (isIdentical && fetchedArticles.length > 0) {
                    setToast("Refreshed! The headlines haven't changed yet.");
                } else {
                    setToast("News updated successfully!");
                }
            }
            if (forceRefresh || data.cooldown) {
                setTimeout(() => setToast(null), 4000);
            }

            setArticles(fetchedArticles);
            sessionStorage.setItem(CACHE_KEY, JSON.stringify({ data: fetchedArticles, timestamp: Date.now() }));
            
            // Ensure minimum visual loading time on refresh so the user knows it actually worked
            if (forceRefresh) {
                setTimeout(() => setLoading(false), 400);
            } else {
                setLoading(false);
            }
        } catch (err) {
            console.error("News fetch error:", err);
            setArticles([]);
            setLoading(false);
        }
    }, [articles]);

    // Fetch on initial load only
    useEffect(() => {
        fetchNews(CATEGORIES[0].query, false);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const handleCategoryClick = (index: number) => {
        setActiveCategory(index);
        setIsSearching(false);
        setSearchQuery("");
        fetchNews(CATEGORIES[index].query, false);
    };

    const handleSearch = (e: React.FormEvent) => {
        e.preventDefault();
        if (searchQuery.trim()) {
            setIsSearching(true);
            fetchNews(searchQuery.trim(), false);
        }
    };

    const clearSearch = () => {
        setSearchQuery("");
        setIsSearching(false);
        fetchNews(CATEGORIES[activeCategory].query, false);
    };

    return (
        <div className="w-full max-w-7xl mx-auto space-y-8 relative z-10">
            {/* Header */}
            <header className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-on-surface tracking-tighter flex items-center gap-3">
                        <div className="w-10 h-10 rounded-2xl bg-secondary-container border border-outline-variant/30 flex items-center justify-center">
                            <Newspaper className="w-5 h-5 text-on-secondary-container" />
                        </div>
                        Finance Newsletter
                    </h1>
                    <p className="text-on-surface-variant mt-1">
                        Stay updated with the latest financial news and market insights.
                    </p>
                </div>
                <button
                    onClick={() =>
                        isSearching
                            ? fetchNews(searchQuery, true)
                            : fetchNews(CATEGORIES[activeCategory].query, true)
                    }
                    disabled={loading}
                    className="flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium text-on-surface-variant hover:text-on-surface bg-surface-container hover:bg-surface-variant/50 border border-outline-variant/30 transition-all disabled:opacity-40"
                >
                    <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
                    Refresh
                </button>
            </header>

            {/* Search Bar */}
            <form onSubmit={handleSearch} className="relative">
                <div className="relative rounded-2xl border border-outline-variant/30 bg-surface-container-low overflow-hidden ambient-shadow">
                    <Search className="absolute left-5 top-1/2 -translate-y-1/2 w-5 h-5 text-outline" />
                    <input
                        type="text"
                        placeholder="Search financial news... (e.g. RBI policy, inflation, Tesla earnings)"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="w-full bg-transparent text-on-surface placeholder-on-surface-variant/70 pl-14 pr-20 py-4 outline-none text-sm placeholder:text-outline"
                    />
                    {searchQuery && (
                        <button
                            type="button"
                            onClick={clearSearch}
                            className="absolute right-16 top-1/2 -translate-y-1/2 p-1.5 rounded-lg text-outline hover:text-on-surface hover:bg-surface-variant transition-colors"
                        >
                            <X className="w-4 h-4" />
                        </button>
                    )}
                    <button
                        type="submit"
                        className="absolute right-3 top-1/2 -translate-y-1/2 px-4 py-2 rounded-xl bg-primary-container/10 text-primary-container text-sm font-semibold hover:bg-primary-container/20 transition-colors border border-primary-container/20"
                    >
                        Go
                    </button>
                </div>
            </form>

            {/* Category Tabs */}
            {!isSearching && (
                <div className="flex gap-2 flex-wrap">
                    {CATEGORIES.map((cat, i) => {
                        const Icon = cat.icon;
                        const isActive = activeCategory === i;
                        return (
                            <button
                                key={cat.label}
                                onClick={() => handleCategoryClick(i)}
                                className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium transition-all border ${
                                    isActive
                                        ? "bg-primary-container/10 text-primary border-primary-container/30 shadow-sm"
                                        : "text-on-surface-variant hover:text-on-surface bg-surface-container-lowest hover:bg-surface-variant/50 border-outline-variant/30"
                                }`}
                            >
                                <Icon className="w-4 h-4" />
                                {cat.label}
                            </button>
                        );
                    })}
                </div>
            )}

            {/* Search Active Indicator */}
            {isSearching && (
                <div className="flex items-center gap-3">
                    <div className="flex items-center gap-2 px-4 py-2 rounded-xl bg-primary-container/10 border border-primary-container/20 text-primary-container text-sm font-medium">
                        <Search className="w-4 h-4" />
                        Results for &ldquo;{searchQuery}&rdquo;
                    </div>
                    <button
                        onClick={clearSearch}
                        className="text-on-surface-variant hover:text-on-surface text-sm underline underline-offset-4 transition-colors"
                    >
                        Clear search
                    </button>
                </div>
            )}

            {/* News Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
                <AnimatePresence mode="wait">
                    {loading ? (
                        <motion.div
                            key="skeletons"
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            className="col-span-full grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6"
                        >
                            {Array.from({ length: 6 }).map((_, i) => (
                                <SkeletonCard key={`skeleton-${i}`} />
                            ))}
                        </motion.div>
                    ) : articles.length === 0 ? (
                        <motion.div
                            key="empty"
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            className="col-span-full flex flex-col items-center justify-center py-20 bg-surface-container-lowest rounded-3xl border border-outline-variant/20"
                        >
                            <div className="w-16 h-16 rounded-2xl bg-surface-variant/50 border border-outline-variant/30 flex items-center justify-center mb-4">
                                <Newspaper className="w-8 h-8 text-on-surface-variant" />
                            </div>
                            <p className="text-on-surface text-lg font-bold">No news found</p>
                            <p className="text-on-surface-variant text-sm mt-1">
                                Try a different search term or category
                            </p>
                        </motion.div>
                    ) : (
                        <motion.div
                            key="articles"
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            className="col-span-full grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6"
                        >
                            {articles.map((article, i) => (
                                <motion.a
                                    key={`${article.url}-${i}`}
                                    href={article.url}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    initial={{ opacity: 0, y: 20 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    transition={{ delay: i * 0.03, duration: 0.25 }}
                                    className="group rounded-3xl border border-outline-variant/20 bg-surface-container-lowest overflow-hidden hover:border-primary/30 ambient-shadow-hover transition-all duration-300 flex flex-col h-full"
                                >
                                    {/* Thumbnail */}
                                    <div className="relative h-44 overflow-hidden bg-gradient-to-br from-surface-container-low to-surface-variant/60 flex-shrink-0">
                                        {article.image ? (
                                            <img
                                                src={article.image}
                                                alt=""
                                                loading="lazy"
                                                decoding="async"
                                                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                                                onError={(e) => {
                                                    (e.target as HTMLImageElement).style.display = "none";
                                                }}
                                            />
                                        ) : (
                                            <div className="w-full h-full flex items-center justify-center">
                                                <Newspaper className="w-12 h-12 text-outline" />
                                            </div>
                                        )}
                                        {/* Gradient overlay for better text contrast if we had text overlay */}
                                        <div className="absolute inset-0 bg-gradient-to-t from-black/40 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                                        
                                        {/* Source badge */}
                                        <div className="absolute bottom-3 left-3 flex items-center gap-2">
                                            <span className="px-2.5 py-1 rounded-lg bg-inverse-surface/80 backdrop-blur-md text-[11px] font-semibold text-inverse-on-surface border border-outline-variant/10 shadow-sm">
                                                {article.source}
                                            </span>
                                            <span className="px-2 py-1 rounded-lg bg-surface-container-lowest/90 backdrop-blur-md text-[11px] text-on-surface-variant font-medium border border-outline-variant/20 shadow-sm">
                                                {timeAgo(article.publishedAt)}
                                            </span>
                                        </div>
                                    </div>

                                    {/* Content */}
                                    <div className="p-5 flex flex-col flex-grow">
                                        <h3 className="text-on-surface font-bold leading-snug line-clamp-2 group-hover:text-primary transition-colors">
                                            {article.title}
                                        </h3>
                                        {article.description && (
                                            <p className="text-on-surface-variant text-sm mt-3 line-clamp-2 leading-relaxed flex-grow">
                                                {article.description}
                                            </p>
                                        )}
                                        <div className="flex items-center gap-1.5 mt-5 text-xs text-primary font-semibold uppercase tracking-wider">
                                            Read Article
                                            <ExternalLink className="w-3.5 h-3.5" />
                                        </div>
                                    </div>
                                </motion.a>
                            ))}
                        </motion.div>
                    )}
                </AnimatePresence>
            </div>

            {/* UX Feedback Toast */}
            <AnimatePresence>
                {toast && (
                    <motion.div
                        initial={{ opacity: 0, y: 50 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: 50 }}
                        className="fixed bottom-6 right-6 z-50 px-5 py-3.5 rounded-xl bg-inverse-surface shadow-ambient text-sm font-medium text-inverse-on-surface flex items-center gap-3"
                    >
                        <div className="w-2 h-2 rounded-full bg-primary-fixed animate-pulse" />
                        {toast}
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}
