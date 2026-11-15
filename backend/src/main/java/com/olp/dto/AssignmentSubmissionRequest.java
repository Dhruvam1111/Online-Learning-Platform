package com.olp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for a student submitting an assignment (FR-I68, Step 28).
 */
public class AssignmentSubmissionRequest {

    private String submissionType = "FILE"; // "FILE" or "TEXT"

    @Size(max = 500, message = "File URL must not exceed 500 characters")
    private String fileUrl;

    @Size(max = 255, message = "File name must not exceed 255 characters")
    private String fileName;

    private Long fileSizeBytes;

    private String textContent;

    public AssignmentSubmissionRequest() {}

    public AssignmentSubmissionRequest(String submissionType, String fileUrl, String fileName, Long fileSizeBytes, String textContent) {
        this.submissionType = submissionType != null ? submissionType : "FILE";
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.textContent = textContent;
    }

    public String getSubmissionType() {
        return submissionType;
    }

    public void setSubmissionType(String submissionType) {
        this.submissionType = submissionType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }
}

