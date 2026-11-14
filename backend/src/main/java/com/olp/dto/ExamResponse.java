package com.olp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO representing an examination (FR-I25, Step 38).
 */
public class ExamResponse {

    private Integer id;
    private Integer courseId;
    private String courseTitle;
    private Integer createdById;
    private String createdByName;
    private String title;
    private String description;
    private String instructions;
    private LocalDateTime startWindow;
    private LocalDateTime endWindow;
    private Integer durationMinutes;
    private String timeZone;
    private BigDecimal maxMarks;
    private BigDecimal passingMarks;
    private Integer maxAttempts;
    private BigDecimal minProgressPercentage;
    private Boolean requirePrerequisites;
    private Boolean requireEnrollment;
    private String status;
    private Integer questionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isInsideWindow;
    private LocalDateTime serverTime;

    public ExamResponse() {}

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

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public Integer getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Integer createdById) {
        this.createdById = createdById;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
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

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
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

    public Boolean getIsInsideWindow() {
        return isInsideWindow;
    }

    public void setIsInsideWindow(Boolean insideWindow) {
        isInsideWindow = insideWindow;
    }

    public LocalDateTime getServerTime() {
        return serverTime;
    }

    public void setServerTime(LocalDateTime serverTime) {
        this.serverTime = serverTime;
    }
}

