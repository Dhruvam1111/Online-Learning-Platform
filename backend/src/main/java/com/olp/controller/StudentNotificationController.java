package com.olp.controller;

import com.olp.dto.ApiResponse;
import com.olp.dto.StudentNotificationResponse;
import com.olp.entity.User;
import com.olp.service.StudentNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for student notifications (FR-S65).
 */
@RestController
@RequestMapping("/api/student/notifications")
@PreAuthorize("hasRole('student')")
public class StudentNotificationController {

    private final StudentNotificationService notificationService;

    public StudentNotificationController(StudentNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return user;
    }

    @GetMapping
    public ResponseEntity<List<StudentNotificationResponse>> getNotifications(
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(notificationService.getStudentNotifications(user, unreadOnly));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication authentication) {
        User user = resolveUser(authentication);
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<StudentNotificationResponse> markAsRead(
            @PathVariable Integer id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(notificationService.markAsRead(user, id));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse> markAllAsRead(Authentication authentication) {
        User user = resolveUser(authentication);
        int updated = notificationService.markAllAsRead(user);
        return ResponseEntity.ok(new ApiResponse(true, "Marked " + updated + " notifications as read"));
    }
}
