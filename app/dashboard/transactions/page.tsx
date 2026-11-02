"use client";

import { useState } from "react";
import { useDashboard } from "@/components/DashboardProvider";
import { ReceiptText, Trash2, ArrowUpRight, ArrowDownRight, Filter, Download, Loader2, Mail, ScanLine, MessageSquare } from "lucide-react";
import { format, isAfter, isBefore, startOfDay, endOfDay } from "date-fns";
import { cn } from "@/lib/utils";
import { AddTransactionForm } from "@/components/dashboard/AddTransactionForm";
import { GmailSyncCard } from "@/components/dashboard/GmailSyncCard";
import { BillScanCard } from "@/components/dashboard/BillScanCard";

export default function TransactionsPage() {
    const { transactions, loadingTransactions, deleteTransaction } = useDashboard();
    const [activeTab, setActiveTab] = useState<string>("All");
    const [startDate, setStartDate] = useState<string>("");
    const [endDate, setEndDate] = useState<string>("");
    const [isExporting, setIsExporting] = useState(false);
    
    const dynamicCategories = Array.from(new Set(transactions.map(t => t.category)));
    const categories: string[] = ["All", ...dynamicCategories];

    const filteredTransactions = transactions.filter(t => {
        const matchTab = activeTab === "All" || t.category === activeTab;
        if (!matchTab) return false;
        if (startDate && isBefore(t.date, startOfDay(new Date(startDate)))) return false;
        if (endDate && isAfter(t.date, endOfDay(new Date(endDate)))) return false;
        return true;
    });

    const handleDownloadPDF = async () => {
        setIsExporting(true);
        try {
            const [{ default: jsPDF }, { default: autoTable }] = await Promise.all([
                import("jspdf"),
                import("jspdf-autotable"),
            ]);
            const doc = new jsPDF();
            doc.setFontSize(18);
            doc.text("Transaction Ledger Report", 14, 22);
            doc.setFontSize(11);
            doc.setTextColor(100);
            let subtitle = `Report generated on ${format(new Date(), "MMM dd, yyyy")}`;
            if (startDate || endDate) subtitle += ` | Period: ${startDate ? format(new Date(startDate), "MMM dd, yyyy") : "Start"} to ${endDate ? format(new Date(endDate), "MMM dd, yyyy") : "End"}`;
            doc.text(subtitle, 14, 30);
            const tableColumn = ["Date", "Time", "Title", "Category", "Type", "Amount (INR)"];
            const tableRows = filteredTransactions.map(tx => [
                format(tx.date, "dd/MM/yyyy"), format(tx.date, "hh:mm a"), tx.title, tx.category,
                tx.type.toUpperCase(), `${tx.type === "credit" ? "+" : "-"} ₹${tx.amount.toFixed(2)}`
            ]);
            autoTable(doc, { head: [tableColumn], body: tableRows, startY: 40, theme: "striped", headStyles: { fillColor: [0, 108, 73] } });
            doc.save("transaction_report.pdf");
        } catch (error) {
            console.error("Failed to generate PDF:", error);
        } finally {
            setIsExporting(false);
        }
    };

    return (
        <div className="w-full max-w-7xl mx-auto space-y-8 relative z-10 pb-20">

            {/* ── Download PDF button shimmer animation ── */}
            <style>{`
                @keyframes pdf-shimmer {
                    0%   { transform: translateX(-110%) skewX(-15deg); }
                    100% { transform: translateX(220%)  skewX(-15deg); }
                }
                .pdf-btn::before {
                    content: '';
                    position: absolute;
                    inset: 0;
                    background: linear-gradient(90deg, transparent 0%, rgba(255,255,255,0.28) 50%, transparent 100%);
                    transform: translateX(-110%) skewX(-15deg);
                    transition: none;
                    border-radius: inherit;
                    pointer-events: none;
                }
                .pdf-btn:hover::before {
                    animation: pdf-shimmer 0.6s cubic-bezier(0.4, 0, 0.2, 1) forwards;
                }
                .pdf-btn:hover .pdf-icon {
                    transform: translateY(2px);
                }
                .pdf-icon {
                    transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
                }
            `}</style>

            {/* Header */}
            <header className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-on-surface tracking-tighter flex items-center gap-3">
                        <ReceiptText className="w-8 h-8 text-primary" /> Transaction Ledger
                    </h1>
                    <p className="text-on-surface-variant mt-0.5">Search, filter, export or add new cashflow entries.</p>
                </div>

                <button
                    onClick={handleDownloadPDF}
                    disabled={filteredTransactions.length === 0 || isExporting}
                    className="pdf-btn relative overflow-hidden flex items-center gap-2.5 px-5 py-2.5 rounded-xl font-semibold text-sm transition-all duration-300 disabled:opacity-40 disabled:cursor-not-allowed disabled:pointer-events-none"
                    style={{
                        background: "linear-gradient(135deg, var(--primary), var(--primary-container))",
                        color: "var(--on-primary)",
                        boxShadow: "0 4px 20px -4px color-mix(in srgb, var(--primary) 50%, transparent)",
                    }}
                    onMouseEnter={e => {
                        (e.currentTarget as HTMLButtonElement).style.boxShadow =
                            "0 8px 28px -4px color-mix(in srgb, var(--primary) 70%, transparent), 0 0 0 2px color-mix(in srgb, var(--primary-container) 40%, transparent)";
                        (e.currentTarget as HTMLButtonElement).style.transform = "translateY(-1px)";
                    }}
                    onMouseLeave={e => {
                        (e.currentTarget as HTMLButtonElement).style.boxShadow =
                            "0 4px 20px -4px color-mix(in srgb, var(--primary) 50%, transparent)";
                        (e.currentTarget as HTMLButtonElement).style.transform = "translateY(0)";
                    }}
                >
                    {isExporting
                        ? <Loader2 className="w-4 h-4 animate-spin" />
                        : <Download className="pdf-icon w-4 h-4" />
                    }
                    {isExporting ? "Generating…" : "Download PDF"}
                </button>
            </header>

            {/* Gmail Sync + Add Transaction Section */}
            <div className="w-full grid grid-cols-1 lg:grid-cols-2 gap-6">
                <GmailSyncCard />
                <AddTransactionForm />
            </div>

            {/* Bill / Receipt Scanner */}
            <BillScanCard />

            {/* Date Filters & Category Tabs */}
            <div className="flex flex-col lg:flex-row lg:items-center gap-4">
                {/* Date */}
                <div className="flex items-center gap-3 p-2 rounded-xl text-sm w-full lg:w-auto overflow-x-auto bg-surface-container-low border border-outline-variant/30">
                    <div className="flex items-center gap-2 shrink-0">
                        <span className="text-on-surface-variant ml-2">From:</span>
                        <input type="date" value={startDate} onChange={e => setStartDate(e.target.value)}
                            className="bg-transparent text-on-surface border-none outline-none cursor-pointer w-32" />
                    </div>
                    <div className="w-px h-6 shrink-0 bg-outline-variant/50" />
                    <div className="flex items-center gap-2 shrink-0">
                        <span className="text-on-surface-variant">To:</span>
                        <input type="date" value={endDate} onChange={e => setEndDate(e.target.value)}
                            className="bg-transparent text-on-surface border-none outline-none cursor-pointer w-32" />
                    </div>
                    {(startDate || endDate) && (
                        <button onClick={() => { setStartDate(""); setEndDate(""); }}
                            className="text-xs ml-2 px-2 transition-colors text-primary hover:text-primary/80">
                            Reset
                        </button>
                    )}
                </div>

                {/* Category Tabs */}
                <div className="flex items-center gap-2 overflow-x-auto scrollbar-hide flex-1">
                    <div className="flex items-center gap-2 px-2 py-1.5 rounded-xl shrink-0 bg-surface-container-low border border-outline-variant/30">
                        <Filter className="w-4 h-4 text-outline ml-2" />
                        {categories.map(cat => (
                            <button key={cat} onClick={() => setActiveTab(cat)}
                                className={cn("px-4 py-2 rounded-lg text-sm font-semibold transition-all whitespace-nowrap",
                                    activeTab === cat 
                                        ? "bg-primary-container/40 text-primary shadow-sm border border-primary/20" 
                                        : "text-on-surface-variant hover:text-on-surface hover:bg-surface-variant/30 border border-transparent"
                                )}
                            >
                                {cat}
                            </button>
                        ))}
                    </div>
                </div>
            </div>

            {/* Transactions List */}
            <div className="overflow-hidden min-h-[400px] bg-surface-container-lowest ghost-border shadow-ambient rounded-[1.5rem]">
                <div className="px-6 py-4 border-b border-outline-variant/30 bg-surface-variant/30">
                    <h2 className="text-lg font-bold text-on-surface">Recent Entries</h2>
                </div>
                <div className="divide-y divide-outline-variant/30">
                    {loadingTransactions ? (
                        <div className="flex flex-col items-center justify-center p-20 gap-3 text-on-surface-variant">
                            <Loader2 className="w-8 h-8 animate-spin text-primary/50" />
                            <p>Loading transactions...</p>
                        </div>
                    ) : filteredTransactions.length === 0 ? (
                        <div className="flex flex-col items-center justify-center p-20 text-center text-on-surface-variant">
                            <p>No transactions found for this period/category.</p>
                        </div>
                    ) : (
                        filteredTransactions.map(tx => (
                            <div key={tx.id} className="p-4 sm:p-6 transition-colors flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 group hover:bg-surface-container-low">
                                <div className="flex items-center gap-4">
                                    <div className={cn("w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 shadow-sm",
                                            tx.type === "credit"
                                                ? "bg-primary-container text-on-primary-container"
                                                : "bg-error-container text-on-error-container"
                                        )}>
                                        {tx.type === "credit" ? <ArrowUpRight className="w-6 h-6" /> : <ArrowDownRight className="w-6 h-6" />}
                                    </div>
                                    <div>
                                        <div className="text-on-surface font-semibold text-lg">{tx.title}</div>
                                        <div className="flex items-center gap-3 text-sm mt-1 flex-wrap">
                                            <span className="text-on-surface-variant font-mono">{format(tx.date, "MMM dd, yyyy • hh:mm a")}</span>
                                            <span className="w-1.5 h-1.5 rounded-full bg-outline-variant" />
                                            <span className="px-2 py-0.5 rounded-md text-xs font-medium bg-primary-container/30 border border-primary-container/50 text-primary">
                                                {tx.category}
                                            </span>
                                            {(tx as { source?: string }).source === "gmail_upi" && (
                                                <span className="px-2 py-0.5 rounded-md text-xs font-medium bg-surface-container border border-outline-variant/40 text-on-surface-variant flex items-center gap-1">
                                                    <Mail className="w-2.5 h-2.5" />
                                                    Gmail UPI
                                                </span>
                                            )}
                                            {tx.source === "bill_scan" && (
                                                <span className="px-2 py-0.5 rounded-md text-xs font-medium bg-surface-container border border-outline-variant/40 text-on-surface-variant flex items-center gap-1">
                                                    <ScanLine className="w-2.5 h-2.5" />
                                                    Scanned bill
                                                </span>
                                            )}
                                            {tx.source === "sms" && (
                                                <span className="px-2 py-0.5 rounded-md text-xs font-medium bg-surface-container border border-outline-variant/40 text-on-surface-variant flex items-center gap-1">
                                                    <MessageSquare className="w-2.5 h-2.5" />
                                                    Bank SMS
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                </div>
                                <div className="flex items-center gap-4 w-full sm:w-auto justify-between sm:justify-end">
                                    <div className={cn("text-xl font-bold font-mono text-right", tx.type === "credit" ? "text-primary" : "text-error")}>
                                        {tx.type === "credit" ? "+" : "-"}₹{tx.amount.toFixed(2)}
                                    </div>
                                    <button onClick={() => deleteTransaction(tx.id)} disabled={loadingTransactions}
                                        className="w-10 h-10 rounded-xl flex items-center justify-center text-on-surface-variant hover:text-error hover:bg-error-container transition-colors opacity-100 sm:opacity-0 group-hover:opacity-100 disabled:opacity-50">
                                        <Trash2 className="w-5 h-5" />
                                    </button>
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>
        </div>
    );
}
