package com.olp.dto;

import java.util.List;

/**
 * Request DTO to test/evaluate an individual answer against a Question or QuizQuestion (FR-I22).
 */
public class QuestionEvaluationRequest {

    private String submittedAnswer;
    private List<String> selectedOptions;
    private GradingPolicy policy;

    public QuestionEvaluationRequest() {}

    public QuestionEvaluationRequest(String submittedAnswer) {
        this.submittedAnswer = submittedAnswer;
    }

    public QuestionEvaluationRequest(List<String> selectedOptions) {
        this.selectedOptions = selectedOptions;
    }

    public String getSubmittedAnswer() {
        return submittedAnswer;
    }

    public void setSubmittedAnswer(String submittedAnswer) {
        this.submittedAnswer = submittedAnswer;
    }

    public List<String> getSelectedOptions() {
        return selectedOptions;
    }

    public void setSelectedOptions(List<String> selectedOptions) {
        this.selectedOptions = selectedOptions;
    }

    public GradingPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(GradingPolicy policy) {
        this.policy = policy;
    }
}

