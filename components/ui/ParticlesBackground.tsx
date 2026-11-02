"use client";

import { useEffect, useState } from "react";
import Particles, { initParticlesEngine } from "@tsparticles/react";
import { loadSlim } from "@tsparticles/slim";
import { useTheme } from "next-themes";
import { usePathname } from "next/navigation";
import type { Engine, ISourceOptions } from "@tsparticles/engine";

export function ParticlesBackground() {
  const [init, setInit] = useState(false);
  const [mounted, setMounted] = useState(false);
  const { theme, systemTheme } = useTheme();
  const pathname = usePathname();

  useEffect(() => {
    setMounted(true);
    initParticlesEngine(async (engine: Engine) => {
      await loadSlim(engine);
    }).then(() => {
      setInit(true);
    });
  }, []);

  if (!mounted || !init) {
    return null;
  }

  const currentTheme = theme === "system" ? systemTheme : theme;
  const isDark = currentTheme !== "light";

  // Light theme colors: Veridian Ledger Scheme (greens)
  const lightColors = ["#006c49", "#10b981", "#059669"];
  const lightLinkColor = "#006c49";

  // Dark theme colors: Quantexa Modern Scheme (violet/cyan)
  const darkColors = ["#7C3AED", "#22D3EE", "#9F67FF"];
  const darkLinkColor = "#7C3AED";

  const particleColors = isDark ? darkColors : lightColors;
  const linkColor = isDark ? darkLinkColor : lightLinkColor;
  
  // Base options common to all
  const baseOptions: ISourceOptions = {
    background: { color: { value: "transparent" } },
    fpsLimit: 120,
    detectRetina: true,
    interactivity: {
      events: {
        onClick: { enable: true, mode: "push" },
        onHover: { enable: true, mode: "grab" },
      },
      modes: {
        push: { quantity: 3 },
        grab: { distance: 140, links: { opacity: isDark ? 0.4 : 0.6 } },
      },
    },
    particles: {
      color: { value: particleColors },
      links: {
        color: linkColor,
        distance: 150,
        enable: true,
        opacity: isDark ? 0.2 : 0.4,
        width: 1,
      },
      move: {
        enable: true,
        speed: 1,
        direction: "none",
        outModes: { default: "bounce" },
        random: true,
        straight: false,
      },
      number: {
        density: { enable: true },
        value: 50,
      },
      opacity: {
        value: { min: 0.1, max: isDark ? 0.4 : 0.7 },
        animation: { enable: true, speed: 1, sync: false },
      },
      shape: { type: "circle" },
      size: {
        value: { min: 1, max: 3 },
        animation: { enable: true, speed: 2, sync: false },
      },
    },
  };

  // Adjust options based on current page
  let options = { ...baseOptions };

  if (pathname === "/") {
    // Home: Standard mesh network (baseOptions is good)
  } else if (pathname?.startsWith("/auth")) {
    // Auth Pages: Slow moving, fewer particles, security/login vibe
    options = {
      ...baseOptions,
      particles: {
        ...baseOptions.particles,
        number: { density: { enable: true }, value: 30 },
        move: { ...(baseOptions.particles?.move as Record<string, unknown>), speed: 0.5 },
      }
    };
  } else if (pathname?.includes("/dashboard/goals")) {
    // Goals: Floating upwards (growth) like bubbles
    options = {
      ...baseOptions,
      particles: {
        ...baseOptions.particles,
        links: { ...(baseOptions.particles?.links as Record<string, unknown>), enable: false }, // Disable links for bubbles
        move: { 
            ...(baseOptions.particles?.move as Record<string, unknown>), 
            direction: "top", 
            speed: 1.5,
            outModes: { default: "out" }
        },
        size: { value: { min: 2, max: 6 }, animation: { enable: true, speed: 3, sync: false } },
        shape: { type: "circle" },
        opacity: { value: { min: 0.3, max: isDark ? 0.6 : 0.8 } }
      }
    };
  } else if (pathname?.startsWith("/dashboard")) {
    // Dashboard general: Data stream (geometric shapes)
    options = {
      ...baseOptions,
      particles: {
        ...baseOptions.particles,
        shape: { type: ["circle", "triangle", "edge"] },
        number: { density: { enable: true }, value: 60 },
        move: {
            ...(baseOptions.particles?.move as Record<string, unknown>),
            speed: 1.2,
        }
      }
    };
  }

  // Use a composite key so that particles fully re-render on theme OR pathname change
  const renderKey = `${currentTheme}-${pathname}`;

  return (
    <div className="fixed inset-0 pointer-events-none z-[40]">
      <Particles
        key={renderKey}
        id="tsparticles"
        options={options}
        className="w-full h-full"
      />
    </div>
  );
}
