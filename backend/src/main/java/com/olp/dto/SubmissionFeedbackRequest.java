package com.olp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for adding or updating submission-level overall feedback comments (FR-I31).
 */
public class SubmissionFeedbackRequest {

    @NotBlank(message = "Overall feedback comment cannot be empty")
    @Size(max = 4000, message = "Overall feedback comment must not exceed 4000 characters")
    @JsonAlias({ "comment", "comments", "overallFeedback", "text" })
    private String feedback;

    private Boolean publish;

    public SubmissionFeedbackRequest() {}

    public SubmissionFeedbackRequest(String feedback) {
        this.feedback = feedback;
    }

    public SubmissionFeedbackRequest(String feedback, Boolean publish) {
        this.feedback = feedback;
        this.publish = publish;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Boolean getPublish() {
        return publish;
    }

    public void setPublish(Boolean publish) {
        this.publish = publish;
    }
}
