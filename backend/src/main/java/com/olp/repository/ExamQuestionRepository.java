package com.olp.repository;

import com.olp.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing questions linked to examinations (FR-I25, Step 38).
 */
@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Integer> {

    @Query("SELECT eq FROM ExamQuestion eq JOIN FETCH eq.question WHERE eq.exam.id = :examId ORDER BY eq.orderIndex ASC")
    List<ExamQuestion> findByExamIdOrderByOrderIndexAsc(@Param("examId") Integer examId);

    @Query("SELECT COUNT(eq) FROM ExamQuestion eq WHERE eq.exam.id = :examId")
    long countByExamId(@Param("examId") Integer examId);

    @Query("SELECT CASE WHEN COUNT(eq) > 0 THEN true ELSE false END FROM ExamQuestion eq WHERE eq.exam.id = :examId AND eq.question.id = :questionId")
    boolean existsByExamIdAndQuestionId(@Param("examId") Integer examId, @Param("questionId") Integer questionId);

    @Modifying
    @Query("DELETE FROM ExamQuestion eq WHERE eq.exam.id = :examId")
    void deleteByExamId(@Param("examId") Integer examId);
}

