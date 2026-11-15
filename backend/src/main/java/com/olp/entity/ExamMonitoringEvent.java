package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an individual proctoring or monitoring event captured during an exam attempt (FR-I29, Step 55).
 */
@Entity
@Table(name = "exam_monitoring_events")
public class ExamMonitoringEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 50, nullable = false)
    private MonitoringEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20, nullable = false)
    private MonitoringSeverity severity = MonitoringSeverity.LOW;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "notes", length = 255)
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (eventTimestamp == null) {
            eventTimestamp = LocalDateTime.now();
        }
        if (severity == null) {
            severity = MonitoringSeverity.LOW;
        }
    }

    public ExamMonitoringEvent() {}

    public ExamMonitoringEvent(ExamAttempt attempt, MonitoringEventType eventType, MonitoringSeverity severity,
                               LocalDateTime eventTimestamp, Integer durationSeconds, String metadataJson, String notes) {
        this.attempt = attempt;
        this.eventType = eventType;
        this.severity = severity != null ? severity : MonitoringSeverity.LOW;
        this.eventTimestamp = eventTimestamp != null ? eventTimestamp : LocalDateTime.now();
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

    public ExamAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(ExamAttempt attempt) {
        this.attempt = attempt;
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
        this.severity = severity != null ? severity : MonitoringSeverity.LOW;
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
