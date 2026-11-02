/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import { useState, useMemo } from "react";
import { useDashboard } from "@/components/DashboardProvider";
import dynamic from "next/dynamic";
import { format, subDays, subMonths, isAfter } from "date-fns";
import { BarChart3, Clock, PieChart as PieChartIcon, Smartphone } from "lucide-react";

type TimeRange = "7D" | "1M" | "6M" | "1Y" | "ALL";

// ── Vivid, theme-agnostic multi-color palette for pie ──
const PIE_COLORS = [
    "#7C3AED", // violet
    "#06B6D4", // cyan
    "#F59E0B", // amber
    "#10B981", // emerald
    "#F43F5E", // rose
    "#3B82F6", // blue
    "#A855F7", // purple
    "#F97316", // orange
];

// Credit / Debit accent colors that pop in both themes
const LINE_CREDIT = "#34D399"; // bright emerald-400
const LINE_DEBIT  = "#FB7185"; // bright rose-400
const BAR_CREDIT  = "#22D3EE"; // cyan-400
const BAR_DEBIT   = "#FB923C"; // orange-400

const tooltipStyle = {
    backgroundColor: "var(--surface-container)",
    borderColor: "var(--outline)",
    color: "var(--on-surface)",
    borderRadius: "12px",
    border: "1px solid var(--outline)",
    boxShadow: "0 20px 40px -10px rgba(0,0,0,0.25)",
    fontSize: "13px",
};

// ── Chart skeleton shared by all loading states ──
function ChartSkeleton({ height = 400 }: { height?: number }) {
    return (
        <div className="w-full flex items-end gap-1.5 px-2" style={{ height }}>
            {[30, 55, 40, 75, 50, 90, 35, 60, 80, 45, 70, 95].map((h, i) => (
                <div key={i} className="flex-1 rounded-t-sm animate-pulse"
                    style={{ height: `${h}%`, background: i % 2 === 0 ? "rgba(124,58,237,0.15)" : "rgba(124,58,237,0.07)", animationDelay: `${i * 60}ms` }} />
            ))}
        </div>
    );
}

// ── Dynamically import LineChart ──
const DynamicLineChart = dynamic(
    () => import("recharts").then((m) => {
        const { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } = m;
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        function LC({ data }: { data: any[] }) {
            return (
                <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={data} margin={{ top: 10, right: 20, left: -10, bottom: 0 }}>
                        <defs>
                            <linearGradient id="creditGradient" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%"  stopColor={LINE_CREDIT} stopOpacity={0.2} />
                                <stop offset="95%" stopColor={LINE_CREDIT} stopOpacity={0} />
                            </linearGradient>
                            <linearGradient id="debitGradient" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%"  stopColor={LINE_DEBIT} stopOpacity={0.2} />
                                <stop offset="95%" stopColor={LINE_DEBIT} stopOpacity={0} />
                            </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" stroke="rgba(150,150,200,0.1)" vertical={false} />
                        <XAxis dataKey="date" stroke="var(--on-surface-variant)" fontSize={11} tickLine={false} axisLine={false} />
                        <YAxis stroke="var(--on-surface-variant)" fontSize={11} tickFormatter={v => `₹${v}`} tickLine={false} axisLine={false} width={70} />
                        <Tooltip contentStyle={tooltipStyle} formatter={(v: any) => [`₹${Number(v).toFixed(2)}`]} />
                        <Legend iconType="circle" wrapperStyle={{ fontSize: "12px", paddingTop: "12px" }} />
                        <Line
                            type="monotone" dataKey="credit" name="Credit"
                            stroke={LINE_CREDIT} strokeWidth={3}
                            dot={{ r: 4, strokeWidth: 0, fill: LINE_CREDIT }}
                            activeDot={{ r: 6, strokeWidth: 2, stroke: "var(--surface-container-lowest)" }}
                            isAnimationActive={true} animationBegin={200} animationDuration={1200}
                        />
                        <Line
                            type="monotone" dataKey="debit" name="Debit"
                            stroke={LINE_DEBIT} strokeWidth={3}
                            dot={{ r: 4, strokeWidth: 0, fill: LINE_DEBIT }}
                            activeDot={{ r: 6, strokeWidth: 2, stroke: "var(--surface-container-lowest)" }}
                            isAnimationActive={true} animationBegin={400} animationDuration={1200}
                        />
                    </LineChart>
                </ResponsiveContainer>
            );
        }
        return LC;
    }),
    { ssr: false, loading: () => <ChartSkeleton /> }
);

// ── Dynamically import BarChart ──
const DynamicBarChart = dynamic(
    () => import("recharts").then((m) => {
        const { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } = m;
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        function BC({ data }: { data: any[] }) {
            return (
                <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={data} margin={{ top: 10, right: 20, left: -10, bottom: 0 }} barGap={4}>
                        <defs>
                            <linearGradient id="barCredit" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="0%"   stopColor={BAR_CREDIT} stopOpacity={1} />
                                <stop offset="100%" stopColor={BAR_CREDIT} stopOpacity={0.4} />
                            </linearGradient>
                            <linearGradient id="barDebit" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="0%"   stopColor={BAR_DEBIT} stopOpacity={1} />
                                <stop offset="100%" stopColor={BAR_DEBIT} stopOpacity={0.4} />
                            </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" stroke="rgba(150,150,200,0.1)" vertical={false} />
                        <XAxis dataKey="date" stroke="var(--on-surface-variant)" fontSize={11} tickLine={false} axisLine={false} />
                        <YAxis stroke="var(--on-surface-variant)" fontSize={11} tickFormatter={v => `₹${v}`} tickLine={false} axisLine={false} width={70} />
                        <Tooltip cursor={{ fill: "rgba(150,150,200,0.05)" }} contentStyle={tooltipStyle} formatter={(v: any) => [`₹${Number(v).toFixed(2)}`]} />
                        <Legend iconType="circle" wrapperStyle={{ fontSize: "12px", paddingTop: "12px" }} />
                        <Bar dataKey="credit" name="Credit" fill="url(#barCredit)" radius={[6, 6, 0, 0]} barSize={14}
                            isAnimationActive={true} animationBegin={300} animationDuration={1000} />
                        <Bar dataKey="debit"  name="Debit"  fill="url(#barDebit)"  radius={[6, 6, 0, 0]} barSize={14}
                            isAnimationActive={true} animationBegin={500} animationDuration={1000} />
                    </BarChart>
                </ResponsiveContainer>
            );
        }
        return BC;
    }),
    { ssr: false, loading: () => <ChartSkeleton /> }
);

// ── Dynamically import PieChart ──
const DynamicPieChart = dynamic(
    () => import("recharts").then((m) => {
        const { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } = m;
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        function PC({ data }: { data: { name: string; value: number }[] }) {
            return (
                <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                        <Pie
                            data={data}
                            cx="50%" cy="50%"
                            innerRadius={55} outerRadius={90}
                            paddingAngle={4}
                            dataKey="value"
                            stroke="none"
                            isAnimationActive={true}
                            animationBegin={100}
                            animationDuration={900}
                            animationEasing="ease-out"
                        >
                            {data.map((_, index) => (
                                <Cell
                                    key={`cell-${index}`}
                                    fill={PIE_COLORS[index % PIE_COLORS.length]}
                                    opacity={0.92}
                                />
                            ))}
                        </Pie>
                        <Tooltip
                            formatter={(v: any) => [`₹${Number(v).toFixed(2)}`]}
                            contentStyle={tooltipStyle}
                        />
                    </PieChart>
                </ResponsiveContainer>
            );
        }
        return PC;
    }),
    { ssr: false, loading: () => <ChartSkeleton height={250} /> }
);

export default function ChartsPage() {
    const { transactions } = useDashboard();
    const [timeRange, setRange] = useState<TimeRange>("1M");

    const filteredTransactions = useMemo(() => {
        const now = new Date();
        const cutoff =
            timeRange === "7D" ? subDays(now, 7) :
            timeRange === "1M" ? subMonths(now, 1) :
            timeRange === "6M" ? subMonths(now, 6) :
            timeRange === "1Y" ? subMonths(now, 12) : new Date(0);
        return transactions.filter(t => isAfter(t.date, cutoff));
    }, [transactions, timeRange]);

    const timeSeriesData = useMemo(() => {
        const grouped: Record<string, { date: string; timestamp: number; debit: number; credit: number }> = {};
        filteredTransactions.forEach(t => {
            const dateStr = format(t.date, "MMM dd");
            if (!grouped[dateStr]) grouped[dateStr] = { date: dateStr, timestamp: t.date.getTime(), debit: 0, credit: 0 };
            if (t.type === "debit") grouped[dateStr].debit += t.amount;
            else grouped[dateStr].credit += t.amount;
        });
        const data = Object.values(grouped).sort((a, b) => a.timestamp - b.timestamp);
        if (data.length === 0) data.push({ date: format(new Date(), "MMM dd"), timestamp: Date.now(), debit: 0, credit: 0 });
        return data;
    }, [filteredTransactions]);

    const categoryData = useMemo(() => {
        const grouped: Record<string, number> = {};
        filteredTransactions.forEach(t => { grouped[t.category] = (grouped[t.category] || 0) + t.amount; });
        return Object.entries(grouped).map(([name, value]) => ({ name, value })).sort((a, b) => b.value - a.value);
    }, [filteredTransactions]);

    const upiRangeData = useMemo(() => {
        const ranges: Record<string, number> = {
            "₹10 - ₹500": 0,
            "₹501 - ₹1000": 0,
            "₹1001+": 0,
        };
        filteredTransactions.forEach(t => {
            if (t.source === "gmail_upi" && t.type === "debit") {
                if (t.amount >= 10 && t.amount <= 500) ranges["₹10 - ₹500"] += t.amount;
                else if (t.amount >= 501 && t.amount <= 1000) ranges["₹501 - ₹1000"] += t.amount;
                else if (t.amount >= 1001) ranges["₹1001+"] += t.amount;
            }
        });
        return Object.entries(ranges)
            .filter(([, value]) => value > 0)
            .map(([name, value]) => ({ name, value }));
    }, [filteredTransactions]);

    const totalDebitForPeriod = filteredTransactions.reduce((acc, t) => t.type === "debit" ? acc + t.amount : acc, 0);
    const totalCreditForPeriod = filteredTransactions.reduce((acc, t) => t.type === "credit" ? acc + t.amount : acc, 0);

    return (
        <div className="w-full max-w-7xl mx-auto space-y-8 relative z-10 pb-20">

            {/* ── Page entry animations ── */}
            <style>{`
                @keyframes chartFadeUp {
                    from { opacity: 0; transform: translateY(28px); }
                    to   { opacity: 1; transform: translateY(0); }
                }
                .chart-card {
                    animation: chartFadeUp 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;
                }
                .chart-card:nth-child(1) { animation-delay: 0ms; }
                .chart-card:nth-child(2) { animation-delay: 80ms; }
                .chart-card:nth-child(3) { animation-delay: 160ms; }
                .chart-card:nth-child(4) { animation-delay: 240ms; }
                .chart-card:nth-child(5) { animation-delay: 320ms; }
            `}</style>

            {/* Header */}
            <header className="chart-card flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-on-surface tracking-tighter flex items-center gap-3">
                        <BarChart3 className="w-8 h-8 text-primary" /> Deep Analytics
                    </h1>
                    <p className="text-on-surface-variant mt-0.5">Advanced cashflow and categorization visualizations.</p>
                </div>
                <div className="flex items-center gap-2 p-1 rounded-xl bg-surface-container-low border border-outline/30 shadow-sm">
                    <Clock className="w-4 h-4 text-on-surface-variant ml-2" />
                    {(["7D", "1M", "6M", "1Y", "ALL"] as TimeRange[]).map(range => (
                        <button key={range} onClick={() => setRange(range)}
                            className="px-4 py-1.5 rounded-lg text-xs font-bold transition-all duration-200"
                            style={timeRange === range ? {
                                background: "var(--primary)", color: "var(--on-primary)",
                                boxShadow: "0 2px 8px rgba(0,0,0,0.2)",
                            } : { color: "var(--on-surface-variant)", border: "1px solid transparent" }}
                            onMouseEnter={e => { if (timeRange !== range) (e.currentTarget.style.color = "var(--on-surface)"); }}
                            onMouseLeave={e => { if (timeRange !== range) (e.currentTarget.style.color = "var(--on-surface-variant)"); }}>
                            {range}
                        </button>
                    ))}
                </div>
            </header>

            {/* ── Stat row ── */}
            <div className="chart-card grid grid-cols-2 md:grid-cols-4 gap-4">
                {[
                    { label: "Period Debits",  value: `₹${totalDebitForPeriod.toFixed(2)}`,  color: BAR_DEBIT,   glow: "rgba(251,146,60,0.15)" },
                    { label: "Period Credits", value: `₹${totalCreditForPeriod.toFixed(2)}`, color: LINE_CREDIT, glow: "rgba(52,211,153,0.15)" },
                    { label: "Net Diff",
                      value: `₹${Math.abs(totalCreditForPeriod - totalDebitForPeriod).toFixed(2)}`,
                      color: totalCreditForPeriod >= totalDebitForPeriod ? LINE_CREDIT : BAR_DEBIT,
                      glow:  totalCreditForPeriod >= totalDebitForPeriod ? "rgba(52,211,153,0.15)" : "rgba(251,146,60,0.15)" },
                    { label: "Transactions",   value: String(filteredTransactions.length),   color: "#A78BFA",   glow: "rgba(167,139,250,0.15)" },
                ].map(s => (
                    <div key={s.label}
                        className="p-4 rounded-2xl bg-surface-container-lowest ghost-border ambient-shadow transition-all duration-300 hover:-translate-y-1 hover:shadow-lg"
                        style={{ boxShadow: `0 0 0 1px var(--outline), 0 8px 32px ${s.glow}` }}>
                        <div className="text-xs font-semibold text-on-surface-variant uppercase tracking-wider mb-2">{s.label}</div>
                        <div className="text-xl font-extrabold tracking-tight" style={{ color: s.color }}>{s.value}</div>
                    </div>
                ))}
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
                <div className="lg:col-span-8 space-y-8">

                    {/* Line chart */}
                    <div className="chart-card p-6 h-[400px] flex flex-col bg-surface-container-lowest ghost-border ambient-shadow rounded-[1.5rem]">
                        <h2 className="text-lg font-bold text-on-surface mb-1">Debit vs Credit <span className="text-on-surface-variant font-normal text-sm">(Trend)</span></h2>
                        <p className="text-xs text-on-surface-variant mb-4">Net cashflow movement over selected period</p>
                        <div className="flex-1 w-full min-h-0">
                            <DynamicLineChart data={timeSeriesData} />
                        </div>
                    </div>

                    {/* Bar chart */}
                    <div className="chart-card p-6 h-[400px] flex flex-col bg-surface-container-lowest ghost-border ambient-shadow rounded-[1.5rem]">
                        <h2 className="text-lg font-bold text-on-surface mb-1">Gross Volume <span className="text-on-surface-variant font-normal text-sm">per Day</span></h2>
                        <p className="text-xs text-on-surface-variant mb-4">Daily total credit and debit activity</p>
                        <div className="flex-1 w-full min-h-0">
                            <DynamicBarChart data={timeSeriesData} />
                        </div>
                    </div>

                </div>

                {/* Pie + legend */}
                <div className="lg:col-span-4 space-y-8">
                    <div className="chart-card p-6 flex flex-col bg-surface-container-lowest ghost-border ambient-shadow rounded-[1.5rem]">
                        <h2 className="text-lg font-bold text-on-surface flex items-center gap-2 mb-1">
                            <PieChartIcon className="w-5 h-5 text-primary" /> Spending Distribution
                        </h2>
                        <p className="text-xs text-on-surface-variant mb-3">Breakdown by category</p>
                        <div className="h-[240px] w-full">
                            {categoryData.length > 0
                                ? <DynamicPieChart data={categoryData} />
                                : <div className="w-full h-full flex items-center justify-center text-on-surface-variant text-sm">No expenses for this period.</div>}
                        </div>

                        {/* Legend */}
                        <div className="space-y-2 mt-4">
                            {categoryData.map((entry, index) => {
                                const total = categoryData.reduce((a, c) => a + c.value, 0);
                                const pct = total > 0 ? ((entry.value / total) * 100).toFixed(1) : "0";
                                return (
                                    <div key={entry.name}
                                        className="flex items-center justify-between px-3 py-2 rounded-xl transition-colors hover:bg-surface-variant/30 group">
                                        <div className="flex items-center gap-3">
                                            <div className="w-3 h-3 rounded-full flex-shrink-0 ring-2 ring-offset-1 ring-offset-surface-container-lowest"
                                                style={{ backgroundColor: PIE_COLORS[index % PIE_COLORS.length], '--tw-ring-color': PIE_COLORS[index % PIE_COLORS.length] } as any} />
                                            <span className="text-sm font-medium text-on-surface-variant group-hover:text-on-surface transition-colors">{entry.name}</span>
                                        </div>
                                        <div className="flex items-center gap-2">
                                            <span className="text-xs text-on-surface-variant">{pct}%</span>
                                            <span className="text-sm font-bold text-on-surface">₹{entry.value.toFixed(2)}</span>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>

                    <div className="p-6 flex flex-col bg-surface-container-lowest ghost-border ambient-shadow rounded-[1.5rem]">
                        <h2 className="text-xl font-bold text-on-surface flex items-center gap-2 mb-2">
                            <Smartphone className="w-5 h-5 text-primary" /> UPI Payment Volumes
                        </h2>
                        <div className="h-[250px] w-full">
                            {upiRangeData.length > 0
                                ? <DynamicPieChart data={upiRangeData} />
                                : <div className="w-full h-full flex items-center justify-center text-on-surface-variant text-sm text-center">No UPI payments in this range.</div>}
                        </div>
                        <div className="space-y-3 mt-4">
                            {upiRangeData.map((entry, index) => (
                                <div key={entry.name} className="flex items-center justify-between p-2.5 rounded-xl transition-colors hover:bg-surface-variant/30">
                                    <div className="flex items-center gap-3">
                                        <div className="w-3.5 h-3.5 rounded-full ring-2 ring-surface-container-lowest" style={{ backgroundColor: PIE_COLORS[(index + 2) % PIE_COLORS.length] }} />
                                        <span className="text-sm font-medium text-on-surface-variant hover:text-on-surface">{entry.name}</span>
                                    </div>
                                    <div className="text-sm font-bold text-on-surface">₹{entry.value.toFixed(2)}</div>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
