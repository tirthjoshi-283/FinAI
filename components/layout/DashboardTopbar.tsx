"use client";

import { useState, memo, useEffect } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
    LayoutDashboard,
    BarChart3,
    ReceiptText,
    Bot,
    Newspaper,
    LogOut,
    Zap,
    Shield,
    Target,
    Menu,
    X
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuth } from "@/context/AuthContext";
import { ThemeToggle } from "@/components/ThemeToggle";

const navItems = [
    { title: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
    { title: "Charts", href: "/dashboard/charts", icon: BarChart3 },
    { title: "Transactions", href: "/dashboard/transactions", icon: ReceiptText },
    { title: "Goals", href: "/dashboard/goals", icon: Target },
    { title: "AI", href: "/dashboard/ai-suggestions", icon: Bot },
    { title: "News", href: "/dashboard/newsletter", icon: Newspaper },
];

export const DashboardTopbar = memo(function DashboardTopbar() {
    const pathname = usePathname();
    const { user, userRole, logout } = useAuth();
    const [scrolled, setScrolled] = useState(false);
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

    useEffect(() => {
        const handleScroll = () => {
            setScrolled(window.scrollY > 20);
        };
        window.addEventListener("scroll", handleScroll, { passive: true });
        return () => window.removeEventListener("scroll", handleScroll);
    }, []);

    return (
        <nav className={cn(
            "sticky top-0 left-0 w-full z-[9999] transition-all duration-300 ease-in-out border-b text-sm backdrop-blur-xl pointer-events-auto",
            scrolled ? "bg-surface-container-lowest/80 border-outline-variant/30 shadow-[0_20px_40px_-10px_rgba(6,78,59,0.06)]"
                     : "bg-surface-container-lowest/40 border-outline-variant/10"
        )}>
            <div className="max-w-7xl mx-auto px-4 md:px-8">
                <div className="flex justify-between items-center h-16">
                    {/* Logo */}
                    <Link href="/dashboard" className="flex items-center gap-2.5 text-on-surface font-bold text-[1.1rem] tracking-tight">
                        <div className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 glass-gradient shadow-ambient">
                            <Zap className="w-4 h-4 text-white" />
                        </div>
                        <span className="hidden sm:inline">Fin<span className="text-primary">AI</span></span>
                    </Link>

                    {/* Desktop Navigation */}
                    <div className="hidden lg:flex items-center space-x-6 shrink-0 transition-none">
                        {navItems.map((item) => {
                            const isActive = pathname === item.href;
                            return (
                                <Link
                                    key={item.href}
                                    href={item.href}
                                    className={cn(
                                        "flex items-center gap-2 px-3 py-2 rounded-lg font-medium transition-colors outline-none",
                                        isActive
                                            ? "text-primary bg-primary-container/10"
                                            : "text-on-surface-variant hover:text-on-surface hover:bg-surface-variant/30"
                                    )}
                                >
                                    <item.icon className="w-4 h-4 flex-shrink-0" />
                                    <span>{item.title}</span>
                                </Link>
                            );
                        })}
                    </div>

                    {/* Right side (Admin + Profile + Logout) */}
                    <div className="hidden lg:flex items-center gap-4 shrink-0">
                        {userRole === "admin" && (
                            <Link
                                href="/admin"
                                className="flex items-center gap-2 px-3 py-2 rounded-lg text-primary-container hover:bg-primary-container/10 transition-colors font-medium border border-primary-container/20"
                            >
                                <Shield className="w-4 h-4" />
                                <span>Admin</span>
                            </Link>
                        )}
                        
                        <ThemeToggle />
                        
                        {user && (
                            <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-surface-container-low border border-outline-variant/20">
                                <div className="w-6 h-6 rounded-full flex items-center justify-center text-white text-[10px] font-bold glass-gradient">
                                    {(user.displayName || user.email || "U")[0].toUpperCase()}
                                </div>
                                <span className="text-on-surface font-medium max-w-[100px] truncate">
                                    {user.displayName || user.email?.split("@")[0]}
                                </span>
                            </div>
                        )}

                        <button
                            onClick={logout}
                            className="p-2 text-on-surface-variant hover:text-error hover:bg-error-container/50 rounded-lg transition-colors group"
                            aria-label="Sign Out"
                        >
                            <LogOut className="w-5 h-5 flex-shrink-0 group-hover:scale-110 transition-transform" />
                        </button>
                    </div>

                    {/* Mobile Menu Toggle */}
                    <div className="lg:hidden flex items-center gap-2">
                        {navItems.map(() => {
                           // Show only 2 most important icons on mobile header to save space if needed
                           // But since it's a menu let's just leave the toggle
                           return null;
                        })}
                        <button 
                            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                            className="p-2 rounded-lg bg-surface-container text-on-surface-variant hover:text-on-surface transition-colors"
                        >
                            {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
                        </button>
                    </div>
                </div>
            </div>

            {/* Mobile Navigation Menu */}
            {mobileMenuOpen && (
                <div className="lg:hidden border-t border-outline-variant/20 bg-surface-container-lowest/95 backdrop-blur-xl">
                    <div className="px-4 py-4 space-y-2 max-h-[70vh] overflow-y-auto">
                        {user && (
                            <div className="flex items-center gap-3 px-3 py-3 mb-2 rounded-xl bg-surface-container-low border border-outline-variant/20">
                                <div className="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-bold glass-gradient">
                                    {(user.displayName || user.email || "U")[0].toUpperCase()}
                                </div>
                                <div className="min-w-0 flex-1">
                                    <p className="text-on-surface font-medium truncate text-sm">{user.displayName || "User"}</p>
                                    <p className="text-on-surface-variant text-xs truncate">{user.email}</p>
                                </div>
                            </div>
                        )}

                        {navItems.map((item) => {
                            const isActive = pathname === item.href;
                            return (
                                <Link
                                    key={item.href}
                                    href={item.href}
                                    onClick={() => setMobileMenuOpen(false)}
                                    className={cn(
                                        "flex items-center gap-3 px-4 py-3 rounded-xl font-medium transition-all",
                                        isActive
                                            ? "text-primary bg-primary-container/10 border border-primary-container/20"
                                            : "text-on-surface-variant hover:text-on-surface hover:bg-surface-variant/50"
                                    )}
                                >
                                    <item.icon className={cn("w-5 h-5", isActive ? "text-primary" : "text-on-surface-variant")} />
                                    <span>{item.title}</span>
                                </Link>
                            );
                        })}

                        {userRole === "admin" && (
                            <Link
                                href="/admin"
                                onClick={() => setMobileMenuOpen(false)}
                                className="flex items-center gap-3 px-4 py-3 rounded-xl text-primary-container hover:bg-primary-container/10 transition-colors font-medium border border-primary-container/20 mt-2"
                            >
                                <Shield className="w-5 h-5" />
                                <span>Admin Panel</span>
                            </Link>
                        )}

                        <button
                            onClick={() => {
                                setMobileMenuOpen(false);
                                logout();
                            }}
                            className="flex items-center gap-3 px-4 py-3 w-full rounded-xl text-error hover:bg-error-container/50 transition-colors font-medium mt-4"
                        >
                            <LogOut className="w-5 h-5" />
                            <span>Sign Out</span>
                        </button>
                    </div>
                </div>
            )}
        </nav>
    );
});
