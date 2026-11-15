package com.olp.dto;

import com.olp.entity.AssessmentType;
import java.time.LocalDateTime;

/**
 * Response DTO describing the result publication state and grading progress for an assessment (FR-I34, Step 51).
 */
public class AssessmentPublishStatusResponse {

    private AssessmentType assessmentType;
    private Integer assessmentId;
    private String assessmentTitle;
    private Integer courseId;
    private Boolean resultsPublished;
    private Boolean resultsWithheld;
    private LocalDateTime resultsPublishedAt;
    private long totalSubmissions;
    private long gradedCount;
    private long ungradedCount;
    private Boolean isLocked;
    private int studentsNotifiedCount;
    private String message;

    public AssessmentPublishStatusResponse() {
    }

    public AssessmentPublishStatusResponse(AssessmentType assessmentType,
                                           Integer assessmentId,
                                           String assessmentTitle,
                                           Integer courseId,
                                           Boolean resultsPublished,
                                           Boolean resultsWithheld,
                                           LocalDateTime resultsPublishedAt,
                                           long totalSubmissions,
                                           long gradedCount,
                                           long ungradedCount,
                                           Boolean isLocked,
                                           int studentsNotifiedCount,
                                           String message) {
        this.assessmentType = assessmentType;
        this.assessmentId = assessmentId;
        this.assessmentTitle = assessmentTitle;
        this.courseId = courseId;
        this.resultsPublished = resultsPublished;
        this.resultsWithheld = resultsWithheld;
        this.resultsPublishedAt = resultsPublishedAt;
        this.totalSubmissions = totalSubmissions;
        this.gradedCount = gradedCount;
        this.ungradedCount = ungradedCount;
        this.isLocked = isLocked;
        this.studentsNotifiedCount = studentsNotifiedCount;
        this.message = message;
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

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Boolean getResultsPublished() {
        return resultsPublished;
    }

    public void setResultsPublished(Boolean resultsPublished) {
        this.resultsPublished = resultsPublished;
    }

    public Boolean getResultsWithheld() {
        return resultsWithheld;
    }

    public void setResultsWithheld(Boolean resultsWithheld) {
        this.resultsWithheld = resultsWithheld;
    }

    public LocalDateTime getResultsPublishedAt() {
        return resultsPublishedAt;
    }

    public void setResultsPublishedAt(LocalDateTime resultsPublishedAt) {
        this.resultsPublishedAt = resultsPublishedAt;
    }

    public long getTotalSubmissions() {
        return totalSubmissions;
    }

    public void setTotalSubmissions(long totalSubmissions) {
        this.totalSubmissions = totalSubmissions;
    }

    public long getGradedCount() {
        return gradedCount;
    }

    public void setGradedCount(long gradedCount) {
        this.gradedCount = gradedCount;
    }

    public long getUngradedCount() {
        return ungradedCount;
    }

    public void setUngradedCount(long ungradedCount) {
        this.ungradedCount = ungradedCount;
    }

    public Boolean getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(Boolean isLocked) {
        this.isLocked = isLocked;
    }

    public int getStudentsNotifiedCount() {
        return studentsNotifiedCount;
    }

    public void setStudentsNotifiedCount(int studentsNotifiedCount) {
        this.studentsNotifiedCount = studentsNotifiedCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
