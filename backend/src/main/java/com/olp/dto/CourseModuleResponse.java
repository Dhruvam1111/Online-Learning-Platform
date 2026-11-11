package com.olp.dto;

import com.olp.entity.CourseModule;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CourseModuleResponse {

    private Integer id;
    private Integer courseId;
    private String title;
    private String description;
    private Integer orderIndex;
    private List<CourseSectionResponse> sections = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CourseModuleResponse() {}

    public CourseModuleResponse(Integer id, Integer courseId, String title, String description,
                                Integer orderIndex, List<CourseSectionResponse> sections,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.orderIndex = orderIndex;
        this.sections = sections != null ? sections : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CourseModuleResponse fromEntity(CourseModule module, boolean includeSections) {
        if (module == null) return null;

        List<CourseSectionResponse> sectionResponses = new ArrayList<>();
        if (includeSections && module.getSections() != null) {
            sectionResponses = module.getSections().stream()
                    .map(s -> CourseSectionResponse.fromEntity(s, true))
                    .toList();
        }

        return new CourseModuleResponse(
                module.getId(),
                module.getCourse() != null ? module.getCourse().getId() : null,
                module.getTitle(),
                module.getDescription(),
                module.getOrderIndex(),
                sectionResponses,
                module.getCreatedAt(),
                module.getUpdatedAt()
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

    public List<CourseSectionResponse> getSections() {
        return sections;
    }

    public void setSections(List<CourseSectionResponse> sections) {
        this.sections = sections != null ? sections : new ArrayList<>();
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

