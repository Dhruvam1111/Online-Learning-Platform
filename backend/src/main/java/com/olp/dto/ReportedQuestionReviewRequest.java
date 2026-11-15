package com.olp.dto;

import com.olp.entity.ReportedQuestionReviewAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for reviewing a reported question (FR-I36, Step 53).
 */
public class ReportedQuestionReviewRequest {

    @NotNull(message = "Review action is required (CORRECT, INVALIDATE, DISMISS)")
    private ReportedQuestionReviewAction action;

    @NotBlank(message = "Review comment is required")
    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;

    public ReportedQuestionReviewRequest() {}

    public ReportedQuestionReviewRequest(ReportedQuestionReviewAction action, String comment) {
        this.action = action;
        this.comment = comment;
    }

    public ReportedQuestionReviewAction getAction() {
        return action;
    }

    public void setAction(ReportedQuestionReviewAction action) {
        this.action = action;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
