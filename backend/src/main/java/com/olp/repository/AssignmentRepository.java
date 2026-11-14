package com.olp.repository;

import com.olp.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing assignments (FR-I68, Step 28).
 */
@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Integer> {

    @Query("SELECT a FROM Assignment a WHERE a.course.id = :courseId ORDER BY a.deadline ASC")
    List<Assignment> findByCourseId(@Param("courseId") Integer courseId);

    @Query("SELECT a FROM Assignment a WHERE a.course.id = :courseId AND a.status = :status ORDER BY a.deadline ASC")
    List<Assignment> findByCourseIdAndStatus(@Param("courseId") Integer courseId, @Param("status") String status);

    @Query("SELECT a FROM Assignment a WHERE a.lesson.id = :lessonId ORDER BY a.deadline ASC")
    List<Assignment> findByLessonId(@Param("lessonId") Integer lessonId);

    @Query("SELECT a FROM Assignment a WHERE a.id = :id AND a.course.id = :courseId")
    Optional<Assignment> findByIdAndCourseId(@Param("id") Integer id, @Param("courseId") Integer courseId);

    @Query("SELECT COUNT(a) FROM Assignment a WHERE a.course.id = :courseId")
    long countByCourseId(@Param("courseId") Integer courseId);
}

