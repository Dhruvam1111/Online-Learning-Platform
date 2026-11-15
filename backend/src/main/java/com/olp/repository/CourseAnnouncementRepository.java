package com.olp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.olp.entity.CourseAnnouncement;

public interface CourseAnnouncementRepository extends JpaRepository<CourseAnnouncement, Integer> {

    @Query("SELECT a FROM CourseAnnouncement a JOIN FETCH a.course JOIN FETCH a.instructor WHERE a.course.id = :courseId ORDER BY a.createdAt DESC")
    List<CourseAnnouncement> findByCourseIdOrderByCreatedAtDesc(@Param("courseId") Integer courseId);

    @Query("SELECT a FROM CourseAnnouncement a JOIN FETCH a.course JOIN FETCH a.instructor WHERE a.instructor.id = :instructorId ORDER BY a.createdAt DESC")
    List<CourseAnnouncement> findByInstructorIdOrderByCreatedAtDesc(@Param("instructorId") Integer instructorId);
}

