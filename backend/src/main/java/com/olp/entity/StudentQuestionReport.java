package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an individual student's report on an assessment question (FR-I35 Step 52, FR-S41).
 */
@Entity
@Table(
        name = "student_question_reports",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_student_question_report",
                        columnNames = {"reported_question_id", "student_id"}
                )
        }
)
public class StudentQuestionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_question_id", nullable = false)
    private ReportedQuestion reportedQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "attempt_id")
    private Integer attemptId;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    public StudentQuestionReport() {}

    public StudentQuestionReport(ReportedQuestion reportedQuestion, User student, Integer attemptId, String reason, String explanation) {
        this.reportedQuestion = reportedQuestion;
        this.student = student;
        this.attemptId = attemptId;
        this.reason = reason;
        this.explanation = explanation;
    }

    @PrePersist
    protected void onCreate() {
        if (this.reportedAt == null) {
            this.reportedAt = LocalDateTime.now();
        }
    }

    // ===== Getters and Setters =====

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ReportedQuestion getReportedQuestion() {
        return reportedQuestion;
    }

    public void setReportedQuestion(ReportedQuestion reportedQuestion) {
        this.reportedQuestion = reportedQuestion;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }
}
