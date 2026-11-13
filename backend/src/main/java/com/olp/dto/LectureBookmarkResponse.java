package com.olp.dto;

import java.time.LocalDateTime;

public class LectureBookmarkResponse {
    private Long bookmarkId;
    private Long lectureId;
    private String lectureTitle;
    private Long courseId;
    private String courseTitle;
    private LocalDateTime bookmarkedAt;

    public LectureBookmarkResponse(Long bookmarkId, Long lectureId, String lectureTitle, Long courseId, String courseTitle, LocalDateTime bookmarkedAt) {
        this.bookmarkId = bookmarkId;
        this.lectureId = lectureId;
        this.lectureTitle = lectureTitle;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.bookmarkedAt = bookmarkedAt;
    }

    public Long getBookmarkId() { return bookmarkId; }
    public Long getLectureId() { return lectureId; }
    public String getLectureTitle() { return lectureTitle; }
    public Long getCourseId() { return courseId; }
    public String getCourseTitle() { return courseTitle; }
    public LocalDateTime getBookmarkedAt() { return bookmarkedAt; }
}