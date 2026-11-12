package com.olp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mapping entity linking a Quiz to ordered Questions, with extensible per-quiz marking overrides
 * for FR-I19, FR-I21, FR-I22, and FR-I24.
 */
@Entity
@Table(
        name = "quiz_questions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_quiz_questions_quiz_question", columnNames = {"quiz_id", "question_id"})
        }
)
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 1;

    @Column(precision = 8, scale = 2)
    private BigDecimal marks;

    @Column(name = "negative_marks", precision = 8, scale = 2)
    private BigDecimal negativeMarks;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = Boolean.TRUE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public QuizQuestion() {}

    public QuizQuestion(Quiz quiz, Question question, Integer orderIndex,
                        BigDecimal marks, BigDecimal negativeMarks, Boolean isRequired) {
        this.quiz = quiz;
        this.question = question;
        this.orderIndex = orderIndex != null ? orderIndex : 1;
        this.marks = marks;
        this.negativeMarks = negativeMarks;
        this.isRequired = isRequired != null ? isRequired : Boolean.TRUE;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.orderIndex == null) {
            this.orderIndex = 1;
        }
        if (this.isRequired == null) {
            this.isRequired = Boolean.TRUE;
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
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

    public BigDecimal getNegativeMarks() {
        return negativeMarks;
    }

    public void setNegativeMarks(BigDecimal negativeMarks) {
        this.negativeMarks = negativeMarks;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

