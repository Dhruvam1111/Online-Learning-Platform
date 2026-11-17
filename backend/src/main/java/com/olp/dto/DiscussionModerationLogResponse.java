package com.olp.dto;

import java.time.LocalDateTime;

import com.olp.entity.DiscussionModerationLog;

public class DiscussionModerationLogResponse {

    private Integer id;
    private Integer courseId;
    private String courseTitle;
    private Integer discussionId;
    private Integer replyId;
    private String targetType;
    private Integer targetAuthorId;
    private String targetAuthorName;
    private Integer moderatorId;
    private String moderatorName;
    private String action;
    private String reason;
    private String previousContent;
    private String newContent;
    private boolean escalatedToAdmin;
    private LocalDateTime createdAt;

    public DiscussionModerationLogResponse() {
    }

    public static DiscussionModerationLogResponse fromEntity(DiscussionModerationLog log) {
        DiscussionModerationLogResponse res = new DiscussionModerationLogResponse();
        res.setId(log.getId());
        if (log.getCourse() != null) {
            res.setCourseId(log.getCourse().getId());
            res.setCourseTitle(log.getCourse().getTitle());
        }
        if (log.getDiscussion() != null) {
            res.setDiscussionId(log.getDiscussion().getId());
        }
        if (log.getReply() != null) {
            res.setReplyId(log.getReply().getId());
        }
        res.setTargetType(log.getTargetType());
        if (log.getTargetAuthor() != null) {
            res.setTargetAuthorId(log.getTargetAuthor().getId());
            res.setTargetAuthorName(log.getTargetAuthor().getName());
        }
        if (log.getModerator() != null) {
            res.setModeratorId(log.getModerator().getId());
            res.setModeratorName(log.getModerator().getName());
        }
        res.setAction(log.getAction());
        res.setReason(log.getReason());
        res.setPreviousContent(log.getPreviousContent());
        res.setNewContent(log.getNewContent());
        res.setEscalatedToAdmin(log.isEscalatedToAdmin());
        res.setCreatedAt(log.getCreatedAt());
        return res;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public Integer getDiscussionId() {
        return discussionId;
    }

    public void setDiscussionId(Integer discussionId) {
        this.discussionId = discussionId;
    }

    public Integer getReplyId() {
        return replyId;
    }

    public void setReplyId(Integer replyId) {
        this.replyId = replyId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Integer getTargetAuthorId() {
        return targetAuthorId;
    }

    public void setTargetAuthorId(Integer targetAuthorId) {
        this.targetAuthorId = targetAuthorId;
    }

    public String getTargetAuthorName() {
        return targetAuthorName;
    }

    public void setTargetAuthorName(String targetAuthorName) {
        this.targetAuthorName = targetAuthorName;
    }

    public Integer getModeratorId() {
        return moderatorId;
    }

    public void setModeratorId(Integer moderatorId) {
        this.moderatorId = moderatorId;
    }

    public String getModeratorName() {
        return moderatorName;
    }

    public void setModeratorName(String moderatorName) {
        this.moderatorName = moderatorName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getPreviousContent() {
        return previousContent;
    }

    public void setPreviousContent(String previousContent) {
        this.previousContent = previousContent;
    }

    public String getNewContent() {
        return newContent;
    }

    public void setNewContent(String newContent) {
        this.newContent = newContent;
    }

    public boolean isEscalatedToAdmin() {
        return escalatedToAdmin;
    }

    public void setEscalatedToAdmin(boolean escalatedToAdmin) {
        this.escalatedToAdmin = escalatedToAdmin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
