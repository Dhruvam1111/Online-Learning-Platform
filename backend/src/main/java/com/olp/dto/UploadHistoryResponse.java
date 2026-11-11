package com.olp.dto;

import java.time.LocalDateTime;

public class UploadHistoryResponse {

    private Integer id;
    private Integer instructorId;
    private String category;
    private String action;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private String details;
    private LocalDateTime createdAt;

    public UploadHistoryResponse() {}

    public UploadHistoryResponse(Integer id, Integer instructorId, String category, String action,
                                 String originalFileName, String contentType, Long fileSize,
                                 String details, LocalDateTime createdAt) {
        this.id = id;
        this.instructorId = instructorId;
        this.category = category;
        this.action = action;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.details = details;
        this.createdAt = createdAt;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

