package com.olp.repository;

import com.olp.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing assignment submissions (FR-I68, Step 28).
 */
@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Integer> {

    @Query("SELECT s FROM AssignmentSubmission s WHERE s.assignment.id = :assignmentId ORDER BY s.submittedAt DESC")
    List<AssignmentSubmission> findByAssignmentId(@Param("assignmentId") Integer assignmentId);

    @Query("SELECT s FROM AssignmentSubmission s WHERE s.assignment.id = :assignmentId AND s.student.id = :studentId")
    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(
            @Param("assignmentId") Integer assignmentId,
            @Param("studentId") Integer studentId);

    @Query("SELECT s FROM AssignmentSubmission s WHERE s.student.id = :studentId ORDER BY s.submittedAt DESC")
    List<AssignmentSubmission> findByStudentId(@Param("studentId") Integer studentId);

    @Query("SELECT s FROM AssignmentSubmission s WHERE s.assignment.id = :assignmentId AND s.gradingStatus = :gradingStatus ORDER BY s.submittedAt ASC")
    List<AssignmentSubmission> findByAssignmentIdAndGradingStatus(
            @Param("assignmentId") Integer assignmentId,
            @Param("gradingStatus") String gradingStatus);

    @Query("SELECT COUNT(s) FROM AssignmentSubmission s WHERE s.assignment.id = :assignmentId")
    long countByAssignmentId(@Param("assignmentId") Integer assignmentId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM AssignmentSubmission s WHERE s.assignment.id = :assignmentId AND s.student.id = :studentId")
    boolean existsByAssignmentIdAndStudentId(
            @Param("assignmentId") Integer assignmentId,
            @Param("studentId") Integer studentId);

    @Query("SELECT COUNT(s) FROM AssignmentSubmission s WHERE s.assignment.course.id IN :courseIds AND s.gradingStatus = 'PENDING'")
    long countPendingByCourseIds(@Param("courseIds") Collection<Integer> courseIds);

    @Query("SELECT COUNT(s) FROM AssignmentSubmission s WHERE s.assignment.course.id = :courseId AND s.gradingStatus = 'PENDING'")
    long countPendingByCourseId(@Param("courseId") Integer courseId);

    @Query("SELECT s FROM AssignmentSubmission s WHERE s.assignment.course.id = :courseId AND s.gradingStatus = 'PENDING' ORDER BY s.submittedAt ASC")
    List<AssignmentSubmission> findPendingByCourseId(@Param("courseId") Integer courseId);
}

