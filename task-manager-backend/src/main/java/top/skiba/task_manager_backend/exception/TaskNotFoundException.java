package top.skiba.task_manager_backend.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long id) {
        super("Task with ID = " + id + " does not exist.");
    }
}
