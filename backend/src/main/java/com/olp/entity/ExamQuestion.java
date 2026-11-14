package com.olp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entity linking an exam to an immutable version of a Question in the Question Bank (FR-I25, Step 38).
 */
@Entity
@Table(name = "exam_questions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_exam_question_order", columnNames = {"exam_id", "order_index"})
})
public class ExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "marks", precision = 8, scale = 2, nullable = false)
    private BigDecimal marks = BigDecimal.ONE;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 1;

    public ExamQuestion() {}

    public ExamQuestion(Exam exam, Question question, Integer version, BigDecimal marks, Integer orderIndex) {
        this.exam = exam;
        this.question = question;
        this.version = version != null ? version : 1;
        this.marks = marks != null ? marks : BigDecimal.ONE;
        this.orderIndex = orderIndex != null ? orderIndex : 1;
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

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public BigDecimal getMarks() {
        return marks;
    }

    public void setMarks(BigDecimal marks) {
        this.marks = marks;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}

