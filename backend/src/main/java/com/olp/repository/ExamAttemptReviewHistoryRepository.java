package com.olp.repository;

import com.olp.entity.ExamAttemptReviewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing append-only exam attempt review history (FR-I69, Step 56).
 */
@Repository
public interface ExamAttemptReviewHistoryRepository extends JpaRepository<ExamAttemptReviewHistory, Integer> {

    @Query("SELECT h FROM ExamAttemptReviewHistory h JOIN FETCH h.reviewedBy WHERE h.attempt.id = :attemptId ORDER BY h.createdAt DESC, h.id DESC")
    List<ExamAttemptReviewHistory> findByAttemptIdOrderByCreatedAtDesc(@Param("attemptId") Integer attemptId);

    @Query("SELECT COUNT(h) FROM ExamAttemptReviewHistory h WHERE h.attempt.id = :attemptId")
    long countByAttemptId(@Param("attemptId") Integer attemptId);
}
