package com.olp.dto;

import com.olp.entity.QuizAttemptAnswer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for answer-level feedback comments (FR-I31).
 */
public class AnswerFeedbackResponse {

    private Integer answerId;
    private Integer attemptId;
    private Integer questionId;
    private String questionText;
    private String feedback;
    private Integer graderId;
    private String graderName;
    private LocalDateTime feedbackAt;
    private BigDecimal awardedMarks;
    private BigDecimal maxMarks;
    private String evaluationStatus;
    private boolean isPublished;
    private String message;

    public AnswerFeedbackResponse() {}

    public static AnswerFeedbackResponse fromEntity(QuizAttemptAnswer ans, String message) {
        if (ans == null) return null;
        AnswerFeedbackResponse res = new AnswerFeedbackResponse();
        res.setAnswerId(ans.getId());
        if (ans.getAttempt() != null) {
            res.setAttemptId(ans.getAttempt().getId());
        }
        if (ans.getQuestion() != null) {
            res.setQuestionId(ans.getQuestion().getId());
            res.setQuestionText(ans.getQuestion().getQuestionText());
        }
        res.setFeedback(ans.getFeedback());
        if (ans.getGrader() != null) {
            res.setGraderId(ans.getGrader().getId());
            res.setGraderName(ans.getGrader().getName());
        }
        res.setFeedbackAt(ans.getFeedbackAt());
        res.setAwardedMarks(ans.getAwardedMarks());
        res.setMaxMarks(ans.getMaxMarks());
        if (ans.getEvaluationStatus() != null) {
            res.setEvaluationStatus(ans.getEvaluationStatus().name());
            res.setPublished("PUBLISHED".equals(ans.getEvaluationStatus().name()));
        }
        res.setMessage(message);
        return res;
    }

    public Integer getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Integer answerId) {
        this.answerId = answerId;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

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

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Integer getGraderId() {
        return graderId;
    }

    public void setGraderId(Integer graderId) {
        this.graderId = graderId;
    }

    public String getGraderName() {
        return graderName;
    }

    public void setGraderName(String graderName) {
        this.graderName = graderName;
    }

    public LocalDateTime getFeedbackAt() {
        return feedbackAt;
    }

    public void setFeedbackAt(LocalDateTime feedbackAt) {
        this.feedbackAt = feedbackAt;
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

    public String getEvaluationStatus() {
        return evaluationStatus;
    }

    public void setEvaluationStatus(String evaluationStatus) {
        this.evaluationStatus = evaluationStatus;
    }

    public boolean isPublished() {
        return isPublished;
    }

    public void setPublished(boolean published) {
        isPublished = published;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
