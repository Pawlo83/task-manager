import { type Task, type TaskStatus } from "../types/task";
import { useState, useEffect, useLayoutEffect, useRef, useCallback } from "react";
import { createPortal } from "react-dom";

interface TaskListProps {
    tasks: Task[];
    onChangeStatus: (task: Task, newStatus: TaskStatus) => Promise<void>;
    onDelete: (task: Task) => Promise<void>;
    onEdit: (task: Task, updates: { title: string; description: string; status: TaskStatus }) => Promise<void>;
}

const COLUMNS: { status: TaskStatus; label: string }[] = [
    { status: "NEW", label: "To Do" },
    { status: "IN_PROGRESS", label: "In Progress" },
    { status: "DONE", label: "Done" },
];

const COL_STYLE: Record<TaskStatus, { dot: string; bg: string; badgeBg: string; badgeTx: string }> = {
    NEW: { dot: "var(--col-new)", bg: "var(--col-new-bg)", badgeBg: "var(--col-new-badge-bg)", badgeTx: "var(--col-new-badge-tx)" },
    IN_PROGRESS: { dot: "var(--col-progress)", bg: "var(--col-progress-bg)", badgeBg: "var(--col-progress-badge-bg)", badgeTx: "var(--col-progress-badge-tx)" },
    DONE: { dot: "var(--col-done)", bg: "var(--col-done-bg)", badgeBg: "var(--col-done-badge-bg)", badgeTx: "var(--col-done-badge-tx)" },
};

type SortField = "createdAt" | "title";
type SortDir = "asc" | "desc";

const SORT_CYCLE: { field: SortField; dir: SortDir; label: string }[] = [
    { field: "createdAt", dir: "desc", label: "Newest first" },
    { field: "createdAt", dir: "asc", label: "Oldest first" },
    { field: "title", dir: "asc", label: "Name A → Z" },
    { field: "title", dir: "desc", label: "Name Z → A" },
];

function sortTasks(tasks: Task[], field: SortField, dir: SortDir): Task[] {
    return [...tasks].sort((a, b) => {
        const va = field === "title" ? a.title.toLowerCase() : a.createdAt;
        const vb = field === "title" ? b.title.toLowerCase() : b.createdAt;
        const cmp = va < vb ? -1 : va > vb ? 1 : 0;
        return dir === "asc" ? cmp : -cmp;
    });
}

export function TaskList({ tasks, onChangeStatus, onDelete, onEdit }: TaskListProps) {
    const [draggingId, setDraggingId] = useState<number | null>(null);
    const [dragOverCol, setDragOverCol] = useState<TaskStatus | null>(null);
    const [sortIdx, setSortIdx] = useState(0);

    const sort = SORT_CYCLE[sortIdx];
    const draggingTask = draggingId !== null ? tasks.find(t => t.id === draggingId) ?? null : null;

    const cardRefs = useRef<Map<number, HTMLElement>>(new Map());
    const savedPositions = useRef<Map<number, DOMRect>>(new Map());

    function captureCardPositions() {
        savedPositions.current = new Map();
        cardRefs.current.forEach((el, id) => {
            savedPositions.current.set(id, el.getBoundingClientRect());
        });
    }

    useLayoutEffect(() => {
        if (savedPositions.current.size === 0) return;
        cardRefs.current.forEach((el, id) => {
            const prev = savedPositions.current.get(id);
            if (!prev) return;
            const curr = el.getBoundingClientRect();
            const dy = prev.top - curr.top;
            const dx = prev.left - curr.left;
            if (Math.abs(dy) < 1 && Math.abs(dx) < 1) return;
            el.animate(
                [{ transform: `translate(${dx}px, ${dy}px)` }, { transform: "translate(0,0)" }],
                { duration: 300, easing: "cubic-bezier(0.25,0.46,0.45,0.94)", fill: "none" }
            );
        });
        savedPositions.current = new Map();
    }, [tasks, sortIdx]);

    const registerCard = useCallback((id: number, el: HTMLElement | null) => {
        if (el) cardRefs.current.set(id, el);
        else cardRefs.current.delete(id);
    }, []);

    function handleDragStart(e: React.DragEvent, taskId: number) {
        e.dataTransfer.setData("taskId", String(taskId));
        e.dataTransfer.effectAllowed = "move";
        setDraggingId(taskId);
    }

    function handleDragEnd() {
        setDraggingId(null);
        setDragOverCol(null);
    }

    function handleDragOver(e: React.DragEvent, col: TaskStatus) {
        if (draggingTask && draggingTask.status === col) return;
        e.preventDefault();
        e.dataTransfer.dropEffect = "move";
        setDragOverCol(col);
    }

    async function handleDrop(e: React.DragEvent, col: TaskStatus) {
        e.preventDefault();
        const taskId = Number(e.dataTransfer.getData("taskId"));
        const task = tasks.find(t => t.id === taskId);
        if (task && task.status !== col) {
            captureCardPositions();
            await onChangeStatus(task, col);
        }
        setDraggingId(null);
        setDragOverCol(null);
    }

    function handleSortClick() {
        captureCardPositions();
        setSortIdx(i => (i + 1) % SORT_CYCLE.length);
    }

    return (
        <div>
            <div className="flex items-center justify-end mb-3">
                <button
                    onClick={handleSortClick}
                    className="flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-lg border transition-all duration-150 hover:shadow-sm active:scale-95"
                    style={{ background: "var(--surface)", borderColor: "var(--border)", color: "var(--text-secondary)" }}
                >
                    <svg
                        xmlns="http://www.w3.org/2000/svg"
                        className={`w-3 h-3 transition-transform duration-300 ${sort.dir === "desc" ? "rotate-180" : ""}`}
                        viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
                        strokeLinecap="round" strokeLinejoin="round"
                    >
                        <line x1="12" y1="5" x2="12" y2="19" />
                        <polyline points="5 12 12 5 19 12" />
                    </svg>
                    {sort.label}
                </button>
            </div>

            <div className="grid grid-cols-3 gap-4">
                {COLUMNS.map(({ status, label }) => {
                    const raw = tasks.filter(t => t.status === status);
                    const colTasks = sortTasks(raw, sort.field, sort.dir);
                    const isSameCol = draggingTask?.status === status;
                    const isDragOver = dragOverCol === status && !isSameCol;
                    const style = COL_STYLE[status];

                    return (
                        <div
                            key={status}
                            onDragOver={e => handleDragOver(e, status)}
                            onDragLeave={() => setDragOverCol(null)}
                            onDrop={e => handleDrop(e, status)}
                            className="rounded-xl p-3 min-h-48 transition-all duration-200"
                            style={{
                                background: style.bg,
                                outline: isDragOver ? `2px solid ${style.dot}` : "2px solid transparent",
                                outlineOffset: "2px",
                                transform: isDragOver ? "scale(1.005)" : "scale(1)",
                                opacity: isSameCol && draggingId !== null ? 0.6 : 1,
                            }}
                        >
                            <div className="flex items-center gap-2 mb-3 px-1">
                                <span className="w-2 h-2 rounded-full flex-shrink-0" style={{ background: style.dot }} />
                                <h2 className="text-sm font-semibold" style={{ color: "var(--text)" }}>{label}</h2>
                                <span className="ml-auto text-xs rounded-full px-2 py-0.5 font-medium"
                                    style={{ background: "var(--surface)", color: "var(--text-muted)" }}>
                                    {colTasks.length}
                                </span>
                            </div>

                            {isDragOver && draggingId !== null && (
                                <div className="rounded-lg border-2 border-dashed h-14 mb-2 flex items-center justify-center text-xs font-medium"
                                    style={{ borderColor: style.dot, color: style.dot, opacity: 0.7 }}>
                                    Drop here
                                </div>
                            )}

                            <div className="flex flex-col gap-2">
                                {colTasks.length === 0 && !isDragOver && (
                                    <p className="text-xs text-center py-6" style={{ color: "var(--text-muted)" }}>No tasks yet</p>
                                )}
                                {colTasks.map(task => (
                                    <TaskCard
                                        key={task.id}
                                        task={task}
                                        isDragging={draggingId === task.id}
                                        accentColor={style.dot}
                                        badgeBg={style.badgeBg}
                                        badgeTx={style.badgeTx}
                                        onDragStart={e => handleDragStart(e, task.id)}
                                        onDragEnd={handleDragEnd}
                                        onDelete={onDelete}
                                        onEdit={onEdit}
                                        registerCard={registerCard}
                                    />
                                ))}
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
}

interface TaskCardProps {
    task: Task;
    isDragging: boolean;
    accentColor: string;
    badgeBg: string;
    badgeTx: string;
    onDragStart: (e: React.DragEvent) => void;
    onDragEnd: () => void;
    onDelete: (task: Task) => Promise<void>;
    onEdit: (task: Task, updates: { title: string; description: string; status: TaskStatus }) => Promise<void>;
    registerCard: (id: number, el: HTMLElement | null) => void;
}

function TaskCard({ task, isDragging, accentColor, badgeBg, badgeTx, onDragStart, onDragEnd, onDelete, onEdit, registerCard }: TaskCardProps) {
    const [editingField, setEditingField] = useState<"title" | "description" | null>(null);
    const [editTitle, setEditTitle] = useState(task.title);
    const [editDescription, setEditDescription] = useState(task.description);
    const [isDeleting, setIsDeleting] = useState(false);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [saving, setSaving] = useState(false);
    const [expanded, setExpanded] = useState(false);
    const [isClamped, setIsClamped] = useState(false);
    const cardRef = useRef<HTMLDivElement>(null);
    const titleRef = useRef<HTMLInputElement>(null);
    const descRef = useRef<HTMLTextAreaElement>(null);
    const titleDisplayRef = useRef<HTMLParagraphElement>(null);
    const descDisplayRef = useRef<HTMLParagraphElement>(null);

    const setCardRef = useCallback((el: HTMLDivElement | null) => {
        (cardRef as React.MutableRefObject<HTMLDivElement | null>).current = el;
        registerCard(task.id, el);
    }, [task.id, registerCard]);

    useLayoutEffect(() => {
        if (expanded) return;
        const t = titleDisplayRef.current;
        const d = descDisplayRef.current;
        setIsClamped(
            (t != null && t.scrollHeight > t.clientHeight) ||
            (d != null && d.scrollHeight > d.clientHeight)
        );
    }, [task.title, task.description, expanded]);

    useEffect(() => {
        if (!editingField) {
            setEditTitle(task.title);
            setEditDescription(task.description);
        }
    }, [task.title, task.description, editingField]);

    useEffect(() => {
        if (editingField === "title") titleRef.current?.focus();
        if (editingField === "description") descRef.current?.focus();
    }, [editingField]);

    useEffect(() => {
        if (!editingField) return;
        const handler = (e: KeyboardEvent) => { if (e.key === "Escape") cancelEdit(); };
        window.addEventListener("keydown", handler);
        return () => window.removeEventListener("keydown", handler);
    }, [editingField]);

    function cancelEdit() {
        setEditTitle(task.title);
        setEditDescription(task.description);
        setEditingField(null);
    }

    async function saveEdit() {
        if (!editTitle.trim()) return cancelEdit();
        setSaving(true);
        try {
            await onEdit(task, { title: editTitle.trim(), description: editDescription.trim(), status: task.status });
        } finally {
            setSaving(false);
            setEditingField(null);
        }
    }

    function confirmDelete() {
        setConfirmOpen(false);
        setIsDeleting(true);
        setTimeout(() => onDelete(task), 340);
    }

    function formatDate(dateStr: string) {
        const d = new Date(dateStr);
        if (Number.isNaN(d.getTime())) return "";
        return d.toLocaleDateString('en-GB', { month: 'short', day: 'numeric', year: 'numeric' });
    }

    function statusLabel(s: TaskStatus) {
        return s === "NEW" ? "New" : s === "IN_PROGRESS" ? "In Progress" : "Done";
    }

    const isEditing = editingField !== null;

    return (
        <>
            {confirmOpen && (
                <ConfirmModal
                    title={task.title}
                    cardRef={cardRef}
                    onConfirm={confirmDelete}
                    onCancel={() => setConfirmOpen(false)}
                />
            )}
            <div
                ref={setCardRef}
                draggable={!isEditing}
                onDragStart={onDragStart}
                onDragEnd={onDragEnd}
                className={[
                    "rounded-lg border p-3 group transition-all duration-200",
                    isEditing ? "" : "cursor-grab active:cursor-grabbing hover:-translate-y-0.5 hover:shadow-md",
                    isDragging ? "opacity-40 scale-95" : "opacity-100",
                    isDeleting ? "card-deleting" : "card-enter",
                ].join(" ")}
                style={{ background: "var(--surface)", borderColor: "var(--border)" }}
            >
                <div className="mb-1.5">
                    {editingField === "title" ? (
                        <input
                            ref={titleRef}
                            value={editTitle}
                            onChange={e => setEditTitle(e.target.value)}
                            onKeyDown={e => { if (e.key === "Enter") saveEdit(); }}
                            className="w-full text-sm font-semibold px-2 py-1 rounded border outline-none"
                            style={{ background: "var(--surface-2)", borderColor: accentColor, color: "var(--text)" }}
                        />
                    ) : (
                        <p
                            ref={titleDisplayRef}
                            onClick={() => setEditingField('title')}
                            title="Click to edit"
                            className={`text-sm font-semibold cursor-text transition-colors duration-150 hover:opacity-70 leading-snug ${!expanded ? 'line-clamp-2' : ''}`}
                            style={{
                                color: 'var(--text)',
                                textDecoration: task.status === 'DONE' ? 'line-through' : 'none',
                                opacity: task.status === 'DONE' ? 0.6 : 1,
                            }}
                        >
                            {task.title}
                        </p>
                    )}
                </div>

                <div className="mb-2.5">
                    {editingField === "description" ? (
                        <textarea
                            ref={descRef}
                            value={editDescription}
                            onChange={e => setEditDescription(e.target.value)}
                            rows={2}
                            className="w-full text-xs px-2 py-1 rounded border outline-none resize-none"
                            style={{ background: "var(--surface-2)", borderColor: accentColor, color: "var(--text-secondary)" }}
                        />
                    ) : (
                        <p
                            ref={descDisplayRef}
                            onClick={() => setEditingField("description")}
                            title="Click to edit"
                            className={`text-xs leading-relaxed cursor-text transition-colors duration-150 hover:opacity-70 min-h-[1.2rem] ${expanded ? "" : "line-clamp-2"}`}
                            style={{ color: task.description ? "var(--text-secondary)" : "var(--text-muted)" }}
                        >
                            {task.description || <em>Add description…</em>}
                        </p>
                    )}
                </div>

                {isEditing && (
                    <div className="flex gap-1.5 mb-2.5">
                        <button onClick={saveEdit} disabled={saving}
                            className="text-xs px-2.5 py-1 rounded-md transition-all duration-150 hover:opacity-90 active:scale-95 disabled:opacity-50"
                            style={{ background: accentColor, color: "#fff" }}>
                            {saving ? "Saving…" : "Save"}
                        </button>
                        <button onClick={cancelEdit}
                            className="text-xs px-2.5 py-1 rounded-md border transition-all duration-150 hover:opacity-75"
                            style={{ borderColor: "var(--border)", color: "var(--text-secondary)", background: "var(--surface-2)" }}>
                            Cancel
                        </button>
                        <span className="text-[10px] self-center ml-1" style={{ color: "var(--text-muted)" }}>Esc to cancel</span>
                    </div>
                )}

                <div className="flex items-center justify-between gap-2">
                    <span className="text-[10px]" style={{ color: "var(--text-muted)" }}>{formatDate(task.createdAt)}</span>
                    <div className="flex items-center gap-1.5">
                        {(isClamped || expanded) && (
                            <button
                                onClick={() => setExpanded(v => !v)}
                                className="opacity-0 group-hover:opacity-100 text-[10px] px-1.5 py-0.5 rounded transition-all duration-150"
                                style={{ color: "var(--text-muted)" }}
                            >
                                {expanded ? "▲" : "▼"}
                            </button>
                        )}
                        <span className="text-[10px] px-1.5 py-0.5 rounded-full font-medium"
                            style={{ background: badgeBg, color: badgeTx }}>
                            {statusLabel(task.status)}
                        </span>
                        <button
                            onClick={() => setConfirmOpen(true)}
                            title="Delete task"
                            className="opacity-0 group-hover:opacity-100 p-0.5 rounded transition-all duration-150 hover:scale-110"
                            style={{ color: "var(--text-muted)" }}
                            onMouseEnter={e => {
                                (e.currentTarget as HTMLButtonElement).style.color = "var(--danger)";
                                (e.currentTarget as HTMLButtonElement).style.background = "var(--danger-bg)";
                            }}
                            onMouseLeave={e => {
                                (e.currentTarget as HTMLButtonElement).style.color = "var(--text-muted)";
                                (e.currentTarget as HTMLButtonElement).style.background = "transparent";
                            }}
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none"
                                stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <polyline points="3 6 5 6 21 6" />
                                <path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6" />
                                <path d="M10 11v6M14 11v6" />
                                <path d="M9 6V4a1 1 0 011-1h4a1 1 0 011 1v2" />
                            </svg>
                        </button>
                    </div>
                </div>
            </div>
        </>
    );
}

interface ConfirmModalProps {
    title: string;
    cardRef: React.RefObject<HTMLDivElement | null>;
    onConfirm: () => void;
    onCancel: () => void;
}

function ConfirmModal({ title, cardRef, onConfirm, onCancel }: ConfirmModalProps) {
    const [rect, setRect] = useState<DOMRect | null>(null);
    const cancelRef = useRef<HTMLButtonElement>(null);

    useLayoutEffect(() => {
        if (cardRef.current) setRect(cardRef.current.getBoundingClientRect());
    }, []);

    useEffect(() => {
        cancelRef.current?.focus();
        const handler = (e: KeyboardEvent) => { if (e.key === "Escape") onCancel(); };
        window.addEventListener("keydown", handler);
        return () => window.removeEventListener("keydown", handler);
    }, []);

    const popupStyle: React.CSSProperties = rect
        ? { position: "fixed", top: rect.top + rect.height / 2, left: rect.left, width: rect.width, transform: "translateY(-50%)", zIndex: 9999 }
        : { position: "fixed", top: "50%", left: "50%", transform: "translate(-50%, -50%)", zIndex: 9999 };

    return createPortal(
        <>
            <div
                style={{ position: "fixed", inset: 0, zIndex: 9998, background: "rgba(15, 23, 42, 0.4)", backdropFilter: "blur(2px)" }}
                onClick={onCancel}
            />
            <div style={popupStyle}>
                <div
                    className="rounded-xl shadow-xl px-4 py-3 flex items-center gap-3"
                    style={{ background: "var(--surface)", border: "1px solid var(--border)" }}
                    onClick={e => e.stopPropagation()}
                >
                    <p className="text-sm flex-1 min-w-0 truncate" style={{ color: "var(--text-secondary)" }}>
                        Delete <span className="font-medium" style={{ color: "var(--text)" }}>"{title}"</span>?
                    </p>
                    <button ref={cancelRef} onClick={onCancel}
                        className="text-xs px-2.5 py-1 rounded-lg border flex-shrink-0 transition-all duration-150 hover:opacity-75"
                        style={{ borderColor: "var(--border)", color: "var(--text-secondary)", background: "var(--surface-2)" }}>
                        Cancel
                    </button>
                    <button onClick={onConfirm}
                        className="text-xs px-2.5 py-1 rounded-lg font-medium flex-shrink-0 transition-all duration-150 hover:opacity-90 active:scale-95"
                        style={{ background: "var(--danger)", color: "#fff" }}>
                        Delete
                    </button>
                </div>
            </div>
        </>,
        document.body
    );
}
