/** Shared helpers for the /api/goals routes. */

type GoalDoc = {
    _id: { toString(): string };
    title: string;
    targetAmount: number;
    currentAmount: number;
    emoji?: string;
    deadline?: Date;
    createdAt: Date;
};

export function goalToDto(g: GoalDoc) {
    return {
        id: g._id.toString(),
        title: g.title,
        targetAmount: g.targetAmount,
        currentAmount: g.currentAmount,
        emoji: g.emoji ?? null,
        deadline: g.deadline ?? null,
        createdAt: g.createdAt,
    };
}

/** Positive, finite rupee amount up to ₹10 crore, or null. */
export function parseAmount(value: unknown): number | null {
    const n = typeof value === "number" ? value : Number(value);
    if (!Number.isFinite(n) || n <= 0 || n > 100_000_000) return null;
    return Math.round(n * 100) / 100;
}

/**
 * undefined = not provided; null = clear it; Date = a valid future date (≤ 30 years ahead).
 * Returns the string "invalid" for anything else.
 */
export function parseDeadline(value: unknown): Date | null | undefined | "invalid" {
    if (value === undefined) return undefined;
    if (value === null || value === "") return null;
    const d = new Date(String(value));
    if (isNaN(d.getTime())) return "invalid";
    const now = Date.now();
    if (d.getTime() < now - 24 * 60 * 60 * 1000 || d.getTime() > now + 30 * 365 * 24 * 60 * 60 * 1000) return "invalid";
    return d;
}

export function cleanEmoji(value: unknown): string | undefined {
    if (typeof value !== "string") return undefined;
    const e = value.trim();
    return e && e.length <= 8 ? e : undefined;
}
