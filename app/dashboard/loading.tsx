import { Loader2 } from "lucide-react";

export default function DashboardLoading() {
    return (
        <div className="w-full max-w-7xl mx-auto space-y-8 relative z-10 p-4 md:p-8 animate-pulse">
            {/* Header Skeleton */}
            <header className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8 relative">
                <div>
                    <div className="h-10 w-64 bg-surface-container-high rounded-lg mb-3"></div>
                    <div className="h-5 w-96 bg-surface-container rounded-lg"></div>
                </div>
            </header>

            {/* Top Stat Cards Skeleton */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {[1, 2, 3].map((i) => (
                    <div key={i} className="bg-surface-container-lowest p-6 rounded-[1.5rem] ghost-border h-[160px] flex flex-col justify-between">
                        <div className="flex justify-between items-center">
                            <div className="h-6 w-32 bg-surface-container-high rounded-full"></div>
                            <div className="w-12 h-12 rounded-full bg-surface-container-highest"></div>
                        </div>
                        <div className="space-y-3">
                            <div className="h-8 w-48 bg-surface-container-high rounded-lg"></div>
                            <div className="h-4 w-24 bg-surface-container rounded-lg"></div>
                        </div>
                    </div>
                ))}
            </div>

            {/* Main Content Area Skeleton */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mt-8">
                {/* Left massive block */}
                <div className="lg:col-span-2 h-[500px] bg-surface-container-lowest rounded-[2rem] ghost-border p-6 flex flex-col gap-6">
                    <div className="h-8 w-1/3 bg-surface-container-high rounded-lg"></div>
                    <div className="h-full w-full bg-surface-container rounded-xl"></div>
                </div>
                {/* Right side block */}
                <div className="lg:col-span-1 h-[500px] bg-surface-container-lowest rounded-[2rem] ghost-border p-6 flex flex-col gap-4">
                    <div className="h-8 w-1/2 bg-surface-container-high rounded-lg mb-2"></div>
                    {[1, 2, 3, 4, 5].map((i) => (
                        <div key={i} className="h-16 w-full bg-surface-container rounded-xl"></div>
                    ))}
                </div>
            </div>

            {/* Centered Spinner overlay indicating transition */}
            <div className="fixed inset-0 pointer-events-none flex items-center justify-center z-50">
                <div className="bg-surface-container-highest/50 backdrop-blur-sm p-4 rounded-full shadow-2xl transition-all duration-300">
                    <Loader2 className="w-8 h-8 text-primary animate-spin" />
                </div>
            </div>
        </div>
    );
}
