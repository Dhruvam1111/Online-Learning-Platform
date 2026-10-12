package com.olp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateCourseRequest {

    // Identity fields potentially supplied in malicious or mistaken payloads
    private Integer id;
    private Integer instructorId;

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 200, message = "Title must be 200 characters or fewer")
    private String title;

    private String description;

    public UpdateCourseRequest() {}

    public UpdateCourseRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }

    // ===== Getters and Setters =====

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Integer instructorId) {
        this.instructorId = instructorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
