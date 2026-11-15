package com.olp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for creating/updating Quiz Settings (FR-I20, DR-ED03, DR-ED06).
 */
public class QuizSettingsRequest {

    @JsonAlias("durationMinutes")
    @Min(value = 1, message = "Duration (timeLimitMinutes) must be at least 1 minute")
    private Integer timeLimitMinutes;

    @Min(value = 1, message = "Number of questions must be at least 1")
    private Integer numberOfQuestions;

    @JsonAlias("maxMarks")
    @DecimalMin(value = "0.0", message = "Total marks cannot be negative")
    private BigDecimal totalMarks;

    @DecimalMin(value = "0.0", message = "Passing marks cannot be negative")
    private BigDecimal passingMarks;

    @Min(value = 1, message = "Allowed attempts must be at least 1")
    private Integer maxAttempts;

    @JsonAlias({"startDate", "startTime"})
    private LocalDateTime availableFrom;

    @JsonAlias({"endDate", "endTime"})
    private LocalDateTime availableUntil;

    @JsonAlias({ "showFeedback", "feedbackVisible", "reviewEnabled" })
    private Boolean feedbackReleased;

    private Boolean showCorrectAnswers;
    private Boolean showFeedback;
    private String answerReleaseTiming;
    private Boolean isExam;

    private Boolean negativeMarking;
    private Boolean partialMarking;
    private BigDecimal numericTolerance;

    public QuizSettingsRequest() {}

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

    public Boolean getShowCorrectAnswers() {
        return showCorrectAnswers;
    }

    public void setShowCorrectAnswers(Boolean showCorrectAnswers) {
        this.showCorrectAnswers = showCorrectAnswers;
    }

    public Boolean getShowFeedback() {
        return showFeedback;
    }

    public void setShowFeedback(Boolean showFeedback) {
        this.showFeedback = showFeedback;
    }

    public String getAnswerReleaseTiming() {
        return answerReleaseTiming;
    }

    public void setAnswerReleaseTiming(String answerReleaseTiming) {
        this.answerReleaseTiming = answerReleaseTiming;
    }

    public Boolean getIsExam() {
        return isExam;
    }

    public void setIsExam(Boolean exam) {
        this.isExam = exam;
    }
}

