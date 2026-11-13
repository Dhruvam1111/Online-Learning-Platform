package com.olp.repository;

import com.olp.entity.QuizAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswer, Integer> {

    @Query("SELECT a FROM QuizAttemptAnswer a JOIN FETCH a.question WHERE a.attempt.id = :attemptId ORDER BY a.id ASC")
    List<QuizAttemptAnswer> findByAttemptId(@Param("attemptId") Integer attemptId);

    @Query("SELECT a FROM QuizAttemptAnswer a JOIN FETCH a.question WHERE a.attempt.id = :attemptId AND a.question.id = :questionId")
    Optional<QuizAttemptAnswer> findByAttemptIdAndQuestionId(@Param("attemptId") Integer attemptId,
                                                            @Param("questionId") Integer questionId);

    @Query("SELECT COUNT(a) FROM QuizAttemptAnswer a WHERE a.attempt.id = :attemptId")
    long countByAttemptId(@Param("attemptId") Integer attemptId);
}

