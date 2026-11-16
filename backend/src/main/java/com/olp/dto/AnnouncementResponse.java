package com.olp.dto;

import com.olp.entity.CourseAnnouncement;

import java.time.LocalDateTime;

public class AnnouncementResponse {

    private Integer id;
    private Integer courseId;
    private String courseTitle;
    private Integer instructorId;
    private String instructorName;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer notifiedStudentCount;

    public AnnouncementResponse() {
    }

    public static AnnouncementResponse fromEntity(CourseAnnouncement announcement) {
        return fromEntity(announcement, null);
    }

    public static AnnouncementResponse fromEntity(CourseAnnouncement announcement, Integer notifiedStudentCount) {
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
        response.setContent(announcement.getContent());
        response.setCreatedAt(announcement.getCreatedAt());
        response.setUpdatedAt(announcement.getUpdatedAt());
        response.setNotifiedStudentCount(notifiedStudentCount);
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Integer getNotifiedStudentCount() {
        return notifiedStudentCount;
    }

    public void setNotifiedStudentCount(Integer notifiedStudentCount) {
        this.notifiedStudentCount = notifiedStudentCount;
    }
}

