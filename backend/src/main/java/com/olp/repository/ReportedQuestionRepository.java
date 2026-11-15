package com.olp.repository;

import com.olp.entity.AssessmentType;
import com.olp.entity.ReportedQuestion;
import com.olp.entity.ReportedQuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportedQuestionRepository extends JpaRepository<ReportedQuestion, Integer> {

    @Query("SELECT rq FROM ReportedQuestion rq WHERE rq.assessmentType = :assessmentType AND rq.assessmentId = :assessmentId AND rq.questionId = :questionId")
    Optional<ReportedQuestion> findByAssessmentTypeAndAssessmentIdAndQuestionId(
            @Param("assessmentType") AssessmentType assessmentType,
            @Param("assessmentId") Integer assessmentId,
            @Param("questionId") Integer questionId);

    @Query("SELECT rq FROM ReportedQuestion rq WHERE rq.course.id IN :courseIds ORDER BY rq.lastReportedAt DESC")
    List<ReportedQuestion> findByCourseIdIn(@Param("courseIds") Collection<Integer> courseIds);

    @Query("SELECT rq FROM ReportedQuestion rq WHERE rq.course.id IN :courseIds AND rq.status = :status ORDER BY rq.lastReportedAt DESC")
    List<ReportedQuestion> findByCourseIdInAndStatus(
            @Param("courseIds") Collection<Integer> courseIds,
            @Param("status") ReportedQuestionStatus status);

    @Query("SELECT rq FROM ReportedQuestion rq WHERE rq.course.id = :courseId ORDER BY rq.lastReportedAt DESC")
    List<ReportedQuestion> findByCourseId(@Param("courseId") Integer courseId);

    @Query("SELECT rq FROM ReportedQuestion rq WHERE rq.course.id = :courseId AND rq.status = :status ORDER BY rq.lastReportedAt DESC")
    List<ReportedQuestion> findByCourseIdAndStatus(
            @Param("courseId") Integer courseId,
            @Param("status") ReportedQuestionStatus status);

    @Query("SELECT rq FROM ReportedQuestion rq WHERE rq.escalatedToAdmin = false AND rq.escalateAt <= :cutoffTime AND rq.status IN :statuses")
    List<ReportedQuestion> findUnresolvedForEscalation(
            @Param("cutoffTime") LocalDateTime cutoffTime,
            @Param("statuses") Collection<ReportedQuestionStatus> statuses);

    @Query("SELECT rq FROM ReportedQuestion rq ORDER BY rq.lastReportedAt DESC")
    List<ReportedQuestion> findAllOrderByLastReportedAtDesc();

    @Query("SELECT rq FROM ReportedQuestion rq WHERE rq.status = :status ORDER BY rq.lastReportedAt DESC")
    List<ReportedQuestion> findAllByStatusOrderByLastReportedAtDesc(@Param("status") ReportedQuestionStatus status);

    @Query("SELECT rq FROM ReportedQuestion rq WHERE rq.assessmentType = :assessmentType AND rq.assessmentId = :assessmentId ORDER BY rq.lastReportedAt DESC")
    List<ReportedQuestion> findByAssessment(
            @Param("assessmentType") AssessmentType assessmentType,
            @Param("assessmentId") Integer assessmentId);

    @Query("SELECT rq FROM ReportedQuestion rq WHERE rq.assessmentType = :assessmentType AND rq.assessmentId = :assessmentId AND rq.status = :status ORDER BY rq.lastReportedAt DESC")
    List<ReportedQuestion> findByAssessmentAndStatus(
            @Param("assessmentType") AssessmentType assessmentType,
            @Param("assessmentId") Integer assessmentId,
            @Param("status") ReportedQuestionStatus status);

    @Query("SELECT rq FROM ReportedQuestion rq " +
           "WHERE (:courseId IS NULL OR rq.course.id = :courseId) " +
           "AND (:assessmentType IS NULL OR rq.assessmentType = :assessmentType) " +
           "AND (:assessmentId IS NULL OR rq.assessmentId = :assessmentId) " +
           "AND (:status IS NULL OR rq.status = :status) " +
           "AND rq.course.id IN :accessibleCourseIds " +
           "ORDER BY rq.lastReportedAt DESC")
    List<ReportedQuestion> findWithFilters(
            @Param("accessibleCourseIds") Collection<Integer> accessibleCourseIds,
            @Param("courseId") Integer courseId,
            @Param("assessmentType") AssessmentType assessmentType,
            @Param("assessmentId") Integer assessmentId,
            @Param("status") ReportedQuestionStatus status);

    @Query("SELECT rq FROM ReportedQuestion rq " +
           "WHERE (:courseId IS NULL OR rq.course.id = :courseId) " +
           "AND (:assessmentType IS NULL OR rq.assessmentType = :assessmentType) " +
           "AND (:assessmentId IS NULL OR rq.assessmentId = :assessmentId) " +
           "AND (:status IS NULL OR rq.status = :status) " +
           "ORDER BY rq.lastReportedAt DESC")
    List<ReportedQuestion> findWithFiltersAdmin(
            @Param("courseId") Integer courseId,
            @Param("assessmentType") AssessmentType assessmentType,
            @Param("assessmentId") Integer assessmentId,
            @Param("status") ReportedQuestionStatus status);
}
