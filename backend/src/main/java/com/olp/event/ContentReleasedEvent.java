package com.olp.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Spring ApplicationEvent dispatched when scheduled course content is released (FR-I18, Step 20).
 * Downstream listeners (e.g. Person 3's FR-I41 notification service) consume this event to alert learners.
 */
public class ContentReleasedEvent extends ApplicationEvent {

    private final Integer contentId;
    private final String contentType; // "MATERIAL" or "LESSON"
    private final String title;
    private final Integer courseId;
    private final Integer lessonId;
    private final LocalDateTime releasedAt;

    public ContentReleasedEvent(Object source, Integer contentId, String contentType,
                                String title, Integer courseId, Integer lessonId, LocalDateTime releasedAt) {
        super(source);
        this.contentId = contentId;
        this.contentType = contentType;
        this.title = title;
        this.courseId = courseId;
        this.lessonId = lessonId;
        this.releasedAt = releasedAt;
    }

    public Integer getContentId() {
        return contentId;
    }

    public String getContentType() {
        return contentType;
    }

    public String getTitle() {
        return title;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public Integer getLessonId() {
        return lessonId;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }
}

