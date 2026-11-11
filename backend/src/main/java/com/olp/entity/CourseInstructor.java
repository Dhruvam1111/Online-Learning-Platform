package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an instructor associated with a course (FR-I05, FR-I58, FR-I59).
 * Supports OWNER (course creator) and CO_INSTRUCTOR roles.
 */
@Entity
@Table(name = "course_instructors", uniqueConstraints = {
        @UniqueConstraint(name = "uq_course_instructor", columnNames = {"course_id", "instructor_id"})
})
public class CourseInstructor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @Convert(converter = CourseInstructorRoleConverter.class)
    @Column(nullable = false, length = 50)
    private CourseInstructorRole role = CourseInstructorRole.OWNER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CourseInstructor() {}

    public CourseInstructor(Course course, User instructor, CourseInstructorRole role) {
        this.course = course;
        this.instructor = instructor;
        this.role = role != null ? role : CourseInstructorRole.OWNER;
    }

    public CourseInstructor(Course course, User instructor, String role) {
        this(course, instructor,
                (role != null && !role.trim().isEmpty())
                        ? CourseInstructorRole.fromString(role)
                        : CourseInstructorRole.OWNER);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.role == null) {
            this.role = CourseInstructorRole.OWNER;
        }
    }

    public boolean isOwner() {
        return this.role == CourseInstructorRole.OWNER;
    }

    public boolean isCoInstructor() {
        return this.role == CourseInstructorRole.CO_INSTRUCTOR;
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

    public User getInstructor() {
        return instructor;
    }

    public void setInstructor(User instructor) {
        this.instructor = instructor;
    }

    public CourseInstructorRole getRole() {
        return role;
    }

    public void setRole(CourseInstructorRole role) {
        this.role = role;
    }

    public void setRole(String role) {
        this.role = role != null ? CourseInstructorRole.fromString(role) : null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

