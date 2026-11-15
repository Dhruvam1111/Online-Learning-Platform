package com.olp.dto;

import com.olp.entity.AssessmentType;
import com.olp.entity.ReportedQuestionStatus;

import java.time.LocalDateTime;

/**
 * Inbox item representation for instructor reported questions list (FR-I35 Step 52).
 */
public class ReportedQuestionInboxResponse {

    private Integer id;
    private AssessmentType assessmentType;
    private Integer assessmentId;
    private String assessmentTitle;
    private Integer questionId;
    private String questionText;
    private Integer courseId;
    private String courseTitle;
    private Integer reportCount;
    private ReportedQuestionStatus status;
    private String latestReason;
    private String latestExplanation;
    private LocalDateTime firstReportedAt;
    private LocalDateTime lastReportedAt;
    private LocalDateTime escalateAt;
    private Boolean escalatedToAdmin;
    private LocalDateTime escalatedAt;
    private String reviewLink;

    public ReportedQuestionInboxResponse() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AssessmentType getAssessmentType() {
        return assessmentType;
    }

    public void setAssessmentType(AssessmentType assessmentType) {
        this.assessmentType = assessmentType;
    }

    public Integer getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(Integer assessmentId) {
        this.assessmentId = assessmentId;
    }

    public String getAssessmentTitle() {
        return assessmentTitle;
    }

    public void setAssessmentTitle(String assessmentTitle) {
        this.assessmentTitle = assessmentTitle;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public Integer getReportCount() {
        return reportCount;
    }

    public void setReportCount(Integer reportCount) {
        this.reportCount = reportCount;
    }

    public ReportedQuestionStatus getStatus() {
        return status;
    }

    public void setStatus(ReportedQuestionStatus status) {
        this.status = status;
    }

    public String getLatestReason() {
        return latestReason;
    }

    public void setLatestReason(String latestReason) {
        this.latestReason = latestReason;
    }

    public String getLatestExplanation() {
        return latestExplanation;
    }

    public void setLatestExplanation(String latestExplanation) {
        this.latestExplanation = latestExplanation;
    }

    public LocalDateTime getFirstReportedAt() {
        return firstReportedAt;
    }

    public void setFirstReportedAt(LocalDateTime firstReportedAt) {
        this.firstReportedAt = firstReportedAt;
    }

    public LocalDateTime getLastReportedAt() {
        return lastReportedAt;
    }

    public void setLastReportedAt(LocalDateTime lastReportedAt) {
        this.lastReportedAt = lastReportedAt;
    }

    public LocalDateTime getEscalateAt() {
        return escalateAt;
    }

    public void setEscalateAt(LocalDateTime escalateAt) {
        this.escalateAt = escalateAt;
    }

    public Boolean getEscalatedToAdmin() {
        return escalatedToAdmin;
    }

    public void setEscalatedToAdmin(Boolean escalatedToAdmin) {
        this.escalatedToAdmin = escalatedToAdmin;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }

    public String getReviewLink() {
        return reviewLink;
    }

    public void setReviewLink(String reviewLink) {
        this.reviewLink = reviewLink;
    }
}
