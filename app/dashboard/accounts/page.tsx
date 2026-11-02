"use client";

import { useMemo } from "react";
import { useDashboard } from "@/components/DashboardProvider";
import { ResponsiveContainer, AreaChart, Area, XAxis, Tooltip, YAxis } from "recharts";
import { Wallet, Plus, ArrowUpRight, ArrowDownRight, Building2, Banknote, CreditCard, GraduationCap, Loader2 } from "lucide-react";
import dynamic from "next/dynamic";

const NetWorthVisualizer = dynamic(
    () => import("@/components/dashboard/NetWorthVisualizer").then(mod => mod.NetWorthVisualizer),
    { 
        ssr: false, 
        loading: () => (
            <div className="w-full h-full flex items-center justify-center bg-surface-container-low/50 animate-pulse rounded-xl">
                <Loader2 className="w-8 h-8 text-primary animate-spin opacity-50" />
            </div>
        )
    }
);

const MOCK_HISTORICAL_DATA = [
    { month: "Jan", netWorth: 1850000 },
    { month: "Feb", netWorth: 1920000 },
    { month: "Mar", netWorth: 1890000 },
    { month: "Apr", netWorth: 2100000 },
    { month: "May", netWorth: 2350000 },
    { month: "Jun", netWorth: 2458930 },
];

const MOCK_ASSETS = [
    { id: 1, name: "HDFC Savings", value: 345000, icon: Banknote, trend: "up", percentage: 2.4 },
    { id: 2, name: "Zerodha Portfolio", value: 1250000, icon: ArrowUpRight, trend: "up", percentage: 8.5 },
    { id: 3, name: "Real Estate (Plot)", value: 850000, icon: Building2, trend: "stable", percentage: 0.5 },
];

const MOCK_LIABILITIES = [
    { id: 1, name: "SBI Home Loan", value: -450000, icon: Building2, trend: "down", percentage: 1.2 },
    { id: 2, name: "Education Loan", value: -120000, icon: GraduationCap, trend: "down", percentage: 3.5 },
    { id: 3, name: "HDFC Credit Card", value: -45000, icon: CreditCard, trend: "up", percentage: 12.0 },
];

export default function AccountsPage() {
    const { transactions } = useDashboard();
    
    // In a real app, this would dynamically sum sub-accounts, but we'll use a mix of our dashboard balance + mock structure
    const { totalBalance } = useMemo(() => {
        let credit = 0, debit = 0;
        for (const t of transactions) {
            if (t.type === "credit") credit += t.amount;
            else debit += t.amount;
        }
        return { totalBalance: credit - debit };
    }, [transactions]);

    return (
        <div className="w-full flex flex-col gap-8">
            {/* Header */}
            <header className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-2">
                <div>
                    <h1 className="text-3xl lg:text-4xl font-bold text-on-surface tracking-tight mb-2 flex items-center gap-3">
                        <Wallet className="w-8 h-8 text-primary" />
                        Assets & Liabilities
                    </h1>
                    <p className="text-on-surface-variant">
                        Comprehensive ledger of your entire wealth ecosystem.
                    </p>
                </div>
            </header>

            {/* Net Worth Hero Card */}
            <div className="bg-surface-container-lowest rounded-[2rem] p-8 lg:p-12 relative overflow-hidden ambient-shadow ghost-border flex flex-col lg:flex-row items-center justify-between min-h-[350px]">
                {/* Text Side */}
                <div className="w-full lg:w-1/2 relative z-10">
                    <span className="text-xs uppercase tracking-[0.1em] text-primary font-semibold mb-2 block">
                        Net Worth Equivalent
                    </span>
                    <h2 className="text-5xl lg:text-7xl font-extrabold tracking-tighter mb-4 text-on-surface">
                        ₹{new Intl.NumberFormat('en-IN').format(totalBalance)}
                    </h2>
                    <div className="flex items-center gap-2 mt-4">
                        <div className="bg-primary-container/20 text-primary-container px-3 py-1.5 rounded-full flex items-center gap-1.5 text-sm font-medium border border-primary-container/30">
                            <ArrowUpRight className="w-4 h-4" />
                            +12.4% YTD
                        </div>
                        <span className="text-sm text-on-surface-variant ml-2">Consistent accumulation detected</span>
                    </div>
                </div>

                {/* 3D Visualizer Side */}
                <div className="w-full lg:w-1/2 h-[300px] lg:h-full absolute right-0 bottom-0 lg:top-0 opacity-50 lg:opacity-100 mix-blend-luminosity lg:mix-blend-normal">
                    <NetWorthVisualizer />
                </div>
            </div>

            {/* Real-Time Fluctuation Chart */}
            <div className="bg-surface-container-lowest rounded-3xl p-6 lg:p-8 ambient-shadow ghost-border">
                <div className="flex justify-between items-center mb-6">
                    <h3 className="font-bold text-on-surface text-lg">Wealth Trajectory</h3>
                    <span className="text-xs font-semibold uppercase tracking-wider text-on-surface-variant bg-surface-container px-3 py-1 rounded-full">YTD 2026</span>
                </div>
                <div className="w-full h-[250px]">
                    <ResponsiveContainer width="100%" height="100%">
                        <AreaChart data={MOCK_HISTORICAL_DATA} margin={{ top: 10, right: 0, left: -20, bottom: 0 }}>
                            <defs>
                                <linearGradient id="glowGradient" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.4} />
                                    <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                                </linearGradient>
                            </defs>
                            <XAxis dataKey="month" axisLine={false} tickLine={false} tick={{ fill: '#6c7a71', fontSize: 12 }} dy={10} />
                            <YAxis axisLine={false} tickLine={false} tick={{ fill: '#6c7a71', fontSize: 12 }} tickFormatter={(val) => `₹${(val/100000).toFixed(1)}L`} />
                            <Tooltip 
                                contentStyle={{ backgroundColor: 'white', borderRadius: '12px', border: 'none', boxShadow: '0 10px 25px -5px rgba(6,78,59,0.1)' }}
                                itemStyle={{ color: '#006c49', fontWeight: 600 }}
                                cursor={{ stroke: '#a8cfbc', strokeWidth: 1, strokeDasharray: '4 4' }}
                                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                                formatter={(value: any) => [`₹${new Intl.NumberFormat('en-IN').format(value)}`, 'Net Worth']}
                            />
                            <Area type="monotone" dataKey="netWorth" stroke="#10b981" strokeWidth={3} fillOpacity={1} fill="url(#glowGradient)" activeDot={{ r: 6, fill: '#006c49', stroke: 'white', strokeWidth: 2 }} />
                        </AreaChart>
                    </ResponsiveContainer>
                </div>
            </div>

            {/* Dual Column Layout */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                
                {/* Assets Column */}
                <div className="flex flex-col gap-4">
                    <div className="flex justify-between items-end mb-2">
                        <div>
                            <h3 className="font-bold text-on-surface text-2xl">Assets</h3>
                            <p className="text-sm text-primary font-medium mt-1">Total: ₹{new Intl.NumberFormat('en-IN').format(2445000)}</p>
                        </div>
                        <button className="w-10 h-10 rounded-full bg-surface-container hover:bg-surface-variant flex items-center justify-center text-on-surface-variant hover:text-primary transition-colors">
                            <Plus className="w-5 h-5" />
                        </button>
                    </div>

                    {MOCK_ASSETS.map((asset) => (
                        <div key={asset.id} className="bg-surface-container-lowest rounded-2xl p-5 border border-outline-variant/20 hover:border-primary/30 transition-colors ambient-shadow-hover group flex items-center justify-between">
                            <div className="flex items-center gap-4">
                                <div className="w-12 h-12 rounded-xl bg-primary-container/10 flex items-center justify-center text-primary group-hover:scale-110 transition-transform">
                                    <asset.icon className="w-6 h-6" />
                                </div>
                                <div>
                                    <h4 className="font-semibold text-on-surface">{asset.name}</h4>
                                    <p className="text-xs text-on-surface-variant mt-0.5">Asset</p>
                                </div>
                            </div>
                            <div className="text-right">
                                <p className="font-bold text-on-surface">₹{new Intl.NumberFormat('en-IN').format(asset.value)}</p>
                                <p className={`text-xs font-medium mt-1 flex items-center justify-end gap-1 ${asset.trend === 'up' ? 'text-primary' : 'text-outline'}`}>
                                    {asset.trend === 'up' ? <ArrowUpRight className="w-3 h-3" /> : null}
                                    {asset.percentage}%
                                </p>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Liabilities Column */}
                <div className="flex flex-col gap-4">
                    <div className="flex justify-between items-end mb-2">
                        <div>
                            <h3 className="font-bold text-on-surface text-2xl">Liabilities</h3>
                            <p className="text-sm text-error font-medium mt-1">Total: -₹{new Intl.NumberFormat('en-IN').format(615000)}</p>
                        </div>
                        <button className="w-10 h-10 rounded-full bg-surface-container hover:bg-surface-variant flex items-center justify-center text-on-surface-variant hover:text-error transition-colors">
                            <Plus className="w-5 h-5" />
                        </button>
                    </div>

                    {MOCK_LIABILITIES.map((liability) => (
                        <div key={liability.id} className="bg-surface-container-lowest rounded-2xl p-5 border border-outline-variant/20 hover:border-error/30 transition-colors ambient-shadow-hover group flex items-center justify-between">
                            <div className="flex items-center gap-4">
                                <div className="w-12 h-12 rounded-xl bg-error-container flex items-center justify-center text-on-error-container group-hover:scale-110 transition-transform">
                                    <liability.icon className="w-6 h-6" />
                                </div>
                                <div>
                                    <h4 className="font-semibold text-on-surface">{liability.name}</h4>
                                    <p className="text-xs text-on-surface-variant mt-0.5">Liability</p>
                                </div>
                            </div>
                            <div className="text-right">
                                <p className="font-bold text-on-surface">-₹{new Intl.NumberFormat('en-IN').format(Math.abs(liability.value))}</p>
                                <p className={`text-xs font-medium mt-1 flex items-center justify-end gap-1 ${liability.trend === 'down' ? 'text-primary' : 'text-error'}`}>
                                    {liability.trend === 'down' ? <ArrowDownRight className="w-3 h-3" /> : <ArrowUpRight className="w-3 h-3" />}
                                    {liability.percentage}%
                                </p>
                            </div>
                        </div>
                    ))}
                </div>

            </div>
        </div>
    );
}
