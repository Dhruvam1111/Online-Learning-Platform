package com.olp.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Spring ApplicationEvent dispatched when an assignment is due or reminder triggered (FR-I68, Step 28).
 * Downstream listeners (e.g. Person 3's FR-I41 notification service) consume this event to alert learners.
 */
public class AssignmentDueEvent extends ApplicationEvent {

    private final Integer assignmentId;
    private final Integer courseId;
    private final String title;
    private final LocalDateTime deadlineUtc;
    private final Long hoursUntilDeadline;

    public AssignmentDueEvent(Object source, Integer assignmentId, Integer courseId,
                              String title, LocalDateTime deadlineUtc, Long hoursUntilDeadline) {
        super(source);
        this.assignmentId = assignmentId;
        this.courseId = courseId;
        this.title = title;
        this.deadlineUtc = deadlineUtc;
        this.hoursUntilDeadline = hoursUntilDeadline;
    }

    public Integer getAssignmentId() {
        return assignmentId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getDeadlineUtc() {
        return deadlineUtc;
    }

    public Long getHoursUntilDeadline() {
        return hoursUntilDeadline;
    }
}

