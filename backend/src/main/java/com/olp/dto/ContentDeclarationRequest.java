package com.olp.dto;

import com.olp.entity.LicenseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ContentDeclarationRequest {

    @NotNull(message = "Course ID is required")
    private Integer courseId;

    private Integer lessonId;

    @NotBlank(message = "Declaration confirmation text is required")
    private String declarationText;

    @NotNull(message = "License type is required")
    private LicenseType licenseType = LicenseType.ORIGINAL_CREATOR;

    private String sourceAttribution;

    public ContentDeclarationRequest() {
    }

    public ContentDeclarationRequest(Integer courseId, Integer lessonId, String declarationText,
                                   LicenseType licenseType, String sourceAttribution) {
        this.courseId = courseId;
        this.lessonId = lessonId;
        this.declarationText = declarationText;
        this.licenseType = licenseType;
        this.sourceAttribution = sourceAttribution;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getLessonId() {
        return lessonId;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
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
}

