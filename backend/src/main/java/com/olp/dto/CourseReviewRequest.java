package com.olp.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for admin review actions, such as requesting changes or providing review feedback (FR-I71).
 * Accepts reason, comments, or both.
 */
public class CourseReviewRequest {

    @Size(max = 1000, message = "Reason must be 1000 characters or fewer")
    private String reason;

    @Size(max = 2000, message = "Comments must be 2000 characters or fewer")
    private String comments;

    public CourseReviewRequest() {}

    public CourseReviewRequest(String comments) {
        this.comments = comments;
    }

    public CourseReviewRequest(String reason, String comments) {
        this.reason = reason;
        this.comments = comments;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    /**
     * Resolves the feedback text by checking comments first, then reason.
     */
    public String resolveFeedback() {
        if (comments != null && !comments.trim().isEmpty()) {
            return comments.trim();
        }
        if (reason != null && !reason.trim().isEmpty()) {
            return reason.trim();
        }
        return "";
    }
}

