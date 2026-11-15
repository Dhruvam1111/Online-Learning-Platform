package com.olp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for student reporting an assessment question (FR-I35 Step 52, FR-S41).
 */
public class QuestionReportRequest {

    @NotBlank(message = "Report reason is required")
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;

    private String explanation;

    private Integer attemptId;

    public QuestionReportRequest() {}

    public QuestionReportRequest(String reason, String explanation, Integer attemptId) {
        this.reason = reason;
        this.explanation = explanation;
        this.attemptId = attemptId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }
}
