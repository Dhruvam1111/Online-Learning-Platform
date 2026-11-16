package com.olp.controller;

import com.olp.dto.NotificationResponse;
import com.olp.entity.User;
import com.olp.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Step 5 / FR-I70: Notification Centre REST API.
 * Provides endpoints for listing notifications, fetching unread counts for the badge,
 * and marking single or all notifications as read.
 */
@RestController
@RequestMapping({"/api/notifications", "/api/instructor/notifications"})
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * GET /api/notifications?unreadOnly=false
     * Lists notifications for the authenticated user (newest first).
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        List<NotificationResponse> notifications = notificationService.getUserNotifications(currentUser, unreadOnly);
        return ResponseEntity.ok(notifications);
    }

    /**
     * GET /api/notifications/unread-count
     * Returns the unread badge count for the authenticated user.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        long unreadCount = notificationService.getUnreadCount(currentUser);
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }

    /**
     * PATCH / PUT /api/notifications/{id}/read
     * Marks a specific notification as read.
     */
    @RequestMapping(value = "/{id}/read", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Integer id,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        NotificationResponse updated = notificationService.markAsRead(currentUser, id);
        return ResponseEntity.ok(updated);
    }

    /**
     * PATCH / PUT /api/notifications/read-all
     * Marks all notifications for the current user as read.
     */
    @RequestMapping(value = "/read-all", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> markAllAsRead(Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        int updatedCount = notificationService.markAllAsRead(currentUser);
        return ResponseEntity.ok(Map.of(
                "message", "All notifications marked as read",
                "updatedCount", updatedCount
        ));
    }
}

