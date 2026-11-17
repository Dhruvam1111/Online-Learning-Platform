package com.olp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A persisted answer, unique per exam attempt and question. */
@Entity
@Table(name = "exam_attempt_answers", uniqueConstraints = @UniqueConstraint(
        name = "uq_exam_attempt_answer", columnNames = {"attempt_id", "question_id"}))
public class ExamAttemptAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;
    @Column(name = "marks_awarded", precision = 8, scale = 2)
    private BigDecimal marksAwarded;
    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;
    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    public ExamAttemptAnswer() {}
    public ExamAttemptAnswer(ExamAttempt attempt, Question question, String answerText, LocalDateTime savedAt) {
        this.attempt = attempt; this.question = question; this.answerText = answerText; this.savedAt = savedAt;
    }
    @PrePersist @PreUpdate
    public void ensureSavedAt() { if (savedAt == null) savedAt = LocalDateTime.now(); }
    public Integer getId() { return id; }
    public ExamAttempt getAttempt() { return attempt; }
    public Question getQuestion() { return question; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String text) { this.answerText = text; }
    public BigDecimal getMarksAwarded() { return marksAwarded; }
    public void setMarksAwarded(BigDecimal marks) { this.marksAwarded = marks; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public LocalDateTime getSavedAt() { return savedAt; }
    public void setSavedAt(LocalDateTime at) { this.savedAt = at; }
}
