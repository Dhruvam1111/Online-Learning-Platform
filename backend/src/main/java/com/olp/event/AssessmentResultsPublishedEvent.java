package com.olp.event;

import com.olp.entity.AssessmentType;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Spring ApplicationEvent dispatched when assessment results are published to students (FR-I34, Step 51).
 */
public class AssessmentResultsPublishedEvent extends ApplicationEvent {

    private final AssessmentType assessmentType;
    private final Integer assessmentId;
    private final String title;
    private final Integer courseId;
    private final int studentCount;
    private final LocalDateTime publishedAt;

    public AssessmentResultsPublishedEvent(Object source,
                                          AssessmentType assessmentType,
                                          Integer assessmentId,
                                          String title,
                                          Integer courseId,
                                          int studentCount,
                                          LocalDateTime publishedAt) {
        super(source);
        this.assessmentType = assessmentType;
        this.assessmentId = assessmentId;
        this.title = title;
        this.courseId = courseId;
        this.studentCount = studentCount;
        this.publishedAt = publishedAt;
    }

    public AssessmentType getAssessmentType() {
        return assessmentType;
    }

    public Integer getAssessmentId() {
        return assessmentId;
    }

    public String getTitle() {
        return title;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
}
