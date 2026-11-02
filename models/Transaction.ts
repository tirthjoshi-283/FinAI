import mongoose, { Schema, Document, models, model } from "mongoose";

export interface ITransaction extends Document {
    userId: string;          // Firebase UID
    type: "credit" | "debit";
    amount: number;
    category: string;
    title: string;
    date: Date;
    // Gmail UPI sync metadata — optional, only present for auto-imported entries
    source: "manual" | "gmail_upi" | "bill_scan" | "sms";
    gmailMessageId?: string;  // Gmail message ID — used for deduplication
    merchant?: string;        // e.g., "Swiggy", "Amazon Pay"
    upiRef?: string;          // UTR / UPI reference number
    bankName?: string;        // e.g., "HDFC Bank"
    smsHash?: string;         // hash of the bank SMS (sender + body) — dedup for SMS auto-capture
    accountLast4?: string;    // masked account / card digits from the SMS
    note?: string;            // user's own note, e.g. "Dinner with college friends"
    createdAt: Date;
    updatedAt: Date;
}

const TransactionSchema = new Schema<ITransaction>(
    {
        userId: { type: String, required: true, index: true },
        type: { type: String, enum: ["credit", "debit"], required: true },
        amount: { type: Number, required: true },
        category: { type: String, required: true },
        title: { type: String, required: true },
        date: { type: Date, required: true, default: Date.now },
        // Gmail UPI fields
        source: { type: String, enum: ["manual", "gmail_upi", "bill_scan", "sms"], default: "manual" },
        gmailMessageId: { type: String, default: undefined },
        merchant: { type: String, default: undefined },
        upiRef: { type: String, default: undefined },
        bankName: { type: String, default: undefined },
        smsHash: { type: String, default: undefined },
        accountLast4: { type: String, default: undefined },
        note: { type: String, default: undefined, maxlength: 300 },
    },
    { timestamps: true }
);

// Compound index: satisfies `find({ userId }).sort({ date: -1 })` in one scan
TransactionSchema.index({ userId: 1, date: -1 });
// Index for fast dedup lookup on gmailMessageId
TransactionSchema.index({ userId: 1, gmailMessageId: 1 }, { sparse: true });
// Index for fast dedup lookup on smsHash / upiRef (SMS auto-capture)
TransactionSchema.index({ userId: 1, smsHash: 1 }, { sparse: true });
// Cross-account lookup: an SMS belongs to exactly one person's phone.
TransactionSchema.index({ smsHash: 1 }, { sparse: true });
TransactionSchema.index({ userId: 1, upiRef: 1 }, { sparse: true });

// In dev, hot reload keeps the previously compiled model; rebuild it if its schema is stale
// (e.g. a newly added `source` enum value) instead of requiring a server restart.
const cached = models.Transaction;
if (cached && !cached.schema.path("source")?.options?.enum?.includes("sms")) {
    mongoose.deleteModel("Transaction");
}
export const Transaction = models.Transaction || model<ITransaction>("Transaction", TransactionSchema);
