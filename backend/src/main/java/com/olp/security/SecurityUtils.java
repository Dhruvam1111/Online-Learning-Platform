package com.olp.security;

import com.olp.entity.Course;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Objects;

/**
 * Shared authentication resolution and instructor/course authorization helpers
 * eliminating duplicated boilerplate across service classes.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Resolves the authenticated {@link User} from an {@link Authentication} token,
     * accepting {@link User}, {@link String} email, or {@link UserDetails} principals.
     */
    public static User resolveUser(Authentication authentication, UserRepository userRepository) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        String email = null;
        if (principal instanceof String s) {
            email = s;
        } else if (principal instanceof UserDetails details) {
            email = details.getUsername();
        }
        if (email != null) {
            String lookupEmail = email;
            if (userRepository == null) {
                throw new InvalidCredentialsException("User repository unavailable");
            }
            return userRepository.findByEmail(lookupEmail)
                    .orElseThrow(() -> new InvalidCredentialsException("User not found: " + lookupEmail));
        }
        throw new InvalidCredentialsException("Invalid authentication principal");
    }

    /**
     * Resolves the authenticated {@link User} using {@link Authentication#getName()} fallback
     * when principal is not a {@link User} instance.
     */
    public static User resolveUserByName(Authentication authentication, UserRepository userRepository) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        if (userRepository == null) {
            throw new InvalidCredentialsException("User repository unavailable");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
    }

    /**
     * Validates that the caller is an authenticated instructor or admin, and that instructor accounts are active.
     */
    public static void requireActiveInstructorOrAdmin(User currentUser, String deniedMessage) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        String role = currentUser.getRole();
        boolean isInstructor = "instructor".equalsIgnoreCase(role);
        boolean isAdmin = "admin".equalsIgnoreCase(role);
        if (!isInstructor && !isAdmin) {
            throw new AccessDeniedException(deniedMessage);
        }
        if (!isAdmin && !"active".equalsIgnoreCase(currentUser.getAccountStatus())) {
            throw new AccessDeniedException("Access denied: Instructor account is awaiting admin approval");
        }
    }

    /**
     * Validates that the caller is an authenticated instructor or admin (without requiring active status).
     */
    public static void requireInstructorOrAdmin(User currentUser, String deniedMessage) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        String role = currentUser.getRole();
        if (!"instructor".equalsIgnoreCase(role) && !"admin".equalsIgnoreCase(role)) {
            throw new AccessDeniedException(deniedMessage);
        }
    }

    /**
     * Checks whether the given user is an admin, the primary owner of the course,
     * or an associated co-instructor on the course.
     */
    public static boolean isCourseOwnerOrCoInstructorOrAdmin(
            Course course,
            User currentUser,
            CourseInstructorRepository courseInstructorRepository) {
        if (course == null || currentUser == null) {
            return false;
        }
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            return true;
        }
        if (currentUser.getId() == null) {
            return false;
        }
        boolean isOwner = course.getInstructor() != null
                && Objects.equals(course.getInstructor().getId(), currentUser.getId());
        if (isOwner) {
            return true;
        }
        return courseInstructorRepository != null
                && course.getId() != null
                && courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), currentUser.getId());
    }
}
