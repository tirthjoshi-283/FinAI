// Shown instantly while /dashboard/goals page JS compiles.
export default function GoalsLoading() {
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
                    <div className="h-8 w-48 rounded-xl bg-white/5" />
                    <div className="h-4 w-72 rounded-lg bg-white/5" />
                </div>
                <div className="h-10 w-32 rounded-xl bg-violet-500/10 border border-violet-500/20" />
            </div>

            {/* Goals grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
                {Array.from({ length: 6 }).map((_, i) => (
                    <div key={i} className="p-6 space-y-4" style={card}>
                        <div className="flex justify-between items-start">
                            <div className="space-y-2">
                                <div className="h-5 w-32 rounded-lg bg-white/5" />
                                <div className="h-3 w-20 rounded bg-white/5" />
                            </div>
                            <div className="w-8 h-8 rounded-lg bg-white/5" />
                        </div>
                        <div className="space-y-2">
                            <div className="flex justify-between">
                                <div className="h-3 w-16 rounded bg-white/5" />
                                <div className="h-3 w-12 rounded bg-white/5" />
                            </div>
                            <div className="h-2 w-full rounded-full bg-white/5" />
                        </div>
                        <div className="flex gap-2">
                            <div className="flex-1 h-9 rounded-xl bg-white/5" />
                            <div className="w-9 h-9 rounded-xl bg-white/5" />
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
