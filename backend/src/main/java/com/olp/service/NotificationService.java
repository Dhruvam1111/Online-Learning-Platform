package com.olp.service;

import com.olp.dto.NotificationPayload;
import com.olp.dto.NotificationResponse;
import com.olp.entity.Notification;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.NotificationRepository;
import com.olp.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Step 5 / FR-I70: Reusable Notification Centre service supporting both in-app and email channels.
 * Other modules (Persons 1, 2, and 3) call notify(user, type, payload) to dispatch notifications.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Resolves the authenticated User entity from Spring Security Authentication.
     */
    public User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
    }

    /**
     * Core reusable notify(user, type, payload) method with in-app persistence and email channel.
     */
    @Transactional
    public NotificationResponse notify(User recipient, String type, NotificationPayload payload) {
        if (recipient == null || recipient.getId() == null) {
            throw new IllegalArgumentException("Notification recipient cannot be null");
        }
        if (payload == null || payload.getMessage() == null || payload.getMessage().isBlank()) {
            throw new IllegalArgumentException("Notification message cannot be blank");
        }

        String normalizedType = (type != null && !type.isBlank()) ? type.trim().toUpperCase() : "SYSTEM";
        String title = (payload.getTitle() != null && !payload.getTitle().isBlank())
                ? payload.getTitle().trim()
                : formatDefaultTitle(normalizedType);
        String message = payload.getMessage().trim();
        String link = (payload.getLink() != null && !payload.getLink().isBlank())
                ? payload.getLink().trim()
                : null;

        Notification notification = new Notification(recipient, normalizedType, title, message, link);
        Notification saved = notificationRepository.save(notification);

        if (payload.isSendEmail() && emailService != null && recipient.getEmail() != null) {
            emailService.sendNotificationEmail(
                    recipient.getEmail(),
                    recipient.getName(),
                    normalizedType,
                    title,
                    message,
                    link
            );
        }

        return NotificationResponse.fromEntity(saved);
    }

    /**
     * Convenience overload accepting a Map payload (keys: "title", "message", "link", "sendEmail").
     */
    @Transactional
    public NotificationResponse notify(User recipient, String type, Map<String, Object> payloadMap) {
        if (payloadMap == null) {
            throw new IllegalArgumentException("Notification payload cannot be null");
        }
        String title = payloadMap.get("title") != null ? String.valueOf(payloadMap.get("title")) : null;
        String message = payloadMap.get("message") != null ? String.valueOf(payloadMap.get("message")) : null;
        String link = payloadMap.get("link") != null ? String.valueOf(payloadMap.get("link")) : null;
        boolean sendEmail = payloadMap.get("sendEmail") == null || Boolean.parseBoolean(String.valueOf(payloadMap.get("sendEmail")));

        NotificationPayload payload = new NotificationPayload(title, message, link, sendEmail);
        payload.setMetadata(payloadMap);
        return notify(recipient, type, payload);
    }

    /**
     * Convenience overload for quick notifications.
     */
    @Transactional
    public NotificationResponse notify(User recipient, String type, String message, String link) {
        return notify(recipient, type, NotificationPayload.of(null, message, link));
    }

    /**
     * Batch helper to notify multiple users (e.g., all enrolled students in a course).
     */
    @Transactional
    public List<NotificationResponse> notifyUsers(Iterable<User> recipients, String type, NotificationPayload payload) {
        List<NotificationResponse> results = new ArrayList<>();
        if (recipients == null) {
            return results;
        }
        for (User recipient : recipients) {
            if (recipient != null && recipient.getId() != null) {
                results.add(notify(recipient, type, payload));
            }
        }
        return results;
    }

    /**
     * List notifications for the authenticated user (all or unread only).
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(User currentUser, boolean unreadOnly) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findUnreadByUserIdOrderByCreatedAtDesc(currentUser.getId())
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());

        return notifications.stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    /**
     * Get the count of unread notifications for the badge.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        return notificationRepository.countUnreadByUserId(currentUser.getId());
    }

    /**
     * Mark a single notification owned by the user as read.
     */
    @Transactional
    public NotificationResponse markAsRead(User currentUser, Integer notificationId) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (notificationId == null) {
            throw new IllegalArgumentException("Notification ID cannot be null");
        }

        Notification notification = notificationRepository.findByIdAndUserId(notificationId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.fromEntity(notification);
    }

    /**
     * Mark all unread notifications for the user as read.
     */
    @Transactional
    public int markAllAsRead(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        return notificationRepository.markAllAsReadForUser(currentUser.getId(), LocalDateTime.now());
    }

    private String formatDefaultTitle(String normalizedType) {
        return switch (normalizedType) {
            case "ANNOUNCEMENT" -> "New Course Announcement";
            case "DISCUSSION_REPLY" -> "New Discussion Reply";
            case "MODERATION" -> "Discussion Moderation Notice";
            case "DEADLINE", "ASSIGNMENT_DUE" -> "Upcoming Deadline Reminder";
            case "CONTENT_RELEASE" -> "New Course Content Released";
            case "EXAM_SCHEDULED" -> "New Exam / Quiz Scheduled";
            default -> "Platform Notification";
        };
    }
}

