package com.olp.dto;

import com.olp.entity.StudySchedule;
import java.time.LocalDateTime;

public class StudyScheduleResponse {

    private Integer id;
    private Integer studentId;
    private Integer courseId;
    private String courseTitle;
    private String title;
    private String taskDescription;
    private String itemType;
    private Integer referenceId;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private String priority;
    private Boolean isCompleted;
    private LocalDateTime completedAt;
    private String timeZone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public StudyScheduleResponse() {}

    public static StudyScheduleResponse fromEntity(StudySchedule entity) {
        if (entity == null) return null;
        StudyScheduleResponse dto = new StudyScheduleResponse();
        dto.setId(entity.getId());
        if (entity.getStudent() != null) {
            dto.setStudentId(entity.getStudent().getId());
        }
        if (entity.getCourse() != null) {
            dto.setCourseId(entity.getCourse().getId());
            dto.setCourseTitle(entity.getCourse().getTitle());
        }
        dto.setTitle(entity.getTitle());
        dto.setTaskDescription(entity.getTaskDescription());
        dto.setItemType(entity.getItemType());
        dto.setReferenceId(entity.getReferenceId());
        dto.setScheduledStart(entity.getScheduledStart());
        dto.setScheduledEnd(entity.getScheduledEnd());
        dto.setPriority(entity.getPriority());
        dto.setIsCompleted(entity.getIsCompleted());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setTimeZone(entity.getTimeZone());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }

    public LocalDateTime getScheduledStart() {
        return scheduledStart;
    }

    public void setScheduledStart(LocalDateTime scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public LocalDateTime getScheduledEnd() {
        return scheduledEnd;
    }

    public void setScheduledEnd(LocalDateTime scheduledEnd) {
        this.scheduledEnd = scheduledEnd;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
