package com.olp.dto;

/**
 * Request DTO for withholding assessment results (FR-I34, Step 51).
 */
public class WithholdAssessmentResultsRequest {

    private String reason;

    public WithholdAssessmentResultsRequest() {
    }

    public WithholdAssessmentResultsRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
