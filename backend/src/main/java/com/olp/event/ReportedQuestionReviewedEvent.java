package com.olp.event;

import com.olp.entity.AssessmentType;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Spring ApplicationEvent dispatched when an instructor acts on a reported question (FR-I36 Step 53).
 */
public class ReportedQuestionReviewedEvent extends ApplicationEvent {

    private final Integer reportedQuestionId;
    private final AssessmentType assessmentType;
    private final Integer assessmentId;
    private final Integer questionId;
    private final Integer courseId;
    private final String action;
    private final String comment;
    private final Integer resolvedById;
    private final LocalDateTime resolvedAt;
    private final long affectedAttemptsCount;

    public ReportedQuestionReviewedEvent(Object source,
                                        Integer reportedQuestionId,
                                        AssessmentType assessmentType,
                                        Integer assessmentId,
                                        Integer questionId,
                                        Integer courseId,
                                        String action,
                                        String comment,
                                        Integer resolvedById,
                                        LocalDateTime resolvedAt,
                                        long affectedAttemptsCount) {
        super(source);
        this.reportedQuestionId = reportedQuestionId;
        this.assessmentType = assessmentType;
        this.assessmentId = assessmentId;
        this.questionId = questionId;
        this.courseId = courseId;
        this.action = action;
        this.comment = comment;
        this.resolvedById = resolvedById;
        this.resolvedAt = resolvedAt;
        this.affectedAttemptsCount = affectedAttemptsCount;
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

    public String getAction() {
        return action;
    }

    public String getComment() {
        return comment;
    }

    public Integer getResolvedById() {
        return resolvedById;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public long getAffectedAttemptsCount() {
        return affectedAttemptsCount;
    }
}
