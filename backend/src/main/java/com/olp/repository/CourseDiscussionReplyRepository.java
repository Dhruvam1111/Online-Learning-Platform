package com.olp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.olp.entity.CourseDiscussionReply;

public interface CourseDiscussionReplyRepository extends JpaRepository<CourseDiscussionReply, Integer> {

    @Query("SELECT r FROM CourseDiscussionReply r JOIN FETCH r.discussion d JOIN FETCH d.course JOIN FETCH r.author LEFT JOIN FETCH r.moderatedBy WHERE r.id = :id")
    Optional<CourseDiscussionReply> findByIdWithDetails(@Param("id") Integer id);

    @Query("SELECT r FROM CourseDiscussionReply r JOIN FETCH r.discussion JOIN FETCH r.author LEFT JOIN FETCH r.moderatedBy WHERE r.discussion.id = :discussionId ORDER BY r.isOfficialAnswer DESC, r.createdAt ASC")
    List<CourseDiscussionReply> findAllByDiscussionIdForInstructor(@Param("discussionId") Integer discussionId);

    @Query("SELECT r FROM CourseDiscussionReply r JOIN FETCH r.discussion JOIN FETCH r.author LEFT JOIN FETCH r.moderatedBy WHERE r.discussion.id IN :discussionIds ORDER BY r.isOfficialAnswer DESC, r.createdAt ASC")
    List<CourseDiscussionReply> findAllByDiscussionIdsForInstructor(@Param("discussionIds") List<Integer> discussionIds);

    @Query("SELECT r FROM CourseDiscussionReply r JOIN FETCH r.discussion JOIN FETCH r.author WHERE r.discussion.id = :discussionId AND r.isHidden = false AND r.isDeleted = false ORDER BY r.isOfficialAnswer DESC, r.createdAt ASC")
    List<CourseDiscussionReply> findVisibleByDiscussionId(@Param("discussionId") Integer discussionId);

    @Query("SELECT r FROM CourseDiscussionReply r JOIN FETCH r.discussion JOIN FETCH r.author WHERE r.discussion.id IN :discussionIds AND r.isHidden = false AND r.isDeleted = false ORDER BY r.isOfficialAnswer DESC, r.createdAt ASC")
    List<CourseDiscussionReply> findVisibleByDiscussionIds(@Param("discussionIds") List<Integer> discussionIds);

    @Query("SELECT r FROM CourseDiscussionReply r JOIN FETCH r.discussion d JOIN FETCH d.course JOIN FETCH r.author LEFT JOIN FETCH r.moderatedBy WHERE r.escalatedToAdmin = true ORDER BY r.escalatedAt DESC")
    List<CourseDiscussionReply> findEscalatedReplies();
}
