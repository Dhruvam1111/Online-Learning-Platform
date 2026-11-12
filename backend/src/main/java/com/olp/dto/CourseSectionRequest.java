package com.olp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CourseSectionRequest {

    @NotBlank(message = "Section title is required")
    @Size(max = 200, message = "Section title must be 200 characters or fewer")
    private String title;

    private String description;

    private Integer orderIndex;

    public CourseSectionRequest() {}

    public CourseSectionRequest(String title) {
        this.title = title;
    }

    public CourseSectionRequest(String title, String description, Integer orderIndex) {
        this.title = title;
        this.description = description;
        this.orderIndex = orderIndex;
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
}

