package com.olp.repository;

import com.olp.entity.DiscussionReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionReplyRepository extends JpaRepository<DiscussionReply, Integer> {

    @Query("SELECT r FROM DiscussionReply r JOIN FETCH r.user WHERE r.discussion.id = :discussionId ORDER BY r.createdAt ASC")
    List<DiscussionReply> findByDiscussionIdOrderByCreatedAtAsc(@Param("discussionId") Integer discussionId);

    @Query("SELECT COUNT(r) FROM DiscussionReply r WHERE r.discussion.id = :discussionId")
    int countByDiscussionId(@Param("discussionId") Integer discussionId);
}
