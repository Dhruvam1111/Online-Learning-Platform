package com.olp.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request payload for updating an assignment (FR-I68, Step 28).
 */
public class AssignmentUpdateRequest {

    @Size(max = 200, message = "Assignment title must not exceed 200 characters")
    private String title;

    private String instructions;

    private LocalDateTime deadline;

    @DecimalMin(value = "0.0", message = "Max marks cannot be negative")
    private BigDecimal maxMarks;

    private String rubric;

    private String submissionType;

    private String latePolicy;

    @DecimalMin(value = "0.0", message = "Late penalty percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Late penalty percentage cannot exceed 100%")
    private BigDecimal latePenaltyPercentage;

    private String status;

    public AssignmentUpdateRequest() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(BigDecimal maxMarks) {
        this.maxMarks = maxMarks;
    }

    public String getRubric() {
        return rubric;
    }

    public void setRubric(String rubric) {
        this.rubric = rubric;
    }

    public String getSubmissionType() {
        return submissionType;
    }

    public void setSubmissionType(String submissionType) {
        this.submissionType = submissionType;
    }

    public String getLatePolicy() {
        return latePolicy;
    }

    public void setLatePolicy(String latePolicy) {
        this.latePolicy = latePolicy;
    }

    public BigDecimal getLatePenaltyPercentage() {
        return latePenaltyPercentage;
    }

    public void setLatePenaltyPercentage(BigDecimal latePenaltyPercentage) {
        this.latePenaltyPercentage = latePenaltyPercentage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

