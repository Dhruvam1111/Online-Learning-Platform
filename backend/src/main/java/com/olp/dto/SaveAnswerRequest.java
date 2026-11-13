package com.olp.dto;

import jakarta.validation.constraints.NotNull;

public class SaveAnswerRequest {

    @NotNull(message = "Question ID is required")
    private Integer questionId;

    private String selectedAnswer;

    public SaveAnswerRequest() {}

    public SaveAnswerRequest(Integer questionId, String selectedAnswer) {
        this.questionId = questionId;
        this.selectedAnswer = selectedAnswer;
    }

    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }

    public String getSelectedAnswer() { return selectedAnswer; }
    public void setSelectedAnswer(String selectedAnswer) { this.selectedAnswer = selectedAnswer; }
}