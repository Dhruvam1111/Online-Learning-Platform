package com.olp.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request payload for creating an assignment (FR-I68, Step 28).
 */
public class AssignmentCreateRequest {

    @NotNull(message = "Course ID is required")
    private Integer courseId;

    private Integer moduleId;

    private Integer lessonId;

    @NotBlank(message = "Assignment title cannot be blank")
    @Size(max = 200, message = "Assignment title must not exceed 200 characters")
    private String title;

    private String instructions;

    @NotNull(message = "Assignment deadline is required")
    private LocalDateTime deadline;

    @DecimalMin(value = "0.0", message = "Max marks cannot be negative")
    private BigDecimal maxMarks = new BigDecimal("100.00");

    private String rubric;

    private String submissionType = "FILE";

    private String latePolicy = "REJECT";

    @DecimalMin(value = "0.0", message = "Late penalty percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Late penalty percentage cannot exceed 100%")
    private BigDecimal latePenaltyPercentage = BigDecimal.ZERO;

    private String status = "PUBLISHED";

    public AssignmentCreateRequest() {}

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getModuleId() {
        return moduleId;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
    }

    public Integer getLessonId() {
        return lessonId;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }

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

