package com.olp.dto;

import com.olp.entity.AssessmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request DTO for recording a new result version (FR-I74, Step 49 / FR-I32, Step 48).
 * Enforces mandatory reason/justification and explicit old/new values.
 */
public class CreateResultVersionRequest {

    @NotNull(message = "Assessment type is required")
    private AssessmentType assessmentType;

    @NotNull(message = "Assessment ID is required")
    private Integer assessmentId;

    private String assessmentTitle;

    @NotNull(message = "Student ID is required")
    private Integer studentId;

    private Integer attemptId;

    private BigDecimal oldScore;

    private BigDecimal newScore;

    @NotBlank(message = "Old value is required")
    private String oldValue;

    @NotBlank(message = "New value is required")
    private String newValue;

    @NotBlank(message = "Reason/justification is required")
    private String reason;

    public CreateResultVersionRequest() {
    }

    public CreateResultVersionRequest(AssessmentType assessmentType,
                                      Integer assessmentId,
                                      String assessmentTitle,
                                      Integer studentId,
                                      Integer attemptId,
                                      BigDecimal oldScore,
                                      BigDecimal newScore,
                                      String oldValue,
                                      String newValue,
                                      String reason) {
        this.assessmentType = assessmentType;
        this.assessmentId = assessmentId;
        this.assessmentTitle = assessmentTitle;
        this.studentId = studentId;
        this.attemptId = attemptId;
        this.oldScore = oldScore;
        this.newScore = newScore;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.reason = reason;
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
}
