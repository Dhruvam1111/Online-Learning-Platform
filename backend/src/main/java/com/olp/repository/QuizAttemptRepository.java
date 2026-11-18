package com.olp.repository;

import com.olp.entity.QuizAttempt;
import com.olp.entity.QuizAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Integer> {

    @Query("SELECT a FROM QuizAttempt a JOIN FETCH a.quiz JOIN FETCH a.student WHERE a.id = :id AND a.quiz.id = :quizId")
    Optional<QuizAttempt> findByIdAndQuizId(@Param("id") Integer id, @Param("quizId") Integer quizId);

    @Query("SELECT a FROM QuizAttempt a WHERE a.quiz.id = :quizId AND a.student.id = :studentId ORDER BY a.attemptNumber DESC")
    List<QuizAttempt> findByQuizIdAndStudentIdOrderByAttemptNumberDesc(@Param("quizId") Integer quizId,
                                                                       @Param("studentId") Integer studentId);

    @Query("SELECT a FROM QuizAttempt a WHERE a.quiz.id = :quizId AND a.student.id = :studentId AND a.status = :status ORDER BY a.attemptNumber DESC")
    List<QuizAttempt> findByQuizIdAndStudentIdAndStatusOrderByAttemptNumberDesc(
            @Param("quizId") Integer quizId,
            @Param("studentId") Integer studentId,
            @Param("status") QuizAttemptStatus status);

    @Query("SELECT COUNT(a) FROM QuizAttempt a WHERE a.quiz.id = :quizId AND a.student.id = :studentId")
    long countByQuizIdAndStudentId(@Param("quizId") Integer quizId, @Param("studentId") Integer studentId);

    @Query("SELECT a FROM QuizAttempt a WHERE a.quiz.id = :quizId")
    List<QuizAttempt> findByQuizId(@Param("quizId") Integer quizId);

    @Query("SELECT COUNT(a) FROM QuizAttempt a WHERE a.quiz.id = :quizId")
    long countByQuizId(@Param("quizId") Integer quizId);

    @Query("SELECT a FROM QuizAttempt a JOIN FETCH a.student s WHERE a.quiz.id = :quizId " +
           "AND (a.isPublished = true OR (a.quiz.resultsPublished = true AND a.status = com.olp.entity.QuizAttemptStatus.SUBMITTED)) " +
           "ORDER BY a.obtainedMarks DESC, a.submittedAt DESC")
    List<QuizAttempt> findPublishedAttemptsByQuizId(@Param("quizId") Integer quizId);

    @Query("SELECT a FROM QuizAttempt a WHERE a.quiz.id = :quizId AND a.student.id = :studentId " +
           "AND (a.isPublished = true OR (a.quiz.resultsPublished = true AND a.status = com.olp.entity.QuizAttemptStatus.SUBMITTED)) " +
           "ORDER BY a.attemptNumber ASC")
    List<QuizAttempt> findPublishedAttemptsByQuizIdAndStudentId(
            @Param("quizId") Integer quizId,
            @Param("studentId") Integer studentId);

    @Query("SELECT a FROM QuizAttempt a JOIN FETCH a.quiz q WHERE q.course.id = :courseId AND a.student.id = :studentId " +
           "AND (a.isPublished = true OR (q.resultsPublished = true AND a.status = com.olp.entity.QuizAttemptStatus.SUBMITTED)) " +
           "ORDER BY q.id ASC, a.attemptNumber ASC")
    List<QuizAttempt> findPublishedAttemptsByCourseIdAndStudentId(
            @Param("courseId") Integer courseId,
            @Param("studentId") Integer studentId);
}
