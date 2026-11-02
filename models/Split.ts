import { Schema, Document, models, model } from "mongoose";

export interface ISplitPerson {
    name: string;
    amount: number;   // what this friend owes the user
    settled: boolean;
}

/** A debit the user paid for and shared with friends. */
export interface ISplit extends Document {
    userId: string;
    transactionId: string;
    title: string;
    total: number;
    people: ISplitPerson[];
    createdAt: Date;
    updatedAt: Date;
}

const PersonSchema = new Schema<ISplitPerson>(
    {
        name: { type: String, required: true, maxlength: 40 },
        amount: { type: Number, required: true, min: 0 },
        settled: { type: Boolean, default: false },
    },
    { _id: false }
);

const SplitSchema = new Schema<ISplit>(
    {
        userId: { type: String, required: true, index: true },
        transactionId: { type: String, required: true },
        title: { type: String, required: true },
        total: { type: Number, required: true },
        people: { type: [PersonSchema], default: [] },
    },
    { timestamps: true }
);

SplitSchema.index({ userId: 1, transactionId: 1 }, { unique: true });

export const Split = models.Split || model<ISplit>("Split", SplitSchema);
