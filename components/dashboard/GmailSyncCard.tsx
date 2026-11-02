"use client";

/**
 * GmailSyncCard.tsx
 *
 * Dashboard UI card for the Gmail UPI Auto-Sync feature.
 * Handles: connect, sync, status display, disconnect.
 * Safe: never exposes tokens or raw email data to the UI.
 */

import { useState, useEffect, useCallback } from "react";
import { useAuth } from "@/context/AuthContext";
import { useDashboard } from "@/components/DashboardProvider";
import {
    Mail,
    RefreshCw,
    CheckCircle2,
    XCircle,
    Unlink,
    Zap,
    Clock,
    ShieldCheck,
    ChevronDown,
    ChevronUp,
    ArrowUpRight,
    ArrowDownRight,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { formatDistanceToNow } from "date-fns";

interface GmailStatus {
    connected: boolean;
    lastSyncAt: string | null;
    syncedCount: number;
}

interface SyncResult {
    synced: number;
    skipped: number;
    failed: number;
    message: string;
    transactions: {
        id: string;
        type: "credit" | "debit";
        amount: number;
        category: string;
        title: string;
        date: string;
        bankName?: string;
        merchant?: string;
    }[];
}

export function GmailSyncCard() {
    const { user } = useAuth();
    const { transactions, loadingTransactions } = useDashboard();

    const [status, setStatus] = useState<GmailStatus | null>(null);
    const [loadingStatus, setLoadingStatus] = useState(true);
    const [syncing, setSyncing] = useState(false);
    const [connecting, setConnecting] = useState(false);
    const [disconnecting, setDisconnecting] = useState(false);
    const [lastResult, setLastResult] = useState<SyncResult | null>(null);
    const [showResult, setShowResult] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Count auto-synced transactions
    const gmailTxCount = transactions.filter(
        (t) => (t as { source?: string }).source === "gmail_upi"
    ).length;

    const fetchStatus = useCallback(async () => {
        if (!user) return;
        try {
            const res = await fetch("/api/gmail/status", {
                headers: { Authorization: `Bearer ${await user.getIdToken()}` },
            });
            if (res.ok) {
                const data = await res.json();
                setStatus(data);
            }
        } catch {
            // Silently ignore — status is non-critical
        } finally {
            setLoadingStatus(false);
        }
    }, [user]);

    useEffect(() => {
        fetchStatus();
    }, [fetchStatus]);

    // Check for OAuth callback result in URL
    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        if (params.get("gmailConnected") === "true") {
            fetchStatus();
            // Clean up URL
            const url = new URL(window.location.href);
            url.searchParams.delete("gmailConnected");
            window.history.replaceState({}, "", url.toString());
        }
        if (params.get("gmailError")) {
            setError(`Gmail connection failed: ${params.get("gmailError")}`);
            const url = new URL(window.location.href);
            url.searchParams.delete("gmailError");
            window.history.replaceState({}, "", url.toString());
        }
    }, [fetchStatus]);

    const handleConnect = async () => {
        if (!user) return;
        setConnecting(true);
        setError(null);
        try {
            const res = await fetch("/api/gmail/connect", {
                headers: { Authorization: `Bearer ${await user.getIdToken()}` },
            });
            const data = await res.json();
            if (data.authUrl) {
                // Pass Firebase UID as state param for the callback
                const url = new URL(data.authUrl);
                url.searchParams.set("state", user.uid);
                window.location.href = url.toString();
            } else {
                setError("Failed to get Google auth URL.");
            }
        } catch (e) {
            setError((e as Error).message);
        } finally {
            setConnecting(false);
        }
    };

    const handleSync = async () => {
        if (!user || syncing) return;
        setSyncing(true);
        setError(null);
        setLastResult(null);
        setShowResult(false);
        try {
            const res = await fetch("/api/gmail/sync", {
                method: "POST",
                headers: { Authorization: `Bearer ${await user.getIdToken()}` },
            });
            const data = await res.json();
            if (!res.ok) {
                setError(data.error ?? "Sync failed.");
            } else {
                setLastResult(data);
                setShowResult(true);
                // Refresh status + transactions
                await fetchStatus();
                // Trigger dashboard re-fetch by dispatching a custom event
                window.dispatchEvent(new CustomEvent("gmail-sync-complete"));
            }
        } catch (e) {
            setError((e as Error).message);
        } finally {
            setSyncing(false);
        }
    };

    const handleDisconnect = async () => {
        if (!user || disconnecting) return;
        if (!confirm("Disconnect Gmail? You can reconnect anytime. Already synced transactions will remain.")) return;
        setDisconnecting(true);
        setError(null);
        try {
            const res = await fetch("/api/gmail/disconnect", {
                method: "DELETE",
                headers: { Authorization: `Bearer ${await user.getIdToken()}` },
            });
            if (res.ok) {
                setStatus({ connected: false, lastSyncAt: null, syncedCount: 0 });
                setLastResult(null);
            } else {
                const data = await res.json();
                setError(data.error ?? "Disconnect failed.");
            }
        } catch (e) {
            setError((e as Error).message);
        } finally {
            setDisconnecting(false);
        }
    };

    if (loadingStatus || loadingTransactions) {
        return (
            <div className="bg-surface-container-lowest ghost-border ambient-shadow rounded-2xl p-6 animate-pulse">
                <div className="h-4 bg-surface-variant/50 rounded-full w-1/3 mb-3" />
                <div className="h-3 bg-surface-variant/30 rounded-full w-1/2" />
            </div>
        );
    }

    const isConnected = status?.connected ?? false;
    const lastSyncAt = status?.lastSyncAt ? new Date(status.lastSyncAt) : null;

    return (
        <div className="bg-surface-container-lowest ghost-border ambient-shadow rounded-2xl overflow-hidden">
            {/* Header */}
            <div className="p-6 pb-4 flex items-start justify-between gap-4">
                <div className="flex items-center gap-3">
                    {/* Animated Gmail icon */}
                    <div
                        className={cn(
                            "w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0 transition-all duration-300",
                            isConnected
                                ? "bg-primary-container/30 text-primary shadow-[0_0_20px_-5px_rgba(16,185,129,0.4)]"
                                : "bg-surface-container text-on-surface-variant"
                        )}
                    >
                        <Mail className="w-5 h-5" />
                    </div>
                    <div>
                        <div className="flex items-center gap-2">
                            <h3 className="text-on-surface font-semibold text-base leading-tight">
                                Gmail UPI Auto-Sync
                            </h3>
                            <span
                                className={cn(
                                    "text-[10px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider flex items-center gap-1",
                                    isConnected
                                        ? "bg-primary-container/30 text-primary border border-primary/20"
                                        : "bg-surface-container text-on-surface-variant border border-outline-variant/30"
                                )}
                            >
                                {isConnected ? (
                                    <>
                                        <span className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse inline-block" />
                                        Live
                                    </>
                                ) : (
                                    "Inactive"
                                )}
                            </span>
                        </div>
                        <p className="text-xs text-on-surface-variant mt-0.5">
                            {isConnected
                                ? "Importing UPI alerts from verified bank senders"
                                : "Connect Gmail to auto-import UPI transactions"}
                        </p>
                    </div>
                </div>

                {isConnected && (
                    <button
                        onClick={handleDisconnect}
                        disabled={disconnecting}
                        title="Disconnect Gmail"
                        className="text-on-surface-variant hover:text-error transition-colors p-2 rounded-lg hover:bg-error-container/20 flex-shrink-0 disabled:opacity-50"
                    >
                        <Unlink className="w-4 h-4" />
                    </button>
                )}
            </div>

            {/* Stats row — only when connected */}
            {isConnected && (
                <div className="px-6 pb-4 grid grid-cols-3 gap-3">
                    <div className="bg-surface-container rounded-xl p-3 text-center">
                        <div className="text-primary font-bold text-lg leading-none">{gmailTxCount}</div>
                        <div className="text-on-surface-variant text-[10px] mt-1">Imported</div>
                    </div>
                    <div className="bg-surface-container rounded-xl p-3 text-center">
                        <div className="text-on-surface font-bold text-lg leading-none">
                            {status?.syncedCount ?? 0}
                        </div>
                        <div className="text-on-surface-variant text-[10px] mt-1">Emails Seen</div>
                    </div>
                    <div className="bg-surface-container rounded-xl p-3 text-center">
                        <div className="text-on-surface font-bold text-[11px] leading-tight truncate">
                            {lastSyncAt ? formatDistanceToNow(lastSyncAt, { addSuffix: true }) : "Never"}
                        </div>
                        <div className="text-on-surface-variant text-[10px] mt-1">Last Sync</div>
                    </div>
                </div>
            )}

            {/* Trust badges */}
            {!isConnected && (
                <div className="px-6 pb-4 flex flex-wrap gap-2">
                    {[
                        { icon: ShieldCheck, text: "Read-only access" },
                        { icon: Zap, text: "Banks only" },
                        { icon: Clock, text: "Zero manual entry" },
                    ].map(({ icon: Icon, text }) => (
                        <span
                            key={text}
                            className="flex items-center gap-1.5 text-[11px] text-on-surface-variant bg-surface-container px-2.5 py-1 rounded-full border border-outline-variant/30"
                        >
                            <Icon className="w-3 h-3 text-primary" />
                            {text}
                        </span>
                    ))}
                </div>
            )}

            {/* Error display */}
            {error && (
                <div className="mx-6 mb-4 p-3 rounded-xl bg-error-container/20 border border-error/20 flex items-start gap-2">
                    <XCircle className="w-4 h-4 text-error flex-shrink-0 mt-0.5" />
                    <p className="text-xs text-error leading-relaxed">{error}</p>
                </div>
            )}

            {/* Last sync result */}
            {lastResult && (
                <div className="mx-6 mb-4">
                    <button
                        onClick={() => setShowResult((v) => !v)}
                        className="w-full flex items-center justify-between p-3 rounded-xl bg-primary-container/20 border border-primary/20 text-left"
                    >
                        <div className="flex items-center gap-2">
                            <CheckCircle2 className="w-4 h-4 text-primary flex-shrink-0" />
                            <span className="text-xs text-primary font-medium">{lastResult.message}</span>
                        </div>
                        {lastResult.transactions.length > 0 &&
                            (showResult ? (
                                <ChevronUp className="w-4 h-4 text-primary flex-shrink-0" />
                            ) : (
                                <ChevronDown className="w-4 h-4 text-primary flex-shrink-0" />
                            ))}
                    </button>

                    {showResult && lastResult.transactions.length > 0 && (
                        <div className="mt-2 space-y-1.5 max-h-48 overflow-y-auto pr-1">
                            {lastResult.transactions.map((tx) => (
                                <div
                                    key={tx.id}
                                    className="flex items-center justify-between p-2.5 rounded-lg bg-surface-container border border-outline-variant/20 text-xs"
                                >
                                    <div className="flex items-center gap-2 min-w-0">
                                        <div
                                            className={cn(
                                                "w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0",
                                                tx.type === "credit"
                                                    ? "bg-primary-container/40 text-primary"
                                                    : "bg-error-container/40 text-error"
                                            )}
                                        >
                                            {tx.type === "credit" ? (
                                                <ArrowUpRight className="w-3.5 h-3.5" />
                                            ) : (
                                                <ArrowDownRight className="w-3.5 h-3.5" />
                                            )}
                                        </div>
                                        <div className="min-w-0">
                                            <div className="font-medium text-on-surface truncate">{tx.title}</div>
                                            <div className="text-on-surface-variant truncate">
                                                {tx.bankName} · {tx.category}
                                            </div>
                                        </div>
                                    </div>
                                    <div
                                        className={cn(
                                            "font-bold font-mono flex-shrink-0 ml-2",
                                            tx.type === "credit" ? "text-primary" : "text-error"
                                        )}
                                    >
                                        {tx.type === "credit" ? "+" : "-"}₹{tx.amount.toFixed(2)}
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            )}

            {/* Action buttons */}
            <div className="px-6 pb-6">
                {!isConnected ? (
                    <button
                        onClick={handleConnect}
                        disabled={connecting}
                        id="gmail-connect-btn"
                        className="w-full flex items-center justify-center gap-2.5 px-5 py-3 rounded-xl font-semibold text-sm transition-all duration-300 bg-gradient-to-br from-primary to-primary-container text-on-primary shadow-[0_8px_20px_-8px_rgba(16,185,129,0.5)] hover:shadow-[0_12px_24px_-8px_rgba(16,185,129,0.6)] hover:-translate-y-0.5 disabled:opacity-60 disabled:cursor-not-allowed disabled:transform-none"
                    >
                        {connecting ? (
                            <RefreshCw className="w-4 h-4 animate-spin" />
                        ) : (
                            <Mail className="w-4 h-4" />
                        )}
                        {connecting ? "Redirecting to Google..." : "Connect Gmail Account"}
                    </button>
                ) : (
                    <button
                        onClick={handleSync}
                        disabled={syncing}
                        id="gmail-sync-btn"
                        className="w-full flex items-center justify-center gap-2.5 px-5 py-3 rounded-xl font-semibold text-sm transition-all duration-300 bg-primary-container/30 text-primary border border-primary/20 hover:bg-primary-container/50 hover:border-primary/40 disabled:opacity-60 disabled:cursor-not-allowed"
                    >
                        <RefreshCw className={cn("w-4 h-4", syncing && "animate-spin")} />
                        {syncing ? "Scanning your inbox..." : "Sync UPI Emails Now"}
                    </button>
                )}
            </div>
        </div>
    );
}
