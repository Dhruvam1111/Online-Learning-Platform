package com.olp.controller;

import com.olp.dto.NotificationResponse;
import com.olp.entity.User;
import com.olp.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Step 5 / FR-I70: Notification Centre REST API.
 */
@RestController
@RequestMapping({"/api/notifications", "/api/instructor/notifications"})
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        List<NotificationResponse> notifications = notificationService.getUserNotifications(currentUser, unreadOnly);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        long unreadCount = notificationService.getUnreadCount(currentUser);
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }

    @RequestMapping(value = "/{id}/read", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Integer id,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        NotificationResponse updated = notificationService.markAsRead(currentUser, id);
        return ResponseEntity.ok(updated);
    }

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

