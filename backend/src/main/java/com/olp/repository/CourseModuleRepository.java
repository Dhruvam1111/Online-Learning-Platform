package com.olp.repository;

import com.olp.entity.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseModuleRepository extends JpaRepository<CourseModule, Integer> {

    @Query("SELECT m FROM CourseModule m WHERE m.course.id = :courseId ORDER BY m.orderIndex ASC")
    List<CourseModule> findByCourseIdOrderByOrderIndexAsc(@Param("courseId") Integer courseId);

    @Query("SELECT m FROM CourseModule m WHERE m.id = :id AND m.course.id = :courseId")
    Optional<CourseModule> findByIdAndCourseId(@Param("id") Integer id, @Param("courseId") Integer courseId);

    @Query("SELECT COALESCE(MAX(m.orderIndex), 0) FROM CourseModule m WHERE m.course.id = :courseId")
    int findMaxOrderIndexByCourseId(@Param("courseId") Integer courseId);
}

