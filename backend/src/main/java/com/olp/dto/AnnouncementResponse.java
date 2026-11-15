package com.olp.dto;

import java.time.LocalDateTime;

import com.olp.entity.CourseAnnouncement;

public class AnnouncementResponse {

    private Integer id;
    private Integer courseId;
    private String courseTitle;
    private Integer instructorId;
    private String instructorName;
    private String title;
    private String body;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AnnouncementResponse() {
    }

    public static AnnouncementResponse fromEntity(CourseAnnouncement announcement) {
        AnnouncementResponse response = new AnnouncementResponse();
        response.setId(announcement.getId());
        if (announcement.getCourse() != null) {
            response.setCourseId(announcement.getCourse().getId());
            response.setCourseTitle(announcement.getCourse().getTitle());
        }
        if (announcement.getInstructor() != null) {
            response.setInstructorId(announcement.getInstructor().getId());
            response.setInstructorName(announcement.getInstructor().getName());
        }
        response.setTitle(announcement.getTitle());
        response.setBody(announcement.getBody());
        response.setCreatedAt(announcement.getCreatedAt());
        response.setUpdatedAt(announcement.getUpdatedAt());
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

    public Integer getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Integer instructorId) {
        this.instructorId = instructorId;
    }

    public String getInstructorName() {
        return instructorName;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
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

