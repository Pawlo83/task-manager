package top.skiba.task_manager_backend.service;

import org.springframework.stereotype.Service;
import top.skiba.task_manager_backend.dto.TaskResponseDTO;
import top.skiba.task_manager_backend.dto.TaskRequestDTO;
import top.skiba.task_manager_backend.model.Task;
import top.skiba.task_manager_backend.model.User;
import top.skiba.task_manager_backend.repository.TaskRepository;
import top.skiba.task_manager_backend.exception.TaskNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public TaskResponseDTO createTask(TaskRequestDTO dto, User user) {
        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        task.setUser(user);
        return TaskResponseDTO.fromTask(taskRepository.save(task));
    }

    public List<TaskResponseDTO> getAllTasks(User user) {
        return taskRepository.findAllByUser(user)
            .stream()
            .map(TaskResponseDTO::fromTask)
            .collect(Collectors.toList());
    }

    public TaskResponseDTO getTaskById(Long id, User user) {
        return TaskResponseDTO.fromTask(getTaskEntityById(id, user));
    }

    public TaskResponseDTO updateTask(Long id, TaskRequestDTO dto, User user) {
        Task task = getTaskEntityById(id, user);
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        return TaskResponseDTO.fromTask(taskRepository.save(task));
    }

    public void deleteTask(Long id, User user) {
        taskRepository.delete(getTaskEntityById(id, user));
    }

    private Task getTaskEntityById(Long id, User user) {
        return taskRepository.findByIdAndUser(id, user)
            .orElseThrow(() -> new TaskNotFoundException(id));
    }
}
