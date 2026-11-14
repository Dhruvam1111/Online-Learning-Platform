package com.olp.dto;

import com.olp.entity.ExamAttemptReviewStatus;
import java.time.LocalDateTime;

/**
 * Summary DTO for instructor flagged exam attempts monitoring lists (FR-I69, Step 56).
 */
public class FlaggedAttemptSummaryResponse {

    private Integer attemptId;
    private Integer examId;
    private String examTitle;
    private Integer courseId;
    private Integer studentId;
    private String studentName;
    private Integer attemptNumber;
    private Integer tabSwitchCount;
    private Integer fullscreenExitCount;
    private String monitoringFlags;
    private ExamAttemptReviewStatus reviewStatus;
    private String reviewNotes;
    private Integer reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime escalatedAt;
    private String escalationRecommendation;

    public FlaggedAttemptSummaryResponse() {}

    public FlaggedAttemptSummaryResponse(Integer attemptId, Integer examId, String examTitle,
                                         Integer courseId, Integer studentId, String studentName,
                                         Integer attemptNumber, Integer tabSwitchCount,
                                         Integer fullscreenExitCount, String monitoringFlags,
                                         ExamAttemptReviewStatus reviewStatus, String reviewNotes,
                                         Integer reviewedById, String reviewedByName,
                                         LocalDateTime reviewedAt, LocalDateTime escalatedAt,
                                         String escalationRecommendation) {
        this.attemptId = attemptId;
        this.examId = examId;
        this.examTitle = examTitle;
        this.courseId = courseId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.attemptNumber = attemptNumber;
        this.tabSwitchCount = tabSwitchCount;
        this.fullscreenExitCount = fullscreenExitCount;
        this.monitoringFlags = monitoringFlags;
        this.reviewStatus = reviewStatus;
        this.reviewNotes = reviewNotes;
        this.reviewedById = reviewedById;
        this.reviewedByName = reviewedByName;
        this.reviewedAt = reviewedAt;
        this.escalatedAt = escalatedAt;
        this.escalationRecommendation = escalationRecommendation;
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

    public String getEscalationRecommendation() {
        return escalationRecommendation;
    }

    public void setEscalationRecommendation(String escalationRecommendation) {
        this.escalationRecommendation = escalationRecommendation;
    }
}
