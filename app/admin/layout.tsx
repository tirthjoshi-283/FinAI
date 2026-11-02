import { ReactNode } from "react";
import { AmbientBackground } from "@/components/ui/AmbientBackground";

export default function AdminLayout({ children }: { children: ReactNode }) {
    return (
        <div className="min-h-screen relative">
            <AmbientBackground />
            <div className="relative z-10">{children}</div>
        </div>
    );
}
