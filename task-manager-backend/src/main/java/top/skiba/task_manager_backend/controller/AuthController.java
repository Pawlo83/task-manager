package top.skiba.task_manager_backend.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import top.skiba.task_manager_backend.dto.AuthResponseDTO;
import top.skiba.task_manager_backend.dto.LoginRequestDTO;
import top.skiba.task_manager_backend.dto.RegisterRequestDTO;
import top.skiba.task_manager_backend.model.User;
import top.skiba.task_manager_backend.security.JwtUtil;
import top.skiba.task_manager_backend.service.UserService;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;

    @Value("${app.secure-cookies:false}")
    private boolean secureCookies;

    @Value("${app.cross-origin:false}")
    private boolean crossOrigin;

    public AuthController(UserService userService, JwtUtil jwtUtil, AuthenticationManager authManager) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.authManager = authManager;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping(CsrfToken csrfToken) {
        return ResponseEntity.ok(csrfToken.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO dto,
            HttpServletResponse response) {

        User user = userService.register(dto);
        setJwtCookie(response, jwtUtil.generateToken(user.getUsername()));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new AuthResponseDTO(user.getUsername(), user.getEmail(), ""));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO dto,
            HttpServletResponse response) {

        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = (User) userService.loadUserByUsername(dto.getUsername());
        setJwtCookie(response, jwtUtil.generateToken(user.getUsername()));
        return ResponseEntity.ok(new AuthResponseDTO(user.getUsername(), user.getEmail(), ""));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponseDTO> me(
            @AuthenticationPrincipal User user,
            CsrfToken csrfToken) {
        return ResponseEntity.ok(new AuthResponseDTO(
            user.getUsername(),
            user.getEmail(),
            csrfToken.getToken()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = buildJwtCookie("", 0);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildJwtCookie(token, 86400).toString());
    }

    private ResponseCookie buildJwtCookie(String value, long maxAgeSeconds) {
        boolean needSecure = crossOrigin || secureCookies;
        return ResponseCookie.from("jwt", value)
            .httpOnly(true)
            .path("/")
            .maxAge(maxAgeSeconds)
            .sameSite(crossOrigin ? "None" : "Lax")
            .secure(needSecure)
            .partitioned(crossOrigin)
            .build();
    }
}