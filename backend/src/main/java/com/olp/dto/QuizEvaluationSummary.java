package com.olp.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Summary DTO containing the complete auto-evaluation results of a quiz attempt (FR-I22).
 */
public class QuizEvaluationSummary {

    private Integer quizId;
    private Integer attemptId;
    private BigDecimal totalAwardedMarks = BigDecimal.ZERO;
    private BigDecimal totalMaxMarks = BigDecimal.ZERO;
    private BigDecimal passingMarks;
    private Boolean passed;
    private int correctCount;
    private int incorrectCount;
    private int partiallyCorrectCount;
    private int manualGradingCount;
    private int unansweredCount;
    private List<QuestionEvaluationResult> results = new ArrayList<>();

    public QuizEvaluationSummary() {}

    public Integer getQuizId() {
        return quizId;
    }

    public void setQuizId(Integer quizId) {
        this.quizId = quizId;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public BigDecimal getTotalAwardedMarks() {
        return totalAwardedMarks;
    }

    public void setTotalAwardedMarks(BigDecimal totalAwardedMarks) {
        this.totalAwardedMarks = totalAwardedMarks;
    }

    public BigDecimal getTotalMaxMarks() {
        return totalMaxMarks;
    }

    public void setTotalMaxMarks(BigDecimal totalMaxMarks) {
        this.totalMaxMarks = totalMaxMarks;
    }

    public BigDecimal getPassingMarks() {
        return passingMarks;
    }

    public void setPassingMarks(BigDecimal passingMarks) {
        this.passingMarks = passingMarks;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public int getIncorrectCount() {
        return incorrectCount;
    }

    public void setIncorrectCount(int incorrectCount) {
        this.incorrectCount = incorrectCount;
    }

    public int getPartiallyCorrectCount() {
        return partiallyCorrectCount;
    }

    public void setPartiallyCorrectCount(int partiallyCorrectCount) {
        this.partiallyCorrectCount = partiallyCorrectCount;
    }

    public int getManualGradingCount() {
        return manualGradingCount;
    }

    public void setManualGradingCount(int manualGradingCount) {
        this.manualGradingCount = manualGradingCount;
    }

    public int getUnansweredCount() {
        return unansweredCount;
    }

    public void setUnansweredCount(int unansweredCount) {
        this.unansweredCount = unansweredCount;
    }

    public List<QuestionEvaluationResult> getResults() {
        return results;
    }

    public void setResults(List<QuestionEvaluationResult> results) {
        this.results = results != null ? results : new ArrayList<>();
    }
}

