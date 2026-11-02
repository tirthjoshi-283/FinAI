/** Shared helpers for the /api/splits routes. */

export type SplitPerson = { name: string; amount: number; settled: boolean };

type SplitDoc = {
    _id: { toString(): string };
    transactionId: string;
    title: string;
    total: number;
    people: SplitPerson[];
    createdAt: Date;
};

export function splitToDto(s: SplitDoc) {
    return {
        id: s._id.toString(),
        transactionId: s.transactionId,
        title: s.title,
        total: s.total,
        people: s.people.map((p) => ({ name: p.name, amount: p.amount, settled: p.settled })),
        createdAt: s.createdAt,
    };
}

/** Validates the friends list; returns an error message instead when it's invalid. */
export function cleanPeople(raw: unknown, total: number): SplitPerson[] | string {
    if (!Array.isArray(raw) || raw.length === 0) return "Add at least one friend.";
    if (raw.length > 20) return "At most 20 people per split.";
    const people: SplitPerson[] = [];
    const seen = new Set<string>();
    for (const r of raw) {
        const name = typeof r?.name === "string" ? r.name.trim().slice(0, 40) : "";
        const amount = Math.round(Number(r?.amount) * 100) / 100;
        if (!name) return "Every friend needs a name.";
        if (seen.has(name.toLowerCase())) return "Each friend can appear only once.";
        if (!(amount >= 0)) return "Enter a valid amount for " + name + ".";
        seen.add(name.toLowerCase());
        people.push({ name, amount, settled: Boolean(r?.settled) });
    }
    const owed = people.reduce((s, p) => s + p.amount, 0);
    if (owed > total + 0.01) return "Friends can't owe more than the bill.";
    return people;
}
