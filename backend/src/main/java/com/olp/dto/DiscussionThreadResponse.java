package com.olp.dto;

import com.olp.entity.CourseDiscussion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private String content;
    private boolean answered;
    private boolean pinned;
    private int replyCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DiscussionReplyResponse> replies = new ArrayList<>();

    public DiscussionThreadResponse() {
    }

    public static DiscussionThreadResponse fromEntity(
            CourseDiscussion discussion,
            List<DiscussionReplyResponse> replies) {
        DiscussionThreadResponse response = new DiscussionThreadResponse();
        response.setId(discussion.getId());
        if (discussion.getCourse() != null) {
            response.setCourseId(discussion.getCourse().getId());
            response.setCourseTitle(discussion.getCourse().getTitle());
        }
        if (discussion.getLecture() != null) {
            response.setLectureId(discussion.getLecture().getId());
            response.setLectureTitle(discussion.getLecture().getTitle());
        }
        if (discussion.getAuthor() != null) {
            response.setAuthorId(discussion.getAuthor().getId());
            response.setAuthorName(discussion.getAuthor().getName());
            response.setAuthorRole(discussion.getAuthor().getRole());
        }
        response.setTitle(discussion.getTitle());
        response.setContent(discussion.getContent());
        response.setAnswered(discussion.isAnswered());
        response.setPinned(discussion.isPinned());
        response.setCreatedAt(discussion.getCreatedAt());
        response.setUpdatedAt(discussion.getUpdatedAt());
        List<DiscussionReplyResponse> safeReplies = replies != null ? replies : new ArrayList<>();
        response.setReplies(safeReplies);
        response.setReplyCount(safeReplies.size());
        return response;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public int getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(int replyCount) {
        this.replyCount = replyCount;
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

    public List<DiscussionReplyResponse> getReplies() {
        return replies;
    }

    public void setReplies(List<DiscussionReplyResponse> replies) {
        this.replies = replies;
    }
}

