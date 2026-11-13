package com.olp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Entity storing certificate completion rules and criteria per course (FR-I56, DR-03).
 * Supports minimum lesson completion percentage, quiz/exam passing marks,
 * and mandatory curriculum components (lessons, quizzes).
 */
@Entity
@Table(name = "course_completion_criteria")
public class CourseCompletionCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false, unique = true)
    private Course course;

    @Column(name = "min_lesson_completion_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal minLessonCompletionPercentage = BigDecimal.valueOf(100.00);

    @Column(name = "min_quiz_passing_marks", precision = 8, scale = 2)
    private BigDecimal minQuizPassingMarks;

    @Column(name = "min_quiz_passing_percentage", precision = 5, scale = 2)
    private BigDecimal minQuizPassingPercentage;

    @Column(name = "require_all_quizzes_passed", nullable = false)
    private boolean requireAllQuizzesPassed = false;

    @Column(name = "mandatory_lesson_ids", columnDefinition = "TEXT")
    private String mandatoryLessonIds;

    @Column(name = "mandatory_quiz_ids", columnDefinition = "TEXT")
    private String mandatoryQuizIds;

    @Column(name = "is_locked", nullable = false)
    private boolean isLocked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CourseCompletionCriteria() {}

    public CourseCompletionCriteria(Course course) {
        this.course = course;
        this.minLessonCompletionPercentage = BigDecimal.valueOf(100.00);
        this.requireAllQuizzesPassed = false;
        this.isLocked = false;
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
        if (this.minLessonCompletionPercentage == null) {
            this.minLessonCompletionPercentage = BigDecimal.valueOf(100.00);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods for parsing IDs
    public List<Integer> getMandatoryLessonIdList() {
        return parseIds(this.mandatoryLessonIds);
    }

    public void setMandatoryLessonIdList(List<Integer> ids) {
        this.mandatoryLessonIds = serializeIds(ids);
    }

    public List<Integer> getMandatoryQuizIdList() {
        return parseIds(this.mandatoryQuizIds);
    }

    public void setMandatoryQuizIdList(List<Integer> ids) {
        this.mandatoryQuizIds = serializeIds(ids);
    }

    private static List<Integer> parseIds(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && s.matches("\\d+"))
                .map(Integer::valueOf)
                .distinct()
                .collect(Collectors.toList());
    }

    private static String serializeIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    // Getters and Setters

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

    public BigDecimal getMinLessonCompletionPercentage() {
        return minLessonCompletionPercentage;
    }

    public void setMinLessonCompletionPercentage(BigDecimal minLessonCompletionPercentage) {
        this.minLessonCompletionPercentage = minLessonCompletionPercentage;
    }

    public BigDecimal getMinQuizPassingMarks() {
        return minQuizPassingMarks;
    }

    public void setMinQuizPassingMarks(BigDecimal minQuizPassingMarks) {
        this.minQuizPassingMarks = minQuizPassingMarks;
    }

    public BigDecimal getMinQuizPassingPercentage() {
        return minQuizPassingPercentage;
    }

    public void setMinQuizPassingPercentage(BigDecimal minQuizPassingPercentage) {
        this.minQuizPassingPercentage = minQuizPassingPercentage;
    }

    public boolean isRequireAllQuizzesPassed() {
        return requireAllQuizzesPassed;
    }

    public void setRequireAllQuizzesPassed(boolean requireAllQuizzesPassed) {
        this.requireAllQuizzesPassed = requireAllQuizzesPassed;
    }

    public String getMandatoryLessonIds() {
        return mandatoryLessonIds;
    }

    public void setMandatoryLessonIds(String mandatoryLessonIds) {
        this.mandatoryLessonIds = mandatoryLessonIds;
    }

    public String getMandatoryQuizIds() {
        return mandatoryQuizIds;
    }

    public void setMandatoryQuizIds(String mandatoryQuizIds) {
        this.mandatoryQuizIds = mandatoryQuizIds;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
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
