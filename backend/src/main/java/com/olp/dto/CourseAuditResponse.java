package com.olp.dto;

import com.olp.entity.CourseAudit;
import java.time.LocalDateTime;

public class CourseAuditResponse {

    private Integer id;
    private Integer courseId;
    private Integer actorId;
    private String actorName;
    private String actionType;
    private String changedFields;
    private String previousValues;
    private String newValues;
    private LocalDateTime createdAt;

    public CourseAuditResponse() {
    }

    public CourseAuditResponse(Integer id, Integer courseId, Integer actorId, String actorName,
                               String actionType, String changedFields, String previousValues,
                               String newValues, LocalDateTime createdAt) {
        this.id = id;
        this.courseId = courseId;
        this.actorId = actorId;
        this.actorName = actorName;
        this.actionType = actionType;
        this.changedFields = changedFields;
        this.previousValues = previousValues;
        this.newValues = newValues;
        this.createdAt = createdAt;
    }

    public static CourseAuditResponse fromEntity(CourseAudit audit) {
        if (audit == null) {
            return null;
        }
        Integer courseId = audit.getCourse() != null ? audit.getCourse().getId() : null;
        Integer actorId = audit.getActor() != null ? audit.getActor().getId() : null;
        String actorName = audit.getActor() != null ? audit.getActor().getName() : "Unknown";

        return new CourseAuditResponse(
                audit.getId(),
                courseId,
                actorId,
                actorName,
                audit.getActionType(),
                audit.getChangedFields(),
                audit.getPreviousValues(),
                audit.getNewValues(),
                audit.getCreatedAt()
        );
    }

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getActorId() {
        return actorId;
    }

    public void setActorId(Integer actorId) {
        this.actorId = actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getChangedFields() {
        return changedFields;
    }

    public void setChangedFields(String changedFields) {
        this.changedFields = changedFields;
    }

    public String getPreviousValues() {
        return previousValues;
    }

    public void setPreviousValues(String previousValues) {
        this.previousValues = previousValues;
    }

    public String getNewValues() {
        return newValues;
    }

    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

