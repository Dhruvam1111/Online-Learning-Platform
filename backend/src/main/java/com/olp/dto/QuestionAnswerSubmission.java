package com.olp.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing a submitted answer for a specific question inside an assessment attempt (FR-I22).
 */
public class QuestionAnswerSubmission {

    private Integer questionId;
    private String answer;
    private List<String> selectedOptions = new ArrayList<>();

    public QuestionAnswerSubmission() {}

    public QuestionAnswerSubmission(Integer questionId, String answer) {
        this.questionId = questionId;
        this.answer = answer;
    }

    public QuestionAnswerSubmission(Integer questionId, List<String> selectedOptions) {
        this.questionId = questionId;
        this.selectedOptions = selectedOptions != null ? selectedOptions : new ArrayList<>();
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<String> getSelectedOptions() {
        return selectedOptions;
    }

    public void setSelectedOptions(List<String> selectedOptions) {
        this.selectedOptions = selectedOptions != null ? selectedOptions : new ArrayList<>();
    }
}

