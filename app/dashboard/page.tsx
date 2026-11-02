"use client";

import { useMemo } from "react";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { useDashboard } from "@/components/DashboardProvider";
import { 
    TrendingUp, 
    Droplets, 
    CreditCard, 
    TrendingDown,
    Plus 
} from "lucide-react";

export default function DashboardPage() {
    const { user } = useAuth();
    const { transactions } = useDashboard();

    const { totalBalance, totalCredit, totalDebit } = useMemo(() => {
        let credit = 0, debit = 0;
        for (const t of transactions) {
            if (t.type === "credit") credit += t.amount;
            else debit += t.amount;
        }
        return { totalBalance: credit - debit, totalCredit: credit, totalDebit: debit };
    }, [transactions]);

    const firstName = user?.displayName?.split(' ')[0] || "there";

    // Create a smooth visual integer and decimal breakdown for large fonts
    const formatCurrencyExt = (amount: number) => {
        const absVal = Math.abs(amount).toFixed(2);
        const [integerPart, decimalPart] = absVal.split(".");
        return { 
            sign: amount < 0 ? "-" : "", 
            integerPart: new Intl.NumberFormat('en-IN').format(Number(integerPart)), 
            decimalPart 
        };
    };

    const netWorthParts = formatCurrencyExt(totalBalance);
    const liquidParts = formatCurrencyExt(totalCredit);
    // Mock percentage placeholder as in design would go here

    return (
        <div className="w-full flex flex-col gap-8">
            {/* Welcome Header */}
            <header className="flex justify-between items-end mb-2">
                <div>
                    <h1 className="text-3xl lg:text-4xl font-bold text-on-surface tracking-tight mb-2">
                        Portfolio Overview
                    </h1>
                    <p className="text-on-surface-variant">
                        Good morning {firstName}, your wealth ecosystem is looking vibrant.
                    </p>
                </div>
                <div className="hidden sm:block">
                    <Link href="/dashboard/transactions" className="bg-gradient-to-br from-primary to-primary-container text-on-primary px-6 py-3 rounded-xl font-medium shadow-[0_10px_20px_-10px_rgba(16,185,129,0.4)] hover:shadow-[0_15px_25px_-10px_rgba(16,185,129,0.5)] transition-all duration-300 transform hover:-translate-y-0.5 flex items-center gap-2">
                        <Plus className="w-4 h-4" />
                        New Entry
                    </Link>
                </div>
            </header>

            {/* Bento Grid Layout */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 lg:gap-8">
                
                {/* Total Net Worth Card (Hero) */}
                <div className="lg:col-span-8 bg-surface-container-lowest rounded-[2rem] relative overflow-hidden ambient-shadow flex flex-col justify-between min-h-[400px] ghost-border">
                    {/* Background Decorative Abstract 3D — Light Theme */}
                    <div 
                        className="absolute right-0 top-0 w-1/2 h-full pointer-events-none hero-image-blend dark:hidden block" 
                        style={{ 
                            backgroundImage: "url('/light_hero.png')", 
                            backgroundSize: 'cover', 
                            backgroundPosition: 'center left'
                        }}
                    ></div>
                    {/* Background Decorative Abstract 3D — Dark Theme */}
                    <div 
                        className="absolute right-0 top-0 w-1/2 h-full pointer-events-none hero-image-blend hidden dark:block" 
                        style={{ 
                            backgroundImage: "url('/dark_hero.png')", 
                            backgroundSize: 'cover', 
                            backgroundPosition: 'center left'
                        }}
                    ></div>
                    {/* Gradient fade from card bg into the image */}
                    <div className="absolute inset-0 bg-gradient-to-r from-surface-container-lowest via-surface-container-lowest/95 to-surface-container-lowest/10 pointer-events-none"></div>

                    {/* Content — constrained to left half so it never overlaps the image */}
                    <div className="relative z-10 w-full max-w-[55%] p-8 lg:p-12 flex flex-col justify-between h-full min-h-[400px]">
                        <div>
                            <span className="text-xs uppercase tracking-[0.1em] text-primary font-semibold mb-2 block">
                                Total Net Worth
                            </span>
                            <h2 className={`text-5xl lg:text-6xl font-extrabold tracking-tighter mb-4 ${totalBalance < 0 ? 'text-error' : 'text-on-surface'}`}>
                                {netWorthParts.sign}₹{netWorthParts.integerPart}
                                <span className="text-3xl text-on-surface-variant">.{netWorthParts.decimalPart}</span>
                            </h2>

                            <div className="flex items-center gap-2 mt-4">
                                <div className="bg-primary-container/20 text-primary-container px-3 py-1 rounded-full flex items-center gap-1 text-sm font-medium">
                                    <TrendingUp className="w-3.5 h-3.5" />
                                    All time
                                </div>
                                <span className="text-sm text-on-surface-variant">Cashflow balance</span>
                            </div>
                        </div>

                        <div className="mt-12 flex gap-4">
                            <Link href="/dashboard/charts" className="bg-secondary-container text-on-secondary-container px-5 py-2.5 rounded-xl font-medium hover:bg-secondary-container/80 transition-colors">
                                Analytics
                            </Link>
                            <Link href="/dashboard/transactions" className="text-primary hover:bg-surface-container px-5 py-2.5 rounded-xl font-medium transition-colors">
                                View Ledger
                            </Link>
                        </div>
                    </div>
                </div>

                {/* Side Cards Column */}
                <div className="lg:col-span-4 flex flex-col gap-6 lg:gap-8">
                    
                    {/* Liquid Assets Card (Total Credited) */}
                    <div className="bg-surface-container-lowest rounded-2xl p-6 ambient-shadow flex-1 flex flex-col relative overflow-hidden group ghost-border">
                        <div className="absolute -right-6 -top-6 w-32 h-32 bg-primary/5 rounded-full blur-2xl group-hover:bg-primary/10 transition-colors duration-500"></div>
                        <div className="flex justify-between items-start mb-6 relative z-10">
                            <span className="text-xs uppercase tracking-[0.1em] text-on-surface-variant font-semibold">Liquid Assets</span>
                            <div className="bg-surface-container w-10 h-10 rounded-full flex items-center justify-center text-primary relative z-10">
                                <Droplets className="w-5 h-5" />
                            </div>
                        </div>
                        <div className="mt-auto relative z-10">
                            <h3 className="text-3xl font-bold text-on-surface mb-2">₹{liquidParts.integerPart}</h3>
                            <div className="w-full bg-surface-container h-1.5 rounded-full overflow-hidden">
                                <div className="bg-gradient-to-r from-primary to-primary-container w-[65%] h-full rounded-full"></div>
                            </div>
                            <p className="text-xs text-on-surface-variant mt-3">Representing total lifetime credits</p>
                        </div>
                    </div>

                    {/* Total Debt Card (Total Debited) */}
                    <div className="bg-surface-container-lowest rounded-2xl p-6 ambient-shadow flex-1 flex flex-col relative overflow-hidden group ghost-border">
                        <div className="absolute -left-6 -bottom-6 w-32 h-32 bg-tertiary/5 rounded-full blur-2xl group-hover:bg-tertiary/10 transition-colors duration-500"></div>
                        <div className="flex justify-between items-start mb-6 relative z-10">
                            <span className="text-xs uppercase tracking-[0.1em] text-on-surface-variant font-semibold">Total Liabilities</span>
                            <div className="bg-surface-container w-10 h-10 rounded-full flex items-center justify-center text-tertiary relative z-10">
                                <CreditCard className="w-5 h-5" />
                            </div>
                        </div>
                        <div className="mt-auto relative z-10">
                            <h3 className="text-3xl font-bold text-on-surface mb-2">₹{new Intl.NumberFormat('en-IN').format(totalDebit)}</h3>
                            <div className="flex items-center gap-2">
                                <span className="text-tertiary text-sm font-medium flex items-center">
                                    <TrendingDown className="w-4 h-4 mr-1" />
                                    Total Spends
                                </span>
                                <span className="text-xs text-on-surface-variant">recorded historically</span>
                            </div>
                        </div>
                    </div>

                </div>
            </div>
            
            {/* Mobile Actions */}
            <div className="sm:hidden mt-4">
                <Link href="/dashboard/transactions" className="w-full justify-center bg-gradient-to-br from-primary to-primary-container text-on-primary px-6 py-4 rounded-xl font-medium shadow-[0_10px_20px_-10px_rgba(16,185,129,0.4)] flex items-center gap-2">
                    <Plus className="w-5 h-5" />
                    New Entry
                </Link>
            </div>
        </div>
    );
}
