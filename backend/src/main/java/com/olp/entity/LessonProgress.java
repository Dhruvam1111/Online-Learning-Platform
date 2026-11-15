package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity tracking student completion status on hierarchical course lessons (FR-I56, FR-I06).
 */
@Entity
@Table(name = "lesson_progress", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "lesson_id"})
})
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "lesson_id", nullable = false)
    private Integer lessonId;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt = LocalDateTime.now();

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = true;

    public LessonProgress() {}

    public LessonProgress(Integer studentId, Integer lessonId) {
        this.studentId = studentId;
        this.lessonId = lessonId;
        this.completedAt = LocalDateTime.now();
        this.isCompleted = true;
    }

    public LessonProgress(Integer studentId, Integer lessonId, boolean isCompleted) {
        this.studentId = studentId;
        this.lessonId = lessonId;
        this.completedAt = LocalDateTime.now();
        this.isCompleted = isCompleted;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Integer getLessonId() {
        return lessonId;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}
