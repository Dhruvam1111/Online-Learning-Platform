package com.olp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class StudentExamDtos {
    private StudentExamDtos() {}
    public record ExamListItem(Integer examId, Integer courseId, String title, String state,
                               String instructions, LocalDateTime startsAt, LocalDateTime endsAt,
                               Integer durationMinutes, BigDecimal maxMarks,
                               BigDecimal requiredProgress, Boolean requirePrerequisites,
                               Boolean requireEnrollment) {}
    public record SavedAnswer(Integer questionId, String answerText, LocalDateTime savedAt) {}
    public record AttemptState(Integer attemptId, Integer examId, String status,
                               LocalDateTime serverTime, LocalDateTime expiresAt,
                               long remainingSeconds, List<SavedAnswer> savedAnswers) {}
    public record QuestionResult(Integer questionId, BigDecimal maximumMarks,
                                 BigDecimal obtainedMarks, String feedback) {}
    public record ExamResult(Integer attemptId, Integer examId, String status,
                             BigDecimal obtainedMarks, BigDecimal totalMarks,
                             LocalDateTime publishedAt, boolean questionFeedbackAvailable,
                             List<QuestionResult> questions) {}
}
