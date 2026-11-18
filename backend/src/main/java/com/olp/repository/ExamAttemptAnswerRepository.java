package com.olp.repository;

import com.olp.entity.ExamAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ExamAttemptAnswerRepository extends JpaRepository<ExamAttemptAnswer, Integer> {
    @Query("SELECT a FROM ExamAttemptAnswer a WHERE a.attempt.id = :attemptId AND a.question.id = :questionId")
    Optional<ExamAttemptAnswer> findAnswer(@Param("attemptId") Integer attemptId, @Param("questionId") Integer questionId);

    @Query("SELECT a FROM ExamAttemptAnswer a JOIN FETCH a.question WHERE a.attempt.id = :attemptId ORDER BY a.question.id")
    List<ExamAttemptAnswer> findAnswers(@Param("attemptId") Integer attemptId);
}
