import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

export function middleware(request: NextRequest) {
    const { pathname } = request.nextUrl;
    const token = request.cookies.get("auth-token")?.value;

    const isProtected = pathname.startsWith("/dashboard") || pathname.startsWith("/admin");
    const isAuthPage = pathname.startsWith("/auth");

    if (isProtected && !token) {
        const loginUrl = new URL("/auth/login", request.url);
        loginUrl.searchParams.set("redirect", pathname);
        return NextResponse.redirect(loginUrl);
    }

    if (isAuthPage && token) {
        const role = request.cookies.get("user-role")?.value;
        if (role === "admin") {
            return NextResponse.redirect(new URL("/admin", request.url));
        }
        return NextResponse.redirect(new URL("/dashboard", request.url));
    }

    return NextResponse.next();
}

export const config = {
    matcher: ["/dashboard/:path*", "/admin/:path*", "/auth/:path*"],
};
