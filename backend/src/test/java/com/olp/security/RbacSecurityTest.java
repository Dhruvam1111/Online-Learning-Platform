package com.olp.security;

import com.olp.entity.User;
import com.olp.service.TokenBlacklistService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RbacSecurityTest {

    private RbacService rbacService;
    private CustomAccessDeniedHandler accessDeniedHandler;
    private CustomAuthenticationEntryPoint authenticationEntryPoint;
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        rbacService = new RbacService();
        accessDeniedHandler = new CustomAccessDeniedHandler();
        authenticationEntryPoint = new CustomAuthenticationEntryPoint();
        tokenBlacklistService = new TokenBlacklistService();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // 1. RBAC SERVICE TESTS
    // =========================================================================

    @Test
    void testRbacService_Unauthenticated() {
        assertFalse(rbacService.isAuthenticated());
        assertFalse(rbacService.hasRole("instructor"));
        assertFalse(rbacService.isInstructor());
        assertFalse(rbacService.isStudent());
        assertFalse(rbacService.isAdmin());
        assertNull(rbacService.getCurrentUser());
        assertNull(rbacService.getCurrentUserId());
        assertFalse(rbacService.isCurrentUser(10));
    }

    @Test
    void testRbacService_InstructorUser() {
        User user = new User("Instructor Bob", "bob@olp.com", "hash", "instructor", "active");
        user.setId(42);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertTrue(rbacService.isAuthenticated());
        assertTrue(rbacService.hasRole("instructor"));
        assertTrue(rbacService.hasRole("INSTRUCTOR"));
        assertTrue(rbacService.hasRole("ROLE_INSTRUCTOR"));
        assertTrue(rbacService.isInstructor());
        assertFalse(rbacService.isStudent());
        assertFalse(rbacService.isAdmin());
        assertTrue(rbacService.hasAnyRole("student", "instructor"));
        assertFalse(rbacService.hasAnyRole("admin", "support"));

        assertEquals(user, rbacService.getCurrentUser());
        assertEquals(42, rbacService.getCurrentUserId());
        assertTrue(rbacService.isCurrentUser(42));
        assertFalse(rbacService.isCurrentUser(99));
    }

    @Test
    void testRbacService_StudentUser() {
        User user = new User("Student Alice", "alice@olp.com", "hash", "student", "active");
        user.setId(7);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertTrue(rbacService.isStudent());
        assertFalse(rbacService.isInstructor());
        assertFalse(rbacService.isAdmin());
        assertTrue(rbacService.isCurrentUser(7));
    }

    @Test
    void testRbacService_AdminUser() {
        User user = new User("Admin Root", "admin@olp.com", "hash", "admin", "active");
        user.setId(1);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertTrue(rbacService.isAdmin());
        assertFalse(rbacService.isInstructor());
        assertFalse(rbacService.isStudent());
        assertTrue(rbacService.hasAnyRole("instructor", "admin"));
    }

    // =========================================================================
    // 2. EXCEPTION HANDLER TESTS (401 & 403)
    // =========================================================================

    @Test
    void testCustomAccessDeniedHandler_Returns403Json() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/instructor/courses");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AccessDeniedException exception = new AccessDeniedException("Access is denied");
        accessDeniedHandler.handle(request, response, exception);

        assertEquals(403, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        String body = response.getContentAsString();
        assertTrue(body.contains("\"status\":403"));
        assertTrue(body.contains("Access is denied"));
    }

    @Test
    void testCustomAuthenticationEntryPoint_Returns401Json() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthenticationException exception = new AuthenticationException("Full authentication is required") {};
        authenticationEntryPoint.commence(request, response, exception);

        assertEquals(401, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        String body = response.getContentAsString();
        assertTrue(body.contains("\"status\":401"));
        assertTrue(body.contains("Full authentication is required"));
    }

    // =========================================================================
    // 3. TOKEN BLACKLIST SERVICE TESTS
    // =========================================================================

    @Test
    void testTokenBlacklistService_BlacklistAndRevocation() {
        String token = "sample.jwt.token";
        assertFalse(tokenBlacklistService.isTokenBlacklisted(token));

        tokenBlacklistService.blacklistToken(token);
        assertTrue(tokenBlacklistService.isTokenBlacklisted(token));
        assertFalse(tokenBlacklistService.isTokenBlacklisted(null));
        assertFalse(tokenBlacklistService.isTokenBlacklisted("   "));

        // User session invalidation
        int userId = 101;
        long beforeRevocation = System.currentTimeMillis() - 1000;
        assertFalse(tokenBlacklistService.isUserTokenRevoked(userId, new Date(beforeRevocation)));

        tokenBlacklistService.revokeAllUserTokens(userId);

        // A token issued before revocation is revoked
        assertTrue(tokenBlacklistService.isUserTokenRevoked(userId, new Date(beforeRevocation)));
        // A token issued in the future (after revocation) is not revoked
        assertTrue(tokenBlacklistService.isUserTokenRevoked(userId, new Date(System.currentTimeMillis() - 100)));
        assertFalse(tokenBlacklistService.isUserTokenRevoked(userId, new Date(System.currentTimeMillis() + 10000)));
    }
}

