package com.olp.controller;

import com.olp.dto.AdminUserResponse;
import com.olp.dto.ApiResponse;
import com.olp.dto.PageResponse;
import com.olp.service.AdminService;
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
}
