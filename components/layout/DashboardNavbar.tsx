"use client";

import { memo } from "react";
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
    Target
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuth } from "@/context/AuthContext";

import { ThemeToggle } from "@/components/ThemeToggle";

const navbarItems = [
    { title: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
    { title: "Charts", href: "/dashboard/charts", icon: BarChart3 },
    { title: "Transactions", href: "/dashboard/transactions", icon: ReceiptText },
    { title: "Goals", href: "/dashboard/goals", icon: Target },
    { title: "AI", href: "/dashboard/ai-suggestions", icon: Bot },
    { title: "News", href: "/dashboard/newsletter", icon: Newspaper },
];

export const DashboardNavbar = memo(function DashboardNavbar() {
    const pathname = usePathname();
    const { user, userRole, logout } = useAuth();

    return (
        <header
            className="sticky top-0 left-0 z-40 w-full transition-all duration-300 ease-in-out flex flex-row items-center justify-between px-4 h-16 md:h-20 bg-surface/80 backdrop-blur-[20px] border-b border-outline/30"
        >
            {/* Logo */}
            <div className="flex items-center gap-2.5 text-on-surface font-bold text-xl tracking-tight flex-shrink-0">
                <div className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 bg-primary shadow-ambient"
                    style={{ background: "linear-gradient(135deg, var(--primary), var(--tertiary))" }}>
                    <Zap className="w-4 h-4 text-on-primary" />
                </div>
                <span className="hidden sm:block">
                    Fin<span className="text-primary-container">AI</span>
                </span>
            </div>

            {/* Nav Items (Center) - Scrollable on mobile */}
            <div className="flex-1 overflow-x-auto flex flex-row items-center justify-start md:justify-center px-4 gap-1 sm:gap-2 no-scrollbar">
                {navbarItems.map((item) => {
                    const isActive = pathname === item.href;
                    return (
                        <Link
                            key={item.href}
                            href={item.href}
                            prefetch={true}
                            className={cn(
                                "flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-medium transition-all group relative whitespace-nowrap flex-shrink-0 border",
                                isActive
                                    ? "text-on-surface bg-primary/10 border-primary/20 shadow-[0_0_16px_var(--shadow-color)]"
                                    : "text-on-surface-variant hover:text-on-surface border-transparent hover:bg-surface-variant hover:border-outline/20"
                            )}
                        >
                            {/* Active bottom pill — full width */}
                            {isActive ? (
                                <div
                                    className="absolute bottom-0 left-0 w-full h-[2.5px] rounded-t-full"
                                    style={{ background: "linear-gradient(to right, var(--primary), var(--tertiary))" }}
                                />
                            ) : (
                                /* Hover underline: transition-transform is set directly so no style-jsx needed */
                                <span
                                    aria-hidden="true"
                                    className="absolute bottom-0 left-0 w-full h-[2.5px] rounded-t-full origin-center scale-x-0 group-hover:scale-x-100 transition-transform duration-300 ease-out"
                                    style={{ background: "linear-gradient(to right, var(--primary), var(--primary-container))" }}
                                />
                            )}

                            <item.icon className={cn("w-4 h-4 sm:w-5 sm:h-5 flex-shrink-0 transition-colors", isActive ? "text-primary" : "text-on-surface-variant group-hover:text-on-surface")} />
                            <span className="hidden lg:block transition-all duration-300">
                                {item.title}
                            </span>
                        </Link>
                    );
                })}
            </div>

            {/* Right side group: Admin, User Info, Logout */}
            <div className="flex items-center gap-2 flex-shrink-0">
                
                <ThemeToggle />

                {/* Admin Link */}
                {userRole === "admin" && (
                    <Link
                        href="/admin"
                        prefetch={true}
                        className={cn(
                            "flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-medium transition-all group relative hidden md:flex border",
                            pathname === "/admin" 
                              ? "text-tertiary bg-tertiary/10 border-tertiary/20" 
                              : "text-on-surface-variant hover:text-tertiary border-transparent hover:bg-tertiary/5 hover:border-tertiary/10"
                        )}
                    >
                        <Shield className={cn("w-4 h-4 flex-shrink-0", pathname === "/admin" ? "text-tertiary" : "text-on-surface-variant group-hover:text-tertiary")} />
                        <span>Admin</span>
                    </Link>
                )}

                {/* User Avatar */}
                {user && (
                    <div className="w-9 h-9 rounded-full flex items-center justify-center text-on-primary text-xs font-bold flex-shrink-0 shadow-ambient"
                        style={{ background: "linear-gradient(135deg, var(--primary), var(--primary-container))" }}
                        title={user.email || "User"}
                    >
                        {(user.displayName || user.email || "U")[0].toUpperCase()}
                    </div>
                )}

                {/* Sign Out */}
                <button
                    onClick={logout}
                    title="Sign Out"
                    className="flex items-center justify-center p-2 rounded-xl text-error hover:text-error/80 transition-colors group relative ml-1 hover:bg-error/10"
                >
                    <LogOut className="w-5 h-5 flex-shrink-0" />
                </button>
            </div>
            <style>{`
                .no-scrollbar::-webkit-scrollbar { display: none; }
                .no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
            `}</style>
        </header>
    );
});
