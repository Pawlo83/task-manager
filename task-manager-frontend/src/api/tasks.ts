import { apiFetch, handleResponse } from "./apiUtils";
import { getCsrfToken } from "./auth";
import { type Task, type CreateTaskPayload, type UpdateTaskPayload } from "../types/task";

const API_BASE = `${import.meta.env.VITE_API_BASE_URL ?? ""}/api/tasks`;

function mutationHeaders(): Record<string, string> {
    const headers: Record<string, string> = { "Content-Type": "application/json" };
    const csrf = getCsrfToken();
    if (csrf) headers["X-XSRF-TOKEN"] = csrf;
    return headers;
}

export async function fetchTasks(): Promise<Task[]> {
    const res = await apiFetch(API_BASE, { credentials: "include" });
    return handleResponse<Task[]>(res);
}

export async function createTask(payload: CreateTaskPayload): Promise<Task> {
    const res = await apiFetch(API_BASE, {
        method: "POST",
        headers: mutationHeaders(),
        credentials: "include",
        body: JSON.stringify(payload),
    });
    return handleResponse<Task>(res);
}

export async function updateTask(id: number, payload: UpdateTaskPayload): Promise<Task> {
    const res = await apiFetch(`${API_BASE}/${id}`, {
        method: "PUT",
        headers: mutationHeaders(),
        credentials: "include",
        body: JSON.stringify(payload),
    });
    return handleResponse<Task>(res);
}

export async function deleteTask(id: number): Promise<void> {
    const csrf = getCsrfToken();
    const headers: Record<string, string> = {};
    if (csrf) headers["X-XSRF-TOKEN"] = csrf;
    const res = await apiFetch(`${API_BASE}/${id}`, {
        method: "DELETE",
        credentials: "include",
        headers,
    });
    return handleResponse<void>(res);
}