package com.olp.dto;

import java.util.Map;

/**
 * Step 5 / FR-I70: Reusable payload for NotificationService.notify(user, type, payload).
 */
public class NotificationPayload {

    private String title;
    private String message;
    private String link;
    private boolean sendEmail = true;
    private Map<String, Object> metadata;

    public NotificationPayload() {}

    public NotificationPayload(String title, String message, String link, boolean sendEmail) {
        this.title = title;
        this.message = message;
        this.link = link;
        this.sendEmail = sendEmail;
    }

    public static NotificationPayload of(String message) {
        return new NotificationPayload(null, message, null, false);
    }

    public static NotificationPayload of(String message, String link) {
        return new NotificationPayload(null, message, link, false);
    }

    public static NotificationPayload of(String title, String message, String link) {
        return new NotificationPayload(title, message, link, true);
    }

    public static NotificationPayload inAppOnly(String title, String message, String link) {
        return new NotificationPayload(title, message, link, false);
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

    public boolean isSendEmail() {
        return sendEmail;
    }

    public void setSendEmail(boolean sendEmail) {
        this.sendEmail = sendEmail;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

