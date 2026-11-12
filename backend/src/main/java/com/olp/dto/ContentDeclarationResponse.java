package com.olp.dto;

import com.olp.entity.ContentDeclaration;
import com.olp.entity.LicenseType;
import com.olp.entity.TakedownStatus;

import java.time.LocalDateTime;

public class ContentDeclarationResponse {

    private Integer id;
    private Integer courseId;
    private String courseTitle;
    private Integer lessonId;
    private String lessonTitle;
    private Integer instructorId;
    private String instructorName;
    private String declarationText;
    private LicenseType licenseType;
    private String sourceAttribution;
    private LocalDateTime declaredAt;
    private TakedownStatus takedownStatus;
    private String takedownReason;
    private LocalDateTime takedownFlaggedAt;
    private String instructorResponse;
    private LocalDateTime instructorRespondedAt;
    private LocalDateTime resolvedAt;

    public ContentDeclarationResponse() {
    }

    public static ContentDeclarationResponse fromEntity(ContentDeclaration declaration) {
        ContentDeclarationResponse response = new ContentDeclarationResponse();
        response.setId(declaration.getId());
        if (declaration.getCourse() != null) {
            response.setCourseId(declaration.getCourse().getId());
            response.setCourseTitle(declaration.getCourse().getTitle());
        }
        if (declaration.getLesson() != null) {
            response.setLessonId(declaration.getLesson().getId());
            response.setLessonTitle(declaration.getLesson().getTitle());
        }
        if (declaration.getInstructor() != null) {
            response.setInstructorId(declaration.getInstructor().getId());
            response.setInstructorName(declaration.getInstructor().getName());
        }
        response.setDeclarationText(declaration.getDeclarationText());
        response.setLicenseType(declaration.getLicenseType());
        response.setSourceAttribution(declaration.getSourceAttribution());
        response.setDeclaredAt(declaration.getDeclaredAt());
        response.setTakedownStatus(declaration.getTakedownStatus());
        response.setTakedownReason(declaration.getTakedownReason());
        response.setTakedownFlaggedAt(declaration.getTakedownFlaggedAt());
        response.setInstructorResponse(declaration.getInstructorResponse());
        response.setInstructorRespondedAt(declaration.getInstructorRespondedAt());
        response.setResolvedAt(declaration.getResolvedAt());
        return response;
    }

    // Getters and Setters
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

    public Integer getLessonId() {
        return lessonId;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }

    public String getLessonTitle() {
        return lessonTitle;
    }

    public void setLessonTitle(String lessonTitle) {
        this.lessonTitle = lessonTitle;
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

    public String getDeclarationText() {
        return declarationText;
    }

    public void setDeclarationText(String declarationText) {
        this.declarationText = declarationText;
    }

    public LicenseType getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(LicenseType licenseType) {
        this.licenseType = licenseType;
    }

    public String getSourceAttribution() {
        return sourceAttribution;
    }

    public void setSourceAttribution(String sourceAttribution) {
        this.sourceAttribution = sourceAttribution;
    }

    public LocalDateTime getDeclaredAt() {
        return declaredAt;
    }

    public void setDeclaredAt(LocalDateTime declaredAt) {
        this.declaredAt = declaredAt;
    }

    public TakedownStatus getTakedownStatus() {
        return takedownStatus;
    }

    public void setTakedownStatus(TakedownStatus takedownStatus) {
        this.takedownStatus = takedownStatus;
    }

    public String getTakedownReason() {
        return takedownReason;
    }

    public void setTakedownReason(String takedownReason) {
        this.takedownReason = takedownReason;
    }

    public LocalDateTime getTakedownFlaggedAt() {
        return takedownFlaggedAt;
    }

    public void setTakedownFlaggedAt(LocalDateTime takedownFlaggedAt) {
        this.takedownFlaggedAt = takedownFlaggedAt;
    }

    public String getInstructorResponse() {
        return instructorResponse;
    }

    public void setInstructorResponse(String instructorResponse) {
        this.instructorResponse = instructorResponse;
    }

    public LocalDateTime getInstructorRespondedAt() {
        return instructorRespondedAt;
    }

    public void setInstructorRespondedAt(LocalDateTime instructorRespondedAt) {
        this.instructorRespondedAt = instructorRespondedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}

