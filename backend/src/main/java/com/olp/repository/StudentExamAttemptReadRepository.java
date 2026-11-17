package com.olp.repository;

import com.olp.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import jakarta.persistence.LockModeType;

public interface StudentExamAttemptReadRepository extends JpaRepository<ExamAttempt, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ExamAttempt a WHERE a.id = :attemptId")
    Optional<ExamAttempt> findLocked(@Param("attemptId") Integer attemptId);

    @Query("SELECT a FROM ExamAttempt a WHERE a.student.id = :studentId ORDER BY a.startedAt DESC")
    List<ExamAttempt> findStudentAttempts(@Param("studentId") Integer studentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ExamAttempt a SET a.status = 'SUBMITTED', a.submittedAt = a.expiresAt " +
           "WHERE a.status = 'IN_PROGRESS' AND a.expiresAt <= :now")
    int submitExpired(@Param("now") LocalDateTime now);
}
