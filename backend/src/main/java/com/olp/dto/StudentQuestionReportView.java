package com.olp.dto;

import com.olp.entity.AssessmentType;
import com.olp.entity.ReportedQuestionStatus;
import java.time.LocalDateTime;

/** Only information belonging to the authenticated student's own report. */
public record StudentQuestionReportView(
        Integer reportId,
        Integer reportedQuestionId,
        AssessmentType assessmentType,
        Integer assessmentId,
        Integer questionId,
        Integer attemptId,
        String reason,
        String explanation,
        LocalDateTime reportedAt,
        ReportedQuestionStatus status,
        String resolutionAction,
        String resolutionComment,
        LocalDateTime resolvedAt,
        boolean correctionApplied,
        LocalDateTime correctionAppliedAt
) {
    public boolean resolved() {
        return status == ReportedQuestionStatus.RESOLVED_CORRECTED
                || status == ReportedQuestionStatus.RESOLVED_INVALIDATED
                || status == ReportedQuestionStatus.DISMISSED;
    }
}
