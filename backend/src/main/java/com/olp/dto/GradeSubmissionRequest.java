package com.olp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request DTO for instructors evaluating/grading a descriptive answer submission (FR-I30).
 */
public class GradeSubmissionRequest {

    @NotNull(message = "Awarded marks are required")
    @DecimalMin(value = "0.0", message = "Awarded marks cannot be negative")
    private BigDecimal awardedMarks;

    private String feedback;

    private Boolean publish = Boolean.FALSE;

    private String status = "EVALUATED_UNPUBLISHED";

    public GradeSubmissionRequest() {}

    public GradeSubmissionRequest(BigDecimal awardedMarks, String feedback) {
        this.awardedMarks = awardedMarks;
        this.feedback = feedback;
        this.publish = Boolean.FALSE;
        this.status = "EVALUATED_UNPUBLISHED";
    }

    public GradeSubmissionRequest(BigDecimal awardedMarks, String feedback, Boolean publish) {
        this.awardedMarks = awardedMarks;
        this.feedback = feedback;
        this.publish = publish;
        this.status = (publish != null && publish) ? "PUBLISHED" : "EVALUATED_UNPUBLISHED";
    }

    public BigDecimal getAwardedMarks() {
        return awardedMarks;
    }

    public void setAwardedMarks(BigDecimal awardedMarks) {
        this.awardedMarks = awardedMarks;
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
        if (publish != null && publish) {
            this.status = "PUBLISHED";
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        if ("PUBLISHED".equalsIgnoreCase(status)) {
            this.publish = Boolean.TRUE;
        }
    }
}
