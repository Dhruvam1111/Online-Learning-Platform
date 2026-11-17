package com.olp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.olp.entity.DiscussionModerationLog;

public interface DiscussionModerationLogRepository extends JpaRepository<DiscussionModerationLog, Integer> {

    @Query("SELECT l FROM DiscussionModerationLog l JOIN FETCH l.course JOIN FETCH l.targetAuthor JOIN FETCH l.moderator LEFT JOIN FETCH l.discussion LEFT JOIN FETCH l.reply WHERE l.course.id = :courseId ORDER BY l.createdAt DESC")
    List<DiscussionModerationLog> findByCourseIdOrderByCreatedAtDesc(@Param("courseId") Integer courseId);

    @Query("SELECT l FROM DiscussionModerationLog l JOIN FETCH l.course JOIN FETCH l.targetAuthor JOIN FETCH l.moderator LEFT JOIN FETCH l.discussion LEFT JOIN FETCH l.reply WHERE l.escalatedToAdmin = true ORDER BY l.createdAt DESC")
    List<DiscussionModerationLog> findEscalatedLogsOrderByCreatedAtDesc();
}
