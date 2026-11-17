package com.olp.dto;

import java.time.LocalDateTime;

import com.olp.entity.CourseDiscussionReply;

public class DiscussionReplyResponse {

    private Integer id;
    private Integer discussionId;
    private Integer authorId;
    private String authorName;
    private String authorRole;
    private String body;
    private boolean instructorReply;
    private boolean officialAnswer;
    private boolean hidden;
    private boolean deleted;
    private String moderationStatus;
    private String moderationReason;
    private Integer moderatedById;
    private String moderatedByName;
    private LocalDateTime moderatedAt;
    private boolean escalatedToAdmin;
    private String escalationReason;
    private LocalDateTime escalatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DiscussionReplyResponse() {
    }

    public static DiscussionReplyResponse fromEntity(CourseDiscussionReply reply) {
        DiscussionReplyResponse res = new DiscussionReplyResponse();
        res.setId(reply.getId());
        if (reply.getDiscussion() != null) {
            res.setDiscussionId(reply.getDiscussion().getId());
        }
        if (reply.getAuthor() != null) {
            res.setAuthorId(reply.getAuthor().getId());
            res.setAuthorName(reply.getAuthor().getName());
            res.setAuthorRole(reply.getAuthor().getRole());
        }
        res.setBody(reply.getBody());
        res.setInstructorReply(reply.isInstructorReply());
        res.setOfficialAnswer(reply.isOfficialAnswer());
        res.setHidden(reply.isHidden());
        res.setDeleted(reply.isDeleted());
        res.setModerationStatus(reply.getModerationStatus());
        res.setModerationReason(reply.getModerationReason());
        if (reply.getModeratedBy() != null) {
            res.setModeratedById(reply.getModeratedBy().getId());
            res.setModeratedByName(reply.getModeratedBy().getName());
        }
        res.setModeratedAt(reply.getModeratedAt());
        res.setEscalatedToAdmin(reply.isEscalatedToAdmin());
        res.setEscalationReason(reply.getEscalationReason());
        res.setEscalatedAt(reply.getEscalatedAt());
        res.setCreatedAt(reply.getCreatedAt());
        res.setUpdatedAt(reply.getUpdatedAt());
        return res;
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

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isInstructorReply() {
        return instructorReply;
    }

    public void setInstructorReply(boolean instructorReply) {
        this.instructorReply = instructorReply;
    }

    public boolean isOfficialAnswer() {
        return officialAnswer;
    }

    public void setOfficialAnswer(boolean officialAnswer) {
        this.officialAnswer = officialAnswer;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getModerationStatus() {
        return moderationStatus;
    }

    public void setModerationStatus(String moderationStatus) {
        this.moderationStatus = moderationStatus;
    }

    public String getModerationReason() {
        return moderationReason;
    }

    public void setModerationReason(String moderationReason) {
        this.moderationReason = moderationReason;
    }

    public Integer getModeratedById() {
        return moderatedById;
    }

    public void setModeratedById(Integer moderatedById) {
        this.moderatedById = moderatedById;
    }

    public String getModeratedByName() {
        return moderatedByName;
    }

    public void setModeratedByName(String moderatedByName) {
        this.moderatedByName = moderatedByName;
    }

    public LocalDateTime getModeratedAt() {
        return moderatedAt;
    }

    public void setModeratedAt(LocalDateTime moderatedAt) {
        this.moderatedAt = moderatedAt;
    }

    public boolean isEscalatedToAdmin() {
        return escalatedToAdmin;
    }

    public void setEscalatedToAdmin(boolean escalatedToAdmin) {
        this.escalatedToAdmin = escalatedToAdmin;
    }

    public String getEscalationReason() {
        return escalationReason;
    }

    public void setEscalationReason(String escalationReason) {
        this.escalationReason = escalationReason;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
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
