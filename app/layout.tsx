import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { cn } from "@/lib/utils";
import { AuthProvider } from "@/context/AuthContext";

const inter = Inter({
  subsets: ['latin'],
  variable: '--font-sans',
  weight: ['300', '400', '500', '600', '700', '800', '900'],
  display: 'swap',
});

import { ThemeProvider } from "@/components/ThemeProvider";
import { ParticlesBackground } from "@/components/ui/ParticlesBackground";

export const metadata: Metadata = {
  title: "FinAI – AI-Powered Investment Platform",
  description: "Transform your wealth with AI-driven insights, OSINT market intelligence, and automated risk management—all from one highly-secure dashboard.",
  keywords: ["fintech", "AI investing", "portfolio management", "automated trading"],
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={cn("font-sans", inter.variable)} suppressHydrationWarning>
      <body className={`${inter.variable} antialiased`}>
        <ThemeProvider attribute="class" defaultTheme="dark" enableSystem disableTransitionOnChange>
          <AuthProvider>
            <ParticlesBackground />
            <div className="relative z-10 w-full min-h-screen">
              {children}
            </div>
          </AuthProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
