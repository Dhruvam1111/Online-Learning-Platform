package com.olp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO representing an ordered Question inside a Quiz (FR-I19).
 */
public class QuizQuestionResponse {

    private Integer id;
    private Integer quizId;
    private Integer questionId;
    private Integer orderIndex;
    private BigDecimal marks;
    private BigDecimal effectiveMarks;
    private BigDecimal negativeMarks;
    private BigDecimal effectiveNegativeMarks;
    private Boolean isRequired;
    private String questionText;
    private String questionType;
    private String optionsJson;
    private List<String> options;
    private String correctAnswer;
    private List<String> correctAnswers;
    private BigDecimal defaultMarks;
    private BigDecimal maxMarks;
    private BigDecimal numericalTolerance;
    private String gradingRubric;
    private Boolean requiresManualGrading;
    private String topic;
    private String difficulty;
    private String explanation;
    private Boolean isBankQuestion;
    private LocalDateTime createdAt;

    public QuizQuestionResponse() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getQuizId() {
        return quizId;
    }

    public void setQuizId(Integer quizId) {
        this.quizId = quizId;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public BigDecimal getMarks() {
        return marks;
    }

    public void setMarks(BigDecimal marks) {
        this.marks = marks;
    }

    public BigDecimal getEffectiveMarks() {
        return effectiveMarks;
    }

    public void setEffectiveMarks(BigDecimal effectiveMarks) {
        this.effectiveMarks = effectiveMarks;
    }

    public BigDecimal getNegativeMarks() {
        return negativeMarks;
    }

    public void setNegativeMarks(BigDecimal negativeMarks) {
        this.negativeMarks = negativeMarks;
    }

    public BigDecimal getEffectiveNegativeMarks() {
        return effectiveNegativeMarks;
    }

    public void setEffectiveNegativeMarks(BigDecimal effectiveNegativeMarks) {
        this.effectiveNegativeMarks = effectiveNegativeMarks;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
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
        this.maxMarks = defaultMarks;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public List<String> getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(List<String> correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks != null ? maxMarks : defaultMarks;
    }

    public void setMaxMarks(BigDecimal maxMarks) {
        this.maxMarks = maxMarks;
    }

    public BigDecimal getNumericalTolerance() {
        return numericalTolerance;
    }

    public void setNumericalTolerance(BigDecimal numericalTolerance) {
        this.numericalTolerance = numericalTolerance;
    }

    public String getGradingRubric() {
        return gradingRubric;
    }

    public void setGradingRubric(String gradingRubric) {
        this.gradingRubric = gradingRubric;
    }

    public Boolean getRequiresManualGrading() {
        return requiresManualGrading;
    }

    public void setRequiresManualGrading(Boolean requiresManualGrading) {
        this.requiresManualGrading = requiresManualGrading;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

