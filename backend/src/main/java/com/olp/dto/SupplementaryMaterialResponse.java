package com.olp.dto;

import com.olp.entity.LectureMaterial;

import java.time.LocalDateTime;

public class SupplementaryMaterialResponse {

    private Integer id;
    private Integer lessonId;
    private Integer moduleId;
    private Integer lectureId;
    private Integer uploaderId;
    private String title;
    private String originalFilename;
    private String fileUrl;
    private String fileType;
    private String mimeType;
    private Long fileSizeBytes;
    private Boolean isDownloadable;
    private String scanStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SupplementaryMaterialResponse() {}

    public static SupplementaryMaterialResponse fromEntity(LectureMaterial entity) {
        if (entity == null) {
            return null;
        }
        SupplementaryMaterialResponse resp = new SupplementaryMaterialResponse();
        resp.setId(entity.getId());
        resp.setLessonId(entity.getLessonId());
        resp.setModuleId(entity.getModuleId());
        resp.setLectureId(entity.getLectureId());
        resp.setUploaderId(entity.getUploaderId());
        resp.setTitle(entity.getTitle());
        resp.setOriginalFilename(entity.getOriginalFilename());
        resp.setFileUrl(entity.getFileUrl());
        resp.setFileType(entity.getFileType());
        resp.setMimeType(entity.getMimeType());
        resp.setFileSizeBytes(entity.getFileSizeBytes());
        resp.setIsDownloadable(entity.getIsDownloadable());
        resp.setScanStatus(entity.getScanStatus());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getLessonId() { return lessonId; }
    public void setLessonId(Integer lessonId) { this.lessonId = lessonId; }

    public Integer getModuleId() { return moduleId; }
    public void setModuleId(Integer moduleId) { this.moduleId = moduleId; }

    public Integer getLectureId() { return lectureId; }
    public void setLectureId(Integer lectureId) { this.lectureId = lectureId; }

    public Integer getUploaderId() { return uploaderId; }
    public void setUploaderId(Integer uploaderId) { this.uploaderId = uploaderId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public Boolean getIsDownloadable() { return isDownloadable; }
    public void setIsDownloadable(Boolean downloadable) { isDownloadable = downloadable; }

    public String getScanStatus() { return scanStatus; }
    public void setScanStatus(String scanStatus) { this.scanStatus = scanStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

