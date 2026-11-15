package com.olp.dto;

import com.olp.entity.AssessmentType;

/**
 * Response payload showing count of attempts that would be affected by reviewing a question (FR-I36, Step 53).
 */
public class AffectedAttemptsCountResponse {

    private Integer reportedQuestionId;
    private AssessmentType assessmentType;
    private Integer assessmentId;
    private Integer questionId;
    private long affectedAttemptsCount;

    public AffectedAttemptsCountResponse() {}

    public AffectedAttemptsCountResponse(Integer reportedQuestionId, AssessmentType assessmentType,
                                        Integer assessmentId, Integer questionId, long affectedAttemptsCount) {
        this.reportedQuestionId = reportedQuestionId;
        this.assessmentType = assessmentType;
        this.assessmentId = assessmentId;
        this.questionId = questionId;
        this.affectedAttemptsCount = affectedAttemptsCount;
    }

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

    public long getAffectedAttemptsCount() {
        return affectedAttemptsCount;
    }

    public void setAffectedAttemptsCount(long affectedAttemptsCount) {
        this.affectedAttemptsCount = affectedAttemptsCount;
    }
}
