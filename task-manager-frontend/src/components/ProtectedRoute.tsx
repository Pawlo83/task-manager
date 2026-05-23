import { type ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export function ProtectedRoute({ children }: { children: ReactNode }) {
    const { user, loading } = useAuth();

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center" style={{ background: "var(--bg)" }}>
                <div className="flex items-center gap-2 text-sm" style={{ color: "var(--text-muted)" }}>
                    <svg className="w-4 h-4 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="12" cy="12" r="10" strokeOpacity="0.25" />
                        <path d="M12 2a10 10 0 010 20" />
                    </svg>
                    Loading…
                </div>
            </div>
        );
    }

    if (!user) return <Navigate to="/login" replace />;
    return <>{children}</>;
}
