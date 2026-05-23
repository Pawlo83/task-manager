import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { type Task, type TaskStatus } from "../types/task";
import { fetchTasks, createTask, updateTask, deleteTask } from "../api/tasks";
import { TaskForm } from "../components/TaskForm";
import { TaskList } from "../components/TaskList";
import { useAuth } from "../context/AuthContext";
import { ApiError } from "../api/apiUtils";
import { useTheme } from "../context/ThemeContext";

export function BoardPage() {
    const { user, logout, initError } = useAuth();
    const navigate = useNavigate();

    const [tasks, setTasks] = useState<Task[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [globalMessage, setGlobalMessage] = useState<string | null>(null);
    const { dark, setDark } = useTheme();

    useEffect(() => {
        document.documentElement.classList.toggle("dark", dark);
        localStorage.setItem("theme", dark ? "dark" : "light");
    }, [dark]);

    function handleApiError(err: unknown, fallback = "Something went wrong.") {
        if (err instanceof ApiError) {
            if (err.isUnauthorized) {
                navigate("/login", { replace: true });
                return;
            }
            setError(err.message);
        } else {
            setError(fallback);
        }
    }

    async function loadTasks() {
        setLoading(true);
        setError(null);
        try {
            const data = await fetchTasks();
            setTasks(data);
        } catch (err) {
            handleApiError(err, "Failed to load tasks.");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => { loadTasks(); }, []);

    function flash(msg: string) {
        setGlobalMessage(msg);
        setTimeout(() => setGlobalMessage(null), 3000);
    }

    async function handleCreate(title: string, description: string, status: TaskStatus) {
        try {
            await createTask({ title, description, status });
            await loadTasks();
            flash("Task created");
        } catch (err) { handleApiError(err, "Failed to create task."); }
    }

    async function handleChangeStatus(task: Task, newStatus: TaskStatus) {
        try {
            await updateTask(task.id, { title: task.title, description: task.description, status: newStatus });
            await loadTasks();
        } catch (err) { handleApiError(err, "Failed to update task."); }
    }

    async function handleEdit(task: Task, updates: { title: string; description: string; status: TaskStatus }) {
        try {
            await updateTask(task.id, updates);
            await loadTasks();
            flash("Task updated");
        } catch (err) { handleApiError(err, "Failed to save task."); }
    }

    async function handleDelete(task: Task) {
        try {
            await deleteTask(task.id);
            await loadTasks();
            flash("Task deleted");
        } catch (err) { handleApiError(err, "Failed to delete task."); }
    }

    async function handleLogout() {
        await logout();
        navigate("/login", { replace: true });
    }

    return (
        <div className="min-h-screen" style={{ background: "var(--bg)" }}>
            <header style={{ background: "var(--surface)", borderBottom: "1px solid var(--border)" }}>
                <div className="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between">
                    <div>
                        <h1 className="text-lg font-semibold" style={{ color: "var(--text)" }}>Task Manager</h1>
                        <p className="text-sm mt-0.5" style={{ color: "var(--text-muted)" }}>
                            Kanban board · {user?.username}
                        </p>
                    </div>
                    <div className="flex items-center gap-2">
                        <button onClick={() => setDark(!dark)}
                            title={dark ? "Switch to light mode" : "Switch to dark mode"}
                            className="flex items-center justify-center w-8 h-8 rounded-lg border transition-all duration-150 hover:shadow-sm active:scale-95"
                            style={{ background: "var(--surface-2)", borderColor: "var(--border)", color: "var(--text-secondary)" }}>
                            {dark ? (
                                <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <circle cx="12" cy="12" r="5" /><line x1="12" y1="1" x2="12" y2="3" /><line x1="12" y1="21" x2="12" y2="23" />
                                    <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" /><line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
                                    <line x1="1" y1="12" x2="3" y2="12" /><line x1="21" y1="12" x2="23" y2="12" />
                                    <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" /><line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
                                </svg>
                            ) : (
                                <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z" />
                                </svg>
                            )}
                        </button>
                        <button onClick={loadTasks} disabled={loading}
                            className="flex items-center gap-1.5 text-sm px-3 py-1.5 rounded-lg border transition-all duration-150 hover:shadow-sm active:scale-95 disabled:opacity-50 disabled:cursor-default"
                            style={{ background: "var(--surface-2)", borderColor: "var(--border)", color: "var(--text-secondary)" }}>
                            <svg xmlns="http://www.w3.org/2000/svg" className={`w-3.5 h-3.5 ${loading ? "animate-spin" : ""}`}
                                viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <polyline points="23 4 23 10 17 10" /><polyline points="1 20 1 14 7 14" />
                                <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15" />
                            </svg>
                            {loading ? "Loading…" : "Refresh"}
                        </button>
                        <button onClick={handleLogout}
                            className="flex items-center gap-1.5 text-sm px-3 py-1.5 rounded-lg border transition-all duration-150 hover:shadow-sm active:scale-95"
                            style={{ background: "var(--surface-2)", borderColor: "var(--border)", color: "var(--text-secondary)" }}>
                            <svg xmlns="http://www.w3.org/2000/svg" className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" /><polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" />
                            </svg>
                            Logout
                        </button>
                    </div>
                </div>
            </header>

            <main className="max-w-6xl mx-auto px-6 py-6">
                <div className="mb-4 flex flex-col gap-2">
                    {initError && !error && (
                        <div className="text-sm px-4 py-2.5 rounded-lg border"
                            style={{ background: "var(--error-bg)", color: "var(--error-text)", borderColor: "var(--danger)" }}>
                            ⚠ {initError} Some features may not work until the connection is restored.
                        </div>
                    )}
                    {error && (
                        <div className="text-sm px-4 py-2.5 rounded-lg border"
                            style={{ background: "var(--error-bg)", color: "var(--error-text)", borderColor: "var(--danger)" }}>
                            {error}
                        </div>
                    )}
                    {globalMessage && (
                        <div className="text-sm px-4 py-2.5 rounded-lg border"
                            style={{ background: "var(--success-bg)", color: "var(--success-text)", borderColor: "var(--col-done)" }}>
                            {globalMessage}
                        </div>
                    )}
                </div>

                <TaskForm onCreate={handleCreate} />

                {loading && tasks.length === 0 ? (
                    <div className="flex items-center justify-center h-48" style={{ color: "var(--text-muted)" }}>
                        <div className="flex items-center gap-2 text-sm">
                            <svg className="w-4 h-4 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <circle cx="12" cy="12" r="10" strokeOpacity="0.25" /><path d="M12 2a10 10 0 010 20" />
                            </svg>
                            Loading tasks…
                        </div>
                    </div>
                ) : (
                    <TaskList tasks={tasks} onChangeStatus={handleChangeStatus} onDelete={handleDelete} onEdit={handleEdit} />
                )}
            </main>
        </div>
    );
}