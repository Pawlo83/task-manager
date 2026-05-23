package top.skiba.task_manager_backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "TaskManagerTestSecretKey2025Dev!!");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
    }

    @Test
    void generateToken_returnsNonNullString() {
        String token = jwtUtil.generateToken("testuser");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generateToken_producesThreePartJwt() {
        String token = jwtUtil.generateToken("testuser");
        assertEquals(3, token.split("\\.").length, "JWT must have header.payload.signature");
    }

    @Test
    void extractUsername_returnsCorrectSubject() {
        String token = jwtUtil.generateToken("alice");
        assertEquals("alice", jwtUtil.extractUsername(token));
    }

    @Test
    void isValid_withFreshToken_returnsTrue() {
        String token = jwtUtil.generateToken("testuser");
        assertTrue(jwtUtil.isValid(token));
    }

    @Test
    void isValid_withTamperedSignature_returnsFalse() {
        String token = jwtUtil.generateToken("testuser");
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";
        assertFalse(jwtUtil.isValid(tampered));
    }

    @Test
    void isValid_withExpiredToken_returnsFalse() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L); // already expired
        String expiredToken = jwtUtil.generateToken("testuser");
        assertFalse(jwtUtil.isValid(expiredToken));
    }

    @Test
    void isValid_withGarbageString_returnsFalse() {
        assertFalse(jwtUtil.isValid("not.a.jwt"));
    }

    @Test
    void differentUsersProduceDifferentTokens() {
        String token1 = jwtUtil.generateToken("alice");
        String token2 = jwtUtil.generateToken("bob");
        assertNotEquals(token1, token2);
    }
}
