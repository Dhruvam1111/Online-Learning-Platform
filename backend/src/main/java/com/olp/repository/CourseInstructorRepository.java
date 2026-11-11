package com.olp.repository;

import com.olp.entity.CourseInstructor;
import com.olp.entity.CourseInstructorRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseInstructorRepository extends JpaRepository<CourseInstructor, Integer> {

    @Query("SELECT ci FROM CourseInstructor ci WHERE ci.course.id = :courseId ORDER BY ci.id ASC")
    List<CourseInstructor> findByCourseId(@Param("courseId") Integer courseId);

    @Query("SELECT ci FROM CourseInstructor ci JOIN FETCH ci.instructor WHERE ci.course.id = :courseId ORDER BY ci.id ASC")
    List<CourseInstructor> findByCourseIdWithInstructor(@Param("courseId") Integer courseId);

    @Query("SELECT ci FROM CourseInstructor ci JOIN FETCH ci.course c JOIN FETCH c.instructor WHERE ci.instructor.id = :instructorId ORDER BY c.id ASC")
    List<CourseInstructor> findByInstructorId(@Param("instructorId") Integer instructorId);

    @Query("SELECT ci FROM CourseInstructor ci JOIN FETCH ci.instructor WHERE ci.course.id = :courseId AND ci.instructor.id = :instructorId")
    Optional<CourseInstructor> findByCourseIdAndInstructorId(
            @Param("courseId") Integer courseId,
            @Param("instructorId") Integer instructorId);

    @Query("SELECT COUNT(ci) > 0 FROM CourseInstructor ci WHERE ci.course.id = :courseId AND ci.instructor.id = :instructorId")
    boolean existsByCourseIdAndInstructorId(@Param("courseId") Integer courseId, @Param("instructorId") Integer instructorId);

    @Query("SELECT COUNT(ci) > 0 FROM CourseInstructor ci WHERE ci.course.id = :courseId AND ci.instructor.id = :instructorId AND ci.role = :role")
    boolean existsByCourseIdAndInstructorIdAndRole(
            @Param("courseId") Integer courseId,
            @Param("instructorId") Integer instructorId,
            @Param("role") CourseInstructorRole role);

    @Query("SELECT COUNT(ci) FROM CourseInstructor ci WHERE ci.course.id = :courseId AND ci.role = :role")
    long countByCourseIdAndRole(@Param("courseId") Integer courseId, @Param("role") CourseInstructorRole role);
}

