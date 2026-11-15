package com.olp.event;

import com.olp.entity.AssessmentType;
import com.olp.entity.QuestionCorrectionPolicy;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Spring ApplicationEvent dispatched when batch question corrections are applied across assessment attempts (FR-I37 Step 54).
 */
public class QuestionCorrectionAppliedEvent extends ApplicationEvent {

    private final Integer reportedQuestionId;
    private final AssessmentType assessmentType;
    private final Integer assessmentId;
    private final Integer questionId;
    private final Integer courseId;
    private final QuestionCorrectionPolicy policy;
    private final int affectedAttemptsCount;
    private final Integer appliedById;
    private final LocalDateTime appliedAt;

    public QuestionCorrectionAppliedEvent(Object source,
                                         Integer reportedQuestionId,
                                         AssessmentType assessmentType,
                                         Integer assessmentId,
                                         Integer questionId,
                                         Integer courseId,
                                         QuestionCorrectionPolicy policy,
                                         int affectedAttemptsCount,
                                         Integer appliedById,
                                         LocalDateTime appliedAt) {
        super(source);
        this.reportedQuestionId = reportedQuestionId;
        this.assessmentType = assessmentType;
        this.assessmentId = assessmentId;
        this.questionId = questionId;
        this.courseId = courseId;
        this.policy = policy;
        this.affectedAttemptsCount = affectedAttemptsCount;
        this.appliedById = appliedById;
        this.appliedAt = appliedAt;
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

    public QuestionCorrectionPolicy getPolicy() {
        return policy;
    }

    public int getAffectedAttemptsCount() {
        return affectedAttemptsCount;
    }

    public Integer getAppliedById() {
        return appliedById;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }
}
