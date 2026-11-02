import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        background: "var(--background)",
        foreground: "var(--foreground)",
        // Unified Semantic Tokens
        "surface":                  "var(--surface)",
        "surface-container-low":    "var(--surface-container-low)",
        "surface-container-lowest": "var(--surface-container-lowest)",
        "surface-container":        "var(--surface-container)",
        "surface-variant":          "var(--surface-variant)",
        "on-surface":               "var(--on-surface)",
        "on-surface-variant":       "var(--on-surface-variant)",
        "primary":                  "var(--primary)",
        "primary-container":        "var(--primary-container)",
        "on-primary":               "var(--on-primary)",
        "tertiary":                 "var(--tertiary)",
        "error":                    "var(--error)",
        "error-container":          "var(--error-container)",
        "outline":                  "var(--outline)"
      },
      boxShadow: {
        'ambient': '0 20px 40px -10px var(--shadow-color)',
      },
      animation: {
        marquee:     "marquee 32s linear infinite",
        floatNode:   "floatNode 5s ease-in-out infinite",
        drawLine:    "drawLine 8s ease-in-out infinite",
        auroraShift: "auroraShift 18s ease-in-out infinite",
        bounceDown:  "bounceDown 1.6s ease-in-out infinite",
        fadeSlideUp: "fadeSlideUp 0.7s cubic-bezier(0.22,1,0.36,1) forwards",
        pulseRing:   "pulseRing 2s ease-out infinite",
        slowSpin:    "slowSpin 20s linear infinite",
        shimmer:     "shimmer 2s infinite",
      },
      keyframes: {
        shimmer: {
          '100%': { transform: 'translateX(100%)' },
        }
      }
    },
  },
  plugins: [],
};
export default config;
