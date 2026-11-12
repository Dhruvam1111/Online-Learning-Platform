package com.olp.repository;

import com.olp.entity.QuizAttempt;
import com.olp.entity.QuizAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
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
}

