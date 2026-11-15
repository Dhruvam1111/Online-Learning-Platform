package com.olp.repository;

import com.olp.entity.CourseDiscussionReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseDiscussionReplyRepository extends JpaRepository<CourseDiscussionReply, Integer> {

    @Query("SELECT r FROM CourseDiscussionReply r JOIN FETCH r.author JOIN FETCH r.discussion " +
           "WHERE r.discussion.id = :discussionId ORDER BY r.officialAnswer DESC, r.createdAt ASC, r.id ASC")
    List<CourseDiscussionReply> findByDiscussionIdOrdered(@Param("discussionId") Integer discussionId);

    @Query("SELECT r FROM CourseDiscussionReply r JOIN FETCH r.author JOIN FETCH r.discussion " +
           "WHERE r.discussion.id IN :discussionIds ORDER BY r.officialAnswer DESC, r.createdAt ASC, r.id ASC")
    List<CourseDiscussionReply> findByDiscussionIdsOrdered(@Param("discussionIds") List<Integer> discussionIds);
}

