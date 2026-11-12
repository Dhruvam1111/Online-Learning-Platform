package com.olp.dto;

import java.time.LocalDateTime;

public class InstructorCredentialResponse {

    private Integer id;
    private Integer instructorId;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private String documentType;
    private String status;
    private String downloadUrl;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;

    public InstructorCredentialResponse() {}

    public InstructorCredentialResponse(Integer id, Integer instructorId, String originalFileName,
                                        String contentType, Long fileSize, String documentType,
                                        String status, String downloadUrl,
                                        LocalDateTime uploadedAt, LocalDateTime updatedAt) {
        this.id = id;
        this.instructorId = instructorId;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.documentType = documentType;
        this.status = status;
        this.downloadUrl = downloadUrl;
        this.uploadedAt = uploadedAt;
        this.updatedAt = updatedAt;
    }

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

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

