package com.olp.dto;

import com.olp.entity.ExamMonitoringEvent;
import com.olp.entity.MonitoringEventType;
import com.olp.entity.MonitoringSeverity;

import java.time.LocalDateTime;

public class MonitoringEventResponse {

    private Integer id;
    private Integer attemptId;
    private MonitoringEventType eventType;
    private MonitoringSeverity severity;
    private LocalDateTime eventTimestamp;
    private Integer durationSeconds;
    private String metadataJson;
    private String notes;

    public MonitoringEventResponse() {}

    public MonitoringEventResponse(ExamMonitoringEvent event) {
        if (event != null) {
            this.id = event.getId();
            this.attemptId = event.getAttempt() != null ? event.getAttempt().getId() : null;
            this.eventType = event.getEventType();
            this.severity = event.getSeverity();
            this.eventTimestamp = event.getEventTimestamp();
            this.durationSeconds = event.getDurationSeconds();
            this.metadataJson = event.getMetadataJson();
            this.notes = event.getNotes();
        }
    }

    public MonitoringEventResponse(Integer id, Integer attemptId, MonitoringEventType eventType,
                                   MonitoringSeverity severity, LocalDateTime eventTimestamp,
                                   Integer durationSeconds, String metadataJson, String notes) {
        this.id = id;
        this.attemptId = attemptId;
        this.eventType = eventType;
        this.severity = severity;
        this.eventTimestamp = eventTimestamp;
        this.durationSeconds = durationSeconds;
        this.metadataJson = metadataJson;
        this.notes = notes;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
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
