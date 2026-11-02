import { Zap } from "lucide-react";
import Link from "next/link";
import { ReactNode } from "react";

export default function AuthLayout({ children }: { children: ReactNode }) {
    return (
        <div className="min-h-screen relative flex flex-col items-center justify-center overflow-hidden"
            style={{ background: "linear-gradient(160deg, #05050F 0%, #0A0818 50%, #050510 100%)" }}>
            {/* Violet aurora orb — top left */}
            <div className="fixed top-[-15%] left-[-10%] w-[55%] h-[55%] rounded-full blur-[130px] pointer-events-none"
                style={{ background: "radial-gradient(circle, rgba(124,58,237,0.20) 0%, transparent 70%)", animation: "auroraShift 18s ease-in-out infinite" }} />
            {/* Cyan aurora orb — top right */}
            <div className="fixed top-[15%] right-[-10%] w-[45%] h-[55%] rounded-full blur-[140px] pointer-events-none"
                style={{ background: "radial-gradient(circle, rgba(34,211,238,0.12) 0%, transparent 70%)", animation: "auroraShift 22s ease-in-out infinite reverse", animationDelay: "3s" }} />
            {/* Subtle violet bottom accent */}
            <div className="fixed bottom-[-10%] left-[15%] w-[60%] h-[35%] rounded-full blur-[110px] pointer-events-none"
                style={{ background: "radial-gradient(circle, rgba(99,38,200,0.14) 0%, transparent 70%)", animation: "auroraShift 25s ease-in-out infinite", animationDelay: "8s" }} />
            {/* Subtle dot-grid */}
            <div className="fixed inset-0 opacity-[0.04] pointer-events-none"
                style={{ backgroundImage: "radial-gradient(circle, rgba(255,255,255,1) 1px, transparent 1px)", backgroundSize: "40px 40px" }} />
            {/* Brand mark */}
            <Link href="/" className="absolute top-6 left-8 flex items-center gap-2.5 text-white font-bold text-xl tracking-tight z-20">
                <div className="w-8 h-8 rounded-lg flex items-center justify-center" style={{ background: "linear-gradient(135deg, #7C3AED, #22D3EE)" }}>
                    <Zap className="w-4 h-4 text-white" />
                </div>
                Fin<span style={{ color: "#9F67FF" }}>AI</span>
            </Link>
            <div className="relative z-10 w-full">{children}</div>
        </div>
    );
}
