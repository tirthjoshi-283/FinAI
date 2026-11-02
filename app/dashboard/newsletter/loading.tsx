// Shown instantly while /dashboard/newsletter page JS compiles.
export default function NewsletterLoading() {
    return (
        <div className="w-full max-w-7xl mx-auto space-y-8 relative z-10 animate-pulse">
            {/* Header */}
            <div className="flex justify-between items-center">
                <div className="space-y-2">
                    <div className="h-8 w-56 rounded-xl bg-white/5" />
                    <div className="h-4 w-72 rounded-lg bg-white/5" />
                </div>
                <div className="h-10 w-28 rounded-xl bg-white/5" />
            </div>

            {/* Search */}
            <div className="h-14 rounded-2xl bg-white/5 border border-white/5" />

            {/* Category tabs */}
            <div className="flex gap-2">
                {[0, 1, 2, 3, 4].map(i => (
                    <div key={i} className="h-10 w-28 rounded-xl bg-white/5" />
                ))}
            </div>

            {/* Article grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
                {Array.from({ length: 6 }).map((_, i) => (
                    <div key={i} className="rounded-3xl border border-white/5 bg-black/40 overflow-hidden">
                        <div className="h-44 bg-white/5" />
                        <div className="p-5 space-y-3">
                            <div className="flex items-center gap-2">
                                <div className="h-2.5 bg-white/5 rounded w-16" />
                                <div className="h-2.5 bg-white/5 rounded w-12" />
                            </div>
                            <div className="h-4 bg-white/5 rounded w-full" />
                            <div className="h-4 bg-white/5 rounded w-4/5" />
                            <div className="h-3 bg-white/5 rounded w-full" />
                            <div className="h-3 bg-white/5 rounded w-2/3" />
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
