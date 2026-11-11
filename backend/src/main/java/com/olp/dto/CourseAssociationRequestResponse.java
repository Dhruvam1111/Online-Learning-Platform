package com.olp.dto;

import com.olp.entity.AssociationRequestStatus;
import com.olp.entity.Course;
import com.olp.entity.CourseAssociationRequest;
import com.olp.entity.User;

import java.time.LocalDateTime;

/**
 * Response DTO representing a course association request (FR-I58).
 */
public class CourseAssociationRequestResponse {

    private Integer id;
    private Integer courseId;
    private String courseTitle;
    private String courseCategory;
    private String courseStatus;
    private Integer courseOwnerId;
    private String courseOwnerName;
    private Integer instructorId;
    private String instructorName;
    private String instructorEmail;
    private String requestedRole;
    private String message;
    private AssociationRequestStatus status;
    private String adminNotes;
    private Integer reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CourseAssociationRequestResponse() {
    }

    public static CourseAssociationRequestResponse fromEntity(CourseAssociationRequest req) {
        if (req == null) {
            return null;
        }
        CourseAssociationRequestResponse dto = new CourseAssociationRequestResponse();
        dto.setId(req.getId());

        Course course = req.getCourse();
        if (course != null) {
            dto.setCourseId(course.getId());
            dto.setCourseTitle(course.getTitle());
            dto.setCourseCategory(course.getCategory());
            dto.setCourseStatus(course.getStatus() != null ? course.getStatus().name() : null);
            if (course.getInstructor() != null) {
                dto.setCourseOwnerId(course.getInstructor().getId());
                dto.setCourseOwnerName(course.getInstructor().getName());
            }
        }

        User instructor = req.getInstructor();
        if (instructor != null) {
            dto.setInstructorId(instructor.getId());
            dto.setInstructorName(instructor.getName());
            dto.setInstructorEmail(instructor.getEmail());
        }

        dto.setRequestedRole(req.getRequestedRole());
        dto.setMessage(req.getMessage());
        dto.setStatus(req.getStatus());
        dto.setAdminNotes(req.getAdminNotes());

        User reviewer = req.getReviewedBy();
        if (reviewer != null) {
            dto.setReviewedById(reviewer.getId());
            dto.setReviewedByName(reviewer.getName());
        }

        dto.setReviewedAt(req.getReviewedAt());
        dto.setCreatedAt(req.getCreatedAt());
        dto.setUpdatedAt(req.getUpdatedAt());
        return dto;
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

    public String getCourseCategory() {
        return courseCategory;
    }

    public void setCourseCategory(String courseCategory) {
        this.courseCategory = courseCategory;
    }

    public String getCourseStatus() {
        return courseStatus;
    }

    public void setCourseStatus(String courseStatus) {
        this.courseStatus = courseStatus;
    }

    public Integer getCourseOwnerId() {
        return courseOwnerId;
    }

    public void setCourseOwnerId(Integer courseOwnerId) {
        this.courseOwnerId = courseOwnerId;
    }

    public String getCourseOwnerName() {
        return courseOwnerName;
    }

    public void setCourseOwnerName(String courseOwnerName) {
        this.courseOwnerName = courseOwnerName;
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

    public String getInstructorEmail() {
        return instructorEmail;
    }

    public void setInstructorEmail(String instructorEmail) {
        this.instructorEmail = instructorEmail;
    }

    public String getRequestedRole() {
        return requestedRole;
    }

    public void setRequestedRole(String requestedRole) {
        this.requestedRole = requestedRole;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AssociationRequestStatus getStatus() {
        return status;
    }

    public void setStatus(AssociationRequestStatus status) {
        this.status = status;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public Integer getReviewedById() {
        return reviewedById;
    }

    public void setReviewedById(Integer reviewedById) {
        this.reviewedById = reviewedById;
    }

    public String getReviewedByName() {
        return reviewedByName;
    }

    public void setReviewedByName(String reviewedByName) {
        this.reviewedByName = reviewedByName;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
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

