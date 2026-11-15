package com.olp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO returning eligibility status and typed denial reason for starting an examination (DR-ED07, FR-I25).
 */
public class ExamEligibilityResponse {

    private Integer examId;
    private Integer studentId;
    private Boolean isEligible;
    private String denialReason; // e.g. NOT_ENROLLED, BELOW_MIN_PROGRESS, PREREQUISITE_UNMET, BEFORE_START_WINDOW, AFTER_END_WINDOW, MAX_ATTEMPTS_EXHAUSTED
    private String denialMessage;
    private BigDecimal studentProgressPercentage;
    private BigDecimal requiredProgressPercentage;
    private Integer attemptsUsed;
    private Integer maxAttempts;
    private LocalDateTime startWindow;
    private LocalDateTime endWindow;
    private LocalDateTime serverTime;

    public ExamEligibilityResponse() {}

    public static ExamEligibilityResponse eligible(Integer examId, Integer studentId,
                                                    BigDecimal studentProgress, BigDecimal requiredProgress,
                                                    Integer attemptsUsed, Integer maxAttempts,
                                                    LocalDateTime startWindow, LocalDateTime endWindow,
                                                    LocalDateTime serverTime) {
        ExamEligibilityResponse res = new ExamEligibilityResponse();
        res.setExamId(examId);
        res.setStudentId(studentId);
        res.setIsEligible(true);
        res.setStudentProgressPercentage(studentProgress);
        res.setRequiredProgressPercentage(requiredProgress);
        res.setAttemptsUsed(attemptsUsed);
        res.setMaxAttempts(maxAttempts);
        res.setStartWindow(startWindow);
        res.setEndWindow(endWindow);
        res.setServerTime(serverTime);
        return res;
    }

    public static ExamEligibilityResponse denied(Integer examId, Integer studentId,
                                                  String reason, String message,
                                                  BigDecimal studentProgress, BigDecimal requiredProgress,
                                                  Integer attemptsUsed, Integer maxAttempts,
                                                  LocalDateTime startWindow, LocalDateTime endWindow,
                                                  LocalDateTime serverTime) {
        ExamEligibilityResponse res = new ExamEligibilityResponse();
        res.setExamId(examId);
        res.setStudentId(studentId);
        res.setIsEligible(false);
        res.setDenialReason(reason);
        res.setDenialMessage(message);
        res.setStudentProgressPercentage(studentProgress);
        res.setRequiredProgressPercentage(requiredProgress);
        res.setAttemptsUsed(attemptsUsed);
        res.setMaxAttempts(maxAttempts);
        res.setStartWindow(startWindow);
        res.setEndWindow(endWindow);
        res.setServerTime(serverTime);
        return res;
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

    public Boolean getIsEligible() {
        return isEligible;
    }

    public void setIsEligible(Boolean eligible) {
        isEligible = eligible;
    }

    public String getDenialReason() {
        return denialReason;
    }

    public void setDenialReason(String denialReason) {
        this.denialReason = denialReason;
    }

    public String getDenialMessage() {
        return denialMessage;
    }

    public void setDenialMessage(String denialMessage) {
        this.denialMessage = denialMessage;
    }

    public BigDecimal getStudentProgressPercentage() {
        return studentProgressPercentage;
    }

    public void setStudentProgressPercentage(BigDecimal studentProgressPercentage) {
        this.studentProgressPercentage = studentProgressPercentage;
    }

    public BigDecimal getRequiredProgressPercentage() {
        return requiredProgressPercentage;
    }

    public void setRequiredProgressPercentage(BigDecimal requiredProgressPercentage) {
        this.requiredProgressPercentage = requiredProgressPercentage;
    }

    public Integer getAttemptsUsed() {
        return attemptsUsed;
    }

    public void setAttemptsUsed(Integer attemptsUsed) {
        this.attemptsUsed = attemptsUsed;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public LocalDateTime getStartWindow() {
        return startWindow;
    }

    public void setStartWindow(LocalDateTime startWindow) {
        this.startWindow = startWindow;
    }

    public LocalDateTime getEndWindow() {
        return endWindow;
    }

    public void setEndWindow(LocalDateTime endWindow) {
        this.endWindow = endWindow;
    }

    public LocalDateTime getServerTime() {
        return serverTime;
    }

    public void setServerTime(LocalDateTime serverTime) {
        this.serverTime = serverTime;
    }
}

