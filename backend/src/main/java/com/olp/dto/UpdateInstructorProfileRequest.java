package com.olp.dto;

import jakarta.validation.constraints.Size;

public class UpdateInstructorProfileRequest {

    // Identity fields potentially supplied in malicious or mistaken payloads
    private Integer id;
    private Integer instructorId;
    private Integer userId;

    @Size(max = 100, message = "Name must be 100 characters or fewer")
    private String name;

    @Size(max = 1000, message = "Qualifications must be 1000 characters or fewer")
    private String qualifications;

    @Size(max = 1000, message = "Expertise areas must be 1000 characters or fewer")
    private String expertiseAreas;

    @Size(max = 5000, message = "Bio must be 5000 characters or fewer")
    private String bio;

    public UpdateInstructorProfileRequest() {}

    public UpdateInstructorProfileRequest(String qualifications, String expertiseAreas, String bio) {
        this.qualifications = qualifications;
        this.expertiseAreas = expertiseAreas;
        this.bio = bio;
    }

    public UpdateInstructorProfileRequest(String name, String qualifications, String expertiseAreas, String bio) {
        this.name = name;
        this.qualifications = qualifications;
        this.expertiseAreas = expertiseAreas;
        this.bio = bio;
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

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
