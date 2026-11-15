package com.olp.dto;

import java.time.LocalDateTime;

/**
 * Response payload confirming recorded student consent for exam monitoring (DR-06, NFR-25, FR-I26).
 */
public class ExamConsentResponse {

    private Integer examId;
    private Integer studentId;
    private Boolean consentGiven;
    private LocalDateTime consentGivenAt;
    private Boolean blocked;
    private String message;
    private String warning;

    public ExamConsentResponse() {}

    public ExamConsentResponse(Integer examId, Integer studentId, Boolean consentGiven,
                               LocalDateTime consentGivenAt, Boolean blocked, String message, String warning) {
        this.examId = examId;
        this.studentId = studentId;
        this.consentGiven = consentGiven;
        this.consentGivenAt = consentGivenAt;
        this.blocked = blocked;
        this.message = message;
        this.warning = warning;
    }

    public Integer getExamId() {
        return examId;
    }

    public void setExamId(Integer examId) {
        this.examId = examId;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Boolean getConsentGiven() {
        return consentGiven;
    }

    public void setConsentGiven(Boolean consentGiven) {
        this.consentGiven = consentGiven;
    }

    public LocalDateTime getConsentGivenAt() {
        return consentGivenAt;
    }

    public void setConsentGivenAt(LocalDateTime consentGivenAt) {
        this.consentGivenAt = consentGivenAt;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}
