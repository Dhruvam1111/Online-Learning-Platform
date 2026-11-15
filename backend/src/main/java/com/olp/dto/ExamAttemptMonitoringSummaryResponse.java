package com.olp.dto;

import java.time.LocalDateTime;

public class ExamAttemptMonitoringSummaryResponse {

    private Integer attemptId;
    private Integer examId;
    private String examTitle;
    private Integer studentId;
    private String studentName;
    private String studentEmail;
    private Integer attemptNumber;
    private String status;
    private String reviewStatus;
    private Boolean isFlagged;
    private Integer tabSwitchCount;
    private Integer fullscreenExitCount;
    private Long totalEventsCount;
    private String highestSeverity;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    public ExamAttemptMonitoringSummaryResponse() {}

    public ExamAttemptMonitoringSummaryResponse(Integer attemptId, Integer examId, String examTitle,
                                                Integer studentId, String studentName, String studentEmail,
                                                Integer attemptNumber, String status, String reviewStatus,
                                                Boolean isFlagged, Integer tabSwitchCount, Integer fullscreenExitCount,
                                                Long totalEventsCount, String highestSeverity,
                                                LocalDateTime startedAt, LocalDateTime submittedAt) {
        this.attemptId = attemptId;
        this.examId = examId;
        this.examTitle = examTitle;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.reviewStatus = reviewStatus;
        this.isFlagged = isFlagged;
        this.tabSwitchCount = tabSwitchCount;
        this.fullscreenExitCount = fullscreenExitCount;
        this.totalEventsCount = totalEventsCount;
        this.highestSeverity = highestSeverity;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer getExamId() {
        return examId;
    }

    public void setExamId(Integer examId) {
        this.examId = examId;
    }

    public String getExamTitle() {
        return examTitle;
    }

    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
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

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Boolean getIsFlagged() {
        return isFlagged;
    }

    public void setIsFlagged(Boolean isFlagged) {
        this.isFlagged = isFlagged;
    }

    public Integer getTabSwitchCount() {
        return tabSwitchCount;
    }

    public void setTabSwitchCount(Integer tabSwitchCount) {
        this.tabSwitchCount = tabSwitchCount;
    }

    public Integer getFullscreenExitCount() {
        return fullscreenExitCount;
    }

    public void setFullscreenExitCount(Integer fullscreenExitCount) {
        this.fullscreenExitCount = fullscreenExitCount;
    }

    public Long getTotalEventsCount() {
        return totalEventsCount;
    }

    public void setTotalEventsCount(Long totalEventsCount) {
        this.totalEventsCount = totalEventsCount;
    }

    public String getHighestSeverity() {
        return highestSeverity;
    }

    public void setHighestSeverity(String highestSeverity) {
        this.highestSeverity = highestSeverity;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
