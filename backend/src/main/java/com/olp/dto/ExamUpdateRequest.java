package com.olp.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request payload for updating an examination (FR-I25, Step 38).
 */
public class ExamUpdateRequest {

    @Size(max = 200, message = "Exam title must not exceed 200 characters")
    private String title;

    private String description;

    private String instructions;

    private LocalDateTime startWindow;

    private LocalDateTime endWindow;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    private String timeZone;

    @DecimalMin(value = "0.0", message = "Max marks cannot be negative")
    private BigDecimal maxMarks;

    @DecimalMin(value = "0.0", message = "Passing marks cannot be negative")
    private BigDecimal passingMarks;

    @Min(value = 1, message = "Max attempts must be at least 1")
    private Integer maxAttempts;

    @DecimalMin(value = "0.0", message = "Min progress percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Min progress percentage cannot exceed 100%")
    private BigDecimal minProgressPercentage;

    private Boolean requirePrerequisites;

    private Boolean requireEnrollment;

    private String status;

    private Boolean isRandomized;

    private Long randomSeed;

    private Integer totalSets;

    public ExamUpdateRequest() {}

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

    public Boolean getIsRandomized() {
        return isRandomized;
    }

    public void setIsRandomized(Boolean randomized) {
        isRandomized = randomized;
    }

    public Long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(Long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public Integer getTotalSets() {
        return totalSets;
    }

    public void setTotalSets(Integer totalSets) {
        this.totalSets = totalSets;
    }
}

