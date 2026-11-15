package com.olp.dto;

import com.olp.entity.DiscussionReply;
import java.time.LocalDateTime;

public class DiscussionReplyResponse {

    private Integer id;
    private Integer discussionId;
    private Integer userId;
    private String authorName;
    private String authorRole;
    private String content;
    private Boolean isInstructorReply;
    private Boolean isAcceptedAnswer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DiscussionReplyResponse() {}

    public static DiscussionReplyResponse fromEntity(DiscussionReply entity) {
        if (entity == null) return null;
        DiscussionReplyResponse dto = new DiscussionReplyResponse();
        dto.setId(entity.getId());
        if (entity.getDiscussion() != null) {
            dto.setDiscussionId(entity.getDiscussion().getId());
        }
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setAuthorName(entity.getUser().getName());
            dto.setAuthorRole(entity.getUser().getRole() != null ? entity.getUser().getRole() : "STUDENT");
        }
        dto.setContent(entity.getContent());
        dto.setIsInstructorReply(entity.getIsInstructorReply());
        dto.setIsAcceptedAnswer(entity.getIsAcceptedAnswer());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
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

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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

    public Boolean getIsInstructorReply() {
        return isInstructorReply;
    }

    public void setIsInstructorReply(Boolean isInstructorReply) {
        this.isInstructorReply = isInstructorReply;
    }

    public Boolean getIsAcceptedAnswer() {
        return isAcceptedAnswer;
    }

    public void setIsAcceptedAnswer(Boolean isAcceptedAnswer) {
        this.isAcceptedAnswer = isAcceptedAnswer;
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
