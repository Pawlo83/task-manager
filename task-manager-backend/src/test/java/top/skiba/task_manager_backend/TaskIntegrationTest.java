package top.skiba.task_manager_backend;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import top.skiba.task_manager_backend.dto.LoginRequestDTO;
import top.skiba.task_manager_backend.dto.RegisterRequestDTO;
import top.skiba.task_manager_backend.dto.TaskRequestDTO;
import top.skiba.task_manager_backend.model.TaskStatus;
import top.skiba.task_manager_backend.model.User;
import top.skiba.task_manager_backend.repository.TaskRepository;
import top.skiba.task_manager_backend.repository.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TaskRepository taskRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        User u = new User();
        u.setUsername("integrationuser");
        u.setEmail("integration@test.com");
        u.setPassword(passwordEncoder.encode("password"));
        testUser = userRepository.save(u);
    }

    // ──────────────────────────────────────────────────────────
    // Real auth flow — register → login → JWT cookie → tasks
    // ──────────────────────────────────────────────────────────

    @Test
    void register_thenLogin_thenAccessTasks_fullFlow() throws Exception {
        RegisterRequestDTO reg = new RegisterRequestDTO();
        reg.setUsername("flowuser");
        reg.setEmail("flow@test.com");
        reg.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isCreated())
            .andExpect(cookie().exists("jwt"));

        LoginRequestDTO login = new LoginRequestDTO();
        login.setUsername("flowuser");
        login.setPassword("password123");

        String jwtValue = extractJwtFromSetCookie(
            mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt"))
                .andReturn()
                .getResponse()
                .getHeader("Set-Cookie"));

        mockMvc.perform(get("/api/auth/me")
            .cookie(new jakarta.servlet.http.Cookie("jwt", jwtValue)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("flowuser"))
            .andExpect(jsonPath("$.csrfToken").exists());

        mockMvc.perform(get("/api/tasks")
            .cookie(new jakarta.servlet.http.Cookie("jwt", jwtValue)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        RegisterRequestDTO reg = new RegisterRequestDTO();
        reg.setUsername("authuser");
        reg.setEmail("auth@test.com");
        reg.setPassword("correctpassword");

        mockMvc.perform(post("/api/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isCreated());

        LoginRequestDTO login = new LoginRequestDTO();
        login.setUsername("authuser");
        login.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTasks_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        RegisterRequestDTO reg = new RegisterRequestDTO();
        reg.setUsername("integrationuser");
        reg.setEmail("new@test.com");
        reg.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isConflict());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequestDTO reg = new RegisterRequestDTO();
        reg.setUsername("newuser");
        reg.setEmail("integration@test.com");
        reg.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isConflict());
    }

    // ──────────────────────────────────────────────────────────
    // Create → Read
    // ──────────────────────────────────────────────────────────

    @Test
    void createTask_thenGetById_returnsCreatedTask() throws Exception {
        String body = mockMvc.perform(post("/api/tasks")
            .with(user(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new TaskRequestDTO("Integration Task", "Some description", TaskStatus.NEW))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.title").value("Integration Task"))
            .andExpect(jsonPath("$.status").value("NEW"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/tasks/{id}", id).with(user(testUser)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.title").value("Integration Task"))
            .andExpect(jsonPath("$.description").value("Some description"));
    }

    // ──────────────────────────────────────────────────────────
    // Create multiple → Get all (scoped to owner)
    // ──────────────────────────────────────────────────────────

    @Test
    void createTwoTasks_thenGetAll_returnsBoth() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(user(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new TaskRequestDTO("Task A", "Desc A", TaskStatus.NEW))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tasks")
            .with(user(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new TaskRequestDTO("Task B", "Desc B", TaskStatus.IN_PROGRESS))))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks").with(user(testUser)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAll_whenNoTasksExist_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/tasks").with(user(testUser)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void tasksAreIsolatedPerUser() throws Exception {
        User other = new User();
        other.setUsername("otheruser");
        other.setEmail("other@test.com");
        other.setPassword(passwordEncoder.encode("password"));
        other = userRepository.save(other);

        mockMvc.perform(post("/api/tasks")
            .with(user(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new TaskRequestDTO("Owner Task", null, TaskStatus.NEW))))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks").with(user(other)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    // ──────────────────────────────────────────────────────────
    // Create → Update → Read
    // ──────────────────────────────────────────────────────────

    @Test
    void createTask_thenUpdate_reflectsChanges() throws Exception {
        String body = mockMvc.perform(post("/api/tasks")
            .with(user(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new TaskRequestDTO("Original", "Original Desc", TaskStatus.NEW))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(put("/api/tasks/{id}", id)
            .with(user(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new TaskRequestDTO("Updated", "Updated Desc", TaskStatus.DONE))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated"))
            .andExpect(jsonPath("$.status").value("DONE"));

        mockMvc.perform(get("/api/tasks/{id}", id).with(user(testUser)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated"));
    }

    // ──────────────────────────────────────────────────────────
    // Create → Delete → Read
    // ──────────────────────────────────────────────────────────

    @Test
    void createTask_thenDelete_thenGetByIdReturns404() throws Exception {
        String body = mockMvc.perform(post("/api/tasks")
            .with(user(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new TaskRequestDTO("To Delete", null, TaskStatus.NEW))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(delete("/api/tasks/{id}", id)
            .with(user(testUser)).with(csrf()))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{id}", id).with(user(testUser)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Task with ID = " + id + " does not exist."));
    }

    // ──────────────────────────────────────────────────────────
    // 404 paths
    // ──────────────────────────────────────────────────────────

    @Test
    void getByNonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", 999L).with(user(testUser)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Task with ID = 999 does not exist."))
            .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void updateNonExistentTask_returns404() throws Exception {
        mockMvc.perform(put("/api/tasks/{id}", 999L)
            .with(user(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new TaskRequestDTO("Title", "Desc", TaskStatus.NEW))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Task with ID = 999 does not exist."));
    }

    @Test
    void deleteNonExistentTask_returns404() throws Exception {
        mockMvc.perform(delete("/api/tasks/{id}", 999L)
            .with(user(testUser)).with(csrf()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Task with ID = 999 does not exist."));
    }

    @Test
    void cannotAccessAnotherUsersTask_returns404() throws Exception {
        User other = new User();
        other.setUsername("otheruser2");
        other.setEmail("other2@test.com");
        other.setPassword(passwordEncoder.encode("password"));
        other = userRepository.save(other);

        String body = mockMvc.perform(post("/api/tasks")
            .with(user(other)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new TaskRequestDTO("Other's Task", null, TaskStatus.NEW))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        Long otherId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/tasks/{id}", otherId).with(user(testUser)))
            .andExpect(status().isNotFound());
    }

    // ──────────────────────────────────────────────────────────
    // Unauthenticated
    // ──────────────────────────────────────────────────────────

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/tasks"))
            .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────────────────────
    // Validation — @Size and @NotBlank
    // ──────────────────────────────────────────────────────────

    @Test
    void createTask_withBlankTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(user(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"   \",\"description\":\"desc\",\"status\":\"NEW\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[0]").value("Task title is required."));
    }

    @Test
    void createTask_withTitleOver200Chars_returns400() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(user(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new TaskRequestDTO("a".repeat(201), "desc", TaskStatus.NEW))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void createTask_withDescriptionOver5000Chars_returns400() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(user(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new TaskRequestDTO("Valid Title", "d".repeat(5001), TaskStatus.NEW))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void createTask_withInvalidStatusEnum_returns400() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .with(user(testUser)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Valid\",\"status\":\"INVALID_STATUS\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Wrong data format."));
    }

    // ──────────────────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────────────────

    private String extractJwtFromSetCookie(String setCookieHeader) {
        if (setCookieHeader == null) return null;
        for (String part : setCookieHeader.split(";")) {
            part = part.trim();
            if (part.startsWith("jwt=")) return part.substring(4);
        }
        return null;
    }
}