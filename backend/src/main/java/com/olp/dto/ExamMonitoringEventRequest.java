package com.olp.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class ExamMonitoringEventRequest {

    @NotBlank(message = "Event type is required")
    private String eventType;

    private Integer durationSeconds = 0;

    private String details;

    private LocalDateTime clientTimestamp;

    public ExamMonitoringEventRequest() {}

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds != null ? durationSeconds : 0;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getClientTimestamp() {
        return clientTimestamp;
    }

    public void setClientTimestamp(LocalDateTime clientTimestamp) {
        this.clientTimestamp = clientTimestamp;
    }
}
