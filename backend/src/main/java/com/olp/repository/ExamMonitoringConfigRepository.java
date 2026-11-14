package com.olp.repository;

import com.olp.entity.ExamMonitoringConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing exam monitoring configurations (FR-I26, Step 40).
 */
@Repository
public interface ExamMonitoringConfigRepository extends JpaRepository<ExamMonitoringConfig, Integer> {

    @Query("SELECT mc FROM ExamMonitoringConfig mc WHERE mc.exam.id = :examId")
    Optional<ExamMonitoringConfig> findByExamId(@Param("examId") Integer examId);

    @Query("SELECT CASE WHEN COUNT(mc) > 0 THEN true ELSE false END FROM ExamMonitoringConfig mc WHERE mc.exam.id = :examId")
    boolean existsByExamId(@Param("examId") Integer examId);

    @Modifying
    @Query("DELETE FROM ExamMonitoringConfig mc WHERE mc.exam.id = :examId")
    void deleteByExamId(@Param("examId") Integer examId);
}
