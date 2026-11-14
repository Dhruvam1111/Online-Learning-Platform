package com.olp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for creating or updating a Quiz (FR-I19).
 */
public class QuizRequest {

    private Integer courseId;
    private Integer moduleId;
    private Integer lessonId;

    @NotBlank(message = "Quiz title is required")
    @Size(max = 200, message = "Quiz title must not exceed 200 characters")
    private String title;

    private String description;
    private String instructions;
    private String status;

    @JsonAlias("maxMarks")
    @DecimalMin(value = "0.0", message = "Total marks cannot be negative")
    private BigDecimal totalMarks;

    @DecimalMin(value = "0.0", message = "Passing marks cannot be negative")
    private BigDecimal passingMarks;

    @JsonAlias("durationMinutes")
    @Min(value = 1, message = "Time limit must be at least 1 minute")
    private Integer timeLimitMinutes;

    @Min(value = 1, message = "Number of questions must be at least 1")
    private Integer numberOfQuestions;

    @Min(value = 1, message = "Max attempts must be at least 1")
    private Integer maxAttempts;

    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;

    @JsonAlias({ "showFeedback", "feedbackVisible", "reviewEnabled" })
    private Boolean feedbackReleased;

    private Boolean negativeMarking;
    private Boolean partialMarking;
    private BigDecimal numericTolerance;

    @Valid
    private List<QuizQuestionRequest> questions;

    public QuizRequest() {}

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(BigDecimal totalMarks) {
        this.totalMarks = totalMarks;
    }

    public BigDecimal getPassingMarks() {
        return passingMarks;
    }

    public void setPassingMarks(BigDecimal passingMarks) {
        this.passingMarks = passingMarks;
    }

    public Integer getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    public void setTimeLimitMinutes(Integer timeLimitMinutes) {
        this.timeLimitMinutes = timeLimitMinutes;
    }

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public LocalDateTime getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalDateTime availableFrom) {
        this.availableFrom = availableFrom;
    }

    public LocalDateTime getAvailableUntil() {
        return availableUntil;
    }

    public void setAvailableUntil(LocalDateTime availableUntil) {
        this.availableUntil = availableUntil;
    }

    public Boolean getFeedbackReleased() {
        return feedbackReleased;
    }

    public void setFeedbackReleased(Boolean feedbackReleased) {
        this.feedbackReleased = feedbackReleased;
    }

    public Boolean getShowFeedback() {
        return feedbackReleased;
    }

    public void setShowFeedback(Boolean showFeedback) {
        this.feedbackReleased = showFeedback;
    }

    public Boolean getNegativeMarking() {
        return negativeMarking;
    }

    public void setNegativeMarking(Boolean negativeMarking) {
        this.negativeMarking = negativeMarking;
    }

    public Boolean getPartialMarking() {
        return partialMarking;
    }

    public void setPartialMarking(Boolean partialMarking) {
        this.partialMarking = partialMarking;
    }

    public BigDecimal getNumericTolerance() {
        return numericTolerance;
    }

    public void setNumericTolerance(BigDecimal numericTolerance) {
        this.numericTolerance = numericTolerance;
    }

    public List<QuizQuestionRequest> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuizQuestionRequest> questions) {
        this.questions = questions;
    }
}

