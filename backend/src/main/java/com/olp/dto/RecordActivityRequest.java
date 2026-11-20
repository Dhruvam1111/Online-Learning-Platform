package com.olp.dto;

import jakarta.validation.constraints.NotBlank;

public class RecordActivityRequest {

    @NotBlank(message = "Activity type is required")
    private String activityType;

    private Integer referenceId;

    private String description;

    public RecordActivityRequest() {}

    public RecordActivityRequest(String activityType, Integer referenceId, String description) {
        this.activityType = activityType;
        this.referenceId = referenceId;
        this.description = description;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
