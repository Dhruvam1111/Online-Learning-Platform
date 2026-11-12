package com.olp.dto;

import com.olp.entity.CourseSection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CourseSectionResponse {

    private Integer id;
    private Integer moduleId;
    private String title;
    private String description;
    private Integer orderIndex;
    private List<LessonResponse> lessons = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CourseSectionResponse() {}

    public CourseSectionResponse(Integer id, Integer moduleId, String title, String description,
                                 Integer orderIndex, List<LessonResponse> lessons,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.moduleId = moduleId;
        this.title = title;
        this.description = description;
        this.orderIndex = orderIndex;
        this.lessons = lessons != null ? lessons : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CourseSectionResponse fromEntity(CourseSection section, boolean includeLessons) {
        if (section == null) return null;

        List<LessonResponse> lessonResponses = new ArrayList<>();
        if (includeLessons && section.getLessons() != null) {
            lessonResponses = section.getLessons().stream()
                    .map(LessonResponse::fromEntity)
                    .toList();
        }

        return new CourseSectionResponse(
                section.getId(),
                section.getModule() != null ? section.getModule().getId() : null,
                section.getTitle(),
                section.getDescription(),
                section.getOrderIndex(),
                lessonResponses,
                section.getCreatedAt(),
                section.getUpdatedAt()
        );
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getModuleId() {
        return moduleId;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
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

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public List<LessonResponse> getLessons() {
        return lessons;
    }

    public void setLessons(List<LessonResponse> lessons) {
        this.lessons = lessons != null ? lessons : new ArrayList<>();
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

