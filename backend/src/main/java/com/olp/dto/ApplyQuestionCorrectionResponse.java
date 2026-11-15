package com.olp.dto;

import com.olp.entity.AssessmentType;
import com.olp.entity.QuestionCorrectionPolicy;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response payload returning the results of applying an approved question correction (FR-I37, Step 54).
 */
public class ApplyQuestionCorrectionResponse {

    private Integer reportedQuestionId;
    private AssessmentType assessmentType;
    private Integer assessmentId;
    private Integer questionId;
    private Integer courseId;
    private QuestionCorrectionPolicy policy;
    private boolean alreadyApplied;
    private int affectedAttemptsCount;
    private LocalDateTime appliedAt;
    private Integer appliedById;
    private String appliedByName;
    private String message;
    private List<AttemptCorrectionSummaryResponse> attemptResults;

    public ApplyQuestionCorrectionResponse() {}

    public Integer getReportedQuestionId() {
        return reportedQuestionId;
    }

    public void setReportedQuestionId(Integer reportedQuestionId) {
        this.reportedQuestionId = reportedQuestionId;
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

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public QuestionCorrectionPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(QuestionCorrectionPolicy policy) {
        this.policy = policy;
    }

    public boolean isAlreadyApplied() {
        return alreadyApplied;
    }

    public void setAlreadyApplied(boolean alreadyApplied) {
        this.alreadyApplied = alreadyApplied;
    }

    public int getAffectedAttemptsCount() {
        return affectedAttemptsCount;
    }

    public void setAffectedAttemptsCount(int affectedAttemptsCount) {
        this.affectedAttemptsCount = affectedAttemptsCount;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public Integer getAppliedById() {
        return appliedById;
    }

    public void setAppliedById(Integer appliedById) {
        this.appliedById = appliedById;
    }

    public String getAppliedByName() {
        return appliedByName;
    }

    public void setAppliedByName(String appliedByName) {
        this.appliedByName = appliedByName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<AttemptCorrectionSummaryResponse> getAttemptResults() {
        return attemptResults;
    }

    public void setAttemptResults(List<AttemptCorrectionSummaryResponse> attemptResults) {
        this.attemptResults = attemptResults;
    }
}
