package com.olp.dto;

import com.olp.entity.Notification;
import java.time.LocalDateTime;

/**
 * Step 5 / FR-I70: Response DTO representing an in-app notification.
 */
public class NotificationResponse {

    private Integer id;
    private Integer userId;
    private String type;
    private String title;
    private String message;
    private String link;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public NotificationResponse() {}

    public NotificationResponse(
            Integer id,
            Integer userId,
            String type,
            String title,
            String message,
            String link,
            boolean read,
            LocalDateTime readAt,
            LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.link = link;
        this.read = read;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    public static NotificationResponse fromEntity(Notification n) {
        if (n == null) {
            return null;
        }
        return new NotificationResponse(
                n.getId(),
                n.getUser() != null ? n.getUser().getId() : null,
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getLink(),
                n.isRead(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

