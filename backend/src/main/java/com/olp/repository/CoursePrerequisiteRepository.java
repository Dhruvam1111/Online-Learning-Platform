package com.olp.repository;

import com.olp.entity.CoursePrerequisite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoursePrerequisiteRepository extends JpaRepository<CoursePrerequisite, Integer> {

    @Query("SELECT cp FROM CoursePrerequisite cp WHERE cp.course.id = :courseId ORDER BY cp.id ASC")
    List<CoursePrerequisite> findByCourseIdOrderByIdAsc(@Param("courseId") Integer courseId);
}

