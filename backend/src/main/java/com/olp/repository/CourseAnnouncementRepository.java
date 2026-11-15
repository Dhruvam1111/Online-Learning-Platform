package com.olp.repository;

import com.olp.entity.CourseAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseAnnouncementRepository extends JpaRepository<CourseAnnouncement, Integer> {

    @Query("SELECT a FROM CourseAnnouncement a JOIN FETCH a.instructor JOIN FETCH a.course WHERE a.course.id = :courseId ORDER BY a.createdAt DESC, a.id DESC")
    List<CourseAnnouncement> findByCourseIdOrderByCreatedAtDesc(@Param("courseId") Integer courseId);

    @Query("SELECT a FROM CourseAnnouncement a JOIN FETCH a.instructor JOIN FETCH a.course WHERE a.id = :announcementId AND a.course.id = :courseId")
    Optional<CourseAnnouncement> findByIdAndCourseId(@Param("announcementId") Integer announcementId,
                                                     @Param("courseId") Integer courseId);
}

