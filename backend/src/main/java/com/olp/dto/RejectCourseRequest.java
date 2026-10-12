package com.olp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RejectCourseRequest {

    @NotBlank(message = "Rejection reason cannot be blank")
    @Size(max = 1000, message = "Rejection reason must be 1000 characters or fewer")
    private String reason;

    public RejectCourseRequest() {}

    public RejectCourseRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
