package com.olp.dto;

import com.olp.entity.GradingResultVersion;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing an immutable historical result version of a grade override (FR-I32, DR-01, DR-ED04).
 */
public class GradingResultVersionResponse {

    private Integer versionId;
    private Integer versionNumber;
    private Integer attemptId;
    private Integer answerId;
    private BigDecimal previousMarks;
    private BigDecimal newMarks;
    private BigDecimal maxMarks;
    private String justification;
    private Integer instructorId;
    private String instructorName;
    private String statusBefore;
    private String statusAfter;
    private LocalDateTime createdAt;

    public GradingResultVersionResponse() {}

    public static GradingResultVersionResponse fromEntity(GradingResultVersion v) {
        if (v == null) return null;
        GradingResultVersionResponse dto = new GradingResultVersionResponse();
        dto.setVersionId(v.getId());
        dto.setVersionNumber(v.getVersionNumber());
        if (v.getAttempt() != null) {
            dto.setAttemptId(v.getAttempt().getId());
        }
        if (v.getAnswer() != null) {
            dto.setAnswerId(v.getAnswer().getId());
        }
        dto.setPreviousMarks(v.getPreviousMarks());
        dto.setNewMarks(v.getNewMarks());
        dto.setMaxMarks(v.getMaxMarks());
        dto.setJustification(v.getJustification());
        if (v.getInstructor() != null) {
            dto.setInstructorId(v.getInstructor().getId());
            dto.setInstructorName(v.getInstructor().getName());
        }
        dto.setStatusBefore(v.getStatusBefore());
        dto.setStatusAfter(v.getStatusAfter());
        dto.setCreatedAt(v.getCreatedAt());
        return dto;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Integer answerId) {
        this.answerId = answerId;
    }

    public BigDecimal getPreviousMarks() {
        return previousMarks;
    }

    public void setPreviousMarks(BigDecimal previousMarks) {
        this.previousMarks = previousMarks;
    }

    public BigDecimal getNewMarks() {
        return newMarks;
    }

    public void setNewMarks(BigDecimal newMarks) {
        this.newMarks = newMarks;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(BigDecimal maxMarks) {
        this.maxMarks = maxMarks;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public Integer getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Integer instructorId) {
        this.instructorId = instructorId;
    }

    public String getInstructorName() {
        return instructorName;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }

    public String getStatusBefore() {
        return statusBefore;
    }

    public void setStatusBefore(String statusBefore) {
        this.statusBefore = statusBefore;
    }

    public String getStatusAfter() {
        return statusAfter;
    }

    public void setStatusAfter(String statusAfter) {
        this.statusAfter = statusAfter;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
