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

    @Transactional
    public NotificationResponse notify(User recipient, String type, String message, String link) {
        return notify(recipient, type, NotificationPayload.of(null, message, link));
    }

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

    @Transactional(readOnly = true)
    public long getUnreadCount(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        return notificationRepository.countUnreadByUserId(currentUser.getId());
    }

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

    @Transactional
    public int markAllAsRead(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        return notificationRepository.markAllAsReadForUser(currentUser.getId(), LocalDateTime.now());
    }

    private String formatDefaultTitle(String normalizedType) {
        return switch (normalizedType) {
            case "ANNOUNCEMENT", "COURSE_ANNOUNCEMENT" -> "New Course Announcement";
            case "DISCUSSION_REPLY", "QNA_REPLY" -> "New Discussion Reply";
            case "DISCUSSION_QUESTION" -> "New Student Question";
            case "DIRECT_MESSAGE", "MESSAGE" -> "New Direct Message";
            case "REVIEW_RESPONSE" -> "Instructor Replied to Your Review";
            case "PAYOUT", "FINANCE" -> "Payout & Royalty Update";
            default -> "Platform Notification";
        };
    }
}

