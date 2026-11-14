package com.olp.dto;

import com.olp.entity.EvaluationStatus;
import com.olp.entity.QuestionType;

import java.math.BigDecimal;

/**
 * Result DTO for an evaluated question answer (FR-I22).
 * Returns awarded marks, maximum marks, evaluation status, and grading details.
 */
public class QuestionEvaluationResult {

    private Integer questionId;
    private QuestionType questionType;
    private EvaluationStatus status;
    private BigDecimal awardedMarks;
    private BigDecimal maxMarks;
    private String submittedAnswer;
    private String correctAnswer;
    private BigDecimal tolerance;
    private String feedback;
    private boolean requiresManualGrading;

    public QuestionEvaluationResult() {}

    public QuestionEvaluationResult(Integer questionId,
                                    QuestionType questionType,
                                    EvaluationStatus status,
                                    BigDecimal awardedMarks,
                                    BigDecimal maxMarks,
                                    String submittedAnswer,
                                    String correctAnswer,
                                    BigDecimal tolerance,
                                    String feedback,
                                    boolean requiresManualGrading) {
        this.questionId = questionId;
        this.questionType = questionType;
        this.status = status;
        this.awardedMarks = awardedMarks != null ? awardedMarks : BigDecimal.ZERO;
        this.maxMarks = maxMarks != null ? maxMarks : BigDecimal.ZERO;
        this.submittedAnswer = submittedAnswer;
        this.correctAnswer = correctAnswer;
        this.tolerance = tolerance;
        this.feedback = feedback;
        this.requiresManualGrading = requiresManualGrading;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public void setStatus(EvaluationStatus status) {
        this.status = status;
    }

    public BigDecimal getAwardedMarks() {
        return awardedMarks;
    }

    public void setAwardedMarks(BigDecimal awardedMarks) {
        this.awardedMarks = awardedMarks;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(BigDecimal maxMarks) {
        this.maxMarks = maxMarks;
    }

    public String getSubmittedAnswer() {
        return submittedAnswer;
    }

    public void setSubmittedAnswer(String submittedAnswer) {
        this.submittedAnswer = submittedAnswer;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public BigDecimal getTolerance() {
        return tolerance;
    }

    public void setTolerance(BigDecimal tolerance) {
        this.tolerance = tolerance;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public boolean isRequiresManualGrading() {
        return requiresManualGrading;
    }

    public void setRequiresManualGrading(boolean requiresManualGrading) {
        this.requiresManualGrading = requiresManualGrading;
    }

    public void redactCorrectAnswer() {
        this.correctAnswer = null;
    }

    public void redactFeedback() {
        this.feedback = null;
    }
}

