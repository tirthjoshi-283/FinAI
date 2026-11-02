// Shown instantly while /dashboard/ai-suggestions page JS compiles.
export default function AISuggestionsLoading() {
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
                    <div className="h-8 w-56 rounded-xl bg-white/5" />
                    <div className="h-4 w-72 rounded-lg bg-white/5" />
                </div>
                <div className="h-8 w-36 rounded-full bg-violet-500/10 border border-violet-500/20" />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                {/* Left: Insights */}
                <div className="lg:col-span-7 space-y-6">
                    <div className="p-6 min-h-[400px]" style={card}>
                        <div className="flex justify-between items-center mb-6">
                            <div className="h-6 w-40 rounded-lg bg-white/5" />
                        </div>
                        <div className="space-y-4">
                            {[0, 1, 2].map(i => (
                                <div key={i} className="p-5 rounded-2xl bg-white/5">
                                    <div className="flex items-start gap-4">
                                        <div className="w-12 h-12 rounded-xl bg-white/5 shrink-0" />
                                        <div className="flex-1 space-y-2">
                                            <div className="h-5 w-40 rounded-lg bg-white/5" />
                                            <div className="h-3 w-full rounded bg-white/5" />
                                            <div className="h-3 w-3/4 rounded bg-white/5" />
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Health score */}
                    <div className="p-6" style={card}>
                        <div className="h-6 w-44 rounded-lg bg-white/5 mb-4" />
                        <div className="flex items-center gap-6">
                            <div className="w-24 h-24 rounded-full bg-white/5 shrink-0" />
                            <div className="space-y-2 flex-1">
                                <div className="h-3 w-full rounded bg-white/5" />
                                <div className="h-3 w-4/5 rounded bg-white/5" />
                            </div>
                        </div>
                    </div>
                </div>

                {/* Right: Chat */}
                <div className="lg:col-span-5" style={{ ...card, height: 600 }}>
                    <div className="h-14 border-b border-white/5 flex items-center px-4 gap-3">
                        <div className="w-10 h-10 rounded-full bg-white/5" />
                        <div className="space-y-1">
                            <div className="h-3 w-24 rounded bg-white/5" />
                            <div className="h-2 w-16 rounded bg-white/5" />
                        </div>
                    </div>
                    <div className="p-4 space-y-4">
                        {[0, 1].map(i => (
                            <div key={i} className="flex gap-3">
                                <div className="w-8 h-8 rounded-full bg-white/5 shrink-0" />
                                <div className="h-16 flex-1 rounded-2xl bg-white/5" />
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}
