"use client";

import { useEffect, useState, useCallback, memo } from "react";
import { Newspaper, ExternalLink, ArrowRight, RefreshCw } from "lucide-react";
import Link from "next/link";

interface Article {
    title: string;
    description: string;
    url: string;
    image: string;
    publishedAt: string;
    source: string;
}

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
    return `${diffDays}d ago`;
}

export const FinanceNewsWidget = memo(function FinanceNewsWidget() {
    const [articles, setArticles] = useState<Article[]>([]);
    const [loading, setLoading] = useState(true);

    const fetchNews = useCallback(async (forceRefresh = false) => {
        setLoading(true);
        try {
            const CACHE_KEY = "finai_news_widget_cache";
            if (!forceRefresh) {
                const cached = sessionStorage.getItem(CACHE_KEY);
                if (cached) {
                    try {
                        const { data, timestamp } = JSON.parse(cached);
                        if (Date.now() - timestamp < 3600000) { // 1 hour
                            setArticles(data);
                            setLoading(false);
                            return;
                        }
                    } catch {}
                }
            }

            const apiUrl = forceRefresh 
                ? `/api/news?count=4&forceRefresh=true&t=${Date.now()}`
                : `/api/news?count=4`;
                
            const res = await fetch(apiUrl);
            if (!res.ok) {
                console.error("Widget news fetch failed:", res.status);
                setArticles([]);
                setLoading(false);
                return;
            }
            const data = await res.json();
            setArticles(data.articles || []);
            sessionStorage.setItem(CACHE_KEY, JSON.stringify({ data: data.articles || [], timestamp: Date.now() }));
            
            if (forceRefresh) {
                setTimeout(() => setLoading(false), 400);
            } else {
                setLoading(false);
            }
        } catch (err) {
            console.error("Widget news fetch error:", err);
            setArticles([]);
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchNews(false);
    }, [fetchNews]);

    return (
        <div className="bg-surface-container-lowest p-6 rounded-xl ghost-border ambient-shadow">
            <div className="flex items-center justify-between mb-5">
                <h2 className="text-xl font-bold text-on-surface flex items-center gap-2">
                    <Newspaper className="w-5 h-5 text-primary" /> Headlines
                </h2>
                <button
                    onClick={() => fetchNews(true)}
                    disabled={loading}
                    className="p-1.5 rounded-lg text-on-surface-variant hover:text-primary hover:bg-surface-variant/30 transition-colors disabled:opacity-40"
                    title="Refresh"
                >
                    <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
                </button>
            </div>

            <div className="space-y-3">
                {loading
                    ? Array.from({ length: 4 }).map((_, i) => (
                          <div key={i} className="animate-pulse flex gap-3">
                              <div className="w-16 h-12 rounded-xl bg-surface-variant flex-shrink-0" />
                              <div className="flex-1 space-y-2">
                                  <div className="h-3 bg-surface-variant rounded w-full" />
                                  <div className="h-2 bg-surface-variant rounded w-2/3" />
                              </div>
                          </div>
                      ))
                    : articles.length === 0
                      ? (
                          <p className="text-on-surface-variant text-sm py-4">No news available right now.</p>
                      )
                      : articles.map((article, i) => (
                          <a
                              key={i}
                              href={article.url}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="flex gap-3 p-2.5 rounded-xl hover:bg-surface-container-low hover:shadow-ambient transition-all group cursor-pointer border border-transparent hover:border-outline-variant/30"
                          >
                              <div className="w-16 h-12 rounded-xl overflow-hidden flex-shrink-0 bg-surface-variant">
                                  {article.image ? (
                                      <img
                                          src={article.image}
                                          alt=""
                                          loading="lazy"
                                          decoding="async"
                                          className="w-full h-full object-cover"
                                          onError={(e) => {
                                              (e.target as HTMLImageElement).style.display = "none";
                                          }}
                                      />
                                  ) : (
                                      <div className="w-full h-full flex items-center justify-center">
                                          <Newspaper className="w-5 h-5 text-outline" />
                                      </div>
                                  )}
                              </div>
                              <div className="flex-1 min-w-0">
                                  <p className="text-on-surface text-sm font-medium leading-tight line-clamp-2 group-hover:text-primary transition-colors">
                                      {article.title}
                                  </p>
                                  <div className="flex items-center gap-2 mt-1">
                                      <span className="text-on-surface-variant text-[11px]">{article.source}</span>
                                      <span className="text-outline text-[11px]">•</span>
                                      <span className="text-on-surface-variant text-[11px]">{timeAgo(article.publishedAt)}</span>
                                  </div>
                              </div>
                              <ExternalLink className="w-3.5 h-3.5 text-outline group-hover:text-primary transition-colors flex-shrink-0 mt-0.5" />
                          </a>
                      ))}
            </div>

            <Link
                href="/dashboard/newsletter"
                className="flex items-center justify-center gap-2 mt-5 pt-4 border-t border-outline-variant/30 text-sm text-on-surface-variant hover:text-primary transition-colors font-medium"
            >
                View All News <ArrowRight className="w-4 h-4" />
            </Link>
        </div>
    );
});
