import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";

export function RegisterPage() {
    const { register } = useAuth();
    const navigate = useNavigate();

    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const { dark, setDark } = useTheme();

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        setError(null);
        setLoading(true);
        try {
            await register(username, email, password);
            navigate("/", { replace: true });
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="min-h-screen flex items-center justify-center px-4" style={{ background: "var(--bg)" }}>
            <button
            onClick={() => setDark(!dark)}
            title={dark ? 'Tryb jasny' : 'Tryb ciemny'}
            className="fixed top-4 right-4 flex items-center justify-center w-8 h-8 rounded-lg border transition-all duration-150 hover:shadow-sm active:scale-95"
            style={{ background: 'var(--surface-2)', borderColor: 'var(--border)', color: 'var(--text-secondary)' }}
            >
            {dark ? (
                <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
                <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
                <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
                <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
                </svg>
            ) : (
                <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
                </svg>
            )}
            </button>
            <div className="w-full max-w-sm">
                <div className="mb-8 text-center">
                    <h1 className="text-2xl font-semibold" style={{ color: "var(--text)" }}>Create account</h1>
                    <p className="text-sm mt-1" style={{ color: "var(--text-muted)" }}>Start managing your tasks</p>
                </div>

                <div className="rounded-xl border p-6" style={{ background: "var(--surface)", borderColor: "var(--border)" }}>
                    {error && (
                        <div className="mb-4 text-sm px-3 py-2 rounded-lg border"
                            style={{ background: "var(--error-bg)", color: "var(--error-text)", borderColor: "var(--danger)" }}>
                            {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
                        <div className="flex flex-col gap-1">
                            <label className="text-sm font-medium" style={{ color: "var(--text-secondary)" }}>Username</label>
                            <input
                                type="text"
                                value={username}
                                onChange={e => setUsername(e.target.value)}
                                required
                                minLength={3}
                                autoFocus
                                placeholder="your_username"
                                className="w-full px-3 py-2 rounded-lg border text-sm outline-none transition-all"
                                style={{ background: "var(--surface-2)", borderColor: "var(--border)", color: "var(--text)" }}
                            />
                        </div>

                        <div className="flex flex-col gap-1">
                            <label className="text-sm font-medium" style={{ color: "var(--text-secondary)" }}>Email</label>
                            <input
                                type="email"
                                value={email}
                                onChange={e => setEmail(e.target.value)}
                                required
                                placeholder="you@example.com"
                                className="w-full px-3 py-2 rounded-lg border text-sm outline-none transition-all"
                                style={{ background: "var(--surface-2)", borderColor: "var(--border)", color: "var(--text)" }}
                            />
                        </div>

                        <div className="flex flex-col gap-1">
                            <label className="text-sm font-medium" style={{ color: "var(--text-secondary)" }}>Password</label>
                            <input
                                type="password"
                                value={password}
                                onChange={e => setPassword(e.target.value)}
                                required
                                minLength={8}
                                placeholder="min. 8 characters"
                                className="w-full px-3 py-2 rounded-lg border text-sm outline-none transition-all"
                                style={{ background: "var(--surface-2)", borderColor: "var(--border)", color: "var(--text)" }}
                            />
                        </div>

                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full py-2 rounded-lg text-sm font-medium transition-all duration-150 hover:opacity-90 active:scale-[0.98] disabled:opacity-50 disabled:cursor-default"
                            style={{ background: "var(--primary)", color: "var(--primary-text)" }}
                        >
                            {loading ? "Creating account…" : "Create account"}
                        </button>
                    </form>
                </div>

                <p className="mt-4 text-center text-sm" style={{ color: "var(--text-muted)" }}>
                    Already have an account?{" "}
                    <Link to="/login" className="font-medium" style={{ color: "var(--primary)" }}>
                        Sign in
                    </Link>
                </p>
            </div>
        </div>
    );
}
