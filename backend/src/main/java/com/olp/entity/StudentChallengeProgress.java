package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing student progress toward an active learning challenge (FR-S70).
 */
@Entity
@Table(name = "student_challenge_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uq_student_challenge", columnNames = {"student_id", "challenge_id"})
})
public class StudentChallengeProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private LearningChallenge challenge;

    @Column(name = "current_progress", nullable = false)
    private Integer currentProgress = 0;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public StudentChallengeProgress() {}

    public StudentChallengeProgress(User student, LearningChallenge challenge) {
        this.student = student;
        this.challenge = challenge;
        this.currentProgress = 0;
        this.isCompleted = false;
    }

    @PrePersist
    protected void onCreate() {
        if (this.currentProgress == null) {
            this.currentProgress = 0;
        }
        if (this.isCompleted == null) {
            this.isCompleted = false;
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public LearningChallenge getChallenge() {
        return challenge;
    }

    public void setChallenge(LearningChallenge challenge) {
        this.challenge = challenge;
    }

    public Integer getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(Integer currentProgress) {
        this.currentProgress = currentProgress != null ? currentProgress : 0;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
