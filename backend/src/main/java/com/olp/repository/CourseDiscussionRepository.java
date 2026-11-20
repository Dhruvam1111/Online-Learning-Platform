package com.olp.repository;

import com.olp.entity.CourseDiscussion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseDiscussionRepository extends JpaRepository<CourseDiscussion, Integer> {

    @Query("SELECT d FROM CourseDiscussion d JOIN FETCH d.student JOIN FETCH d.course LEFT JOIN FETCH d.lecture WHERE d.course.id = :courseId ORDER BY d.createdAt DESC")
    List<CourseDiscussion> findByCourseIdOrderByCreatedAtDesc(@Param("courseId") Integer courseId);

    @Query("SELECT d FROM CourseDiscussion d JOIN FETCH d.student JOIN FETCH d.course LEFT JOIN FETCH d.lecture WHERE d.course.id = :courseId AND d.category = :category ORDER BY d.createdAt DESC")
    List<CourseDiscussion> findByCourseIdAndCategory(@Param("courseId") Integer courseId, @Param("category") String category);

    @Query("SELECT d FROM CourseDiscussion d JOIN FETCH d.student JOIN FETCH d.course LEFT JOIN FETCH d.lecture WHERE d.student.id = :studentId ORDER BY d.createdAt DESC")
    List<CourseDiscussion> findByStudentId(@Param("studentId") Integer studentId);

    @Query("SELECT COUNT(d) FROM CourseDiscussion d WHERE d.student.id = :studentId")
    long countByStudentId(@Param("studentId") Integer studentId);
}
