import { type FormEvent, useState } from "react";
import { type TaskStatus } from "../types/task";

interface TaskFormProps {
    onCreate: (title: string, description: string, status: TaskStatus) => Promise<void>;
}

const STATUS_OPTIONS: { value: TaskStatus; label: string }[] = [
    { value: "NEW", label: "To Do" },
    { value: "IN_PROGRESS", label: "In Progress" },
    { value: "DONE", label: "Done" },
];

export function TaskForm({ onCreate }: TaskFormProps) {
    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [status, setStatus] = useState<TaskStatus>("NEW");
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [expanded, setExpanded] = useState(false);

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();
        setError(null);
        if (!title.trim()) { setError("Title is required"); return; }
        try {
            setSubmitting(true);
            await onCreate(title.trim(), description.trim(), status);
            setTitle("");
            setDescription("");
            setStatus("NEW");
            setExpanded(false);
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <div
            className="rounded-xl border mb-6 overflow-hidden transition-all duration-200"
            style={{ background: "var(--surface)", borderColor: "var(--border)" }}
        >
            <div
                className="flex items-center gap-3 px-4 py-3 cursor-pointer select-none"
                onClick={() => setExpanded(v => !v)}
            >
                <div
                    className="flex items-center justify-center w-6 h-6 rounded-full transition-all duration-200"
                    style={{
                        background: expanded ? "var(--primary)" : "var(--surface-2)",
                        color: expanded ? "var(--primary-text)" : "var(--text-muted)",
                    }}
                >
                    <svg
                        xmlns="http://www.w3.org/2000/svg"
                        className={`w-3.5 h-3.5 transition-transform duration-200 ${expanded ? "rotate-45" : ""}`}
                        viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
                        strokeLinecap="round" strokeLinejoin="round"
                    >
                        <line x1="12" y1="5" x2="12" y2="19" />
                        <line x1="5" y1="12" x2="19" y2="12" />
                    </svg>
                </div>
                <span className="text-sm font-medium" style={{ color: expanded ? "var(--primary)" : "var(--text-secondary)" }}>
                    Add a task
                </span>
            </div>

            {expanded && (
                <form onSubmit={handleSubmit} className="px-4 pb-4 border-t" style={{ borderColor: "var(--border)" }}>
                    <div className="pt-3 flex flex-col gap-3">
                        {error && (
                            <p className="text-xs px-2 py-1.5 rounded" style={{ color: "var(--error-text)", background: "var(--error-bg)" }}>
                                {error}
                            </p>
                        )}

                        <input
                            type="text"
                            value={title}
                            onChange={e => setTitle(e.target.value)}
                            placeholder="Task title"
                            autoFocus
                            className="w-full text-sm px-3 py-2 rounded-lg border outline-none transition-all duration-150"
                            style={{ background: "var(--surface-2)", borderColor: "var(--border)", color: "var(--text)" }}
                            onFocus={e => (e.target.style.borderColor = "var(--primary)")}
                            onBlur={e => (e.target.style.borderColor = "var(--border)")}
                        />

                        <textarea
                            value={description}
                            onChange={e => setDescription(e.target.value)}
                            placeholder="Description (optional)"
                            rows={2}
                            className="w-full text-sm px-3 py-2 rounded-lg border outline-none resize-none transition-all duration-150"
                            style={{ background: "var(--surface-2)", borderColor: "var(--border)", color: "var(--text)" }}
                            onFocus={e => (e.target.style.borderColor = "var(--primary)")}
                            onBlur={e => (e.target.style.borderColor = "var(--border)")}
                        />

                        <div className="flex items-center justify-between gap-3">
                            <div className="flex items-center gap-1.5">
                                {STATUS_OPTIONS.map(opt => (
                                    <button
                                        key={opt.value}
                                        type="button"
                                        onClick={() => setStatus(opt.value)}
                                        className="text-xs px-2.5 py-1 rounded-full border transition-all duration-150"
                                        style={{
                                            background: status === opt.value ? "var(--primary)" : "var(--surface-2)",
                                            borderColor: status === opt.value ? "var(--primary)" : "var(--border)",
                                            color: status === opt.value ? "var(--primary-text)" : "var(--text-secondary)",
                                            fontWeight: status === opt.value ? 600 : 400,
                                        }}
                                    >
                                        {opt.label}
                                    </button>
                                ))}
                            </div>

                            <div className="flex gap-2">
                                <button
                                    type="button"
                                    onClick={() => setExpanded(false)}
                                    className="text-sm px-3 py-1.5 rounded-lg border transition-all duration-150 hover:opacity-75"
                                    style={{ borderColor: "var(--border)", color: "var(--text-secondary)", background: "var(--surface-2)" }}
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    disabled={submitting}
                                    className="text-sm px-4 py-1.5 rounded-lg transition-all duration-150 hover:opacity-90 active:scale-95 disabled:opacity-50"
                                    style={{ background: "var(--primary)", color: "var(--primary-text)" }}
                                >
                                    {submitting ? "Adding…" : "Add task"}
                                </button>
                            </div>
                        </div>
                    </div>
                </form>
            )}
        </div>
    );
}
