package top.skiba.task_manager_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import top.skiba.task_manager_backend.dto.RegisterRequestDTO;
import top.skiba.task_manager_backend.model.User;
import top.skiba.task_manager_backend.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    private RegisterRequestDTO validDto;

    @BeforeEach
    void setUp() {
        validDto = new RegisterRequestDTO();
        validDto.setUsername("newuser");
        validDto.setEmail("new@example.com");
        validDto.setPassword("password123");
    }

    // ──────────────────────────────────────────────────────────
    // register
    // ──────────────────────────────────────────────────────────

    @Test
    void register_success_savesAndReturnsUser() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");

        User saved = new User();
        saved.setUsername("newuser");
        saved.setEmail("new@example.com");
        saved.setPassword("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.register(validDto);

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("encoded", result.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateUsername_throwsConflict() {
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> userService.register(validDto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Username"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> userService.register(validDto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Email"));
        verify(userRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────
    // loadUserByUsername
    // ──────────────────────────────────────────────────────────

    @Test
    void loadUserByUsername_found_returnsUser() {
        User user = new User();
        user.setUsername("existinguser");
        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(user));

        var result = userService.loadUserByUsername("existinguser");

        assertNotNull(result);
        assertEquals("existinguser", result.getUsername());
    }

    @Test
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
            () -> userService.loadUserByUsername("ghost"));
    }
}
