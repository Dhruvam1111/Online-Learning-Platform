package com.olp.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for assigning or reassigning an instructor to a course (FR-A10, ADM-025).
 */
public class AssignCourseInstructorRequest {

    @NotNull(message = "Instructor ID is required")
    private Integer instructorId;

    /**
     * Optional role: OWNER or CO_INSTRUCTOR (defaults to CO_INSTRUCTOR).
     */
    private String role;

    public AssignCourseInstructorRequest() {
    }

    public AssignCourseInstructorRequest(Integer instructorId, String role) {
        this.instructorId = instructorId;
        this.role = role;
    }

    public Integer getInstructorId() { return instructorId; }
    public void setInstructorId(Integer instructorId) { this.instructorId = instructorId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}