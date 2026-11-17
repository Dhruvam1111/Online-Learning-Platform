package com.olp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.olp.entity.CourseDiscussion;

public interface CourseDiscussionRepository extends JpaRepository<CourseDiscussion, Integer> {

    @Query("SELECT d FROM CourseDiscussion d JOIN FETCH d.course JOIN FETCH d.author LEFT JOIN FETCH d.lecture LEFT JOIN FETCH d.moderatedBy WHERE d.id = :id")
    Optional<CourseDiscussion> findByIdWithDetails(@Param("id") Integer id);

    @Query("SELECT d FROM CourseDiscussion d JOIN FETCH d.course JOIN FETCH d.author LEFT JOIN FETCH d.lecture LEFT JOIN FETCH d.moderatedBy WHERE d.course.id = :courseId ORDER BY d.isPinned DESC, d.createdAt DESC")
    List<CourseDiscussion> findAllByCourseIdForInstructor(@Param("courseId") Integer courseId);

    @Query("SELECT d FROM CourseDiscussion d JOIN FETCH d.course JOIN FETCH d.author LEFT JOIN FETCH d.lecture LEFT JOIN FETCH d.moderatedBy WHERE d.course.id = :courseId AND d.isAnswered = false ORDER BY d.isPinned DESC, d.createdAt DESC")
    List<CourseDiscussion> findUnansweredByCourseIdForInstructor(@Param("courseId") Integer courseId);

    @Query("SELECT d FROM CourseDiscussion d JOIN FETCH d.course JOIN FETCH d.author LEFT JOIN FETCH d.lecture LEFT JOIN FETCH d.moderatedBy WHERE d.course.id = :courseId AND d.lecture.id = :lectureId ORDER BY d.isPinned DESC, d.createdAt DESC")
    List<CourseDiscussion> findByCourseIdAndLectureIdForInstructor(@Param("courseId") Integer courseId, @Param("lectureId") Integer lectureId);

    @Query("SELECT d FROM CourseDiscussion d JOIN FETCH d.course JOIN FETCH d.author LEFT JOIN FETCH d.lecture LEFT JOIN FETCH d.moderatedBy WHERE d.course.id = :courseId AND d.lecture.id = :lectureId AND d.isAnswered = false ORDER BY d.isPinned DESC, d.createdAt DESC")
    List<CourseDiscussion> findUnansweredByCourseIdAndLectureIdForInstructor(@Param("courseId") Integer courseId, @Param("lectureId") Integer lectureId);

    @Query("SELECT d FROM CourseDiscussion d JOIN FETCH d.course JOIN FETCH d.author LEFT JOIN FETCH d.lecture WHERE d.course.id = :courseId AND d.isHidden = false AND d.isDeleted = false ORDER BY d.isPinned DESC, d.createdAt DESC")
    List<CourseDiscussion> findVisibleByCourseId(@Param("courseId") Integer courseId);

    @Query("SELECT d FROM CourseDiscussion d JOIN FETCH d.course JOIN FETCH d.author LEFT JOIN FETCH d.lecture WHERE d.course.id = :courseId AND d.isAnswered = false AND d.isHidden = false AND d.isDeleted = false ORDER BY d.isPinned DESC, d.createdAt DESC")
    List<CourseDiscussion> findVisibleUnansweredByCourseId(@Param("courseId") Integer courseId);

    @Query("SELECT d FROM CourseDiscussion d JOIN FETCH d.course JOIN FETCH d.author LEFT JOIN FETCH d.lecture LEFT JOIN FETCH d.moderatedBy WHERE d.escalatedToAdmin = true ORDER BY d.escalatedAt DESC")
    List<CourseDiscussion> findEscalatedDiscussions();
}
