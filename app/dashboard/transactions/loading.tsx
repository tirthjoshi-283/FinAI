// Shown instantly by Next.js while /dashboard/transactions page JS compiles.
// No JavaScript executes — this is pure static HTML that renders in <10ms.

export default function TransactionsLoading() {
    const card = {
        background: "rgba(13,13,26,0.70)",
        border: "1px solid rgba(255,255,255,0.07)",
        borderRadius: "1.5rem",
    };

    return (
        <div className="w-full max-w-7xl mx-auto space-y-8 relative z-10 pb-20 animate-pulse">
            {/* Header */}
            <div className="flex justify-between items-center">
                <div className="space-y-2">
                    <div className="h-8 w-64 rounded-xl bg-white/5" />
                    <div className="h-4 w-80 rounded-lg bg-white/5" />
                </div>
                <div className="h-10 w-36 rounded-xl bg-violet-500/10 border border-violet-500/20" />
            </div>

            {/* Filters skeleton */}
            <div className="flex gap-4">
                <div className="h-10 w-64 rounded-xl bg-white/5" />
                <div className="h-10 flex-1 rounded-xl bg-white/5" />
            </div>

            {/* Transaction ledger card */}
            <div style={card} className="overflow-hidden">
                {/* Table header */}
                <div
                    className="px-6 py-4 border-b"
                    style={{ borderColor: "rgba(124,58,237,0.10)", background: "rgba(124,58,237,0.04)" }}
                >
                    <div className="h-5 w-36 rounded bg-white/5" />
                </div>

                {/* Rows */}
                <div className="divide-y" style={{ borderColor: "rgba(255,255,255,0.05)" }}>
                    {Array.from({ length: 8 }).map((_, i) => (
                        <div
                            key={i}
                            className="p-4 sm:p-6 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4"
                        >
                            <div className="flex items-center gap-4">
                                <div className="w-12 h-12 rounded-xl bg-white/5 flex-shrink-0" />
                                <div className="space-y-2">
                                    <div className="h-4 w-48 rounded bg-white/5" />
                                    <div className="h-3 w-32 rounded bg-white/5" />
                                </div>
                            </div>
                            <div className="h-6 w-24 rounded-lg bg-white/5" />
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}
