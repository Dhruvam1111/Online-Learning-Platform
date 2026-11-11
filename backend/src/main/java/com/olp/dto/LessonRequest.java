package com.olp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LessonRequest {

    @NotBlank(message = "Lesson title is required")
    @Size(max = 200, message = "Lesson title must be 200 characters or fewer")
    private String title;

    private String lessonType; // 'VIDEO', 'MATERIAL', 'QUIZ', 'ASSIGNMENT'

    private String content;

    @Size(max = 500, message = "Content URL must be 500 characters or fewer")
    private String contentUrl;

    private Integer durationMinutes;

    private Integer orderIndex;

    private String metadata;

    public LessonRequest() {}

    public LessonRequest(String title, String lessonType) {
        this.title = title;
        this.lessonType = lessonType;
    }

    public LessonRequest(String title, String lessonType, String content, String contentUrl,
                         Integer durationMinutes, Integer orderIndex, String metadata) {
        this.title = title;
        this.lessonType = lessonType;
        this.content = content;
        this.contentUrl = contentUrl;
        this.durationMinutes = durationMinutes;
        this.orderIndex = orderIndex;
        this.metadata = metadata;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLessonType() {
        return lessonType;
    }

    public void setLessonType(String lessonType) {
        this.lessonType = lessonType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}

