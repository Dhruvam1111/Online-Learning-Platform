package com.olp.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.olp.entity.CourseDiscussion;

public class DiscussionThreadResponse {

    private Integer id;
    private Integer courseId;
    private String courseTitle;
    private Integer lectureId;
    private String lectureTitle;
    private Integer authorId;
    private String authorName;
    private String authorRole;
    private String title;
    private String body;
    private boolean answered;
    private boolean pinned;
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
    private int replyCount;
    private List<DiscussionReplyResponse> replies = new ArrayList<>();

    public DiscussionThreadResponse() {
    }

    public static DiscussionThreadResponse fromEntity(CourseDiscussion discussion, List<DiscussionReplyResponse> replies) {
        DiscussionThreadResponse res = new DiscussionThreadResponse();
        res.setId(discussion.getId());
        if (discussion.getCourse() != null) {
            res.setCourseId(discussion.getCourse().getId());
            res.setCourseTitle(discussion.getCourse().getTitle());
        }
        if (discussion.getLecture() != null) {
            res.setLectureId(discussion.getLecture().getId());
            res.setLectureTitle(discussion.getLecture().getTitle());
        }
        if (discussion.getAuthor() != null) {
            res.setAuthorId(discussion.getAuthor().getId());
            res.setAuthorName(discussion.getAuthor().getName());
            res.setAuthorRole(discussion.getAuthor().getRole());
        }
        res.setTitle(discussion.getTitle());
        res.setBody(discussion.getBody());
        res.setAnswered(discussion.isAnswered());
        res.setPinned(discussion.isPinned());
        res.setHidden(discussion.isHidden());
        res.setDeleted(discussion.isDeleted());
        res.setModerationStatus(discussion.getModerationStatus());
        res.setModerationReason(discussion.getModerationReason());
        if (discussion.getModeratedBy() != null) {
            res.setModeratedById(discussion.getModeratedBy().getId());
            res.setModeratedByName(discussion.getModeratedBy().getName());
        }
        res.setModeratedAt(discussion.getModeratedAt());
        res.setEscalatedToAdmin(discussion.isEscalatedToAdmin());
        res.setEscalationReason(discussion.getEscalationReason());
        res.setEscalatedAt(discussion.getEscalatedAt());
        res.setCreatedAt(discussion.getCreatedAt());
        res.setUpdatedAt(discussion.getUpdatedAt());
        List<DiscussionReplyResponse> safeReplies = replies != null ? replies : new ArrayList<>();
        res.setReplies(safeReplies);
        res.setReplyCount(safeReplies.size());
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

    public Integer getLectureId() {
        return lectureId;
    }

    public void setLectureId(Integer lectureId) {
        this.lectureId = lectureId;
    }

    public String getLectureTitle() {
        return lectureTitle;
    }

    public void setLectureTitle(String lectureTitle) {
        this.lectureTitle = lectureTitle;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isAnswered() {
        return answered;
    }

    public void setAnswered(boolean answered) {
        this.answered = answered;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
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

    public int getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(int replyCount) {
        this.replyCount = replyCount;
    }

    public List<DiscussionReplyResponse> getReplies() {
        return replies;
    }

    public void setReplies(List<DiscussionReplyResponse> replies) {
        this.replies = replies;
    }
}
