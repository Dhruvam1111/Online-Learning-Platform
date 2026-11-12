package com.olp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO to attach an existing question (via questionId) or create and attach
 * a new question inside a Quiz, with ordering and optional per-quiz marking overrides (FR-I19).
 */
public class QuizQuestionRequest {

    private Integer questionId;

    private String questionText;
    private String questionType;
    private String optionsJson;
    private String correctAnswer;

    @DecimalMin(value = "0.0", message = "Default marks cannot be negative")
    private BigDecimal defaultMarks;

    @DecimalMin(value = "0.0", message = "Marks cannot be negative")
    private BigDecimal marks;

    @DecimalMin(value = "0.0", message = "Negative marks cannot be negative")
    private BigDecimal negativeMarks;

    @Size(max = 100, message = "Topic must not exceed 100 characters")
    private String topic;

    @Size(max = 30, message = "Difficulty must not exceed 30 characters")
    private String difficulty;

    private String explanation;
    private Boolean isBankQuestion;

    @Min(value = 1, message = "Order index must be at least 1")
    private Integer orderIndex;

    private Boolean isRequired;

    public QuizQuestionRequest() {}

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

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public BigDecimal getDefaultMarks() {
        return defaultMarks;
    }

    public void setDefaultMarks(BigDecimal defaultMarks) {
        this.defaultMarks = defaultMarks;
    }

    public BigDecimal getMarks() {
        return marks;
    }

    public void setMarks(BigDecimal marks) {
        this.marks = marks;
    }

    public BigDecimal getNegativeMarks() {
        return negativeMarks;
    }

    public void setNegativeMarks(BigDecimal negativeMarks) {
        this.negativeMarks = negativeMarks;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Boolean getIsBankQuestion() {
        return isBankQuestion;
    }

    public void setIsBankQuestion(Boolean isBankQuestion) {
        this.isBankQuestion = isBankQuestion;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }
}

