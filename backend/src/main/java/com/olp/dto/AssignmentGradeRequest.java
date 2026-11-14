package com.olp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request payload for grading an assignment submission (FR-I68, Step 28).
 */
public class AssignmentGradeRequest {

    @NotNull(message = "Awarded marks cannot be null")
    @DecimalMin(value = "0.0", message = "Awarded marks cannot be negative")
    private BigDecimal awardedMarks;

    private String feedback;

    public AssignmentGradeRequest() {}

    public AssignmentGradeRequest(BigDecimal awardedMarks, String feedback) {
        this.awardedMarks = awardedMarks;
        this.feedback = feedback;
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
}

