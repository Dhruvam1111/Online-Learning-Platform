package com.olp.repository;

import com.olp.entity.CourseDiscussion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseDiscussionRepository extends JpaRepository<CourseDiscussion, Integer> {

    @Query("SELECT d FROM CourseDiscussion d JOIN FETCH d.author JOIN FETCH d.course LEFT JOIN FETCH d.lecture " +
           "WHERE d.course.id = :courseId ORDER BY d.pinned DESC, d.createdAt DESC, d.id DESC")
    List<CourseDiscussion> findByCourseIdOrdered(@Param("courseId") Integer courseId);

    @Query("SELECT d FROM CourseDiscussion d JOIN FETCH d.author JOIN FETCH d.course LEFT JOIN FETCH d.lecture " +
           "WHERE d.course.id = :courseId AND d.answered = false ORDER BY d.pinned DESC, d.createdAt DESC, d.id DESC")
    List<CourseDiscussion> findUnansweredByCourseIdOrdered(@Param("courseId") Integer courseId);

    @Query("SELECT d FROM CourseDiscussion d JOIN FETCH d.author JOIN FETCH d.course LEFT JOIN FETCH d.lecture " +
           "WHERE d.id = :discussionId AND d.course.id = :courseId")
    Optional<CourseDiscussion> findByIdAndCourseId(
            @Param("discussionId") Integer discussionId,
            @Param("courseId") Integer courseId);

    @Query("SELECT COUNT(d) FROM CourseDiscussion d WHERE d.course.id = :courseId AND d.answered = false")
    long countUnansweredByCourseId(@Param("courseId") Integer courseId);
}

