export type TaskStatus = "NEW" | "IN_PROGRESS" | "DONE";

export interface Task {
    id: number;
    title: string;
    description: string;
    status: TaskStatus;
    createdAt: string;
}

export interface CreateTaskPayload {
    title: string;
    description: string;
    status: TaskStatus;
}

export interface UpdateTaskPayload {
    title: string;
    description: string;
    status: TaskStatus;
}