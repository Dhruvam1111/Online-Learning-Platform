package com.olp.dto;

import com.olp.entity.ExamMonitoringLog;
import java.time.LocalDateTime;

public class ExamMonitoringLogDto {

    private Integer id;
    private Integer attemptId;
    private Integer studentId;
    private Integer examId;
    private String eventType;
    private LocalDateTime eventTimestamp;
    private Integer durationSeconds;
    private String details;
    private String severity;
    private String ruleTriggered;

    public ExamMonitoringLogDto() {}

    public static ExamMonitoringLogDto fromEntity(ExamMonitoringLog log) {
        if (log == null) return null;
        ExamMonitoringLogDto dto = new ExamMonitoringLogDto();
        dto.setId(log.getId());
        if (log.getAttempt() != null) dto.setAttemptId(log.getAttempt().getId());
        if (log.getStudent() != null) dto.setStudentId(log.getStudent().getId());
        if (log.getExam() != null) dto.setExamId(log.getExam().getId());
        dto.setEventType(log.getEventType());
        dto.setEventTimestamp(log.getEventTimestamp());
        dto.setDurationSeconds(log.getDurationSeconds());
        dto.setDetails(log.getDetails());
        dto.setSeverity(log.getSeverity());
        dto.setRuleTriggered(log.getRuleTriggered());
        return dto;
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

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Integer getExamId() {
        return examId;
    }

    public void setExamId(Integer examId) {
        this.examId = examId;
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
}
