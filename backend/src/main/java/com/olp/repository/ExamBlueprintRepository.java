package com.olp.repository;

import com.olp.entity.ExamBlueprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing exam blueprints (FR-I27, Step 39).
 */
@Repository
public interface ExamBlueprintRepository extends JpaRepository<ExamBlueprint, Integer> {

    @Query("SELECT b FROM ExamBlueprint b WHERE b.exam.id = :examId ORDER BY b.orderIndex ASC")
    List<ExamBlueprint> findByExamIdOrderByOrderIndexAsc(@Param("examId") Integer examId);

    @Modifying
    @Query("DELETE FROM ExamBlueprint b WHERE b.exam.id = :examId")
    void deleteByExamId(@Param("examId") Integer examId);
}
