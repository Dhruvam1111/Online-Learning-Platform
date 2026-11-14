package com.olp.repository;

import com.olp.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing student exam attempts (FR-I25, Step 38).
 */
@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Integer> {

    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.exam.id = :examId AND ea.student.id = :studentId ORDER BY ea.attemptNumber DESC")
    List<ExamAttempt> findByExamIdAndStudentIdOrderByAttemptNumberDesc(@Param("examId") Integer examId,
                                                                      @Param("studentId") Integer studentId);

    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.exam.id = :examId AND ea.student.id = :studentId AND ea.attemptNumber = :attemptNumber")
    Optional<ExamAttempt> findByExamIdAndStudentIdAndAttemptNumber(@Param("examId") Integer examId,
                                                                  @Param("studentId") Integer studentId,
                                                                  @Param("attemptNumber") Integer attemptNumber);

    @Query("SELECT COUNT(ea) FROM ExamAttempt ea WHERE ea.exam.id = :examId AND ea.student.id = :studentId")
    long countByExamIdAndStudentId(@Param("examId") Integer examId, @Param("studentId") Integer studentId);

    @Query("SELECT COUNT(ea) FROM ExamAttempt ea WHERE ea.exam.id = :examId")
    long countByExamId(@Param("examId") Integer examId);

    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.exam.id = :examId AND ea.student.id = :studentId AND ea.status = 'IN_PROGRESS' ORDER BY ea.id DESC")
    List<ExamAttempt> findActiveAttempts(@Param("examId") Integer examId, @Param("studentId") Integer studentId);
}

