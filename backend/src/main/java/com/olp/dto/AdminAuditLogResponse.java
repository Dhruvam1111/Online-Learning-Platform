package com.olp.dto;

import com.olp.entity.AdminAuditLog;
import com.olp.entity.User;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for immutable Admin Audit Log entries (FR-A31, ADM-015).
 */
public class AdminAuditLogResponse {

    private Long id;
    private Integer actorId;
    private String actorName;
    private String actorEmail;
    private String action;
    private String targetType;
    private String targetId;
    private String targetName;
    private String details;
    private String previousState;
    private String newState;
    private String ipAddress;
    private LocalDateTime createdAt;

    public AdminAuditLogResponse() {
    }

    public AdminAuditLogResponse(Long id, Integer actorId, String actorName, String actorEmail,
                                 String action, String targetType, String targetId,
                                 String targetName, String details, String previousState,
                                 String newState, String ipAddress, LocalDateTime createdAt) {
        this.id = id;
        this.actorId = actorId;
        this.actorName = actorName;
        this.actorEmail = actorEmail;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetName = targetName;
        this.details = details;
        this.previousState = previousState;
        this.newState = newState;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public static AdminAuditLogResponse fromEntity(AdminAuditLog log) {
        if (log == null) {
            return null;
        }
        User actor = log.getActor();
        return new AdminAuditLogResponse(
                log.getId(),
                actor != null ? actor.getId() : null,
                actor != null ? actor.getName() : null,
                actor != null ? actor.getEmail() : null,
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getTargetName(),
                log.getDetails(),
                log.getPreviousState(),
                log.getNewState(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getPreviousState() {
        return previousState;
    }

    public void setPreviousState(String previousState) {
        this.previousState = previousState;
    }

    public String getNewState() {
        return newState;
    }

    public void setNewState(String newState) {
        this.newState = newState;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
