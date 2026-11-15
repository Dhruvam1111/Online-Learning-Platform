package com.olp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DiscussionQuestionRequest {

    @NotBlank(message = "Question title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Question content is required")
    private String content;

    private Integer lectureId;

    public DiscussionQuestionRequest() {
    }

    public DiscussionQuestionRequest(String title, String content, Integer lectureId) {
        this.title = title;
        this.content = content;
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

    public Integer getLectureId() {
        return lectureId;
    }

    public void setLectureId(Integer lectureId) {
        this.lectureId = lectureId;
    }
}

