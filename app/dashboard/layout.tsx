import { ReactNode } from "react";
import { DashboardTopbar } from "@/components/layout/DashboardTopbar";
import { DashboardBackground } from "@/components/ui/DashboardBackground";
import { DashboardProvider } from "@/components/DashboardProvider";

export default function DashboardLayout({ children }: { children: ReactNode }) {
    return (
        <div className="min-h-screen relative flex flex-col w-full">
            {/* Isolated Dashboard Background */}
            <DashboardBackground />

            <DashboardProvider>
                {/* Glass Topbar Shell */}
                <DashboardTopbar />

                {/* Main Content Area */}
                <main className="flex-1 w-full relative z-10 p-4 md:p-6 lg:p-8 min-w-0 transition-all duration-300 max-w-7xl mx-auto">
                    {children}
                </main>
            </DashboardProvider>
        </div>
    );
}
