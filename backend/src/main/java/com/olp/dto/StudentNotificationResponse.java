package com.olp.dto;

import com.olp.entity.StudentNotification;
import java.time.LocalDateTime;

public class StudentNotificationResponse {

    private Integer id;
    private Integer studentId;
    private Integer courseId;
    private String courseTitle;
    private String type;
    private String title;
    private String message;
    private Integer referenceId;
    private String referenceUrl;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public StudentNotificationResponse() {}

    public static StudentNotificationResponse fromEntity(StudentNotification entity) {
        if (entity == null) return null;
        StudentNotificationResponse dto = new StudentNotificationResponse();
        dto.setId(entity.getId());
        if (entity.getStudent() != null) {
            dto.setStudentId(entity.getStudent().getId());
        }
        if (entity.getCourse() != null) {
            dto.setCourseId(entity.getCourse().getId());
            dto.setCourseTitle(entity.getCourse().getTitle());
        }
        dto.setType(entity.getType());
        dto.setTitle(entity.getTitle());
        dto.setMessage(entity.getMessage());
        dto.setReferenceId(entity.getReferenceId());
        dto.setReferenceUrl(entity.getReferenceUrl());
        dto.setIsRead(entity.getIsRead());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
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

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceUrl() {
        return referenceUrl;
    }

    public void setReferenceUrl(String referenceUrl) {
        this.referenceUrl = referenceUrl;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
