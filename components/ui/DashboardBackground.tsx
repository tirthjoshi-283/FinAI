"use client";

import { memo } from "react";

export const DashboardBackground = memo(function DashboardBackground() {
    return (
        <div
            className="fixed inset-0 z-[-10] pointer-events-none overflow-hidden bg-surface"
        >
            {/* Subtle atmospheric depth: slight gradient shift */}
            <div
                className="absolute inset-0 opacity-[0.4]"
                style={{
                    background: "radial-gradient(circle at top right, rgba(16, 185, 129, 0.08) 0%, transparent 60%)",
                }}
            />
            <div
                className="absolute inset-0 opacity-[0.3]"
                style={{
                    background: "radial-gradient(circle at bottom left, rgba(0, 108, 73, 0.05) 0%, transparent 50%)",
                }}
            />

            {/* Soft Dot-grid overlay for editorial texture */}
            <div
                className="absolute inset-0 opacity-[0.15]"
                style={{
                    backgroundImage: "radial-gradient(circle, var(--outline-variant) 1px, transparent 1px)",
                    backgroundSize: "32px 32px",
                }}
            />
        </div>
    );
});
