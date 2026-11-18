package com.olp.repository;

import com.olp.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing student exam attempts (FR-I25, Step 38).
 */
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Integer> {

    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.exam.id = :examId AND ea.student.id = :studentId ORDER BY ea.attemptNumber DESC")
    List<ExamAttempt> findByExamIdAndStudentIdOrderByAttemptNumberDesc(@Param("examId") Integer examId,
                                                                      @Param("studentId") Integer studentId);

    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.exam.id = :examId AND ea.student.id = :studentId AND ea.attemptNumber = :attemptNumber")
    Optional<ExamAttempt> findByExamIdAndStudentIdAndAttemptNumber(@Param("examId") Integer examId,
                                                                  @Param("studentId") Integer studentId,
                                                                  @Param("attemptNumber") Integer attemptNumber);

    @Query("SELECT COUNT(ea) FROM ExamAttempt ea WHERE ea.exam.id = :examId AND ea.student.id = :studentId")
    long countByExamIdAndStudentId(@Param("examId") Integer examId, @Param("studentId") Integer studentId);

    @Query("SELECT COUNT(ea) FROM ExamAttempt ea WHERE ea.exam.id = :examId")
    long countByExamId(@Param("examId") Integer examId);

    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.exam.id = :examId AND ea.student.id = :studentId AND ea.status = 'IN_PROGRESS' ORDER BY ea.id DESC")
    List<ExamAttempt> findActiveAttempts(@Param("examId") Integer examId, @Param("studentId") Integer studentId);

    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.exam.id = :examId")
    List<ExamAttempt> findByExamId(@Param("examId") Integer examId);

    @Query("SELECT COUNT(ea) FROM ExamAttempt ea WHERE ea.exam.id = :examId AND ea.status != 'GRADED'")
    long countUngradedByExamId(@Param("examId") Integer examId);

    @Query("SELECT COUNT(ea) FROM ExamAttempt ea WHERE ea.exam.id = :examId AND ea.status = 'GRADED'")
    long countGradedByExamId(@Param("examId") Integer examId);

    @Query("SELECT ea FROM ExamAttempt ea JOIN FETCH ea.exam ex JOIN FETCH ea.student st LEFT JOIN FETCH ea.reviewedBy " +
           "WHERE ex.course.id IN :courseIds " +
           "AND (:status IS NULL OR ea.reviewStatus = :status) " +
           "ORDER BY ea.id DESC")
    List<ExamAttempt> findFlaggedByCourseIds(@Param("courseIds") java.util.Collection<Integer> courseIds,
                                            @Param("status") com.olp.entity.ExamAttemptReviewStatus status);

    @Query("SELECT ea FROM ExamAttempt ea JOIN FETCH ea.exam ex JOIN FETCH ea.student st LEFT JOIN FETCH ea.reviewedBy " +
           "WHERE ex.id = :examId " +
           "AND (:status IS NULL OR ea.reviewStatus = :status) " +
           "ORDER BY ea.id DESC")
    List<ExamAttempt> findFlaggedByExamId(@Param("examId") Integer examId,
                                         @Param("status") com.olp.entity.ExamAttemptReviewStatus status);

    @Query("SELECT ea FROM ExamAttempt ea JOIN FETCH ea.student s WHERE ea.exam.id = :examId " +
           "AND (ea.isPublished = true OR (ea.exam.resultsPublished = true AND ea.status = 'SUBMITTED')) " +
           "ORDER BY ea.obtainedMarks DESC, ea.submittedAt DESC")
    List<ExamAttempt> findPublishedAttemptsByExamId(@Param("examId") Integer examId);

    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.exam.id = :examId AND ea.student.id = :studentId " +
           "AND (ea.isPublished = true OR (ea.exam.resultsPublished = true AND ea.status = 'SUBMITTED')) " +
           "ORDER BY ea.attemptNumber ASC")
    List<ExamAttempt> findPublishedAttemptsByExamIdAndStudentId(
            @Param("examId") Integer examId,
            @Param("studentId") Integer studentId);

    @Query("SELECT ea FROM ExamAttempt ea JOIN FETCH ea.exam e WHERE e.course.id = :courseId AND ea.student.id = :studentId " +
           "AND (ea.isPublished = true OR (e.resultsPublished = true AND ea.status = 'SUBMITTED')) " +
           "ORDER BY e.id ASC, ea.attemptNumber ASC")
    List<ExamAttempt> findPublishedAttemptsByCourseIdAndStudentId(
            @Param("courseId") Integer courseId,
            @Param("studentId") Integer studentId);
}
