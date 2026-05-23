import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { me, login as apiLogin, logout as apiLogout, register as apiRegister, ping as apiPing, type AuthUser } from "../api/auth";
import { ApiError } from "../api/apiUtils";

interface AuthContextValue {
    user: AuthUser | null;
    loading: boolean;
    initError: string | null;
    login: (username: string, password: string) => Promise<void>;
    register: (username: string, email: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<AuthUser | null>(null);
    const [loading, setLoading] = useState(true);
    const [initError, setInitError] = useState<string | null>(null);

    useEffect(() => {
        apiPing()
            .catch(() => { })
            .then(() => me())
            .then((fetchedUser) => {
                setUser(fetchedUser);
            })
            .catch((err) => {
                setUser(null);
                if (err instanceof ApiError && err.isNetworkError) {
                    setInitError("Cannot connect to the server.");
                }
            })
            .finally(() => setLoading(false));
    }, []);

    async function login(username: string, password: string) {
        const u = await apiLogin(username, password);
        setUser(u);
    }

    async function register(username: string, email: string, password: string) {
        const u = await apiRegister(username, email, password);
        setUser(u);
    }

    async function logout() {
        await apiLogout();
        setUser(null);
    }

    return (
        <AuthContext.Provider value={{ user, loading, initError, login, register, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
    return ctx;
}