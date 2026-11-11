package com.olp.dto;

import jakarta.validation.constraints.Size;

public class CoursePrerequisiteRequest {

    private String type; // 'COURSE' or 'SKILL'

    private Integer requiredCourseId;

    @Size(max = 200, message = "Prerequisite title must be 200 characters or fewer")
    private String title;

    @Size(max = 500, message = "Prerequisite description must be 500 characters or fewer")
    private String description;

    public CoursePrerequisiteRequest() {}

    public CoursePrerequisiteRequest(String type, String title) {
        this.type = type;
        this.title = title;
    }

    public CoursePrerequisiteRequest(String type, Integer requiredCourseId, String title, String description) {
        this.type = type;
        this.requiredCourseId = requiredCourseId;
        this.title = title;
        this.description = description;
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
}

