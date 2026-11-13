package com.olp.dto;

import com.olp.entity.CourseSubmission;
import java.time.LocalDateTime;

public class CourseSubmissionResponse {

    private Integer id;
    private Integer courseId;
    private String courseTitle;
    private Integer instructorId;
    private String instructorName;
    private String status;
    private LocalDateTime submittedAt;
    private String comments;
    private String snapshotData;
    private long submissionNumber;
    private String message;

    public CourseSubmissionResponse() {}

    public static CourseSubmissionResponse fromEntity(CourseSubmission sub, long submissionNumber) {
        if (sub == null) return null;
        CourseSubmissionResponse resp = new CourseSubmissionResponse();
        resp.setId(sub.getId());
        resp.setCourseId(sub.getCourse() != null ? sub.getCourse().getId() : null);
        resp.setCourseTitle(sub.getCourse() != null ? sub.getCourse().getTitle() : null);
        resp.setInstructorId(sub.getInstructor() != null ? sub.getInstructor().getId() : null);
        resp.setInstructorName(sub.getInstructor() != null ? sub.getInstructor().getName() : null);
        resp.setStatus(sub.getStatus());
        resp.setSubmittedAt(sub.getSubmittedAt());
        resp.setComments(sub.getComments());
        resp.setSnapshotData(sub.getSnapshotData());
        resp.setSubmissionNumber(submissionNumber);
        resp.setMessage("Course submitted for admin review successfully");
        return resp;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getSnapshotData() {
        return snapshotData;
    }

    public void setSnapshotData(String snapshotData) {
        this.snapshotData = snapshotData;
    }

    public long getSubmissionNumber() {
        return submissionNumber;
    }

    public void setSubmissionNumber(long submissionNumber) {
        this.submissionNumber = submissionNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

