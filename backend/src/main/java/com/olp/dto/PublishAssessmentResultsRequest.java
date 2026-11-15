package com.olp.dto;

/**
 * Request DTO for publishing assessment results (FR-I34, Step 51).
 * Requires explicit confirmation if ungraded items remain.
 */
public class PublishAssessmentResultsRequest {

    private Boolean confirmUngraded = false;
    private Boolean notifyStudents = true;

    public PublishAssessmentResultsRequest() {
    }

    public PublishAssessmentResultsRequest(Boolean confirmUngraded, Boolean notifyStudents) {
        this.confirmUngraded = confirmUngraded != null ? confirmUngraded : false;
        this.notifyStudents = notifyStudents != null ? notifyStudents : true;
    }

    public Boolean getConfirmUngraded() {
        return confirmUngraded;
    }

    public void setConfirmUngraded(Boolean confirmUngraded) {
        this.confirmUngraded = confirmUngraded;
    }

    public Boolean getNotifyStudents() {
        return notifyStudents;
    }

    public void setNotifyStudents(Boolean notifyStudents) {
        this.notifyStudents = notifyStudents;
    }
}
