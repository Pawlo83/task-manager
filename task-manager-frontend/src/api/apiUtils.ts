export class ApiError extends Error {
    readonly status: number | null;

    constructor(status: number | null, message: string) {
        super(message);
        this.name = "ApiError";
        this.status = status;
    }

    get isNetworkError() { return this.status === null; }
    get isUnauthorized() { return this.status === 401; }
    get isForbidden() { return this.status === 403; }
    get isNotFound() { return this.status === 404; }
    get isConflict() { return this.status === 409; }
    get isServerError() { return this.status !== null && this.status >= 500; }
}

export async function handleResponse<T>(res: Response): Promise<T> {
    if (res.ok) {
        if (res.status === 204) return undefined as T;
        return res.json();
    }

    let serverMessage: string | null = null;
    try {
        const data = await res.json();
        serverMessage = data.message ?? data.error ?? null;
    } catch { /* ignore parse errors */ }

    const message = friendlyMessage(res.status, serverMessage);
    throw new ApiError(res.status, message);
}

export async function apiFetch(url: string, options?: RequestInit): Promise<Response> {
    try {
        return await fetch(url, options);
    } catch {
        throw new ApiError(null, "Cannot connect to the server. Check your connection.");
    }
}

function friendlyMessage(status: number, serverMessage: string | null): string {
    if (serverMessage && status < 500) return serverMessage;

    switch (status) {
        case 400: return "Invalid request. Please check your input.";
        case 401: return "Session expired. Please log in again.";
        case 403: return "You don't have permission to do that.";
        case 404: return "The requested item was not found.";
        case 409: return serverMessage ?? "This already exists.";
        case 422: return "The data you submitted is invalid.";
        case 429: return "Too many requests. Please slow down.";
        case 500:
        case 502:
        case 503: return "Server error. Please try again later.";
        default:  return `Request failed (${status}). Please try again.`;
    }
}