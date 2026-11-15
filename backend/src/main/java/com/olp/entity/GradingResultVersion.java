package com.olp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable entity representing a versioned grading result / grade override (FR-I32, DR-01, DR-ED04).
 * Preserves complete audit trail of previous marks, new marks, justification, actor, and timestamp.
 */
@Entity
@Table(name = "grading_result_versions")
public class GradingResultVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id")
    private QuizAttemptAnswer answer;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Column(name = "previous_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal previousMarks;

    @Column(name = "new_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal newMarks;

    @Column(name = "max_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal maxMarks;

    @Column(name = "justification", nullable = false, columnDefinition = "TEXT")
    private String justification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @Column(name = "status_before", length = 30)
    private String statusBefore;

    @Column(name = "status_after", length = 30)
    private String statusAfter;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public GradingResultVersion() {}

    public GradingResultVersion(QuizAttempt attempt, QuizAttemptAnswer answer, Integer versionNumber,
                                BigDecimal previousMarks, BigDecimal newMarks, BigDecimal maxMarks,
                                String justification, User instructor, String statusBefore,
                                String statusAfter, LocalDateTime createdAt) {
        this.attempt = attempt;
        this.answer = answer;
        this.versionNumber = versionNumber;
        this.previousMarks = previousMarks;
        this.newMarks = newMarks;
        this.maxMarks = maxMarks;
        this.justification = justification;
        this.instructor = instructor;
        this.statusBefore = statusBefore;
        this.statusAfter = statusAfter;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
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

    public QuizAttemptAnswer getAnswer() {
        return answer;
    }

    public void setAnswer(QuizAttemptAnswer answer) {
        this.answer = answer;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public BigDecimal getPreviousMarks() {
        return previousMarks;
    }

    public void setPreviousMarks(BigDecimal previousMarks) {
        this.previousMarks = previousMarks;
    }

    public BigDecimal getNewMarks() {
        return newMarks;
    }

    public void setNewMarks(BigDecimal newMarks) {
        this.newMarks = newMarks;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(BigDecimal maxMarks) {
        this.maxMarks = maxMarks;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public User getInstructor() {
        return instructor;
    }

    public void setInstructor(User instructor) {
        this.instructor = instructor;
    }

    public String getStatusBefore() {
        return statusBefore;
    }

    public void setStatusBefore(String statusBefore) {
        this.statusBefore = statusBefore;
    }

    public String getStatusAfter() {
        return statusAfter;
    }

    public void setStatusAfter(String statusAfter) {
        this.statusAfter = statusAfter;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
