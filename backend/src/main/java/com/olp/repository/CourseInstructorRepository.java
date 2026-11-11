package com.olp.repository;

import com.olp.entity.CourseInstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseInstructorRepository extends JpaRepository<CourseInstructor, Integer> {

    @Query("SELECT ci FROM CourseInstructor ci WHERE ci.course.id = :courseId")
    List<CourseInstructor> findByCourseId(@Param("courseId") Integer courseId);

    @Query("SELECT ci FROM CourseInstructor ci WHERE ci.instructor.id = :instructorId")
    List<CourseInstructor> findByInstructorId(@Param("instructorId") Integer instructorId);

    @Query("SELECT COUNT(ci) > 0 FROM CourseInstructor ci WHERE ci.course.id = :courseId AND ci.instructor.id = :instructorId")
    boolean existsByCourseIdAndInstructorId(@Param("courseId") Integer courseId, @Param("instructorId") Integer instructorId);
}

