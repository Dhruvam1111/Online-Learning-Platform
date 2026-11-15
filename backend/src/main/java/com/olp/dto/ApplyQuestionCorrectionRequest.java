package com.olp.dto;

import com.olp.entity.QuestionCorrectionPolicy;

/**
 * Request payload for applying approved question corrections to results (FR-I37, Step 54).
 */
public class ApplyQuestionCorrectionRequest {

    private QuestionCorrectionPolicy policy;
    private String justification;
    private Boolean notifyStudents = Boolean.TRUE;

    public ApplyQuestionCorrectionRequest() {}

    public ApplyQuestionCorrectionRequest(QuestionCorrectionPolicy policy, String justification) {
        this.policy = policy;
        this.justification = justification;
        this.notifyStudents = Boolean.TRUE;
    }

    public ApplyQuestionCorrectionRequest(QuestionCorrectionPolicy policy, String justification, Boolean notifyStudents) {
        this.policy = policy;
        this.justification = justification;
        this.notifyStudents = notifyStudents != null ? notifyStudents : Boolean.TRUE;
    }

    public QuestionCorrectionPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(QuestionCorrectionPolicy policy) {
        this.policy = policy;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public Boolean getNotifyStudents() {
        return notifyStudents;
    }

    public void setNotifyStudents(Boolean notifyStudents) {
        this.notifyStudents = notifyStudents;
    }
}
