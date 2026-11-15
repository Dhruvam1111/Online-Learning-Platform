package com.olp.dto;

import com.olp.entity.GradingAlertType;
import com.olp.entity.PendingGradingAlert;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for an alert/notification delivered to an instructor (FR-I33, Step 50).
 */
public class PendingGradingAlertResponse {

    private Integer id;
    private Integer instructorId;
    private Integer courseId;
    private String courseTitle;
    private GradingAlertType alertType;
    private Integer pendingCount;
    private String title;
    private String message;
    private Boolean isRead;
    private LocalDateTime sentAt;
    private LocalDate digestDate;

    public PendingGradingAlertResponse() {
    }

    public static PendingGradingAlertResponse fromEntity(PendingGradingAlert alert) {
        if (alert == null) {
            return null;
        }
        PendingGradingAlertResponse dto = new PendingGradingAlertResponse();
        dto.setId(alert.getId());
        if (alert.getInstructor() != null) {
            dto.setInstructorId(alert.getInstructor().getId());
        }
        if (alert.getCourse() != null) {
            dto.setCourseId(alert.getCourse().getId());
            dto.setCourseTitle(alert.getCourse().getTitle());
        }
        dto.setAlertType(alert.getAlertType());
        dto.setPendingCount(alert.getPendingCount());
        dto.setTitle(alert.getTitle());
        dto.setMessage(alert.getMessage());
        dto.setIsRead(alert.getIsRead());
        dto.setSentAt(alert.getSentAt());
        dto.setDigestDate(alert.getDigestDate());
        return dto;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Integer instructorId) {
        this.instructorId = instructorId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public GradingAlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(GradingAlertType alertType) {
        this.alertType = alertType;
    }

    public Integer getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Integer pendingCount) {
        this.pendingCount = pendingCount;
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

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDate getDigestDate() {
        return digestDate;
    }

    public void setDigestDate(LocalDate digestDate) {
        this.digestDate = digestDate;
    }
}
