package com.olp.repository;

import com.olp.entity.CourseCompletionCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for course completion criteria storage and lookup (FR-I56, DR-03).
 */
@Repository
public interface CourseCompletionCriteriaRepository extends JpaRepository<CourseCompletionCriteria, Integer> {

    @Query("SELECT c FROM CourseCompletionCriteria c JOIN FETCH c.course WHERE c.course.id = :courseId")
    Optional<CourseCompletionCriteria> findByCourseId(@Param("courseId") Integer courseId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CourseCompletionCriteria c WHERE c.course.id = :courseId")
    boolean existsByCourseId(@Param("courseId") Integer courseId);

    void deleteByCourseId(Integer courseId);
}
