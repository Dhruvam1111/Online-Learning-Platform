package com.olp.dto;

import com.olp.entity.CoursePrerequisite;
import java.time.LocalDateTime;

public class CoursePrerequisiteResponse {

    private Integer id;
    private Integer courseId;
    private String type;
    private Integer requiredCourseId;
    private String title;
    private String description;
    private LocalDateTime createdAt;

    public CoursePrerequisiteResponse() {}

    public CoursePrerequisiteResponse(Integer id, Integer courseId, String type, Integer requiredCourseId,
                                    String title, String description, LocalDateTime createdAt) {
        this.id = id;
        this.courseId = courseId;
        this.type = type;
        this.requiredCourseId = requiredCourseId;
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static CoursePrerequisiteResponse fromEntity(CoursePrerequisite cp) {
        if (cp == null) {
            return null;
        }
        return new CoursePrerequisiteResponse(
                cp.getId(),
                cp.getCourse() != null ? cp.getCourse().getId() : null,
                cp.getPrerequisiteType(),
                cp.getRequiredCourse() != null ? cp.getRequiredCourse().getId() : null,
                cp.getTitle(),
                cp.getDescription(),
                cp.getCreatedAt()
        );
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getRequiredCourseId() {
        return requiredCourseId;
    }

    public void setRequiredCourseId(Integer requiredCourseId) {
        this.requiredCourseId = requiredCourseId;
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

