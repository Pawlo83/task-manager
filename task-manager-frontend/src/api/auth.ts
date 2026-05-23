import { apiFetch, handleResponse, ApiError } from "./apiUtils";

const BASE = `${import.meta.env.VITE_API_BASE_URL ?? ""}/api/auth`;

export interface AuthUser {
    username: string;
    email: string;
    csrfToken?: string;
}

// ── CSRF token management ─────────────────────────────────────────────────────
let csrfFallback: string | null = null;

export function getCsrfToken(): string | null {
    return readCsrfCookie() ?? csrfFallback;
}

function readCsrfCookie(): string | null {
    const match = document.cookie
        .split("; ")
        .find(row => row.startsWith("XSRF-TOKEN="));
    return match ? decodeURIComponent(match.split("=")[1]) : null;
}

// ── helpers ───────────────────────────────────────────────────────────────────

function csrfHeaders(extra: Record<string, string> = {}): Record<string, string> {
    const headers = { ...extra };
    const csrf = getCsrfToken();
    if (csrf) headers["X-XSRF-TOKEN"] = csrf;
    return headers;
}

// ── auth endpoints ────────────────────────────────────────────────────────────

export async function ping(): Promise<void> {
    const res = await apiFetch(`${BASE}/ping`, { credentials: "include" });
    if (res.ok) {
        const token = await res.text();
        if (token) csrfFallback = token;
    }
}

export async function register(username: string, email: string, password: string): Promise<AuthUser> {
    const res = await apiFetch(`${BASE}/register`, {
        method: "POST",
        headers: csrfHeaders({ "Content-Type": "application/json" }),
        credentials: "include",
        body: JSON.stringify({ username, email, password }),
    });
    await handleResponse<AuthUser>(res);
    const user = await me();
    if (!user) throw new ApiError(401, "Session lost after registration. Please log in.");
    return user;
}

export async function login(username: string, password: string): Promise<AuthUser> {
    const res = await apiFetch(`${BASE}/login`, {
        method: "POST",
        headers: csrfHeaders({ "Content-Type": "application/json" }),
        credentials: "include",
        body: JSON.stringify({ username, password }),
    });
    if (res.status === 401) throw new ApiError(401, "Incorrect username or password.");
    await handleResponse<AuthUser>(res);
    const user = await me();
    if (!user) throw new ApiError(401, "Session lost after login. Please try again.");
    return user;
}

export async function logout(): Promise<void> {
    const res = await apiFetch(`${BASE}/logout`, {
        method: "POST",
        credentials: "include",
        headers: csrfHeaders(),
    });
    csrfFallback = null;
    if (!res.ok && res.status !== 401 && res.status !== 403) {
        await handleResponse<void>(res);
    }
}

export async function me(): Promise<AuthUser | null> {
    const res = await apiFetch(`${BASE}/me`, { credentials: "include" });
    if (res.status === 401) return null;
    const user = await handleResponse<AuthUser>(res);
    if (user?.csrfToken) csrfFallback = user.csrfToken;
    return user ?? null;
}