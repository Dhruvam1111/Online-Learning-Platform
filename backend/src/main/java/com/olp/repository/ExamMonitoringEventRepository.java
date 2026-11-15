package com.olp.repository;

import com.olp.entity.ExamMonitoringEvent;
import com.olp.entity.MonitoringSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for exam monitoring and proctoring event logs (FR-I29, Step 55).
 */
@Repository
public interface ExamMonitoringEventRepository extends JpaRepository<ExamMonitoringEvent, Integer> {

    @Query("SELECT eme FROM ExamMonitoringEvent eme WHERE eme.attempt.id = :attemptId ORDER BY eme.eventTimestamp ASC")
    List<ExamMonitoringEvent> findByAttemptIdOrderByEventTimestampAsc(@Param("attemptId") Integer attemptId);

    @Query("SELECT COUNT(eme) FROM ExamMonitoringEvent eme WHERE eme.attempt.id = :attemptId")
    long countByAttemptId(@Param("attemptId") Integer attemptId);

    @Query("SELECT COUNT(eme) FROM ExamMonitoringEvent eme WHERE eme.attempt.id = :attemptId AND eme.severity = :severity")
    long countByAttemptIdAndSeverity(@Param("attemptId") Integer attemptId, @Param("severity") MonitoringSeverity severity);
}
