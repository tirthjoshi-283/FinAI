"use client";

import { useState } from "react";
import { useDashboard } from "@/components/DashboardProvider";
import { Target, PlusCircle, Trash2, HandCoins, CheckCircle2 } from "lucide-react";
import { format } from "date-fns";
import dynamic from "next/dynamic";
import { Loader2 } from "lucide-react";

// Dynamically import heavy 3D components so they don't block the UI thread during navigation clicks
const GoalVisualizer = dynamic(
    () => import("@/components/dashboard/GoalVisualizer").then(mod => mod.GoalVisualizer),
    { 
        ssr: false, 
        loading: () => (
            <div className="w-full h-full flex items-center justify-center bg-surface-container-low/50 animate-pulse rounded-xl">
                <Loader2 className="w-8 h-8 text-primary animate-spin opacity-50" />
            </div>
        )
    }
);

export default function GoalsPage() {
    const { goals, loadingGoals, addGoal, addFundsToGoal, deleteGoal } = useDashboard();
    const [title, setTitle] = useState("");
    const [targetAmount, setTargetAmount] = useState("");
    
    // Quick Add Funds States
    const [fundingGoalId, setFundingGoalId] = useState<string | null>(null);
    const [fundingAmount, setFundingAmount] = useState("");

    const handleCreateGoal = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!title || !targetAmount) return;

        await addGoal({
            title,
            targetAmount: parseFloat(targetAmount)
        });

        setTitle("");
        setTargetAmount("");
    };

    const handleAddFunds = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!fundingGoalId || !fundingAmount) return;

        await addFundsToGoal(fundingGoalId, parseFloat(fundingAmount));
        setFundingGoalId(null);
        setFundingAmount("");
    };

    return (
        <div className="w-full max-w-7xl mx-auto space-y-8 relative z-10 pb-20">
            {/* Net Worth Hero Card */}
            <div className="bg-surface-container-lowest rounded-[2rem] p-8 lg:p-12 relative overflow-hidden ambient-shadow ghost-border flex flex-col lg:flex-row items-center justify-between min-h-[350px]">
                {/* Text Side */}
                <div className="w-full lg:w-1/2 relative z-10">
                    <h1 className="text-5xl lg:text-7xl font-extrabold tracking-tighter mb-4 text-on-surface">
                        Financial Targets
                    </h1>
                    <p className="text-xl text-on-surface-variant font-medium">
                        Set horizons. Execute. Achieve freedom.
                    </p>
                </div>

                {/* 3D Visualizer Side */}
                <div className="w-full lg:w-1/2 h-[300px] lg:h-full absolute right-0 bottom-0 lg:top-0 opacity-80 lg:opacity-100 mix-blend-luminosity lg:mix-blend-normal">
                    <GoalVisualizer />
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Create Goal Form */}
                <div className="lg:col-span-1">
                    <div className="bg-surface-container-lowest p-6 rounded-xl ghost-border ambient-shadow sticky top-24">
                        <h2 className="text-xl font-bold text-on-surface mb-6 flex items-center gap-2">
                            <PlusCircle className="w-5 h-5 text-primary" /> Create New Goal
                        </h2>

                        <form onSubmit={handleCreateGoal} className="space-y-4">
                            <div>
                                <label className="text-xs text-on-surface-variant font-semibold uppercase tracking-wider mb-1 block">Goal Name</label>
                                <input
                                    type="text"
                                    value={title}
                                    onChange={(e) => setTitle(e.target.value)}
                                    placeholder="e.g. Dream Car, Vacation..."
                                    required
                                    className="w-full bg-surface-container-low border border-transparent rounded-xl px-4 py-3 text-on-surface placeholder:text-outline/50 outline-none focus:border-primary/40 focus:shadow-[inset_0_0_8px_rgba(0,108,73,0.05)] transition-all"
                                />
                            </div>

                            <div>
                                <label className="text-xs text-on-surface-variant font-semibold uppercase tracking-wider mb-1 block">Target Amount (₹)</label>
                                <input
                                    type="number"
                                    value={targetAmount}
                                    onChange={(e) => setTargetAmount(e.target.value)}
                                    placeholder="100000"
                                    min="1"
                                    step="0.01"
                                    required
                                    className="w-full bg-surface-container-low border border-transparent rounded-xl px-4 py-3 text-on-surface placeholder:text-outline/50 outline-none focus:border-primary/40 focus:shadow-[inset_0_0_8px_rgba(0,108,73,0.05)] transition-all"
                                />
                            </div>

                            <button
                                type="submit"
                                className="w-full py-4 rounded-xl bg-primary text-on-primary font-bold transition-all shadow-ambient hover:opacity-90 mt-2"
                            >
                                Set Target
                            </button>
                        </form>
                    </div>
                </div>

                {/* Goals List */}
                <div className="lg:col-span-2 space-y-6">
                    {loadingGoals ? (
                        <div className="animate-pulse space-y-4">
                            {[1, 2].map(i => (
                                <div key={i} className="h-48 rounded-xl bg-surface-variant opacity-50" />
                            ))}
                        </div>
                    ) : goals.length === 0 ? (
                        <div className="bg-surface-container-lowest p-12 text-center rounded-xl ghost-border ambient-shadow">
                            <Target className="w-16 h-16 text-outline mx-auto mb-4" />
                            <h3 className="text-xl font-bold text-on-surface mb-2">No active goals</h3>
                            <p className="text-on-surface-variant">Create your first financial goal to start tracking progress.</p>
                        </div>
                    ) : (
                        goals.map(goal => {
                            const progress = Math.min((goal.currentAmount / goal.targetAmount) * 100, 100);
                            const isCompleted = progress >= 100;

                            return (
                                <div key={goal.id} className="relative bg-surface-container-lowest p-6 sm:p-8 rounded-xl ghost-border ambient-shadow overflow-hidden group">
                                    {/* Background glow if completed */}
                                    {isCompleted && (
                                        <div className="absolute inset-0 bg-primary/5 pointer-events-none" />
                                    )}
                                    
                                    <div className="relative z-10 flex flex-col sm:flex-row justify-between items-start gap-4 mb-6">
                                        <div>
                                            <h3 className="text-2xl font-bold text-on-surface flex items-center gap-3">
                                                {goal.title}
                                                {isCompleted && <CheckCircle2 className="w-6 h-6 text-primary drop-shadow-[0_0_8px_rgba(0,108,73,0.3)]" />}
                                            </h3>
                                            <p className="text-on-surface-variant text-sm mt-1">
                                                Started on {format(goal.createdAt, 'MMM dd, yyyy')}
                                            </p>
                                        </div>
                                        <div className="flex gap-2 w-full sm:w-auto">
                                            {!isCompleted && fundingGoalId !== goal.id && (
                                                <button
                                                    onClick={() => setFundingGoalId(goal.id)}
                                                    className="flex-1 sm:flex-none px-4 py-2 rounded-xl bg-surface-container hover:bg-primary-container hover:text-on-primary-container text-on-surface-variant transition-all font-medium flex items-center justify-center gap-2"
                                                >
                                                    <HandCoins className="w-4 h-4" /> Add Funds
                                                </button>
                                            )}
                                            <button
                                                onClick={() => deleteGoal(goal.id)}
                                                className="p-2.5 rounded-xl bg-surface-container text-on-surface-variant hover:text-error hover:bg-error-container transition-colors flex-shrink-0"
                                                title="Delete Goal"
                                            >
                                                <Trash2 className="w-4 h-4" />
                                            </button>
                                        </div>
                                    </div>

                                    {/* Add Funds Quick Form */}
                                    {fundingGoalId === goal.id && (
                                        <form onSubmit={handleAddFunds} className="relative z-10 mb-6 flex gap-2 w-full max-w-md animate-in fade-in slide-in-from-top-4 duration-200">
                                            <input
                                                type="number"
                                                value={fundingAmount}
                                                onChange={e => setFundingAmount(e.target.value)}
                                                placeholder="Amount to add (₹)"
                                                min="1"
                                                step="0.01"
                                                required
                                                autoFocus
                                                className="flex-1 bg-surface-container-low border border-transparent rounded-xl px-4 py-2 text-on-surface outline-none focus:border-primary/40 focus:shadow-[inset_0_0_8px_rgba(0,108,73,0.05)] transition-all"
                                            />
                                            <button type="submit" className="px-4 py-2 glass-gradient rounded-xl text-white font-bold hover:opacity-90 shadow-ambient transition-all">
                                                Confirm
                                            </button>
                                            <button 
                                                type="button" 
                                                onClick={() => { setFundingGoalId(null); setFundingAmount(""); }}
                                                className="px-4 py-2 bg-surface-container rounded-xl text-on-surface-variant hover:text-on-surface transition-colors"
                                            >
                                                Cancel
                                            </button>
                                        </form>
                                    )}

                                    {/* Tracker / Progress Bar */}
                                    <div className="relative z-10 space-y-3">
                                        <div className="flex justify-between text-sm font-medium">
                                            <span className="text-on-surface">
                                                <span className="text-xl font-bold tracking-tight mr-1">
                                                    ₹{goal.currentAmount.toLocaleString('en-IN', { maximumFractionDigits: 2 })}
                                                </span>
                                                saved
                                            </span>
                                            <span className="text-on-surface-variant">
                                                Target: <span className="text-on-surface font-semibold">₹{goal.targetAmount.toLocaleString('en-IN', { maximumFractionDigits: 2 })}</span>
                                            </span>
                                        </div>
                                        
                                        <div className="h-4 w-full bg-surface-container rounded-full overflow-hidden border border-outline-variant/30">
                                            <div 
                                                className={`h-full rounded-full transition-all duration-1000 ease-out relative overflow-hidden ${isCompleted ? 'bg-primary-container shadow-[0_0_12px_rgba(0,108,73,0.2)]' : 'bg-primary shadow-[0_0_12px_rgba(0,108,73,0.2)]'}`}
                                                style={{ width: `${progress}%` }}
                                            >
                                                {/* Shimmer effect for progress bar */}
                                                <div className="absolute top-0 left-0 w-full h-full bg-gradient-to-r from-transparent via-white/40 to-transparent -translate-x-full animate-[shimmer_2s_infinite]" />
                                            </div>
                                        </div>
                                        
                                        <div className="flex justify-between text-xs text-on-surface-variant font-medium">
                                            <span>
                                                {isCompleted ? "Goal Reached! 🎉" : `${progress.toFixed(1)}% Achieved`}
                                            </span>
                                            {!isCompleted && (
                                                <span>₹{(goal.targetAmount - goal.currentAmount).toLocaleString('en-IN', { maximumFractionDigits: 2 })} remaining</span>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            );
                        })
                    )}
                </div>
            </div>
        </div>
    );
}
