package com.olp.dto;

import com.olp.entity.AssessmentType;
import com.olp.entity.ResultVersion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data transfer object for viewing a version of an assessment result in history (FR-I74, Step 49).
 * Contains all five mandatory history fields: old value, new value, reason, actor, and timestamp.
 */
public class ResultVersionResponse {

    private Integer id;
    private Integer courseId;
    private AssessmentType assessmentType;
    private Integer assessmentId;
    private String assessmentTitle;
    private Integer studentId;
    private String studentName;
    private String studentEmail;
    private Integer attemptId;
    private BigDecimal oldScore;
    private BigDecimal newScore;
    private String oldValue;
    private String newValue;
    private String reason;
    private Integer actorId;
    private String actorName;
    private String actorEmail;
    private LocalDateTime createdAt;

    public ResultVersionResponse() {
    }

    public static ResultVersionResponse fromEntity(ResultVersion entity) {
        if (entity == null) {
            return null;
        }
        ResultVersionResponse dto = new ResultVersionResponse();
        dto.setId(entity.getId());
        if (entity.getCourse() != null) {
            dto.setCourseId(entity.getCourse().getId());
        }
        dto.setAssessmentType(entity.getAssessmentType());
        dto.setAssessmentId(entity.getAssessmentId());
        dto.setAssessmentTitle(entity.getAssessmentTitle());
        if (entity.getStudent() != null) {
            dto.setStudentId(entity.getStudent().getId());
            dto.setStudentName(entity.getStudent().getName());
            dto.setStudentEmail(entity.getStudent().getEmail());
        }
        dto.setAttemptId(entity.getAttemptId());
        dto.setOldScore(entity.getOldScore());
        dto.setNewScore(entity.getNewScore());
        dto.setOldValue(entity.getOldValue());
        dto.setNewValue(entity.getNewValue());
        dto.setReason(entity.getReason());
        if (entity.getActor() != null) {
            dto.setActorId(entity.getActor().getId());
            dto.setActorName(entity.getActor().getName());
            dto.setActorEmail(entity.getActor().getEmail());
        }
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

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

    public AssessmentType getAssessmentType() {
        return assessmentType;
    }

    public void setAssessmentType(AssessmentType assessmentType) {
        this.assessmentType = assessmentType;
    }

    public Integer getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(Integer assessmentId) {
        this.assessmentId = assessmentId;
    }

    public String getAssessmentTitle() {
        return assessmentTitle;
    }

    public void setAssessmentTitle(String assessmentTitle) {
        this.assessmentTitle = assessmentTitle;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public BigDecimal getOldScore() {
        return oldScore;
    }

    public void setOldScore(BigDecimal oldScore) {
        this.oldScore = oldScore;
    }

    public BigDecimal getNewScore() {
        return newScore;
    }

    public void setNewScore(BigDecimal newScore) {
        this.newScore = newScore;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
