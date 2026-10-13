package com.olp.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateCourseStatusRequest {

    @NotBlank(message = "Status is required")
    private String status;

    public UpdateCourseStatusRequest() {}

    public UpdateCourseStatusRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
