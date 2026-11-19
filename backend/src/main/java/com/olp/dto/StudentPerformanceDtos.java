package com.olp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** All scores are exposed only after the corresponding result is published. */
public final class StudentPerformanceDtos {
    private StudentPerformanceDtos() {}

    public record Result(Integer attemptId, Integer courseId, String courseTitle,
                         String type, Integer assessmentId, String title,
                         BigDecimal awardedMarks, BigDecimal maximumMarks,
                         Double percentage, LocalDateTime assessedAt) {}

    public record CourseSummary(Integer courseId, String title,
                                double lectureCompletionPercentage, int completedLectures,
                                int totalLectures, int publishedResultCount,
                                Double averageAssessmentPercentage) {}

    public record Overview(int enrolledCourseCount, int publishedResultCount,
                           Double averageAssessmentPercentage, Double averageCourseCompletionPercentage,
                           List<CourseSummary> courses) {}

    public record Topic(String topic, int evaluatedAnswers, BigDecimal awardedMarks,
                        BigDecimal maximumMarks, double percentage, boolean needsPractice) {}

    public record TrendPoint(LocalDateTime assessedAt, String type, Integer assessmentId,
                             Integer attemptId, double percentage, double runningAveragePercentage) {}
}
