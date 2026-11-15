package com.olp.dto;

import jakarta.validation.constraints.NotBlank;

public class CourseDiscussionRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    private String category = "GENERAL";

    private Integer lectureId;

    public CourseDiscussionRequest() {}

    public CourseDiscussionRequest(String title, String content, String category, Integer lectureId) {
        this.title = title;
        this.content = content;
        this.category = category != null ? category : "GENERAL";
        this.lectureId = lectureId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getLectureId() {
        return lectureId;
    }

    public void setLectureId(Integer lectureId) {
        this.lectureId = lectureId;
    }
}
