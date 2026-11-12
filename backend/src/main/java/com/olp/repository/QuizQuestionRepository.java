package com.olp.repository;

import com.olp.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Integer> {

    @Query("SELECT qq FROM QuizQuestion qq JOIN FETCH qq.question WHERE qq.quiz.id = :quizId ORDER BY qq.orderIndex ASC, qq.id ASC")
    List<QuizQuestion> findByQuizIdOrderByOrderIndexAsc(@Param("quizId") Integer quizId);

    @Query("SELECT qq FROM QuizQuestion qq JOIN FETCH qq.question WHERE qq.id = :id AND qq.quiz.id = :quizId")
    Optional<QuizQuestion> findByIdAndQuizId(@Param("id") Integer id, @Param("quizId") Integer quizId);

    @Query("SELECT qq FROM QuizQuestion qq JOIN FETCH qq.question WHERE qq.quiz.id = :quizId AND qq.question.id = :questionId")
    Optional<QuizQuestion> findByQuizIdAndQuestionId(@Param("quizId") Integer quizId, @Param("questionId") Integer questionId);

    @Query("SELECT CASE WHEN COUNT(qq) > 0 THEN true ELSE false END FROM QuizQuestion qq WHERE qq.quiz.id = :quizId AND qq.question.id = :questionId")
    boolean existsByQuizIdAndQuestionId(@Param("quizId") Integer quizId, @Param("questionId") Integer questionId);

    @Query("SELECT COALESCE(MAX(qq.orderIndex), 0) FROM QuizQuestion qq WHERE qq.quiz.id = :quizId")
    int findMaxOrderIndexByQuizId(@Param("quizId") Integer quizId);
}

