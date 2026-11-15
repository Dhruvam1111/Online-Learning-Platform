package com.olp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_discussion_replies")
public class CourseDiscussionReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discussion_id", nullable = false)
    private CourseDiscussion discussion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "author_role", nullable = false, length = 20)
    private String authorRole = "student";

    @NotBlank(message = "Reply content is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_official_answer", nullable = false)
    private boolean officialAnswer = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CourseDiscussionReply() {
    }

    public CourseDiscussionReply(
            CourseDiscussion discussion,
            User author,
            String authorRole,
            String content,
            boolean officialAnswer) {
        this.discussion = discussion;
        this.author = author;
        this.authorRole = (authorRole != null && !authorRole.isBlank()) ? authorRole.trim().toLowerCase() : "student";
        this.content = content;
        this.officialAnswer = officialAnswer;
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
        if (this.authorRole == null || this.authorRole.isBlank()) {
            this.authorRole = "student";
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

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
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

