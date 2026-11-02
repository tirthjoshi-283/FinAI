"use client";

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { useAuth } from '@/context/AuthContext';

export type TransactionCategory = string;
export type TransactionType = 'credit' | 'debit';

export interface Transaction {
    id: string;
    amount: number;
    type: TransactionType;
    category: TransactionCategory;
    date: Date;
    title: string;
    // Gmail UPI fields — optional, only present on auto-imported transactions
    source?: "manual" | "gmail_upi" | "bill_scan" | "sms";
    bankName?: string;
    merchant?: string;
    upiRef?: string;
}

export interface Goal {
    id: string;
    title: string;
    targetAmount: number;
    currentAmount: number;
    createdAt: Date;
}

export type ScanBillResult =
    | { success: true; transaction: Transaction; summary: string }
    | { success: false; error: string };

interface DashboardContextType {
    transactions: Transaction[];
    loadingTransactions: boolean;
    addTransaction: (tx: Omit<Transaction, 'id'>) => Promise<{ success: boolean; error?: string }>;
    deleteTransaction: (id: string) => Promise<void>;
    scanBill: (file: File) => Promise<ScanBillResult>;
    updateTransactionCategory: (id: string, category: string) => Promise<boolean>;
    goals: Goal[];
    loadingGoals: boolean;
    addGoal: (goal: Omit<Goal, 'id' | 'currentAmount' | 'createdAt'>) => Promise<void>;
    addFundsToGoal: (id: string, amountToAdd: number) => Promise<void>;
    deleteGoal: (id: string) => Promise<void>;
}

const DashboardContext = createContext<DashboardContextType | undefined>(undefined);

export function DashboardProvider({ children }: { children: ReactNode }) {
    const { user } = useAuth();
    const [transactions, setTransactions] = useState<Transaction[]>([]);
    const [loadingTransactions, setLoadingTransactions] = useState<boolean>(true);
    const [goals, setGoals] = useState<Goal[]>([]);
    const [loadingGoals, setLoadingGoals] = useState<boolean>(true);

    // Re-fetch transactions after Gmail sync completes (triggered by GmailSyncCard)
    useEffect(() => {
        const handleGmailSync = async () => {
            if (!user) return;
            const token = await user.getIdToken();
            fetch("/api/transactions", { headers: { Authorization: `Bearer ${token}` } })
                .then((r) => r.ok ? r.json() : null)
                .then((data) => {
                    if (!data) return;
                    const txs = (data.transactions || []).map((t: { date: string | number | Date }) => ({
                        ...t,
                        date: new Date(t.date),
                    }));
                    setTransactions(txs);
                })
                .catch(console.error);
        };
        window.addEventListener("gmail-sync-complete", handleGmailSync);
        return () => window.removeEventListener("gmail-sync-complete", handleGmailSync);
    }, [user]);

    useEffect(() => {
        const fetchTransactionsAndGoals = async () => {
            if (!user) {
                setTransactions([]);
                setGoals([]);
                setLoadingTransactions(false);
                setLoadingGoals(false);
                return;
            }
            try {
                setLoadingTransactions(true);
                setLoadingGoals(true);
                const token = await user.getIdToken();
                const [txRes, goalsRes] = await Promise.all([
                    fetch("/api/transactions", { headers: { 'Authorization': `Bearer ${token}` } }),
                    fetch("/api/goals", { headers: { 'Authorization': `Bearer ${token}` } })
                ]);

                if (txRes.ok) {
                    const data = await txRes.json();
                    const txs = (data.transactions || []).map((t: { date: string | number | Date }) => ({
                        ...t,
                        date: new Date(t.date)
                    }));
                    setTransactions(txs);
                } else {
                    console.error("Failed to fetch transactions");
                }

                if (goalsRes.ok) {
                    const data = await goalsRes.json();
                    const fetchedGoals = (data.goals || []).map((g: { id: string; title: string; targetAmount: number; currentAmount: number; createdAt: string | number | Date }) => ({
                        ...g,
                        createdAt: new Date(g.createdAt)
                    }));
                    setGoals(fetchedGoals);
                } else {
                    console.error("Failed to fetch goals");
                }
            } catch (err) {
                console.error("Error fetching dashboard data:", err);
            } finally {
                setLoadingTransactions(false);
                setLoadingGoals(false);
            }
        };

        fetchTransactionsAndGoals();
    }, [user]);

    const addTransaction = async (tx: Omit<Transaction, 'id'>): Promise<{ success: boolean; error?: string }> => {
        if (!user) return { success: false, error: "Not logged in" };
        try {
            const token = await user.getIdToken();
            const res = await fetch("/api/transactions", {
                method: "POST",
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    amount: tx.amount,
                    type: tx.type,
                    category: tx.category,
                    title: tx.title,
                    date: tx.date.toISOString(),
                })
            });
            if (res.ok) {
                const data = await res.json();
                const newTx: Transaction = {
                    ...data.transaction,
                    date: new Date(data.transaction.date)
                };
                setTransactions(prev => [newTx, ...prev].sort((a, b) => b.date.getTime() - a.date.getTime()));
                return { success: true };
            } else {
                const errData = await res.json();
                console.error("Failed to add transaction:", errData.error);
                return { success: false, error: errData.error };
            }
        } catch (err) {
            console.error("Error adding transaction:", err);
            return { success: false, error: (err as Error).message };
        }
    };

    const deleteTransaction = async (id: string) => {
        if (!user) return;
        try {
            const token = await user.getIdToken();
            const res = await fetch(`/api/transactions/${id}`, {
                method: "DELETE",
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (res.ok) {
                setTransactions(prev => prev.filter(t => t.id !== id));
            }
        } catch (err) {
            console.error("Error deleting transaction:", err);
        }
    };

    const scanBill = async (file: File): Promise<ScanBillResult> => {
        if (!user) return { success: false, error: "Not logged in" };
        try {
            const token = await user.getIdToken();
            const body = new FormData();
            body.append("file", file);
            const res = await fetch("/api/transactions/scan", {
                method: "POST",
                headers: { 'Authorization': `Bearer ${token}` },
                body
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok) return { success: false, error: data.error || "Upload failed" };

            const newTx: Transaction = { ...data.transaction, date: new Date(data.transaction.date) };
            setTransactions(prev => [newTx, ...prev].sort((a, b) => b.date.getTime() - a.date.getTime()));
            return { success: true, transaction: newTx, summary: data.summary ?? "" };
        } catch (err) {
            console.error("Error scanning bill:", err);
            return { success: false, error: (err as Error).message };
        }
    };

    const updateTransactionCategory = async (id: string, category: string): Promise<boolean> => {
        if (!user) return false;
        try {
            const token = await user.getIdToken();
            const res = await fetch(`/api/transactions/${id}`, {
                method: "PATCH",
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ category })
            });
            if (res.ok) {
                setTransactions(prev => prev.map(t => t.id === id ? { ...t, category } : t));
            }
            return res.ok;
        } catch (err) {
            console.error("Error updating transaction:", err);
            return false;
        }
    };

    const addGoal = async (goal: Omit<Goal, 'id' | 'currentAmount' | 'createdAt'>) => {
        if (!user) return;
        try {
            const token = await user.getIdToken();
            const res = await fetch("/api/goals", {
                method: "POST",
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(goal)
            });
            if (res.ok) {
                const data = await res.json();
                const newGoal: Goal = {
                    ...data.goal,
                    createdAt: new Date(data.goal.createdAt)
                };
                setGoals(prev => [newGoal, ...prev].sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime()));
            }
        } catch (err) {
            console.error("Error adding goal:", err);
        }
    };

    const addFundsToGoal = async (id: string, amountToAdd: number) => {
        if (!user) return;
        try {
            const token = await user.getIdToken();
            const res = await fetch(`/api/goals/${id}`, {
                method: "PATCH",
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ amountToAdd })
            });
            if (res.ok) {
                const data = await res.json();
                setGoals(prev => prev.map(g => g.id === id ? { ...g, currentAmount: data.goal.currentAmount } : g));
            }
        } catch (err) {
            console.error("Error updating goal funds:", err);
        }
    };

    const deleteGoal = async (id: string) => {
        if (!user) return;
        try {
            const token = await user.getIdToken();
            const res = await fetch(`/api/goals/${id}`, {
                method: "DELETE",
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (res.ok) {
                setGoals(prev => prev.filter(g => g.id !== id));
            }
        } catch (err) {
            console.error("Error deleting goal:", err);
        }
    };

    return (
        <DashboardContext.Provider value={{
            transactions, loadingTransactions, addTransaction, deleteTransaction, scanBill, updateTransactionCategory,
            goals, loadingGoals, addGoal, addFundsToGoal, deleteGoal
        }}>
            {children}
        </DashboardContext.Provider>
    );
}

export function useDashboard() {
    const context = useContext(DashboardContext);
    if (context === undefined) {
        throw new Error('useDashboard must be used within a DashboardProvider');
    }
    return context;
}
