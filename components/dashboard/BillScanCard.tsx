"use client";

import { useRef, useState } from "react";
import { format } from "date-fns";
import { useDashboard, Transaction } from "@/components/DashboardProvider";
import { DEBIT_CATEGORIES } from "@/lib/categories";
import { ScanLine, UploadCloud, Loader2, CheckCircle2, AlertCircle, Undo2, FileText, ImageIcon } from "lucide-react";
import { cn } from "@/lib/utils";

const ACCEPT = "image/jpeg,image/png,image/webp,application/pdf";
const MAX_UPLOAD_BYTES = 4 * 1024 * 1024;
const MAX_IMAGE_SIDE = 1600;

type ScanItem = {
    key: string;
    fileName: string;
    isPdf: boolean;
    status: "queued" | "scanning" | "done" | "error" | "undone";
    error?: string;
    transaction?: Transaction;
    summary?: string;
};

/** Downscale large phone photos so they upload fast and stay under the 4 MB AI limit. */
async function prepareImage(file: File): Promise<File> {
    if (file.size < 1024 * 1024) return file;
    try {
        const bitmap = await createImageBitmap(file);
        const scale = Math.min(1, MAX_IMAGE_SIDE / Math.max(bitmap.width, bitmap.height));
        const canvas = document.createElement("canvas");
        canvas.width = Math.round(bitmap.width * scale);
        canvas.height = Math.round(bitmap.height * scale);
        canvas.getContext("2d")!.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
        const blob = await new Promise<Blob | null>((r) => canvas.toBlob(r, "image/jpeg", 0.85));
        return blob ? new File([blob], file.name.replace(/\.\w+$/, ".jpg"), { type: "image/jpeg" }) : file;
    } catch {
        return file;
    }
}

export function BillScanCard() {
    const { scanBill, deleteTransaction, updateTransactionCategory } = useDashboard();
    const inputRef = useRef<HTMLInputElement>(null);
    const [items, setItems] = useState<ScanItem[]>([]);
    const [dragging, setDragging] = useState(false);
    const busy = items.some((i) => i.status === "queued" || i.status === "scanning");

    const patchItem = (key: string, patch: Partial<ScanItem>) =>
        setItems((prev) => prev.map((i) => (i.key === key ? { ...i, ...patch } : i)));

    const handleFiles = async (fileList: FileList | null) => {
        if (!fileList || fileList.length === 0) return;
        const files = Array.from(fileList);
        const queued: ScanItem[] = files.map((f, idx) => ({
            key: `${Date.now()}-${idx}-${f.name}`,
            fileName: f.name,
            isPdf: f.type === "application/pdf",
            status: "queued",
        }));
        setItems((prev) => [...queued, ...prev]);

        // One at a time keeps us well inside the AI rate limits.
        for (let i = 0; i < files.length; i++) {
            const { key } = queued[i];
            const original = files[i];

            if (!ACCEPT.split(",").includes(original.type)) {
                patchItem(key, { status: "error", error: "Unsupported file. Use JPG, PNG, WEBP or PDF." });
                continue;
            }
            patchItem(key, { status: "scanning" });
            const file = original.type.startsWith("image/") ? await prepareImage(original) : original;
            if (file.size > MAX_UPLOAD_BYTES) {
                patchItem(key, { status: "error", error: "File is larger than 4 MB." });
                continue;
            }

            const result = await scanBill(file);
            if (result.success) {
                patchItem(key, { status: "done", transaction: result.transaction, summary: result.summary });
            } else {
                patchItem(key, { status: "error", error: result.error });
            }
        }
    };

    const undo = async (item: ScanItem) => {
        if (!item.transaction) return;
        await deleteTransaction(item.transaction.id);
        patchItem(item.key, { status: "undone" });
    };

    const changeCategory = async (item: ScanItem, category: string) => {
        if (!item.transaction) return;
        const ok = await updateTransactionCategory(item.transaction.id, category);
        if (ok) patchItem(item.key, { transaction: { ...item.transaction, category } });
    };

    return (
        <div className="bg-surface-container-lowest p-6 rounded-xl ghost-border ambient-shadow">
            <div className="flex items-start justify-between gap-4 mb-5">
                <div>
                    <h2 className="text-xl font-bold text-on-surface flex items-center gap-2">
                        <ScanLine className="w-5 h-5 text-primary" /> Scan Bills &amp; Receipts
                    </h2>
                    <p className="text-sm text-on-surface-variant mt-1">
                        Upload restaurant bills, shopping invoices, tax receipts and more. AI reads the total and date,
                        picks the category, and adds the entry as a debit.
                    </p>
                </div>
                {items.length > 0 && !busy && (
                    <button
                        type="button"
                        onClick={() => setItems([])}
                        className="text-xs text-on-surface-variant hover:text-on-surface shrink-0"
                    >
                        Clear list
                    </button>
                )}
            </div>

            <button
                type="button"
                onClick={() => inputRef.current?.click()}
                onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
                onDragLeave={() => setDragging(false)}
                onDrop={(e) => { e.preventDefault(); setDragging(false); handleFiles(e.dataTransfer.files); }}
                className={cn(
                    "w-full flex flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed px-4 py-8 transition-all",
                    dragging
                        ? "border-primary bg-primary-container/20"
                        : "border-outline-variant/50 bg-surface-container-low hover:border-primary/50 hover:bg-surface-container"
                )}
            >
                <UploadCloud className="w-8 h-8 text-primary" />
                <span className="font-semibold text-on-surface">Drop files here or click to upload</span>
                <span className="text-xs text-on-surface-variant">JPG, PNG, WEBP or PDF · up to 4 MB · multiple files allowed</span>
            </button>
            <input
                ref={inputRef}
                type="file"
                accept={ACCEPT}
                multiple
                className="hidden"
                onChange={(e) => { handleFiles(e.target.files); e.target.value = ""; }}
            />

            {items.length > 0 && (
                <ul className="mt-5 space-y-3">
                    {items.map((item) => (
                        <li key={item.key} className="rounded-xl border border-outline-variant/30 bg-surface-container-low p-4">
                            <div className="flex items-center gap-3 min-w-0">
                                {item.isPdf
                                    ? <FileText className="w-4 h-4 text-on-surface-variant shrink-0" />
                                    : <ImageIcon className="w-4 h-4 text-on-surface-variant shrink-0" />}
                                <span className="text-sm text-on-surface-variant truncate flex-1">{item.fileName}</span>
                                {(item.status === "queued" || item.status === "scanning") && (
                                    <span className="flex items-center gap-1.5 text-xs text-primary shrink-0">
                                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                        {item.status === "queued" ? "Waiting…" : "Reading bill…"}
                                    </span>
                                )}
                                {item.status === "done" && <CheckCircle2 className="w-4 h-4 text-primary shrink-0" />}
                                {item.status === "error" && <AlertCircle className="w-4 h-4 text-error shrink-0" />}
                                {item.status === "undone" && <span className="text-xs text-on-surface-variant shrink-0">Removed</span>}
                            </div>

                            {item.status === "error" && <p className="text-sm text-error mt-2">{item.error}</p>}

                            {item.status === "done" && item.transaction && (
                                <div className="mt-3 flex flex-col sm:flex-row sm:items-center gap-3 justify-between">
                                    <div className="min-w-0">
                                        <div className="font-semibold text-on-surface truncate">{item.transaction.title}</div>
                                        <div className="text-xs text-on-surface-variant mt-0.5">
                                            {format(item.transaction.date, "MMM dd, yyyy")}
                                            {item.summary ? ` · ${item.summary}` : ""}
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-2 shrink-0">
                                        <select
                                            value={item.transaction.category}
                                            onChange={(e) => changeCategory(item, e.target.value)}
                                            aria-label="Category"
                                            className="bg-surface-container border border-outline-variant/40 rounded-lg px-2 py-1.5 text-xs font-medium text-primary outline-none cursor-pointer"
                                        >
                                            {DEBIT_CATEGORIES.map((c) => (
                                                <option key={c} value={c} className="bg-surface-container-lowest text-on-surface">{c}</option>
                                            ))}
                                        </select>
                                        <span className="font-mono font-bold text-error">-₹{item.transaction.amount.toFixed(2)}</span>
                                        <button
                                            type="button"
                                            onClick={() => undo(item)}
                                            title="Undo — remove this entry"
                                            className="w-8 h-8 rounded-lg flex items-center justify-center text-on-surface-variant hover:text-error hover:bg-error-container transition-colors"
                                        >
                                            <Undo2 className="w-4 h-4" />
                                        </button>
                                    </div>
                                </div>
                            )}
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}
