package com.olp.dto;

import java.time.LocalDateTime;

public class InstructorProfileResponse {

    private Integer id;
    private String name;
    private String email;
    private String role;
    private String qualifications;
    private String expertiseAreas;
    private String bio;
    private String profilePictureUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InstructorProfileResponse() {}

    public InstructorProfileResponse(Integer id, String name, String email, String role,
                                     String qualifications, String expertiseAreas, String bio) {
        this(id, name, email, role, qualifications, expertiseAreas, bio, null, null, null);
    }

    public InstructorProfileResponse(Integer id, String name, String email, String role,
                                     String qualifications, String expertiseAreas, String bio,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, email, role, qualifications, expertiseAreas, bio, null, createdAt, updatedAt);
    }

    public InstructorProfileResponse(Integer id, String name, String email, String role,
                                     String qualifications, String expertiseAreas, String bio,
                                     String profilePictureUrl,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.qualifications = qualifications;
        this.expertiseAreas = expertiseAreas;
        this.bio = bio;
        this.profilePictureUrl = profilePictureUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ===== Getters and Setters =====

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getInstructorId() {
        return id;
    }

    public void setInstructorId(Integer instructorId) {
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
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
