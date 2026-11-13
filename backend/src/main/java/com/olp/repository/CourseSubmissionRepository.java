package com.olp.repository;

import com.olp.entity.CourseSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseSubmissionRepository extends JpaRepository<CourseSubmission, Integer> {

    @Query("SELECT cs FROM CourseSubmission cs WHERE cs.course.id = :courseId ORDER BY cs.submittedAt DESC, cs.id DESC")
    List<CourseSubmission> findByCourseIdOrderBySubmittedAtDesc(@Param("courseId") Integer courseId);

    @Query("SELECT cs FROM CourseSubmission cs WHERE cs.instructor.id = :instructorId ORDER BY cs.submittedAt DESC, cs.id DESC")
    List<CourseSubmission> findByInstructorIdOrderBySubmittedAtDesc(@Param("instructorId") Integer instructorId);

    @Query("SELECT COUNT(cs) FROM CourseSubmission cs WHERE cs.course.id = :courseId")
    long countByCourseId(@Param("courseId") Integer courseId);
}

