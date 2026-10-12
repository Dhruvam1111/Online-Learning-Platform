package com.olp.controller;

import com.olp.dto.AdminCourseResponse;
import com.olp.dto.AdminUserResponse;
import com.olp.dto.ApiResponse;
import com.olp.dto.PageResponse;
import com.olp.dto.RejectCourseRequest;
import com.olp.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('admin')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Get all users with optional filtering and pagination.
     * FR-A02: Account and Access Management
     */
    @GetMapping("/users")
    public ResponseEntity<PageResponse<AdminUserResponse>> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PageResponse<AdminUserResponse> response = adminService.getUsers(
                role, status, search, page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    /**
     * List all pending instructor applications.
     * FR-A03: Verify and approve new instructor registrations
     */
    @GetMapping("/instructors/pending")
    public ResponseEntity<PageResponse<AdminUserResponse>> getPendingInstructors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<AdminUserResponse> response = adminService.getPendingInstructors(page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Approve a pending instructor account.
     * FR-A03, FR-I01
     */
    @PutMapping("/instructors/{id}/approve")
    public ResponseEntity<AdminUserResponse> approveInstructor(@PathVariable Integer id) {
        AdminUserResponse response = adminService.approveInstructor(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Reject an instructor application.
     * FR-A03
     */
    @PutMapping("/instructors/{id}/reject")
    public ResponseEntity<AdminUserResponse> rejectInstructor(@PathVariable Integer id) {
        AdminUserResponse response = adminService.rejectInstructor(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Suspend a user account (student or instructor).
     * FR-A02
     */
    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<AdminUserResponse> suspendUser(@PathVariable Integer id) {
        AdminUserResponse response = adminService.suspendUser(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Reactivate a suspended user account.
     * FR-A02
     */
    @PutMapping("/users/{id}/activate")
    public ResponseEntity<AdminUserResponse> activateUser(@PathVariable Integer id) {
        AdminUserResponse response = adminService.activateUser(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Permanently delete a user account.
     * FR-A02
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Integer id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(new ApiResponse(true, "User deleted successfully"));
    }

    // =========================================================================
    // COURSE GOVERNANCE & MODERATION (Features 15, 16, 17)
    // =========================================================================

    /**
     * View master list of all courses with status and search filters.
     * FR-A09: Master course directory
     */
    @GetMapping("/courses")
    public ResponseEntity<PageResponse<AdminCourseResponse>> getCourses(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PageResponse<AdminCourseResponse> response = adminService.getCourses(
                status, search, page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    /**
     * List all courses currently awaiting admin approval (status = 'pending_review').
     * FR-A06: Course review queue
     */
    @GetMapping("/courses/pending")
    public ResponseEntity<PageResponse<AdminCourseResponse>> getPendingCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<AdminCourseResponse> response = adminService.getPendingCourses(page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Approve a submitted course (publishes it to the catalog).
     * FR-A07: Approve course
     */
    @PutMapping("/courses/{id}/approve")
    public ResponseEntity<AdminCourseResponse> approveCourse(@PathVariable Integer id) {
        AdminCourseResponse response = adminService.approveCourse(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a submitted course with reasons provided to instructor.
     * FR-A07: Reject course with feedback
     */
    @PutMapping("/courses/{id}/reject")
    public ResponseEntity<AdminCourseResponse> rejectCourse(
            @PathVariable Integer id,
            @Valid @RequestBody RejectCourseRequest request) {

        AdminCourseResponse response = adminService.rejectCourse(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Emergency unpublish of a live course (reverts status to 'draft').
     * FR-A08: Unpublish live course
     */
    @PutMapping("/courses/{id}/unpublish")
    public ResponseEntity<AdminCourseResponse> unpublishCourse(@PathVariable Integer id) {
        AdminCourseResponse response = adminService.unpublishCourse(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Permanently remove a course from the platform.
     * FR-A08: Remove course
     */
    @DeleteMapping("/courses/{id}")
    public ResponseEntity<ApiResponse> deleteCourse(@PathVariable Integer id) {
        adminService.deleteCourse(id);
        return ResponseEntity.ok(new ApiResponse(true, "Course deleted successfully"));
    }
}
