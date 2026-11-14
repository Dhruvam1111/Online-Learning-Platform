package com.olp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for adding or updating answer-level feedback comments (FR-I31).
 */
public class AnswerFeedbackRequest {

    @NotBlank(message = "Feedback comment cannot be empty")
    @Size(max = 2000, message = "Feedback comment must not exceed 2000 characters")
    @JsonAlias({ "comment", "comments", "text" })
    private String feedback;

    private Boolean publish;

    public AnswerFeedbackRequest() {}

    public AnswerFeedbackRequest(String feedback) {
        this.feedback = feedback;
    }

    public AnswerFeedbackRequest(String feedback, Boolean publish) {
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
