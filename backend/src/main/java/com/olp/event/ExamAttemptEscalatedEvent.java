package com.olp.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Spring ApplicationEvent dispatched when an instructor escalates a flagged exam attempt to administration (FR-I69, FR-A19, Step 56).
 */
public class ExamAttemptEscalatedEvent extends ApplicationEvent {

    private final Integer attemptId;
    private final Integer examId;
    private final String examTitle;
    private final Integer courseId;
    private final Integer studentId;
    private final String reason;
    private final String recommendation;
    private final Integer escalatedById;
    private final LocalDateTime escalatedAt;

    public ExamAttemptEscalatedEvent(Object source,
                                    Integer attemptId,
                                    Integer examId,
                                    String examTitle,
                                    Integer courseId,
                                    Integer studentId,
                                    String reason,
                                    String recommendation,
                                    Integer escalatedById,
                                    LocalDateTime escalatedAt) {
        super(source);
        this.attemptId = attemptId;
        this.examId = examId;
        this.examTitle = examTitle;
        this.courseId = courseId;
        this.studentId = studentId;
        this.reason = reason;
        this.recommendation = recommendation;
        this.escalatedById = escalatedById;
        this.escalatedAt = escalatedAt;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public Integer getExamId() {
        return examId;
    }

    public String getExamTitle() {
        return examTitle;
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

    public String getRecommendation() {
        return recommendation;
    }

    public Integer getEscalatedById() {
        return escalatedById;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }
}
