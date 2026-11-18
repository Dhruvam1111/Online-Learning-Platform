package com.olp.dto;

import java.time.LocalDateTime;

public class StudentAssessmentAttemptDto {

    private Integer attemptId;
    private int attemptNumber;
    private Double obtainedMarks;
    private Double totalMarks;
    private Double percentage;
    private boolean passed;
    private String status;
    private boolean isPublished;
    private LocalDateTime submittedAt;
    private String feedback;

    public StudentAssessmentAttemptDto() {
    }

    public StudentAssessmentAttemptDto(
            Integer attemptId,
            int attemptNumber,
            Double obtainedMarks,
            Double totalMarks,
            Double percentage,
            boolean passed,
            String status,
            boolean isPublished,
            LocalDateTime submittedAt,
            String feedback) {
        this.attemptId = attemptId;
        this.attemptNumber = attemptNumber;
        this.obtainedMarks = obtainedMarks;
        this.totalMarks = totalMarks;
        this.percentage = percentage;
        this.passed = passed;
        this.status = status;
        this.isPublished = isPublished;
        this.submittedAt = submittedAt;
        this.feedback = feedback;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public Double getObtainedMarks() {
        return obtainedMarks;
    }

    public void setObtainedMarks(Double obtainedMarks) {
        this.obtainedMarks = obtainedMarks;
    }

    public Double getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(Double totalMarks) {
        this.totalMarks = totalMarks;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isPublished() {
        return isPublished;
    }

    public void setPublished(boolean isPublished) {
        this.isPublished = isPublished;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
