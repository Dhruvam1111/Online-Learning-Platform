package com.olp.repository;

import com.olp.dto.MyCourseResponse;
import com.olp.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}