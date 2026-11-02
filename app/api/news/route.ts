import { NextRequest, NextResponse } from "next/server";

interface TransformedArticle {
    title: string;
    description: string;
    url: string;
    image: string;
    publishedAt: string;
    source: string;
    sourceUrl: string;
}

// Multi-query in-memory cache to stay within GNews free-tier limits (100 req/day)
const cache = new Map<string, { articles: TransformedArticle[]; timestamp: number }>();
const CACHE_DURATION = 15 * 60 * 1000; // 15 minutes

export async function GET(request: NextRequest) {
    try {
        const { searchParams } = new URL(request.url);
        const query = searchParams.get("q") || "finance";
        const count = Math.min(parseInt(searchParams.get("count") || "10"), 10);
        const forceRefresh = searchParams.get("forceRefresh") === "true";

        const cached = cache.get(query);
        
        // 1. If NOT forcing a refresh, use the 15-minute cache
        if (!forceRefresh && cached && Date.now() - cached.timestamp < CACHE_DURATION) {
            return NextResponse.json(
                { articles: cached.articles.slice(0, count), fromCache: true },
                {
                    headers: {
                        // Publicly cacheable — 15 min fresh, serve stale for up to 30 min while revalidating
                        "Cache-Control": "public, s-maxage=900, stale-while-revalidate=1800",
                    },
                }
            );
        }

        // 2. If forcing a refresh, enforce a 60-second cooldown to protect the free API quota (100 req/day)
        if (forceRefresh && cached && Date.now() - cached.timestamp < 60 * 1000) {
            return NextResponse.json({
                articles: cached.articles.slice(0, count),
                fromCache: true,
                cooldown: true
            });
        }

        const apiKey = process.env.GNEWS_API_KEY;

        if (!apiKey || apiKey === "your_gnews_api_key_here") {
            return NextResponse.json({
                articles: [],
                fromCache: false,
                message: "GNEWS_API_KEY is not configured in .env.local",
            });
        }

        // GNews API pattern: https://gnews.io/api/v4/{endpoint}?{parameters}&apikey=YOUR_API_KEY
        let apiUrl: string;
        if (query === "finance") {
            apiUrl = `https://gnews.io/api/v4/top-headlines?category=business&lang=en&max=${count}&apikey=${apiKey}`;
        } else {
            apiUrl = `https://gnews.io/api/v4/search?q=${encodeURIComponent(query + " finance")}&lang=en&max=${count}&apikey=${apiKey}`;
        }

        const fetchOptions: RequestInit = forceRefresh
            ? { cache: "no-store" } // Bypass Next.js Data Cache
            : { next: { revalidate: 900 } }; // Cache for 15 mins by default

        const response = await fetch(apiUrl, fetchOptions);

        if (!response.ok) {
            const errorText = await response.text();
            console.error("GNews API error:", response.status, errorText);
            return NextResponse.json(
                { error: "Failed to fetch news", articles: [] },
                { status: 200 }
            );
        }

        const data = await response.json();

        // Transform articles to a clean flat shape
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const articles: TransformedArticle[] = (data.articles || []).map((article: any) => ({
            title: article.title || "",
            description: article.description || "",
            url: article.url || "",
            image: article.image || "",
            publishedAt: article.publishedAt || "",
            source: typeof article.source === "string" ? article.source : article.source?.name || "Unknown",
            sourceUrl: typeof article.source === "string" ? "" : article.source?.url || "",
        }));

        // Cache the TRANSFORMED articles per query
        cache.set(query, {
            articles,
            timestamp: Date.now(),
        });

        return NextResponse.json({ articles, fromCache: false });
    } catch (error) {
        console.error("News API route error:", error);
        return NextResponse.json(
            { error: "Internal server error", articles: [] },
            { status: 200 }
        );
    }
}
