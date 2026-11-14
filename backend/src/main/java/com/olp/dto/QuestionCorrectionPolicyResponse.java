package com.olp.dto;

import com.olp.entity.QuestionCorrectionPolicy;

/**
 * Response DTO exposing question correction policy details and lock status (FR-I66, Step 41).
 * Read by FR-I37 (apply corrections to results).
 */
public class QuestionCorrectionPolicyResponse {

    private Integer examId;
    private String examTitle;
    private QuestionCorrectionPolicy policy;
    private String policyDescription;
    private Boolean isLocked;
    private Long attemptCount;
    private String message;

    public QuestionCorrectionPolicyResponse() {}

    public QuestionCorrectionPolicyResponse(Integer examId, String examTitle,
                                           QuestionCorrectionPolicy policy,
                                           Boolean isLocked, Long attemptCount,
                                           String message) {
        this.examId = examId;
        this.examTitle = examTitle;
        this.policy = policy;
        this.policyDescription = policy != null ? policy.getDescription() : null;
        this.isLocked = isLocked;
        this.attemptCount = attemptCount;
        this.message = message;
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

    public QuestionCorrectionPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(QuestionCorrectionPolicy policy) {
        this.policy = policy;
        this.policyDescription = policy != null ? policy.getDescription() : null;
    }

    public String getPolicyDescription() {
        return policyDescription;
    }

    public void setPolicyDescription(String policyDescription) {
        this.policyDescription = policyDescription;
    }

    public Boolean getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(Boolean isLocked) {
        this.isLocked = isLocked;
    }

    public Long getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Long attemptCount) {
        this.attemptCount = attemptCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
