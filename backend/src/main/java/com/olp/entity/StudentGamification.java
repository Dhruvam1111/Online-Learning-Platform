package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing student gamification state: points, learning streaks, and level (FR-S66, FR-S67, FR-S69).
 */
@Entity
@Table(name = "student_gamification")
public class StudentGamification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private User student;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;

    @Column(name = "current_streak_days", nullable = false)
    private Integer currentStreakDays = 0;

    @Column(name = "longest_streak_days", nullable = false)
    private Integer longestStreakDays = 0;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StudentGamification() {}

    public StudentGamification(User student) {
        this.student = student;
        this.totalPoints = 0;
        this.currentStreakDays = 0;
        this.longestStreakDays = 0;
        this.level = 1;
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
        if (this.totalPoints == null) {
            this.totalPoints = 0;
        }
        if (this.currentStreakDays == null) {
            this.currentStreakDays = 0;
        }
        if (this.longestStreakDays == null) {
            this.longestStreakDays = 0;
        }
        if (this.level == null) {
            this.level = 1;
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

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints != null ? totalPoints : 0;
    }

    public Integer getCurrentStreakDays() {
        return currentStreakDays;
    }

    public void setCurrentStreakDays(Integer currentStreakDays) {
        this.currentStreakDays = currentStreakDays != null ? currentStreakDays : 0;
    }

    public Integer getLongestStreakDays() {
        return longestStreakDays;
    }

    public void setLongestStreakDays(Integer longestStreakDays) {
        this.longestStreakDays = longestStreakDays != null ? longestStreakDays : 0;
    }

    public LocalDate getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(LocalDate lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level != null ? level : 1;
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
