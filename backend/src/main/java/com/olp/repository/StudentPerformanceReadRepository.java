package com.olp.repository;

import com.olp.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/** Read-only projection queries; all attempts are scoped to the authenticated student. */
public interface StudentPerformanceReadRepository extends JpaRepository<Enrollment, Integer> {
    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course WHERE e.student.id = :studentId")
    List<Enrollment> enrollments(@Param("studentId") Integer studentId);

    @Query("SELECT a FROM ExamAttempt a JOIN FETCH a.exam ex JOIN FETCH ex.course " +
           "WHERE a.student.id = :studentId ORDER BY a.submittedAt DESC, a.id DESC")
    List<ExamAttempt> exams(@Param("studentId") Integer studentId);

    @Query("SELECT a FROM QuizAttempt a JOIN FETCH a.quiz q JOIN FETCH q.course " +
           "WHERE a.student.id = :studentId ORDER BY a.submittedAt DESC, a.id DESC")
    List<QuizAttempt> quizzes(@Param("studentId") Integer studentId);

    @Query("SELECT a FROM AssignmentSubmission a JOIN FETCH a.assignment ass JOIN FETCH ass.course " +
           "WHERE a.student.id = :studentId ORDER BY a.submittedAt DESC, a.id DESC")
    List<AssignmentSubmission> assignments(@Param("studentId") Integer studentId);
    /** Verify student enrollment using the Enrollment entity properties. */
    boolean existsByStudentIdAndCourseId(Integer studentId, Integer courseId);
}
