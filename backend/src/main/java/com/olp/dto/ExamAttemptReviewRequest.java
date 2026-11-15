package com.olp.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for instructor review actions on flagged exam attempts (FR-I69, Step 56).
 * Actions: FLAG, CLEAR, ESCALATE, UPDATE_NOTES
 */
public class ExamAttemptReviewRequest {

    @NotBlank(message = "Review action is required")
    private String action; // "FLAG", "CLEAR", "ESCALATE", "UPDATE_NOTES"

    private String notes;

    private String reason;

    private String recommendation; // e.g. "WARNING", "DISQUALIFY_ATTEMPT", "MANUAL_AUDIT", "NO_ACTION"

    public ExamAttemptReviewRequest() {}

    public ExamAttemptReviewRequest(String action, String notes, String reason, String recommendation) {
        this.action = action;
        this.notes = notes;
        this.reason = reason;
        this.recommendation = recommendation;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}
