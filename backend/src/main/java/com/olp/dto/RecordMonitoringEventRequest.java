package com.olp.dto;

import com.olp.entity.MonitoringEventType;
import com.olp.entity.MonitoringSeverity;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class RecordMonitoringEventRequest {

    @NotNull(message = "Event type is required")
    private MonitoringEventType eventType;

    private MonitoringSeverity severity;

    private LocalDateTime eventTimestamp;

    private Integer durationSeconds;

    private String metadataJson;

    private String notes;

    public RecordMonitoringEventRequest() {}

    public RecordMonitoringEventRequest(MonitoringEventType eventType, MonitoringSeverity severity,
                                        LocalDateTime eventTimestamp, Integer durationSeconds,
                                        String metadataJson, String notes) {
        this.eventType = eventType;
        this.severity = severity;
        this.eventTimestamp = eventTimestamp;
        this.durationSeconds = durationSeconds;
        this.metadataJson = metadataJson;
        this.notes = notes;
    }

    public MonitoringEventType getEventType() {
        return eventType;
    }

    public void setEventType(MonitoringEventType eventType) {
        this.eventType = eventType;
    }

    public MonitoringSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(MonitoringSeverity severity) {
        this.severity = severity;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
