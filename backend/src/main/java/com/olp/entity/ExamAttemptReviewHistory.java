package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Append-only immutable audit record of review decisions and escalations for an exam attempt (FR-I69, Step 56).
 */
@Entity
@Table(name = "exam_attempt_review_history")
public class ExamAttemptReviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @Column(name = "previous_status", length = 30)
    private String previousStatus;

    @Column(name = "new_status", length = 30, nullable = false)
    private String newStatus;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = 100)
    private String recommendation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", nullable = false)
    private User reviewedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ExamAttemptReviewHistory() {}

    public ExamAttemptReviewHistory(ExamAttempt attempt, String previousStatus, String newStatus,
                                    String action, String notes, String recommendation,
                                    User reviewedBy, LocalDateTime createdAt) {
        this.attempt = attempt;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.action = action;
        this.notes = notes;
        this.recommendation = recommendation;
        this.reviewedBy = reviewedBy;
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

    public ExamAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(ExamAttempt attempt) {
        this.attempt = attempt;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
