// Shown instantly by Next.js while /dashboard/charts page JS compiles.
// Eliminates the frozen/blank screen when navigating to this route.

export default function ChartsLoading() {
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
                    <div className="h-8 w-52 rounded-xl bg-white/5" />
                    <div className="h-4 w-80 rounded-lg bg-white/5" />
                </div>
                <div className="flex gap-1 p-1 rounded-xl bg-white/5">
                    {["7D", "1M", "6M", "1Y", "ALL"].map(r => (
                        <div key={r} className="h-7 w-10 rounded-lg bg-white/5" />
                    ))}
                </div>
            </div>

            {/* Stat row */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                {[0, 1, 2, 3].map(i => (
                    <div key={i} className="p-4 rounded-2xl" style={card}>
                        <div className="h-3 w-20 rounded bg-white/5 mb-2" />
                        <div className="h-7 w-28 rounded-lg bg-white/5" />
                    </div>
                ))}
            </div>

            {/* Charts */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
                <div className="lg:col-span-8 space-y-8">
                    {[0, 1].map(i => (
                        <div key={i} className="p-6 h-[400px] flex flex-col" style={card}>
                            <div className="h-6 w-44 rounded-lg bg-white/5 mb-6" />
                            <div className="flex-1 flex items-end gap-2">
                                {[30, 55, 40, 75, 50, 90, 35, 60, 80, 45, 70, 95].map((h, j) => (
                                    <div
                                        key={j}
                                        className="flex-1 rounded-t-sm"
                                        style={{
                                            height: `${h}%`,
                                            background: j % 2 === 0
                                                ? "rgba(124,58,237,0.14)"
                                                : "rgba(34,211,238,0.10)",
                                        }}
                                    />
                                ))}
                            </div>
                        </div>
                    ))}
                </div>

                <div className="lg:col-span-4">
                    <div className="p-6" style={card}>
                        <div className="h-6 w-40 rounded-lg bg-white/5 mb-4" />
                        {/* Pie placeholder */}
                        <div className="w-40 h-40 rounded-full mx-auto bg-white/5 border-[16px] border-violet-500/10" />
                        <div className="space-y-3 mt-6">
                            {[0, 1, 2, 3].map(i => (
                                <div key={i} className="flex justify-between items-center p-2 rounded-xl">
                                    <div className="flex items-center gap-2">
                                        <div className="w-3 h-3 rounded-full bg-white/10" />
                                        <div className="h-3 w-20 rounded bg-white/5" />
                                    </div>
                                    <div className="h-3 w-16 rounded bg-white/5" />
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
