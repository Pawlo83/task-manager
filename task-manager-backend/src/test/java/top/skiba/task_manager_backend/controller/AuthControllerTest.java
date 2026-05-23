package top.skiba.task_manager_backend.controller;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import top.skiba.task_manager_backend.dto.LoginRequestDTO;
import top.skiba.task_manager_backend.dto.RegisterRequestDTO;
import top.skiba.task_manager_backend.model.User;
import top.skiba.task_manager_backend.security.JwtUtil;
import top.skiba.task_manager_backend.service.UserService;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({top.skiba.task_manager_backend.config.SecurityConfig.class, top.skiba.task_manager_backend.config.PasswordConfig.class})
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserService userService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private AuthenticationManager authenticationManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded-password");
    }

    // ──────────────────────────────────────────────────────────
    // POST /api/auth/register
    // ──────────────────────────────────────────────────────────

    @Test
    void register_success_returns201WithBodyAndCookie() throws Exception {
        when(userService.register(any())).thenReturn(testUser);
        when(jwtUtil.generateToken("testuser")).thenReturn("mock-jwt-token");

        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        dto.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("jwt=mock-jwt-token")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        when(userService.register(any()))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken."));

        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setUsername("existing");
        dto.setEmail("new@example.com");
        dto.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isConflict());
    }

    @Test
    void register_blankUsername_returns400() throws Exception {
        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setUsername("");
        dto.setEmail("test@example.com");
        dto.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        dto.setPassword("seven77");

        mockMvc.perform(post("/api/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_exactMinimumPassword_returns201() throws Exception {
        when(userService.register(any())).thenReturn(testUser);
        when(jwtUtil.generateToken("testuser")).thenReturn("mock-jwt-token");

        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        dto.setPassword("eight888");

        mockMvc.perform(post("/api/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setUsername("testuser");
        dto.setEmail("not-an-email");
        dto.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest());
    }

    // ──────────────────────────────────────────────────────────
    // POST /api/auth/login
    // ──────────────────────────────────────────────────────────

    @Test
    void login_success_returns200WithBodyAndCookie() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(null);
        when(userService.loadUserByUsername("testuser")).thenReturn(testUser);
        when(jwtUtil.generateToken("testuser")).thenReturn("mock-jwt-token");

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("testuser");
        dto.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("jwt=mock-jwt-token")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("testuser");
        dto.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void login_blankUsername_returns400() throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("");
        dto.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest());
    }

    // ──────────────────────────────────────────────────────────
    // POST /api/auth/logout
    // ──────────────────────────────────────────────────────────

    @Test
    void logout_returns204AndClearsJwtCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
            .with(user(testUser)).with(csrf()))
            .andExpect(status().isNoContent())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("jwt=")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    // ──────────────────────────────────────────────────────────
    // GET /api/auth/me
    // ──────────────────────────────────────────────────────────

    @Test
    void me_authenticated_returns200WithUser() throws Exception {
        mockMvc.perform(get("/api/auth/me")
            .with(user(testUser)).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void me_authenticated_returnsCsrfTokenInBody() throws Exception {
        mockMvc.perform(get("/api/auth/me")
            .with(user(testUser)).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.csrfToken").exists());
    }

    @Test
    void me_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
}