package com.olp.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Event notifying course instructors of a reported question with review link and reason (FR-I35 Step 52).
 */
public class QuestionReportNotificationEvent extends ApplicationEvent {

    private final Integer instructorId;
    private final Integer reportedQuestionId;
    private final Integer courseId;
    private final String courseTitle;
    private final String assessmentTitle;
    private final String reason;
    private final String reviewLink;
    private final LocalDateTime sentAt;

    public QuestionReportNotificationEvent(Object source,
                                           Integer instructorId,
                                           Integer reportedQuestionId,
                                           Integer courseId,
                                           String courseTitle,
                                           String assessmentTitle,
                                           String reason,
                                           String reviewLink,
                                           LocalDateTime sentAt) {
        super(source);
        this.instructorId = instructorId;
        this.reportedQuestionId = reportedQuestionId;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.assessmentTitle = assessmentTitle;
        this.reason = reason;
        this.reviewLink = reviewLink;
        this.sentAt = sentAt;
    }

    public Integer getInstructorId() {
        return instructorId;
    }

    public Integer getReportedQuestionId() {
        return reportedQuestionId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public String getAssessmentTitle() {
        return assessmentTitle;
    }

    public String getReason() {
        return reason;
    }

    public String getReviewLink() {
        return reviewLink;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
