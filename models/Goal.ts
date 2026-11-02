import mongoose from "mongoose";

const GoalSchema = new mongoose.Schema({
    userId: { type: String, required: true, index: true },
    title: { type: String, required: true },
    targetAmount: { type: Number, required: true },
    currentAmount: { type: Number, default: 0 },
    emoji: { type: String, default: undefined, maxlength: 8 },
    /** Optional date the user wants to reach the goal by. */
    deadline: { type: Date, default: undefined },
    createdAt: { type: Date, default: Date.now, index: true }
});

// In dev, hot reload keeps the previously compiled model; rebuild it if its schema is stale.
if (mongoose.models.Goal && !mongoose.models.Goal.schema.path("deadline")) {
    mongoose.deleteModel("Goal");
}
// Avoid re-compiling the model if it already exists
export const Goal = mongoose.models.Goal || mongoose.model("Goal", GoalSchema);
