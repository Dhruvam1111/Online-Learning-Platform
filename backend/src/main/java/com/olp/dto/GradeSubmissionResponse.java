package com.olp.dto;

/**
 * Response DTO returned after grading a descriptive submission (FR-I30).
 */
public class GradeSubmissionResponse {

    private String message;
    private DescriptiveSubmissionResponse submission;

    public GradeSubmissionResponse() {}

    public GradeSubmissionResponse(String message, DescriptiveSubmissionResponse submission) {
        this.message = message;
        this.submission = submission;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DescriptiveSubmissionResponse getSubmission() {
        return submission;
    }

    public void setSubmission(DescriptiveSubmissionResponse submission) {
        this.submission = submission;
    }
}
