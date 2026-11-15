package com.olp.dto;

import com.olp.entity.ExamAttemptReviewStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Detailed review response for an exam attempt including history, flags, and marks (DR-07, FR-I69, Step 56).
 */
public class ExamAttemptReviewResponse {

    private Integer attemptId;
    private Integer examId;
    private String examTitle;
    private Integer courseId;
    private Integer studentId;
    private String studentName;
    private Integer attemptNumber;
    private ExamAttemptReviewStatus reviewStatus;
    private String reviewNotes;
    private Integer reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime escalatedAt;
    private String escalationReason;
    private String escalationRecommendation;
    private Integer tabSwitchCount;
    private Integer fullscreenExitCount;
    private String monitoringFlags;
    private BigDecimal obtainedMarks;
    private BigDecimal totalMarks;
    private String message;
    private List<ExamAttemptReviewHistoryResponse> history = new ArrayList<>();

    public ExamAttemptReviewResponse() {}

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

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
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

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public ExamAttemptReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(ExamAttemptReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public Integer getReviewedById() {
        return reviewedById;
    }

    public void setReviewedById(Integer reviewedById) {
        this.reviewedById = reviewedById;
    }

    public String getReviewedByName() {
        return reviewedByName;
    }

    public void setReviewedByName(String reviewedByName) {
        this.reviewedByName = reviewedByName;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }

    public String getEscalationReason() {
        return escalationReason;
    }

    public void setEscalationReason(String escalationReason) {
        this.escalationReason = escalationReason;
    }

    public String getEscalationRecommendation() {
        return escalationRecommendation;
    }

    public void setEscalationRecommendation(String escalationRecommendation) {
        this.escalationRecommendation = escalationRecommendation;
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

    public String getMonitoringFlags() {
        return monitoringFlags;
    }

    public void setMonitoringFlags(String monitoringFlags) {
        this.monitoringFlags = monitoringFlags;
    }

    public BigDecimal getObtainedMarks() {
        return obtainedMarks;
    }

    public void setObtainedMarks(BigDecimal obtainedMarks) {
        this.obtainedMarks = obtainedMarks;
    }

    public BigDecimal getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(BigDecimal totalMarks) {
        this.totalMarks = totalMarks;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ExamAttemptReviewHistoryResponse> getHistory() {
        return history;
    }

    public void setHistory(List<ExamAttemptReviewHistoryResponse> history) {
        this.history = history != null ? history : new ArrayList<>();
    }
}
