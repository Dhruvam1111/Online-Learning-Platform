package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an earned badge awarded to a student (FR-S68, FR-S69).
 */
@Entity
@Table(name = "student_badges", uniqueConstraints = {
        @UniqueConstraint(name = "uq_student_badge", columnNames = {"student_id", "badge_id"})
})
public class StudentBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "awarded_at", nullable = false, updatable = false)
    private LocalDateTime awardedAt;

    public StudentBadge() {}

    public StudentBadge(User student, Badge badge) {
        this.student = student;
        this.badge = badge;
        this.awardedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.awardedAt == null) {
            this.awardedAt = LocalDateTime.now();
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

    public Badge getBadge() {
        return badge;
    }

    public void setBadge(Badge badge) {
        this.badge = badge;
    }

    public LocalDateTime getAwardedAt() {
        return awardedAt;
    }

    public void setAwardedAt(LocalDateTime awardedAt) {
        this.awardedAt = awardedAt;
    }
}
