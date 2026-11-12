package com.olp.security;

import com.olp.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Reusable RBAC helper bean available in SpEL expressions (e.g., @PreAuthorize("@rbac.isInstructor()"))
 * and for programmatic authorization checks across the platform.
 */
@Component("rbac")
public class RbacService {

    /**
     * Check if the currently authenticated user has the specified role.
     */
    public boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        String targetRole = role.startsWith("ROLE_") ? role.toUpperCase() : "ROLE_" + role.toUpperCase();
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities == null) {
            return false;
        }

        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(targetRole));
    }

    /**
     * Check if the currently authenticated user has any of the specified roles.
     */
    public boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Shorthand check for instructor role.
     */
    public boolean isInstructor() {
        return hasRole("instructor");
    }

    /**
     * Shorthand check for student role.
     */
    public boolean isStudent() {
        return hasRole("student");
    }

    /**
     * Shorthand check for admin role.
     */
    public boolean isAdmin() {
        return hasRole("admin");
    }

    /**
     * Check if a request has an authenticated non-anonymous user.
     */
    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    /**
     * Get the authenticated User entity from the security context principal.
     */
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    /**
     * Get the authenticated user's ID.
     */
    public Integer getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * Check if the current authenticated user owns the given resource by matching user ID.
     */
    public boolean isCurrentUser(Integer userId) {
        Integer currentId = getCurrentUserId();
        return currentId != null && currentId.equals(userId);
    }
}

