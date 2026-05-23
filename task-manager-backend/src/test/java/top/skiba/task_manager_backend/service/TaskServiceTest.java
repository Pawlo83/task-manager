package top.skiba.task_manager_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.skiba.task_manager_backend.dto.TaskRequestDTO;
import top.skiba.task_manager_backend.dto.TaskResponseDTO;
import top.skiba.task_manager_backend.exception.TaskNotFoundException;
import top.skiba.task_manager_backend.model.Task;
import top.skiba.task_manager_backend.model.TaskStatus;
import top.skiba.task_manager_backend.model.User;
import top.skiba.task_manager_backend.repository.TaskRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @InjectMocks private TaskService taskService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
    }

    // ──────────────────────────────────────────────────────────
    // createTask
    // ──────────────────────────────────────────────────────────

    @Test
    void createTask_returnsSavedTask() {
        TaskRequestDTO dto = new TaskRequestDTO("Test1", "Desc1", TaskStatus.NEW);

        Task savedTask = new Task();
        savedTask.setId(1L);
        savedTask.setTitle("Test1");
        savedTask.setDescription("Desc1");
        savedTask.setStatus(TaskStatus.NEW);
        savedTask.setUser(testUser);

        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskResponseDTO result = taskService.createTask(dto, testUser);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test1", result.getTitle());
        verify(taskRepository).save(any(Task.class));
    }

    // ──────────────────────────────────────────────────────────
    // getAllTasks
    // ──────────────────────────────────────────────────────────

    @Test
    void getAllTasks_returnsOwnerTasksOnly() {
        Task task1 = new Task(); task1.setId(1L); task1.setTitle("Task1"); task1.setUser(testUser);
        Task task2 = new Task(); task2.setId(2L); task2.setTitle("Task2"); task2.setUser(testUser);

        when(taskRepository.findAllByUser(testUser)).thenReturn(List.of(task1, task2));

        List<TaskResponseDTO> result = taskService.getAllTasks(testUser);

        assertEquals(2, result.size());
        assertEquals("Task1", result.get(0).getTitle());
        assertEquals("Task2", result.get(1).getTitle());
        verify(taskRepository).findAllByUser(testUser);
    }

    @Test
    void getAllTasks_returnsEmptyListWhenNoTasks() {
        when(taskRepository.findAllByUser(testUser)).thenReturn(Collections.emptyList());

        List<TaskResponseDTO> result = taskService.getAllTasks(testUser);

        assertTrue(result.isEmpty());
        verify(taskRepository).findAllByUser(testUser);
    }

    // ──────────────────────────────────────────────────────────
    // getTaskById
    // ──────────────────────────────────────────────────────────

    @Test
    void getTaskById_returnsTask() {
        Task task = new Task(); task.setId(1L); task.setTitle("Task1"); task.setUser(testUser);

        when(taskRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(task));

        TaskResponseDTO result = taskService.getTaskById(1L, testUser);

        assertNotNull(result);
        assertEquals("Task1", result.getTitle());
        verify(taskRepository).findByIdAndUser(1L, testUser);
    }

    @Test
    void getTaskById_throwsExceptionWhenNotFound() {
        when(taskRepository.findByIdAndUser(99L, testUser)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.getTaskById(99L, testUser));
    }

    // ──────────────────────────────────────────────────────────
    // updateTask
    // ──────────────────────────────────────────────────────────

    @Test
    void updateTask_returnsUpdatedTask() {
        Task existing = new Task(); existing.setId(1L); existing.setTitle("Old"); existing.setStatus(TaskStatus.NEW); existing.setUser(testUser);
        TaskRequestDTO dto = new TaskRequestDTO("New Title", "New Desc", TaskStatus.DONE);

        when(taskRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenReturn(existing);

        TaskResponseDTO result = taskService.updateTask(1L, dto, testUser);

        assertEquals("New Title", result.getTitle());
        assertEquals("New Desc", result.getDescription());
        assertEquals(TaskStatus.DONE, result.getStatus());
        verify(taskRepository).save(existing);
    }

    @Test
    void updateTask_throwsExceptionWhenNotFound() {
        when(taskRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,
            () -> taskService.updateTask(999L, new TaskRequestDTO("T", "D", TaskStatus.NEW), testUser));
        verify(taskRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────
    // deleteTask
    // ──────────────────────────────────────────────────────────

    @Test
    void deleteTask_deletesTask() {
        Task task = new Task(); task.setId(1L); task.setUser(testUser);

        when(taskRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(task));

        taskService.deleteTask(1L, testUser);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteTask_throwsExceptionWhenNotFound() {
        when(taskRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.deleteTask(999L, testUser));
        verify(taskRepository, never()).delete(any());
    }
}
