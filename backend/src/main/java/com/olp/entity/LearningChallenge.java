package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an active learning challenge for students (FR-S70).
 */
@Entity
@Table(name = "learning_challenges")
public class LearningChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "points_reward", nullable = false)
    private Integer pointsReward = 50;

    @Column(name = "target_type", length = 50, nullable = false)
    private String targetType;

    @Column(name = "target_count", nullable = false)
    private Integer targetCount = 5;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public LearningChallenge() {}

    public LearningChallenge(Course course, String title, String description, Integer pointsReward,
                             String targetType, Integer targetCount, LocalDateTime startDate,
                             LocalDateTime endDate, Boolean isActive) {
        this.course = course;
        this.title = title;
        this.description = description;
        this.pointsReward = pointsReward != null ? pointsReward : 50;
        this.targetType = targetType;
        this.targetCount = targetCount != null ? targetCount : 5;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive != null ? isActive : true;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.pointsReward == null) {
            this.pointsReward = 50;
        }
        if (this.targetCount == null) {
            this.targetCount = 5;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPointsReward() {
        return pointsReward;
    }

    public void setPointsReward(Integer pointsReward) {
        this.pointsReward = pointsReward;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Integer getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(Integer targetCount) {
        this.targetCount = targetCount;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
