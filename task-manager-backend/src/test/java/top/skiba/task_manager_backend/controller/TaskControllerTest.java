package top.skiba.task_manager_backend.controller;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import top.skiba.task_manager_backend.config.PasswordConfig;
import top.skiba.task_manager_backend.config.SecurityConfig;
import top.skiba.task_manager_backend.dto.TaskRequestDTO;
import top.skiba.task_manager_backend.dto.TaskResponseDTO;
import top.skiba.task_manager_backend.exception.TaskNotFoundException;
import top.skiba.task_manager_backend.model.TaskStatus;
import top.skiba.task_manager_backend.model.User;
import top.skiba.task_manager_backend.security.JwtUtil;
import top.skiba.task_manager_backend.service.TaskService;
import top.skiba.task_manager_backend.service.UserService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, PasswordConfig.class})
class TaskControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private TaskService taskService;
    @MockitoBean private UserService userService;
    @MockitoBean private JwtUtil jwtUtil;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded");
    }

    private RequestPostProcessor asUser(User user) {
        return SecurityMockMvcRequestPostProcessors.authentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    // ──────────────────────────────────────────────────────────
    // POST /api/tasks
    // ──────────────────────────────────────────────────────────

    @Test
    void createTaskReturns201() throws Exception {
        TaskResponseDTO response = new TaskResponseDTO(1L, "Task1", "Desc1", TaskStatus.NEW, LocalDateTime.now());
        when(taskService.createTask(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/tasks")
            .with(asUser(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new TaskRequestDTO("Task1", "Desc1", TaskStatus.NEW))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Task1"))
            .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void createTaskReturns400WhenTitleEmpty() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(asUser(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new TaskRequestDTO("", "Desc1", TaskStatus.NEW))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0]").value("Task title is required."));
    }

    @Test
    void createTaskReturns400WhenTitleTooLong() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(asUser(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new TaskRequestDTO("a".repeat(201), "Desc", TaskStatus.NEW))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void createTaskReturns400WhenDescriptionTooLong() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(asUser(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new TaskRequestDTO("Title", "d".repeat(5001), TaskStatus.NEW))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void createTaskReturns400WhenStatusInvalid() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(asUser(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Task1\",\"status\":\"STATUS\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Wrong data format."));
    }

    @Test
    void createTaskReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new TaskRequestDTO("T", null, TaskStatus.NEW))))
            .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────────────────────
    // GET /api/tasks
    // ──────────────────────────────────────────────────────────

    @Test
    void getAllTasksReturns200WithList() throws Exception {
        when(taskService.getAllTasks(any())).thenReturn(List.of(
            new TaskResponseDTO(1L, "Task1", "Desc1", TaskStatus.NEW, LocalDateTime.now()),
            new TaskResponseDTO(2L, "Task2", "Desc2", TaskStatus.IN_PROGRESS, LocalDateTime.now())));

        mockMvc.perform(get("/api/tasks").with(asUser(testUser)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllTasksReturns200WhenEmpty() throws Exception {
        when(taskService.getAllTasks(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/tasks").with(asUser(testUser)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    // ──────────────────────────────────────────────────────────
    // GET /api/tasks/{id}
    // ──────────────────────────────────────────────────────────

    @Test
    void getByIdReturns200() throws Exception {
        when(taskService.getTaskById(eq(1L), any())).thenReturn(
            new TaskResponseDTO(1L, "Task1", "Desc1", TaskStatus.NEW, LocalDateTime.now()));

        mockMvc.perform(get("/api/tasks/{id}", 1L).with(asUser(testUser)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    // ──────────────────────────────────────────────────────────
    // PUT /api/tasks/{id}
    // ──────────────────────────────────────────────────────────

    @Test
    void updateTaskReturns200() throws Exception {
        when(taskService.updateTask(eq(1L), any(), any())).thenReturn(
            new TaskResponseDTO(1L, "Updated", "New Desc", TaskStatus.DONE, LocalDateTime.now()));

        mockMvc.perform(put("/api/tasks/{id}", 1L)
            .with(asUser(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new TaskRequestDTO("Updated", "New Desc", TaskStatus.DONE))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated"))
            .andExpect(jsonPath("$.status").value("DONE"));
    }

    // ──────────────────────────────────────────────────────────
    // DELETE /api/tasks/{id}
    // ──────────────────────────────────────────────────────────

    @Test
    void deleteTaskReturns204() throws Exception {
        doNothing().when(taskService).deleteTask(eq(1L), any());

        mockMvc.perform(delete("/api/tasks/{id}", 1L)
            .with(asUser(testUser)).with(csrf()))
            .andExpect(status().isNoContent());
    }

    // ──────────────────────────────────────────────────────────
    // TaskNotFoundException → 404
    // ──────────────────────────────────────────────────────────

    @Test
    void taskNotFound_onGet_hasCorrectShape() throws Exception {
        when(taskService.getTaskById(eq(42L), any())).thenThrow(new TaskNotFoundException(42L));

        mockMvc.perform(get("/api/tasks/{id}", 42L).with(asUser(testUser)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void taskNotFound_onDelete_hasCorrectShape() throws Exception {
        doThrow(new TaskNotFoundException(7L)).when(taskService).deleteTask(eq(7L), any());

        mockMvc.perform(delete("/api/tasks/{id}", 7L)
            .with(asUser(testUser)).with(csrf()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void taskNotFound_onUpdate_hasCorrectShape() throws Exception {
        when(taskService.updateTask(eq(5L), any(), any())).thenThrow(new TaskNotFoundException(5L));

        mockMvc.perform(put("/api/tasks/{id}", 5L)
            .with(asUser(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new TaskRequestDTO("T", "D", TaskStatus.NEW))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    // ──────────────────────────────────────────────────────────
    // Validation response shape
    // ──────────────────────────────────────────────────────────

    @Test
    void validationFailure_hasErrorsArray() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(asUser(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"\",\"description\":\"desc\",\"status\":\"NEW\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[0]").value("Task title is required."))
            .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void validationFailure_doesNotContainMessageField() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(asUser(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"  \",\"status\":\"NEW\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void malformedJson_hasMessageField() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(asUser(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{ not valid json }"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Wrong data format."))
            .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void invalidEnumValue_hasMessageField() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(asUser(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Valid\",\"status\":\"NOT_A_STATUS\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Wrong data format."));
    }

    @Test
    void invalidEnumValue_doesNotContainErrorsArray() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(asUser(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Valid\",\"status\":\"NOT_A_STATUS\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").doesNotExist());
    }
}