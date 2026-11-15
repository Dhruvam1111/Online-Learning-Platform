package com.olp.dto;

import com.olp.entity.CourseDiscussionReply;

import java.time.LocalDateTime;

public class DiscussionReplyResponse {

    private Integer id;
    private Integer discussionId;
    private Integer authorId;
    private String authorName;
    private String authorRole;
    private String content;
    private boolean officialAnswer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DiscussionReplyResponse() {
    }

    public static DiscussionReplyResponse fromEntity(CourseDiscussionReply reply) {
        DiscussionReplyResponse response = new DiscussionReplyResponse();
        response.setId(reply.getId());
        if (reply.getDiscussion() != null) {
            response.setDiscussionId(reply.getDiscussion().getId());
        }
        if (reply.getAuthor() != null) {
            response.setAuthorId(reply.getAuthor().getId());
            response.setAuthorName(reply.getAuthor().getName());
        }
        response.setAuthorRole(reply.getAuthorRole());
        response.setContent(reply.getContent());
        response.setOfficialAnswer(reply.isOfficialAnswer());
        response.setCreatedAt(reply.getCreatedAt());
        response.setUpdatedAt(reply.getUpdatedAt());
        return response;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDiscussionId() {
        return discussionId;
    }

    public void setDiscussionId(Integer discussionId) {
        this.discussionId = discussionId;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorRole() {
        return authorRole;
    }

    public void setAuthorRole(String authorRole) {
        this.authorRole = authorRole;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isOfficialAnswer() {
        return officialAnswer;
    }

    public void setOfficialAnswer(boolean officialAnswer) {
        this.officialAnswer = officialAnswer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

