package com.onlinelearning;

import com.onlinelearning.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, expiration);
    }

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtTokenProvider.generateToken(1L, "student@test.com", "Test Student", "student");
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));

        assertEquals("student@test.com", jwtTokenProvider.getEmailFromToken(token));
        assertEquals(1L, jwtTokenProvider.getUserIdFromToken(token));
        assertEquals("student", jwtTokenProvider.getRoleFromToken(token));
    }

    @Test
    void testValidateInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.jwt.token"));
    }
}
