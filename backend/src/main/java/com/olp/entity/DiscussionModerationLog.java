package com.olp.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "discussion_moderation_logs")
public class DiscussionModerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discussion_id")
    private CourseDiscussion discussion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_id")
    private CourseDiscussionReply reply;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_author_id", nullable = false)
    private User targetAuthor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id", nullable = false)
    private User moderator;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "previous_content", columnDefinition = "TEXT")
    private String previousContent;

    @Column(name = "new_content", columnDefinition = "TEXT")
    private String newContent;

    @Column(name = "escalated_to_admin", nullable = false)
    private boolean escalatedToAdmin = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DiscussionModerationLog() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public CourseDiscussion getDiscussion() {
        return discussion;
    }

    public void setDiscussion(CourseDiscussion discussion) {
        this.discussion = discussion;
    }

    public CourseDiscussionReply getReply() {
        return reply;
    }

    public void setReply(CourseDiscussionReply reply) {
        this.reply = reply;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public User getTargetAuthor() {
        return targetAuthor;
    }

    public void setTargetAuthor(User targetAuthor) {
        this.targetAuthor = targetAuthor;
    }

    public User getModerator() {
        return moderator;
    }

    public void setModerator(User moderator) {
        this.moderator = moderator;
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
