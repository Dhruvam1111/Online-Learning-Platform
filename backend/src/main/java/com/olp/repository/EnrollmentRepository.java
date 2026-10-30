package com.olp.repository;

import com.olp.dto.MyCourseResponse;
import com.olp.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {

    /**
     * One query, no N+1: selects only the fields the dashboard needs.
     * Most recently enrolled courses come first.
     */
    @Query("SELECT new com.olp.dto.MyCourseResponse(c.id, c.title, i.name, e.enrolledAt) " +
           "FROM Enrollment e " +
           "JOIN e.course c " +
           "JOIN c.instructor i " +
           "WHERE e.student.id = :studentId " +
           "ORDER BY e.enrolledAt DESC")
    List<MyCourseResponse> findMyCourses(@Param("studentId") Integer studentId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Enrollment e WHERE e.student.id = :studentId AND e.course.id = :courseId")
    boolean existsByStudentIdAndCourseId(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.course.id = :courseId")
    Optional<Enrollment> findByStudentIdAndCourseId(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    @Modifying
    @Query("DELETE FROM Enrollment e WHERE e.student.id = :studentId AND e.course.id = :courseId")
    void deleteByStudentIdAndCourseId(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId")
    long countByCourseId(@Param("courseId") Integer courseId);
}
