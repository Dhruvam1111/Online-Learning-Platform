package com.olp.dto;

/**
 * Request payload for course deletion confirmation (FR-I12).
 */
public class DeleteCourseRequest {

    private Boolean confirm;
    private String reason;

    public DeleteCourseRequest() {}

    public DeleteCourseRequest(Boolean confirm) {
        this.confirm = confirm;
    }

    public DeleteCourseRequest(Boolean confirm, String reason) {
        this.confirm = confirm;
        this.reason = reason;
    }

    public Boolean getConfirm() {
        return confirm;
    }

    public void setConfirm(Boolean confirm) {
        this.confirm = confirm;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

