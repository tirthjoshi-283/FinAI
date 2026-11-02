"use client";

import { useState } from "react";
import { useDashboard, TransactionType } from "@/components/DashboardProvider";
import { PlusCircle, ArrowUpRight, ArrowDownRight } from "lucide-react";
import { CREDIT_CATEGORIES, DEBIT_CATEGORIES } from "@/lib/categories";

export function AddTransactionForm() {
    const { addTransaction } = useDashboard();

    const [amount, setAmount] = useState("");
    const [title, setTitle] = useState("");
    const [category, setCategory] = useState<string>("Food");
    const [type, setType] = useState<TransactionType>("debit");

    const debitCategories: readonly string[] = DEBIT_CATEGORIES;
    const creditCategories: readonly string[] = CREDIT_CATEGORIES;

    const currentCategories = type === "credit" ? creditCategories : debitCategories;

    const handleTypeChange = (newType: TransactionType) => {
        setType(newType);
        setCategory(newType === "credit" ? creditCategories[0] : debitCategories[0]);
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!amount || !title) return;

        addTransaction({
            amount: parseFloat(amount),
            title,
            category,
            type,
            date: new Date() // Use current time
        });

        setAmount("");
        setTitle("");
        setCategory(type === "credit" ? creditCategories[0] : debitCategories[0]);
    };

    return (
        <div className="bg-surface-container-lowest p-6 rounded-xl ghost-border ambient-shadow">
            <h2 className="text-xl font-bold text-on-surface mb-6 flex items-center gap-2">
                <PlusCircle className="w-5 h-5 text-primary" /> New Transaction
            </h2>

            <form onSubmit={handleSubmit} className="space-y-4">
                {/* Type Toggle */}
                <div className="flex bg-surface rounded-xl p-1 gap-1">
                    <button
                        type="button"
                        onClick={() => handleTypeChange("debit")}
                        className={`flex-1 flex items-center justify-center gap-2 py-2 rounded-lg text-sm font-semibold transition-all ${type === "debit" ? "bg-error-container text-on-error-container shadow-sm" : "text-on-surface-variant hover:text-on-surface hover:bg-surface-variant/30"}`}
                    >
                        <ArrowDownRight className="w-4 h-4" /> Debit
                    </button>
                    <button
                        type="button"
                        onClick={() => handleTypeChange("credit")}
                        className={`flex-1 flex items-center justify-center gap-2 py-2 rounded-lg text-sm font-semibold transition-all ${type === "credit" ? "bg-primary-container text-on-primary-container shadow-sm" : "text-on-surface-variant hover:text-on-surface hover:bg-surface-variant/30"}`}
                    >
                        <ArrowUpRight className="w-4 h-4" /> Credit
                    </button>
                </div>

                <div>
                    <label className="text-xs text-on-surface-variant font-semibold uppercase tracking-wider mb-1 block">Amount (₹)</label>
                    <input
                        type="number"
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                        placeholder="0.00"
                        step="0.01"
                        required
                        className="w-full bg-surface-container-low border border-transparent rounded-xl px-4 py-3 text-on-surface placeholder:text-outline/50 outline-none focus:border-primary/40 focus:shadow-[inset_0_0_8px_rgba(0,108,73,0.05)] transition-all"
                    />
                </div>

                <div>
                    <label className="text-xs text-on-surface-variant font-semibold uppercase tracking-wider mb-1 block">Description</label>
                    <input
                        type="text"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        placeholder="e.g. Amazon Purchase"
                        required
                        className="w-full bg-surface-container-low border border-transparent rounded-xl px-4 py-3 text-on-surface placeholder:text-outline/50 outline-none focus:border-primary/40 focus:shadow-[inset_0_0_8px_rgba(0,108,73,0.05)] transition-all"
                    />
                </div>

                <div>
                    <label className="text-xs text-on-surface-variant font-semibold uppercase tracking-wider mb-1 block">Category</label>
                    <select
                        value={category}
                        onChange={(e) => setCategory(e.target.value)}
                        className="w-full bg-surface-container-low border border-transparent rounded-xl px-4 py-3 text-on-surface outline-none focus:border-primary/40 focus:shadow-[inset_0_0_8px_rgba(0,108,73,0.05)] transition-all cursor-pointer appearance-none"
                    >
                        {currentCategories.map(cat => (
                            <option key={cat} value={cat} className="bg-surface-container-lowest text-on-surface">{cat}</option>
                        ))}
                    </select>
                </div>

                <button
                    type="submit"
                    className="w-full py-4 rounded-xl bg-primary text-on-primary font-bold transition-all hover:opacity-90 shadow-ambient mt-2"
                >
                    Add Transaction
                </button>
            </form>
        </div>
    );
}

