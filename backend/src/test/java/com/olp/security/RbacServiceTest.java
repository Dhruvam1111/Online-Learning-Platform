package com.olp.security;

import com.olp.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RbacServiceTest {

    private final RbacService rbacService = new RbacService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void hasRole_handlesNullBlankUnauthenticatedAndRolePrefix() {
        assertFalse(rbacService.hasRole(null));
        assertFalse(rbacService.hasRole("   "));
        assertFalse(rbacService.hasRole("student"));

        TestingAuthenticationToken unauthenticated = new TestingAuthenticationToken("user", "pw", "ROLE_student");
        unauthenticated.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(unauthenticated);
        assertFalse(rbacService.hasRole("student"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user", "pw", List.of(new SimpleGrantedAuthority("ROLE_instructor"))
                )
        );
        assertTrue(rbacService.hasRole("instructor"));
        assertTrue(rbacService.hasRole("INSTRUCTOR"));
        assertTrue(rbacService.hasRole("ROLE_INSTRUCTOR"));
        assertTrue(rbacService.hasRole("ROLE_instructor"));
        assertFalse(rbacService.hasRole("admin"));
    }

    @Test
    void hasAnyRole_andShorthands_matchAssignedRole() {
        assertFalse(rbacService.hasAnyRole((String[]) null));
        assertFalse(rbacService.hasAnyRole());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin", "pw", List.of(new SimpleGrantedAuthority("ROLE_admin"))
                )
        );

        assertTrue(rbacService.isAdmin());
        assertFalse(rbacService.isInstructor());
        assertFalse(rbacService.isStudent());
        assertTrue(rbacService.hasAnyRole("student", "admin"));
        assertFalse(rbacService.hasAnyRole("student", "instructor"));
    }

    @Test
    void isAuthenticated_distinguishesAnonymousFromAuthenticatedPrincipal() {
        assertFalse(rbacService.isAuthenticated());

        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
                )
        );
        assertFalse(rbacService.isAuthenticated());

        User user = new User("Student One", "s1@olp.test", "hash", "student");
        user.setId(15);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority("ROLE_student"))
                )
        );

        assertTrue(rbacService.isAuthenticated());
        assertTrue(rbacService.isStudent());
        assertSame(user, rbacService.getCurrentUser());
        assertEquals(15, rbacService.getCurrentUserId());
        assertTrue(rbacService.isCurrentUser(15));
        assertFalse(rbacService.isCurrentUser(99));
        assertFalse(rbacService.isCurrentUser(null));
    }

    @Test
    void getCurrentUser_returnsNullWhenPrincipalIsNotUserEntity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("string-principal", null, List.of())
        );

        assertNull(rbacService.getCurrentUser());
        assertNull(rbacService.getCurrentUserId());
        assertFalse(rbacService.isCurrentUser(1));
    }
}
