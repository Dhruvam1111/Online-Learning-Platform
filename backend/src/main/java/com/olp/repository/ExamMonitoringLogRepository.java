package com.olp.repository;

import com.olp.entity.ExamMonitoringLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamMonitoringLogRepository extends JpaRepository<ExamMonitoringLog, Integer> {

    @Query("SELECT l FROM ExamMonitoringLog l WHERE l.attempt.id = :attemptId ORDER BY l.eventTimestamp ASC")
    List<ExamMonitoringLog> findByAttemptIdOrderByTimestampAsc(@Param("attemptId") Integer attemptId);

    @Query("SELECT COUNT(l) FROM ExamMonitoringLog l WHERE l.attempt.id = :attemptId AND l.eventType = :eventType")
    long countByAttemptIdAndEventType(@Param("attemptId") Integer attemptId, @Param("eventType") String eventType);

    @Query("SELECT COUNT(l) FROM ExamMonitoringLog l WHERE l.attempt.id = :attemptId AND l.severity = 'VIOLATION'")
    long countViolationsByAttemptId(@Param("attemptId") Integer attemptId);
}
