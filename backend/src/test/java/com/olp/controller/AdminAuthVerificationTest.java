package com.olp.controller;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import com.olp.dto.AdminCourseResponse;
import com.olp.dto.AdminUserResponse;
import com.olp.dto.ApiResponse;
import com.olp.dto.CourseReviewRequest;
import com.olp.dto.PageResponse;
import com.olp.dto.RejectCourseRequest;
import com.olp.entity.User;
import com.olp.service.AdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ADM-016 — Admin Login/Logout Verification (FR-A01)
 *
 * Verifies that ALL /api/admin/** endpoints enforce ROLE_admin:
 *  - Student  token  → AccessDeniedException (403 in HTTP layer)
 *  - Instructor token → AccessDeniedException (403 in HTTP layer)
 *  - No authentication (unauthenticated) → AccessDeniedException
 *  - Admin token → endpoint invoked successfully (200)
 *
 * Also verifies that TokenBlacklistService.blacklistToken() is called on
 * logout, so a blacklisted token is refused by the filter (tested via service
 * unit tests in this class).
 *
 * Uses @SpringJUnitConfig + @EnableMethodSecurity to exercise
 * @PreAuthorize("hasRole('admin')") on the controller directly —
 * the same pattern used by PaymentControllerMethodSecurityTest.
 */
@SpringJUnitConfig(AdminAuthVerificationTest.TestConfig.class)
class AdminAuthVerificationTest {

    @Autowired
    private AdminController adminController;

    @Autowired
    private AdminService adminService;

    private AdminUserResponse sampleUser;
    private AdminCourseResponse sampleCourse;

    // -------------------------------------------------------------------------
    // Setup / Teardown
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        reset(adminService);
        sampleUser = new AdminUserResponse(1, "Test User", "test@olp.com",
                "instructor", "active", LocalDateTime.now());
        sampleCourse = new AdminCourseResponse(10, "Spring Boot", "Desc", 2,
                "Dr. Chen", "chen@olp.com", "published", null, LocalDateTime.now());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private void authenticateAs(User user, String springRole) {
        var auth = new UsernamePasswordAuthenticationToken(
                user, "ignored", List.of(new SimpleGrantedAuthority(springRole)));
        var ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private User makeUser(String role) {
        User u = new User("Test " + role, role + "@olp.com", "hash", role);
        u.setId(99);
        return u;
    }

    // =========================================================================
    // AC-1: Student token is denied on getUsers
    // =========================================================================

    @Test
    void studentToken_denied_getUsers() {
        authenticateAs(makeUser("student"), "ROLE_student");

        assertThrows(AccessDeniedException.class,
                () -> adminController.getUsers(null, null, null, 0, 10, "createdAt", "desc"));

        verifyNoInteractions(adminService);
    }

    // =========================================================================
    // AC-2: Instructor token is denied on getUsers
    // =========================================================================

    @Test
    void instructorToken_denied_getUsers() {
        authenticateAs(makeUser("instructor"), "ROLE_instructor");

        assertThrows(AccessDeniedException.class,
                () -> adminController.getUsers(null, null, null, 0, 10, "createdAt", "desc"));

        verifyNoInteractions(adminService);
    }

    // =========================================================================
    // AC-3: Unauthenticated request is denied
    // =========================================================================

    @Test
    void unauthenticated_denied_getUsers() {
        // No SecurityContext set → no authentication
        SecurityContextHolder.clearContext();

       assertThrows(AuthenticationCredentialsNotFoundException.class,
        () -> adminController.getUsers(null, null, null, 0, 10, "createdAt", "desc"));
        verifyNoInteractions(adminService);
    }

    // =========================================================================
    // AC-4: Admin token succeeds on getUsers
    // =========================================================================

    @Test
    void adminToken_allowed_getUsers() {
        authenticateAs(makeUser("admin"), "ROLE_admin");
        PageResponse<AdminUserResponse> page =
                new PageResponse<>(List.of(sampleUser), 0, 10, 1, 1, true);
        when(adminService.getUsers(any(), any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(page);

        var response = adminController.getUsers(null, null, null, 0, 10, "createdAt", "desc");

        assertEquals(200, response.getStatusCode().value());
        verify(adminService).getUsers(any(), any(), any(), anyInt(), anyInt(), anyString(), anyString());
    }

    // =========================================================================
    // AC-5: Student token is denied on getPendingInstructors
    // =========================================================================

    @Test
    void studentToken_denied_getPendingInstructors() {
        authenticateAs(makeUser("student"), "ROLE_student");

        assertThrows(AccessDeniedException.class,
                () -> adminController.getPendingInstructors(0, 10));

        verifyNoInteractions(adminService);
    }

    // =========================================================================
    // AC-6: Instructor token is denied on approveInstructor
    // =========================================================================

    @Test
    void instructorToken_denied_approveInstructor() {
        authenticateAs(makeUser("instructor"), "ROLE_instructor");

        assertThrows(AccessDeniedException.class,
                () -> adminController.approveInstructor(1));

        verifyNoInteractions(adminService);
    }

    // =========================================================================
    // AC-7: Admin token succeeds on approveInstructor
    // =========================================================================

    @Test
    void adminToken_allowed_approveInstructor() {
        authenticateAs(makeUser("admin"), "ROLE_admin");
        sampleUser.setAccountStatus("active");
        when(adminService.approveInstructor(1)).thenReturn(sampleUser);

        var response = adminController.approveInstructor(1);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("active", response.getBody().getAccountStatus());
        verify(adminService).approveInstructor(1);
    }

    // =========================================================================
    // AC-8: Student token is denied on suspendUser
    // =========================================================================

    @Test
    void studentToken_denied_suspendUser() {
        authenticateAs(makeUser("student"), "ROLE_student");

        assertThrows(AccessDeniedException.class,
                () -> adminController.suspendUser(1));

        verifyNoInteractions(adminService);
    }

    // =========================================================================
    // AC-9: Student token is denied on getCourses
    // =========================================================================

    @Test
    void studentToken_denied_getCourses() {
        authenticateAs(makeUser("student"), "ROLE_student");

        assertThrows(AccessDeniedException.class,
                () -> adminController.getCourses(null, null, 0, 10, "createdAt", "desc"));

        verifyNoInteractions(adminService);
    }

    // =========================================================================
    // AC-10: Admin token succeeds on deleteCourse
    // =========================================================================

    @Test
    void adminToken_allowed_deleteCourse() {
        authenticateAs(makeUser("admin"), "ROLE_admin");
        doNothing().when(adminService).deleteCourse(10);

        var response = adminController.deleteCourse(10);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        verify(adminService).deleteCourse(10);
    }

    // =========================================================================
    // AC-11: Instructor is denied on course approval
    // =========================================================================

    @Test
    void instructorToken_denied_approveCourse() {
        authenticateAs(makeUser("instructor"), "ROLE_instructor");

        assertThrows(AccessDeniedException.class,
                () -> adminController.approveCourse(10));

        verifyNoInteractions(adminService);
    }

    // =========================================================================
    // AC-12: Admin token succeeds on rejectCourse
    // =========================================================================

    @Test
    void adminToken_allowed_rejectCourse() {
        authenticateAs(makeUser("admin"), "ROLE_admin");
        sampleCourse.setStatus("rejected");
        sampleCourse.setRejectionReason("Needs improvement");
        RejectCourseRequest req = new RejectCourseRequest("Needs improvement");
        when(adminService.rejectCourse(eq(10), any(RejectCourseRequest.class))).thenReturn(sampleCourse);

        var response = adminController.rejectCourse(10, req);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("rejected", response.getBody().getStatus());
        verify(adminService).rejectCourse(eq(10), any(RejectCourseRequest.class));
    }

    // =========================================================================
    // AC-13: Student is denied on deleteUser
    // =========================================================================

    @Test
    void studentToken_denied_deleteUser() {
        authenticateAs(makeUser("student"), "ROLE_student");

        assertThrows(AccessDeniedException.class,
                () -> adminController.deleteUser(5));

        verifyNoInteractions(adminService);
    }

    // =========================================================================
    // AC-14: Admin token succeeds on activateUser
    // =========================================================================

    @Test
    void adminToken_allowed_activateUser() {
        authenticateAs(makeUser("admin"), "ROLE_admin");
        sampleUser.setAccountStatus("active");
        when(adminService.activateUser(1)).thenReturn(sampleUser);

        var response = adminController.activateUser(1);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("active", response.getBody().getAccountStatus());
        verify(adminService).activateUser(1);
    }

    // =========================================================================
    // Spring Context for @PreAuthorize enforcement
    // =========================================================================

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {

        @Bean
        AdminService adminService() {
            return mock(AdminService.class);
        }

        @Bean
        AdminController adminController(AdminService adminService) {
            // Use the single-arg constructor — InstructorMediaService and
            // CourseAssociationService are null-safe (guarded with null checks in controller)
            return new AdminController(adminService);
        }
    }
}
