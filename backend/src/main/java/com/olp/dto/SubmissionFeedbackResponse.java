package com.olp.dto;

import com.olp.entity.QuizAttempt;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for submission-level overall feedback comments (FR-I31).
 */
public class SubmissionFeedbackResponse {

    private Integer attemptId;
    private Integer quizId;
    private String quizTitle;
    private Integer studentId;
    private String studentName;
    private String studentEmail;
    private String overallFeedback;
    private Integer graderId;
    private String graderName;
    private LocalDateTime feedbackAt;
    private boolean isPublished;
    private BigDecimal obtainedMarks;
    private BigDecimal totalMarks;
    private String message;

    public SubmissionFeedbackResponse() {}

    public static SubmissionFeedbackResponse fromEntity(QuizAttempt attempt, String message) {
        if (attempt == null) return null;
        SubmissionFeedbackResponse res = new SubmissionFeedbackResponse();
        res.setAttemptId(attempt.getId());
        if (attempt.getQuiz() != null) {
            res.setQuizId(attempt.getQuiz().getId());
            res.setQuizTitle(attempt.getQuiz().getTitle());
            res.setTotalMarks(attempt.getQuiz().getTotalMarks());
        }
        if (attempt.getStudent() != null) {
            res.setStudentId(attempt.getStudent().getId());
            res.setStudentName(attempt.getStudent().getName());
            res.setStudentEmail(attempt.getStudent().getEmail());
        }
        res.setOverallFeedback(attempt.getOverallFeedback());
        if (attempt.getGrader() != null) {
            res.setGraderId(attempt.getGrader().getId());
            res.setGraderName(attempt.getGrader().getName());
        }
        res.setFeedbackAt(attempt.getFeedbackAt());
        res.setPublished(attempt.isPublished());
        res.setObtainedMarks(attempt.getObtainedMarks());
        res.setMessage(message);
        return res;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer getQuizId() {
        return quizId;
    }

    public void setQuizId(Integer quizId) {
        this.quizId = quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getOverallFeedback() {
        return overallFeedback;
    }

    public void setOverallFeedback(String overallFeedback) {
        this.overallFeedback = overallFeedback;
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

    public boolean isPublished() {
        return isPublished;
    }

    public void setPublished(boolean published) {
        isPublished = published;
    }

    public BigDecimal getObtainedMarks() {
        return obtainedMarks;
    }

    public void setObtainedMarks(BigDecimal obtainedMarks) {
        this.obtainedMarks = obtainedMarks;
    }

    public BigDecimal getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(BigDecimal totalMarks) {
        this.totalMarks = totalMarks;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
