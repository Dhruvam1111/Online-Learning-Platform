package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an activity or violation log during a monitored examination attempt
 * (FR-S52, FR-S53, FR-S54, FR-S55, FR-S60, FR-S61, DR-05, DR-06).
 */
@Entity
@Table(name = "exam_monitoring_logs")
public class ExamMonitoringLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @Column(name = "duration_seconds")
    private Integer durationSeconds = 0;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "severity", length = 20, nullable = false)
    private String severity = "INFO";

    @Column(name = "rule_triggered", length = 100)
    private String ruleTriggered;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ExamMonitoringLog() {}

    public ExamMonitoringLog(ExamAttempt attempt, User student, Exam exam,
                             String eventType, LocalDateTime eventTimestamp,
                             Integer durationSeconds, String details,
                             String severity, String ruleTriggered) {
        this.attempt = attempt;
        this.student = student;
        this.exam = exam;
        this.eventType = eventType;
        this.eventTimestamp = eventTimestamp != null ? eventTimestamp : LocalDateTime.now();
        this.durationSeconds = durationSeconds != null ? durationSeconds : 0;
        this.details = details;
        this.severity = severity != null ? severity : "INFO";
        this.ruleTriggered = ruleTriggered;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.eventTimestamp == null) {
            this.eventTimestamp = LocalDateTime.now();
        }
        if (this.durationSeconds == null) {
            this.durationSeconds = 0;
        }
        if (this.severity == null) {
            this.severity = "INFO";
        }
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

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
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

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getRuleTriggered() {
        return ruleTriggered;
    }

    public void setRuleTriggered(String ruleTriggered) {
        this.ruleTriggered = ruleTriggered;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
