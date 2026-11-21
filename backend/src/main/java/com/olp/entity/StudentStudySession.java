package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** A student-owned, manually scheduled study activity (FR-S52). All timestamps are UTC. */
@Entity
@Table(name = "student_study_sessions")
public class StudentStudySession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "starts_at_utc", nullable = false)
    private LocalDateTime startsAtUtc;

    @Column(name = "ends_at_utc", nullable = false)
    private LocalDateTime endsAtUtc;

    @Column(name = "is_completed", nullable = false)
    private boolean completed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now(java.time.Clock.systemUTC());
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() { updatedAt = LocalDateTime.now(java.time.Clock.systemUTC()); }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getStartsAtUtc() { return startsAtUtc; }
    public void setStartsAtUtc(LocalDateTime startsAtUtc) { this.startsAtUtc = startsAtUtc; }
    public LocalDateTime getEndsAtUtc() { return endsAtUtc; }
    public void setEndsAtUtc(LocalDateTime endsAtUtc) { this.endsAtUtc = endsAtUtc; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
