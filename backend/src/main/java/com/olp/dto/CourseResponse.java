package com.olp.dto;

import com.olp.entity.Course;
import com.olp.entity.CourseStatus;
import java.time.LocalDateTime;

public class CourseResponse {

    private Integer id;
    private Integer instructorId;
    private String title;
    private String description;
    private CourseStatus status;
    private LocalDateTime createdAt;

    public CourseResponse() {}

    public CourseResponse(Integer id, Integer instructorId, String title, String description,
                          CourseStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.instructorId = instructorId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static CourseResponse fromEntity(Course course) {
        if (course == null) {
            return null;
        }
        return new CourseResponse(
                course.getId(),
                course.getInstructor() != null ? course.getInstructor().getId() : null,
                course.getTitle(),
                course.getDescription(),
                course.getStatus(),
                course.getCreatedAt()
        );
    }

    // ===== Getters and Setters =====

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Integer instructorId) {
        this.instructorId = instructorId;
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

    public CourseStatus getStatus() {
        return status;
    }

    public void setStatus(CourseStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
