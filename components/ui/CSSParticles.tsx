"use client";

import { useMemo } from "react";

interface CSSParticlesProps {
    count?: number;
    color?: string;
    speed?: "slow" | "medium" | "fast";
    direction?: "up" | "down" | "diagonal";
}

export function CSSParticles({
    count = 20,            // Reduced from 50–60 → 20
    color = "rgba(255,255,255,0.4)",
    speed = "medium",
    direction = "up",
}: CSSParticlesProps) {
    const particles = useMemo(() => {
        return Array.from({ length: count }).map((_, i) => {
            const size = Math.random() * 2.5 + 1;    // smaller: 1–3.5px
            const left = Math.random() * 100;
            const top = Math.random() * 100;

            let duration = 0;
            if (speed === "slow")   duration = 18 + Math.random() * 20;
            if (speed === "medium") duration = 10 + Math.random() * 12;
            if (speed === "fast")   duration = 5 + Math.random() * 7;

            const delay = -(Math.random() * 20); // start at random phase
            return { id: i, size, left, top, duration, delay };
        });
    }, [count, speed]);

    // Use the keyframe name that's now defined in globals.css
    const animationName =
        direction === "up" ? "float-up" :
        direction === "down" ? "float-down" : "float-diagonal";

    return (
        <div className="absolute inset-0 overflow-hidden pointer-events-none z-[-5]">
            {/* No <style> tag here — keyframes live in globals.css */}
            {particles.map((p) => (
                <div
                    key={p.id}
                    className="absolute rounded-full"
                    style={{
                        width: `${p.size}px`,
                        height: `${p.size}px`,
                        backgroundColor: color,
                        left: `${p.left}%`,
                        top: `${p.top}%`,
                        animationName,
                        animationDuration: `${p.duration}s`,
                        animationDelay: `${p.delay}s`,
                        animationIterationCount: "infinite",
                        animationTimingFunction: "linear",
                        // Removed boxShadow — it was recomputed every GPU frame
                        opacity: 0.7,
                        // Hint to browser to promote to its own compositor layer
                        willChange: "transform",
                    }}
                />
            ))}
        </div>
    );
}
