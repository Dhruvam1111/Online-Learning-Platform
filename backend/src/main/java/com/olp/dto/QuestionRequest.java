package com.olp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating and updating independent, reusable Questions (FR-I21).
 * Supports MULTIPLE_CHOICE, MULTIPLE_SELECT, TRUE_FALSE, NUMERICAL, and DESCRIPTIVE.
 */
public class QuestionRequest {

    private Integer courseId;

    @NotBlank(message = "Question text is required")
    private String questionText;

    @NotBlank(message = "Question type is required")
    private String questionType;

    private List<String> options;
    private String optionsJson;

    private String correctAnswer;
    private List<String> correctAnswers;

    @JsonAlias("maxMarks")
    @DecimalMin(value = "0.0", message = "Marks cannot be negative")
    private BigDecimal defaultMarks;

    @DecimalMin(value = "0.0", message = "Negative marks cannot be negative")
    private BigDecimal negativeMarks;

    @JsonAlias("tolerance")
    @DecimalMin(value = "0.0", message = "Numerical tolerance cannot be negative")
    private BigDecimal numericalTolerance;

    @JsonAlias("rubric")
    private String gradingRubric;

    private Boolean requiresManualGrading;

    @Size(max = 100, message = "Topic must not exceed 100 characters")
    private String topic;

    @Size(max = 30, message = "Difficulty must not exceed 30 characters")
    private String difficulty;

    private String explanation;
    private Boolean isBankQuestion;
    private String origin;
    private String tags;

    public QuestionRequest() {}

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
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

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
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

    public List<String> getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(List<String> correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public BigDecimal getDefaultMarks() {
        return defaultMarks;
    }

    public void setDefaultMarks(BigDecimal defaultMarks) {
        this.defaultMarks = defaultMarks;
    }

    public BigDecimal getMaxMarks() {
        return defaultMarks;
    }

    public void setMaxMarks(BigDecimal maxMarks) {
        this.defaultMarks = maxMarks;
    }

    public BigDecimal getNegativeMarks() {
        return negativeMarks;
    }

    public void setNegativeMarks(BigDecimal negativeMarks) {
        this.negativeMarks = negativeMarks;
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

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}

