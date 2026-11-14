package com.olp.repository;

import com.olp.entity.EvaluationStatus;
import com.olp.entity.QuizAttemptAnswer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for persistent quiz attempt answers and manual descriptive grading records (FR-I22, FR-I30).
 */
@Repository
public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswer, Integer> {

    @Query("SELECT a FROM QuizAttemptAnswer a JOIN FETCH a.question WHERE a.attempt.id = :attemptId ORDER BY a.id ASC")
    List<QuizAttemptAnswer> findByAttemptId(@Param("attemptId") Integer attemptId);

    @Query("SELECT a FROM QuizAttemptAnswer a JOIN FETCH a.question WHERE a.attempt.id = :attemptId AND a.question.id = :questionId")
    Optional<QuizAttemptAnswer> findByAttemptIdAndQuestionId(@Param("attemptId") Integer attemptId,
                                                            @Param("questionId") Integer questionId);

    @Query("SELECT COUNT(a) FROM QuizAttemptAnswer a WHERE a.attempt.id = :attemptId")
    long countByAttemptId(@Param("attemptId") Integer attemptId);

    @Query("SELECT a FROM QuizAttemptAnswer a " +
           "JOIN FETCH a.question q " +
           "JOIN FETCH a.attempt att " +
           "JOIN FETCH att.student s " +
           "JOIN FETCH att.quiz qz " +
           "JOIN FETCH qz.course c " +
           "LEFT JOIN FETCH a.grader gr " +
           "WHERE a.id = :id")
    Optional<QuizAttemptAnswer> findByIdWithDetails(@Param("id") Integer id);

    @Query("SELECT a FROM QuizAttemptAnswer a " +
           "JOIN FETCH a.question q " +
           "JOIN FETCH a.attempt att " +
           "JOIN FETCH att.student s " +
           "JOIN FETCH att.quiz qz " +
           "JOIN FETCH qz.course c " +
           "LEFT JOIN FETCH a.grader gr " +
           "WHERE c.id IN :courseIds " +
           "AND (:courseId IS NULL OR c.id = :courseId) " +
           "AND (:quizId IS NULL OR qz.id = :quizId) " +
           "AND (q.questionType = com.olp.entity.QuestionType.DESCRIPTIVE OR q.requiresManualGrading = true) " +
           "AND (:status IS NULL OR a.evaluationStatus = :status) " +
           "ORDER BY CASE WHEN a.evaluationStatus = com.olp.entity.EvaluationStatus.MANUAL_GRADING_REQUIRED THEN 0 " +
           "              WHEN a.evaluationStatus = com.olp.entity.EvaluationStatus.EVALUATED_UNPUBLISHED THEN 1 " +
           "              ELSE 2 END ASC, " +
           "att.submittedAt DESC, a.id DESC")
    List<QuizAttemptAnswer> findDescriptiveGradingQueue(
            @Param("courseIds") Collection<Integer> courseIds,
            @Param("courseId") Integer courseId,
            @Param("quizId") Integer quizId,
            @Param("status") EvaluationStatus status);

    @Query(value = "SELECT a FROM QuizAttemptAnswer a " +
                   "JOIN FETCH a.question q " +
                   "JOIN FETCH a.attempt att " +
                   "JOIN FETCH att.student s " +
                   "JOIN FETCH att.quiz qz " +
                   "JOIN FETCH qz.course c " +
                   "LEFT JOIN FETCH a.grader gr " +
                   "WHERE c.id IN :courseIds " +
                   "AND (:courseId IS NULL OR c.id = :courseId) " +
                   "AND (:quizId IS NULL OR qz.id = :quizId) " +
                   "AND (q.questionType = com.olp.entity.QuestionType.DESCRIPTIVE OR q.requiresManualGrading = true) " +
                   "AND (:status IS NULL OR a.evaluationStatus = :status) " +
                   "ORDER BY CASE WHEN a.evaluationStatus = com.olp.entity.EvaluationStatus.MANUAL_GRADING_REQUIRED THEN 0 " +
                   "              WHEN a.evaluationStatus = com.olp.entity.EvaluationStatus.EVALUATED_UNPUBLISHED THEN 1 " +
                   "              ELSE 2 END ASC, " +
                   "att.submittedAt DESC, a.id DESC",
           countQuery = "SELECT COUNT(a) FROM QuizAttemptAnswer a " +
                        "WHERE a.attempt.quiz.course.id IN :courseIds " +
                        "AND (:courseId IS NULL OR a.attempt.quiz.course.id = :courseId) " +
                        "AND (:quizId IS NULL OR a.attempt.quiz.id = :quizId) " +
                        "AND (a.question.questionType = com.olp.entity.QuestionType.DESCRIPTIVE OR a.question.requiresManualGrading = true) " +
                        "AND (:status IS NULL OR a.evaluationStatus = :status)")
    Page<QuizAttemptAnswer> findDescriptiveGradingQueuePaged(
            @Param("courseIds") Collection<Integer> courseIds,
            @Param("courseId") Integer courseId,
            @Param("quizId") Integer quizId,
            @Param("status") EvaluationStatus status,
            Pageable pageable);

    @Query("SELECT a FROM QuizAttemptAnswer a " +
           "JOIN FETCH a.attempt att " +
           "WHERE att.quiz.id = :quizId " +
           "AND a.evaluationStatus = com.olp.entity.EvaluationStatus.EVALUATED_UNPUBLISHED")
    List<QuizAttemptAnswer> findEvaluatedUnpublishedByQuizId(@Param("quizId") Integer quizId);

    @Query("SELECT a FROM QuizAttemptAnswer a " +
           "WHERE a.attempt.id = :attemptId " +
           "AND a.evaluationStatus = com.olp.entity.EvaluationStatus.EVALUATED_UNPUBLISHED")
    List<QuizAttemptAnswer> findEvaluatedUnpublishedByAttemptId(@Param("attemptId") Integer attemptId);
}
