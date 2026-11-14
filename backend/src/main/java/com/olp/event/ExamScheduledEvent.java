package com.olp.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Spring ApplicationEvent dispatched when a new examination is scheduled (FR-I25, Step 38).
 * Downstream notification listeners (Person 3 / FR-I41) consume this event to notify learners.
 */
public class ExamScheduledEvent extends ApplicationEvent {

    private final Integer examId;
    private final Integer courseId;
    private final String title;
    private final LocalDateTime startWindowUtc;
    private final LocalDateTime endWindowUtc;
    private final Integer durationMinutes;

    public ExamScheduledEvent(Object source, Integer examId, Integer courseId, String title,
                              LocalDateTime startWindowUtc, LocalDateTime endWindowUtc,
                              Integer durationMinutes) {
        super(source);
        this.examId = examId;
        this.courseId = courseId;
        this.title = title;
        this.startWindowUtc = startWindowUtc;
        this.endWindowUtc = endWindowUtc;
        this.durationMinutes = durationMinutes;
    }

    public Integer getExamId() {
        return examId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getStartWindowUtc() {
        return startWindowUtc;
    }

    public LocalDateTime getEndWindowUtc() {
        return endWindowUtc;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }
}

