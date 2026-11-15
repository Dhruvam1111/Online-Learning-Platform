package com.olp.event;

import com.olp.entity.AssessmentType;
import com.olp.entity.QuestionCorrectionPolicy;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Spring ApplicationEvent dispatched for each individual student attempt modified by an approved question correction (FR-I37 Step 54).
 */
public class StudentScoreCorrectedEvent extends ApplicationEvent {

    private final Integer studentId;
    private final Integer courseId;
    private final AssessmentType assessmentType;
    private final Integer assessmentId;
    private final String assessmentTitle;
    private final Integer attemptId;
    private final BigDecimal oldScore;
    private final BigDecimal newScore;
    private final QuestionCorrectionPolicy policy;
    private final boolean certificateEligible;
    private final LocalDateTime correctedAt;

    public StudentScoreCorrectedEvent(Object source,
                                     Integer studentId,
                                     Integer courseId,
                                     AssessmentType assessmentType,
                                     Integer assessmentId,
                                     String assessmentTitle,
                                     Integer attemptId,
                                     BigDecimal oldScore,
                                     BigDecimal newScore,
                                     QuestionCorrectionPolicy policy,
                                     boolean certificateEligible,
                                     LocalDateTime correctedAt) {
        super(source);
        this.studentId = studentId;
        this.courseId = courseId;
        this.assessmentType = assessmentType;
        this.assessmentId = assessmentId;
        this.assessmentTitle = assessmentTitle;
        this.attemptId = attemptId;
        this.oldScore = oldScore;
        this.newScore = newScore;
        this.policy = policy;
        this.certificateEligible = certificateEligible;
        this.correctedAt = correctedAt;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public AssessmentType getAssessmentType() {
        return assessmentType;
    }

    public Integer getAssessmentId() {
        return assessmentId;
    }

    public String getAssessmentTitle() {
        return assessmentTitle;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public BigDecimal getOldScore() {
        return oldScore;
    }

    public BigDecimal getNewScore() {
        return newScore;
    }

    public QuestionCorrectionPolicy getPolicy() {
        return policy;
    }

    public boolean isCertificateEligible() {
        return certificateEligible;
    }

    public LocalDateTime getCorrectedAt() {
        return correctedAt;
    }
}
