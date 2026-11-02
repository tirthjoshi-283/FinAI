import { Schema, Document, models, model } from "mongoose";

/** A monthly spending limit for one debit category. */
export interface IBudget extends Document {
    userId: string;
    category: string;
    limit: number; // ₹ per calendar month
    createdAt: Date;
    updatedAt: Date;
}

const BudgetSchema = new Schema<IBudget>(
    {
        userId: { type: String, required: true },
        category: { type: String, required: true },
        limit: { type: Number, required: true, min: 1 },
    },
    { timestamps: true }
);

BudgetSchema.index({ userId: 1, category: 1 }, { unique: true });

export const Budget = models.Budget || model<IBudget>("Budget", BudgetSchema);
