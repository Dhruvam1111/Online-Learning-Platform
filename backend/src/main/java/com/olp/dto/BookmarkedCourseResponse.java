package com.olp.dto;

import java.time.LocalDateTime;

public class BookmarkedCourseResponse {

    private Integer courseId;
    private String title;
    private String description;
    private String instructorName;
    private LocalDateTime bookmarkedAt;

    public BookmarkedCourseResponse() {}

    public BookmarkedCourseResponse(Integer courseId, String title, String description, String instructorName, LocalDateTime bookmarkedAt) {
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.instructorName = instructorName;
        this.bookmarkedAt = bookmarkedAt;
    }

    // Getters and Setters
    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public LocalDateTime getBookmarkedAt() { return bookmarkedAt; }
    public void setBookmarkedAt(LocalDateTime bookmarkedAt) { this.bookmarkedAt = bookmarkedAt; }
}