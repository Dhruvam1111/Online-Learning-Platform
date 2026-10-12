package com.olp.dto;

public class InstructorProfileResponse {

    private Integer id;
    private String name;
    private String email;
    private String role;
    private String qualifications;
    private String expertiseAreas;
    private String bio;

    public InstructorProfileResponse() {}

    public InstructorProfileResponse(Integer id, String name, String email, String role,
                                     String qualifications, String expertiseAreas, String bio) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
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
}
