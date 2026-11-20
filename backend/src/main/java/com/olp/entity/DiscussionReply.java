package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a reply or instructor response in a course discussion (FR-S64).
 */
@Entity
@Table(name = "discussion_replies")
public class DiscussionReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discussion_id", nullable = false)
    private CourseDiscussion discussion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "is_instructor_reply", nullable = false)
    private Boolean isInstructorReply = false;

    @Column(name = "is_accepted_answer", nullable = false)
    private Boolean isAcceptedAnswer = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public DiscussionReply() {}

    public DiscussionReply(CourseDiscussion discussion, User user, String content,
                           Boolean isInstructorReply, Boolean isAcceptedAnswer) {
        this.discussion = discussion;
        this.user = user;
        this.content = content;
        this.isInstructorReply = isInstructorReply != null ? isInstructorReply : false;
        this.isAcceptedAnswer = isAcceptedAnswer != null ? isAcceptedAnswer : false;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.isInstructorReply == null) {
            this.isInstructorReply = false;
        }
        if (this.isAcceptedAnswer == null) {
            this.isAcceptedAnswer = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public CourseDiscussion getDiscussion() {
        return discussion;
    }

    public void setDiscussion(CourseDiscussion discussion) {
        this.discussion = discussion;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
