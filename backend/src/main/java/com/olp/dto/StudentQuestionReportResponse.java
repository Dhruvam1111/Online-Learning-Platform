package com.olp.dto;

import java.time.LocalDateTime;

/**
 * Representation of an individual student's question report (FR-I35 Step 52, FR-I36 Step 53).
 */
public class StudentQuestionReportResponse {

    private Integer id;
    private Integer studentId;
    private String studentName;
    private String studentEmail;
    private Integer attemptId;
    private String reason;
    private String explanation;
    private LocalDateTime reportedAt;

    public StudentQuestionReportResponse() {}

    public StudentQuestionReportResponse(Integer id, Integer studentId, String studentName, String studentEmail,
                                         Integer attemptId, String reason, String explanation, LocalDateTime reportedAt) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.attemptId = attemptId;
        this.reason = reason;
        this.explanation = explanation;
        this.reportedAt = reportedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }
}
