package com.olp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing an exam blueprint rule for generating randomized exam sets (FR-I27, Step 39).
 * Defines target topic, difficulty tier, question quota, and mark weighting.
 */
@Entity
@Table(name = "exam_blueprints")
public class ExamBlueprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(length = 100)
    private String topic;

    @Column(length = 30)
    private String difficulty;

    @Column(name = "question_count", nullable = false)
    private Integer questionCount = 1;

    @Column(name = "marks_per_question", precision = 8, scale = 2, nullable = false)
    private BigDecimal marksPerQuestion = BigDecimal.ONE;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ExamBlueprint() {}

    public ExamBlueprint(Exam exam, String topic, String difficulty,
                         Integer questionCount, BigDecimal marksPerQuestion, Integer orderIndex) {
        this.exam = exam;
        this.topic = topic;
        this.difficulty = difficulty != null ? difficulty.toUpperCase() : null;
        this.questionCount = questionCount != null ? questionCount : 1;
        this.marksPerQuestion = marksPerQuestion != null ? marksPerQuestion : BigDecimal.ONE;
        this.orderIndex = orderIndex != null ? orderIndex : 1;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.questionCount == null) {
            this.questionCount = 1;
        }
        if (this.marksPerQuestion == null) {
            this.marksPerQuestion = BigDecimal.ONE;
        }
        if (this.orderIndex == null) {
            this.orderIndex = 1;
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
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
        this.difficulty = difficulty != null ? difficulty.toUpperCase() : null;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public BigDecimal getMarksPerQuestion() {
        return marksPerQuestion;
    }

    public void setMarksPerQuestion(BigDecimal marksPerQuestion) {
        this.marksPerQuestion = marksPerQuestion;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
