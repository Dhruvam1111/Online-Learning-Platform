package com.olp.event;

import com.olp.entity.AssessmentType;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Spring ApplicationEvent dispatched when an unresolved reported question escalates to administration (FR-I35 Step 52, FR-A15).
 */
public class ReportedQuestionEscalatedEvent extends ApplicationEvent {

    private final Integer reportedQuestionId;
    private final AssessmentType assessmentType;
    private final Integer assessmentId;
    private final Integer questionId;
    private final Integer courseId;
    private final Integer reportCount;
    private final LocalDateTime firstReportedAt;
    private final LocalDateTime escalatedAt;

    public ReportedQuestionEscalatedEvent(Object source,
                                          Integer reportedQuestionId,
                                          AssessmentType assessmentType,
                                          Integer assessmentId,
                                          Integer questionId,
                                          Integer courseId,
                                          Integer reportCount,
                                          LocalDateTime firstReportedAt,
                                          LocalDateTime escalatedAt) {
        super(source);
        this.reportedQuestionId = reportedQuestionId;
        this.assessmentType = assessmentType;
        this.assessmentId = assessmentId;
        this.questionId = questionId;
        this.courseId = courseId;
        this.reportCount = reportCount;
        this.firstReportedAt = firstReportedAt;
        this.escalatedAt = escalatedAt;
    }

    public Integer getReportedQuestionId() {
        return reportedQuestionId;
    }

    public AssessmentType getAssessmentType() {
        return assessmentType;
    }

    public Integer getAssessmentId() {
        return assessmentId;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public Integer getReportCount() {
        return reportCount;
    }

    public LocalDateTime getFirstReportedAt() {
        return firstReportedAt;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }
}
