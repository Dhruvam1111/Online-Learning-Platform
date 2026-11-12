package com.olp.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-key-that-is-at-least-32-bytes-long!!";
    private static final String OTHER_SECRET = "another-unit-test-secret-key-that-is-32-bytes-or-more";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 3_600_000L);
    }

    @Test
    void testConstructor_WeakSecretIsRejected() {
        assertThrows(WeakKeyException.class, () -> new JwtUtil("too-short", 1000L));
    }

    @Test
    void testGenerateToken_ProducesThreePartCompactJwt() {
        String token = jwtUtil.generateToken(7, "student");

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void testGenerateToken_RoundTripsUserIdAndRole() {
        String token = jwtUtil.generateToken(42, "instructor");

        assertEquals(42, jwtUtil.getUserIdFromToken(token));
        assertEquals("instructor", jwtUtil.getRoleFromToken(token));
    }

    @Test
    void testGenerateToken_DifferentUsersGetDifferentTokens() {
        assertNotEquals(jwtUtil.generateToken(1, "student"), jwtUtil.generateToken(2, "student"));
    }

    @Test
    void testValidateToken_ValidTokenReturnsTrue() {
        assertTrue(jwtUtil.validateToken(jwtUtil.generateToken(1, "admin")));
    }

    @Test
    void testValidateToken_ExpiredTokenReturnsFalse() {
        JwtUtil expiring = new JwtUtil(SECRET, -1000L);

        assertFalse(expiring.validateToken(expiring.generateToken(1, "student")));
    }

    @Test
    void testValidateToken_TokenSignedWithDifferentSecretReturnsFalse() {
        JwtUtil other = new JwtUtil(OTHER_SECRET, 3_600_000L);

        assertFalse(jwtUtil.validateToken(other.generateToken(1, "student")));
    }

    @Test
    void testValidateToken_TamperedSignatureReturnsFalse() {
        String token = jwtUtil.generateToken(1, "student");
        String[] parts = token.split("\\.");
        char first = parts[2].charAt(0);
        String tamperedSignature = (first == 'A' ? 'B' : 'A') + parts[2].substring(1);

        assertFalse(jwtUtil.validateToken(parts[0] + "." + parts[1] + "." + tamperedSignature));
    }

    @Test
    void testValidateToken_TamperedPayloadReturnsFalse() {
        String token = jwtUtil.generateToken(1, "student");
        String[] parts = token.split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"1\",\"role\":\"admin\"}".getBytes());

        assertFalse(jwtUtil.validateToken(parts[0] + "." + forgedPayload + "." + parts[2]));
    }

    @Test
    void testValidateToken_MalformedTokenReturnsFalse() {
        assertFalse(jwtUtil.validateToken("not-a-jwt"));
        assertFalse(jwtUtil.validateToken("a.b.c"));
    }

    @Test
    void testValidateToken_NullEmptyAndBlankReturnFalse() {
        assertFalse(jwtUtil.validateToken(null));
        assertFalse(jwtUtil.validateToken(""));
        assertFalse(jwtUtil.validateToken("   "));
    }

    @Test
    void testGetUserIdFromToken_InvalidTokenThrows() {
        assertThrows(JwtException.class, () -> jwtUtil.getUserIdFromToken("garbage.token.value"));
    }

    @Test
    void testGetUserIdFromToken_ExpiredTokenThrows() {
        JwtUtil expiring = new JwtUtil(SECRET, -1000L);
        String token = expiring.generateToken(5, "student");

        assertThrows(JwtException.class, () -> jwtUtil.getUserIdFromToken(token));
    }

    @Test
    void testGetRoleFromToken_InvalidTokenThrows() {
        assertThrows(JwtException.class, () -> jwtUtil.getRoleFromToken("garbage.token.value"));
    }

    @Test
    void testGetRoleFromToken_ForeignSecretTokenThrows() {
        String foreign = new JwtUtil(OTHER_SECRET, 3_600_000L).generateToken(1, "admin");

        assertThrows(JwtException.class, () -> jwtUtil.getRoleFromToken(foreign));
    }
}
