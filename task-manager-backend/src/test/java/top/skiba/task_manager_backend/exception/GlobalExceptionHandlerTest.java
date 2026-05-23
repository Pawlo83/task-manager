package top.skiba.task_manager_backend.exception;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.skiba.task_manager_backend.controller.TaskController;
import top.skiba.task_manager_backend.dto.TaskRequestDTO;
import top.skiba.task_manager_backend.model.TaskStatus;
import top.skiba.task_manager_backend.model.User;
import top.skiba.task_manager_backend.security.JwtUtil;
import top.skiba.task_manager_backend.service.TaskService;
import top.skiba.task_manager_backend.service.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class GlobalExceptionHandlerTest {

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

    // ──────────────────────────────────────────────────────────
    // TaskNotFoundException → 404
    // ──────────────────────────────────────────────────────────

    @Test
    void taskNotFound_onGet_hasCorrectShape() throws Exception {
        when(taskService.getTaskById(eq(42L), any(User.class))).thenThrow(new TaskNotFoundException(42L));

        mockMvc.perform(get("/api/tasks/{id}", 42L).with(user(testUser)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Task with ID = 42 does not exist."))
            .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void taskNotFound_onDelete_hasCorrectShape() throws Exception {
        doThrow(new TaskNotFoundException(7L)).when(taskService).deleteTask(eq(7L), any(User.class));

        mockMvc.perform(delete("/api/tasks/{id}", 7L).with(user(testUser)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Task with ID = 7 does not exist."));
    }

    @Test
    void taskNotFound_onUpdate_hasCorrectShape() throws Exception {
        when(taskService.updateTask(eq(5L), any(), any(User.class))).thenThrow(new TaskNotFoundException(5L));

        mockMvc.perform(put("/api/tasks/{id}", 5L)
            .with(user(testUser))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new TaskRequestDTO("T", "D", TaskStatus.NEW))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Task with ID = 5 does not exist."));
    }

    // ──────────────────────────────────────────────────────────
    // MethodArgumentNotValidException → 400 (errors array)
    // ──────────────────────────────────────────────────────────

    @Test
    void validationFailure_hasErrorsArray() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(user(testUser))
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
            .with(user(testUser))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"  \",\"status\":\"NEW\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").doesNotExist());
    }

    // ──────────────────────────────────────────────────────────
    // HttpMessageNotReadableException → 400 (message field)
    // ──────────────────────────────────────────────────────────

    @Test
    void malformedJson_hasMessageField() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(user(testUser))
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
            .with(user(testUser))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Valid\",\"status\":\"NOT_A_STATUS\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Wrong data format."));
    }

    @Test
    void invalidEnumValue_doesNotContainErrorsArray() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(user(testUser))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Valid\",\"status\":\"NOT_A_STATUS\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").doesNotExist());
    }
}
