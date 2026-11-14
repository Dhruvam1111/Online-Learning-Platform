package com.olp.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Request payload for creating and scheduling an examination (FR-I25, Step 38).
 */
public class ExamCreateRequest {

    @NotNull(message = "Course ID is required")
    private Integer courseId;

    @NotBlank(message = "Exam title cannot be blank")
    @Size(max = 200, message = "Exam title must not exceed 200 characters")
    private String title;

    private String description;

    private String instructions;

    @NotNull(message = "Start window is required")
    private LocalDateTime startWindow;

    @NotNull(message = "End window is required")
    private LocalDateTime endWindow;

    @NotNull(message = "Duration in minutes is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes = 60;

    private String timeZone = "UTC";

    @DecimalMin(value = "0.0", message = "Max marks cannot be negative")
    private BigDecimal maxMarks = new BigDecimal("100.00");

    @DecimalMin(value = "0.0", message = "Passing marks cannot be negative")
    private BigDecimal passingMarks;

    @Min(value = 1, message = "Max attempts must be at least 1")
    private Integer maxAttempts = 1;

    @DecimalMin(value = "0.0", message = "Min progress percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Min progress percentage cannot exceed 100%")
    private BigDecimal minProgressPercentage = BigDecimal.ZERO;

    private Boolean requirePrerequisites = true;

    private Boolean requireEnrollment = true;

    private String status = "SCHEDULED";

    private List<Integer> questionIds = new ArrayList<>();

    public ExamCreateRequest() {}

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public LocalDateTime getStartWindow() {
        return startWindow;
    }

    public void setStartWindow(LocalDateTime startWindow) {
        this.startWindow = startWindow;
    }

    public LocalDateTime getEndWindow() {
        return endWindow;
    }

    public void setEndWindow(LocalDateTime endWindow) {
        this.endWindow = endWindow;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(BigDecimal maxMarks) {
        this.maxMarks = maxMarks;
    }

    public BigDecimal getPassingMarks() {
        return passingMarks;
    }

    public void setPassingMarks(BigDecimal passingMarks) {
        this.passingMarks = passingMarks;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public BigDecimal getMinProgressPercentage() {
        return minProgressPercentage;
    }

    public void setMinProgressPercentage(BigDecimal minProgressPercentage) {
        this.minProgressPercentage = minProgressPercentage;
    }

    public Boolean getRequirePrerequisites() {
        return requirePrerequisites;
    }

    public void setRequirePrerequisites(Boolean requirePrerequisites) {
        this.requirePrerequisites = requirePrerequisites;
    }

    public Boolean getRequireEnrollment() {
        return requireEnrollment;
    }

    public void setRequireEnrollment(Boolean requireEnrollment) {
        this.requireEnrollment = requireEnrollment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Integer> getQuestionIds() {
        return questionIds;
    }

    public void setQuestionIds(List<Integer> questionIds) {
        this.questionIds = questionIds != null ? questionIds : new ArrayList<>();
    }
}

