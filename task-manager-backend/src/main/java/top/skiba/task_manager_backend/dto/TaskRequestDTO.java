package top.skiba.task_manager_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import top.skiba.task_manager_backend.model.TaskStatus;

public class TaskRequestDTO {

    @NotBlank(message = "Task title is required.")
    @Size(max = 200, message = "Title must not exceed 200 characters.")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters.")
    private String description;

    private TaskStatus status;

    public TaskRequestDTO() {}

    public TaskRequestDTO(String title, String description, TaskStatus status) {
        this.title = title;
        this.description = description;
        this.status = status;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
}