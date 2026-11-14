package com.olp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity storing an evaluated answer for a question within a student's quiz attempt (FR-I22).
 */
@Entity
@Table(
        name = "quiz_attempt_answers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_quiz_attempt_answers_attempt_question",
                        columnNames = {"attempt_id", "question_id"})
        }
)
public class QuizAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "submitted_answer", columnDefinition = "TEXT")
    private String submittedAnswer;

    @Column(name = "awarded_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal awardedMarks = BigDecimal.ZERO;

    @Column(name = "max_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal maxMarks = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_status", nullable = false, length = 30)
    private EvaluationStatus evaluationStatus = EvaluationStatus.UNANSWERED;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grader_id")
    private User grader;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public QuizAttemptAnswer() {}

    public QuizAttemptAnswer(QuizAttempt attempt, Question question, String submittedAnswer,
                             BigDecimal awardedMarks, BigDecimal maxMarks, EvaluationStatus evaluationStatus,
                             String feedback, LocalDateTime evaluatedAt) {
        this.attempt = attempt;
        this.question = question;
        this.submittedAnswer = submittedAnswer;
        this.awardedMarks = awardedMarks != null ? awardedMarks : BigDecimal.ZERO;
        this.maxMarks = maxMarks != null ? maxMarks : BigDecimal.ZERO;
        this.evaluationStatus = evaluationStatus != null ? evaluationStatus : EvaluationStatus.UNANSWERED;
        this.feedback = feedback;
        this.evaluatedAt = evaluatedAt != null ? evaluatedAt : LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.awardedMarks == null) {
            this.awardedMarks = BigDecimal.ZERO;
        }
        if (this.maxMarks == null) {
            this.maxMarks = BigDecimal.ZERO;
        }
        if (this.evaluationStatus == null) {
            this.evaluationStatus = EvaluationStatus.UNANSWERED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public QuizAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(QuizAttempt attempt) {
        this.attempt = attempt;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public String getSubmittedAnswer() {
        return submittedAnswer;
    }

    public void setSubmittedAnswer(String submittedAnswer) {
        this.submittedAnswer = submittedAnswer;
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

    public EvaluationStatus getEvaluationStatus() {
        return evaluationStatus;
    }

    public void setEvaluationStatus(EvaluationStatus evaluationStatus) {
        this.evaluationStatus = evaluationStatus;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public User getGrader() {
        return grader;
    }

    public void setGrader(User grader) {
        this.grader = grader;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

