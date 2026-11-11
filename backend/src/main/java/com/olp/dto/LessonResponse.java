package com.olp.dto;

import com.olp.entity.Lesson;
import java.time.LocalDateTime;

public class LessonResponse {

    private Integer id;
    private Integer sectionId;
    private String title;
    private String lessonType;
    private String content;
    private String contentUrl;
    private Integer durationMinutes;
    private Integer orderIndex;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LessonResponse() {}

    public LessonResponse(Integer id, Integer sectionId, String title, String lessonType,
                          String content, String contentUrl, Integer durationMinutes,
                          Integer orderIndex, String metadata, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.sectionId = sectionId;
        this.title = title;
        this.lessonType = lessonType;
        this.content = content;
        this.contentUrl = contentUrl;
        this.durationMinutes = durationMinutes;
        this.orderIndex = orderIndex;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static LessonResponse fromEntity(Lesson lesson) {
        if (lesson == null) return null;

        return new LessonResponse(
                lesson.getId(),
                lesson.getSection() != null ? lesson.getSection().getId() : null,
                lesson.getTitle(),
                lesson.getLessonType() != null ? lesson.getLessonType().name() : null,
                lesson.getContent(),
                lesson.getContentUrl(),
                lesson.getDurationMinutes(),
                lesson.getOrderIndex(),
                lesson.getMetadata(),
                lesson.getCreatedAt(),
                lesson.getUpdatedAt()
        );
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSectionId() {
        return sectionId;
    }

    public void setSectionId(Integer sectionId) {
        this.sectionId = sectionId;
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

