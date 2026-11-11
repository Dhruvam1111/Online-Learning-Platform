package com.olp.dto;

/**
 * Response DTO for searching courses available for instructor association (FR-I58).
 */
public class AvailableCourseAssociationResponse {

    private Integer courseId;
    private String title;
    private String description;
    private String category;
    private String status;
    private Integer ownerInstructorId;
    private String ownerInstructorName;
    private boolean alreadyAssociated;
    private String associationStatus; // OWNER, ASSOCIATED, PENDING, REJECTED, AVAILABLE
    private Integer activeRequestId;

    public AvailableCourseAssociationResponse() {
    }

    public AvailableCourseAssociationResponse(
            Integer courseId,
            String title,
            String description,
            String category,
            String status,
            Integer ownerInstructorId,
            String ownerInstructorName,
            boolean alreadyAssociated,
            String associationStatus,
            Integer activeRequestId) {
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.status = status;
        this.ownerInstructorId = ownerInstructorId;
        this.ownerInstructorName = ownerInstructorName;
        this.alreadyAssociated = alreadyAssociated;
        this.associationStatus = associationStatus;
        this.activeRequestId = activeRequestId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getOwnerInstructorId() {
        return ownerInstructorId;
    }

    public void setOwnerInstructorId(Integer ownerInstructorId) {
        this.ownerInstructorId = ownerInstructorId;
    }

    public String getOwnerInstructorName() {
        return ownerInstructorName;
    }

    public void setOwnerInstructorName(String ownerInstructorName) {
        this.ownerInstructorName = ownerInstructorName;
    }

    public boolean isAlreadyAssociated() {
        return alreadyAssociated;
    }

    public void setAlreadyAssociated(boolean alreadyAssociated) {
        this.alreadyAssociated = alreadyAssociated;
    }

    public String getAssociationStatus() {
        return associationStatus;
    }

    public void setAssociationStatus(String associationStatus) {
        this.associationStatus = associationStatus;
    }

    public Integer getActiveRequestId() {
        return activeRequestId;
    }

    public void setActiveRequestId(Integer activeRequestId) {
        this.activeRequestId = activeRequestId;
    }
}

