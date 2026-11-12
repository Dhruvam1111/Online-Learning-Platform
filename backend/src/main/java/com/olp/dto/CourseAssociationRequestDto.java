package com.olp.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for submitting a course association request (FR-I58).
 */
public class CourseAssociationRequestDto {

    private Integer courseId;

    @Size(max = 50, message = "Requested role must be 50 characters or fewer")
    private String requestedRole;

    @Size(max = 1000, message = "Message must be 1000 characters or fewer")
    private String message;

    public CourseAssociationRequestDto() {
    }

    public CourseAssociationRequestDto(Integer courseId, String message) {
        this.courseId = courseId;
        this.message = message;
    }

    public CourseAssociationRequestDto(Integer courseId, String requestedRole, String message) {
        this.courseId = courseId;
        this.requestedRole = requestedRole;
        this.message = message;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
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
}

