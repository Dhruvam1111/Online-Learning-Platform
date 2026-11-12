package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing structured prerequisites for a course (FR-I05).
 * Can be another course or a skill/knowledge requirement.
 * Designed to be reusable for student enrollment eligibility checks.
 */
@Entity
@Table(name = "course_prerequisites")
public class CoursePrerequisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "prerequisite_type", nullable = false, length = 20)
    private String prerequisiteType = "SKILL"; // 'COURSE' or 'SKILL'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_course_id")
    private Course requiredCourse;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CoursePrerequisite() {}

    public CoursePrerequisite(Course course, String prerequisiteType, Course requiredCourse, String title, String description) {
        this.course = course;
        this.prerequisiteType = prerequisiteType != null ? prerequisiteType.toUpperCase() : "SKILL";
        this.requiredCourse = requiredCourse;
        this.title = title;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.prerequisiteType == null) {
            this.prerequisiteType = "SKILL";
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

    public String getPrerequisiteType() {
        return prerequisiteType;
    }

    public void setPrerequisiteType(String prerequisiteType) {
        this.prerequisiteType = prerequisiteType != null ? prerequisiteType.toUpperCase() : "SKILL";
    }

    public Course getRequiredCourse() {
        return requiredCourse;
    }

    public void setRequiredCourse(Course requiredCourse) {
        this.requiredCourse = requiredCourse;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

