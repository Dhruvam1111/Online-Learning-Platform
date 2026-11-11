package com.olp.dto;

import java.time.LocalDateTime;

/**
 * Reusable Instructor Summary DTO for instructor cards on course pages,
 * course listings, and public instructor overview.
 * Contains the instructor's basic details, qualifications, expertise areas, and bio.
 */
public class InstructorSummaryResponse {

    private Integer id;
    private Integer instructorId;
    private String name;
    private String email;
    private String qualifications;
    private String expertiseAreas;
    private String bio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InstructorSummaryResponse() {}

    public InstructorSummaryResponse(Integer id, String name, String email,
                                     String qualifications, String expertiseAreas, String bio) {
        this(id, name, email, qualifications, expertiseAreas, bio, null, null);
    }

    public InstructorSummaryResponse(Integer id, String name, String email,
                                     String qualifications, String expertiseAreas, String bio,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.instructorId = id;
        this.name = name;
        this.email = email;
        this.qualifications = qualifications;
        this.expertiseAreas = expertiseAreas;
        this.bio = bio;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ===== Getters and Setters =====

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
        this.instructorId = id;
    }

    public Integer getInstructorId() {
        return instructorId != null ? instructorId : id;
    }

    public void setInstructorId(Integer instructorId) {
        this.instructorId = instructorId;
        this.id = instructorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getQualifications() {
        return qualifications;
    }

    public void setQualifications(String qualifications) {
        this.qualifications = qualifications;
    }

    public String getExpertiseAreas() {
        return expertiseAreas;
    }

    public void setExpertiseAreas(String expertiseAreas) {
        this.expertiseAreas = expertiseAreas;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
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

