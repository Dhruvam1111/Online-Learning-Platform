package com.olp.event;

import com.olp.entity.AssessmentType;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Spring ApplicationEvent dispatched when a student reports a question in an assessment (FR-I35 Step 52, FR-S41).
 */
public class QuestionReportedEvent extends ApplicationEvent {

    private final Integer reportedQuestionId;
    private final AssessmentType assessmentType;
    private final Integer assessmentId;
    private final Integer questionId;
    private final Integer courseId;
    private final Integer studentId;
    private final String reason;
    private final Integer reportCount;
    private final LocalDateTime reportedAt;

    public QuestionReportedEvent(Object source,
                                 Integer reportedQuestionId,
                                 AssessmentType assessmentType,
                                 Integer assessmentId,
                                 Integer questionId,
                                 Integer courseId,
                                 Integer studentId,
                                 String reason,
                                 Integer reportCount,
                                 LocalDateTime reportedAt) {
        super(source);
        this.reportedQuestionId = reportedQuestionId;
        this.assessmentType = assessmentType;
        this.assessmentId = assessmentId;
        this.questionId = questionId;
        this.courseId = courseId;
        this.studentId = studentId;
        this.reason = reason;
        this.reportCount = reportCount;
        this.reportedAt = reportedAt;
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

    public Integer getStudentId() {
        return studentId;
    }

    public String getReason() {
        return reason;
    }

    public Integer getReportCount() {
        return reportCount;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }
}
