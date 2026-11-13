package com.olp.dto;

import com.olp.entity.VideoUpload;
import com.olp.entity.VideoUploadStatus;

import java.time.LocalDateTime;

public class VideoUploadResponse {

    private String uploadId;
    private Integer lessonId;
    private String lessonTitle;
    private Integer uploaderId;
    private String filename;
    private String fileType;
    private Long totalBytes;
    private Long receivedBytes;
    private Long chunkSize;
    private Integer totalChunks;
    private VideoUploadStatus status;
    private String statusBadge;
    private Double progressPercent;
    private String storagePath;
    private String hlsUrl;
    private String dashUrl;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public VideoUploadResponse() {}

    public static VideoUploadResponse fromEntity(VideoUpload entity) {
        if (entity == null) {
            return null;
        }
        VideoUploadResponse response = new VideoUploadResponse();
        response.setUploadId(entity.getId());
        if (entity.getLesson() != null) {
            response.setLessonId(entity.getLesson().getId());
            response.setLessonTitle(entity.getLesson().getTitle());
        }
        if (entity.getUploader() != null) {
            response.setUploaderId(entity.getUploader().getId());
        }
        response.setFilename(entity.getFilename());
        response.setFileType(entity.getFileType());
        response.setTotalBytes(entity.getTotalBytes());
        response.setReceivedBytes(entity.getReceivedBytes());
        response.setChunkSize(entity.getChunkSize());
        response.setTotalChunks(entity.getTotalChunks());
        response.setStatus(entity.getStatus());
        if (entity.getStatus() != null) {
            String badge = switch (entity.getStatus()) {
                case READY -> "READY";
                case TRANSCODING -> "PROCESSING";
                case COMPLETED -> "UPLOADED";
                case UPLOADING, INITIATED -> "UPLOADING";
                case FAILED -> "FAILED";
                case CANCELLED -> "CANCELLED";
            };
            response.setStatusBadge(badge);
        }

        if (entity.getTotalBytes() != null && entity.getTotalBytes() > 0 && entity.getReceivedBytes() != null) {
            double percent = ((double) entity.getReceivedBytes() / entity.getTotalBytes()) * 100.0;
            response.setProgressPercent(Math.min(100.0, Math.round(percent * 100.0) / 100.0));
        } else {
            response.setProgressPercent(0.0);
        }

        response.setStoragePath(entity.getStoragePath());
        response.setHlsUrl(entity.getHlsUrl());
        response.setDashUrl(entity.getDashUrl());
        response.setFailureReason(entity.getFailureReason());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    // Getters and Setters
    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
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

    public Integer getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(Integer uploaderId) {
        this.uploaderId = uploaderId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(Long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public Long getReceivedBytes() {
        return receivedBytes;
    }

    public void setReceivedBytes(Long receivedBytes) {
        this.receivedBytes = receivedBytes;
    }

    public Long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Long chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }

    public VideoUploadStatus getStatus() {
        return status;
    }

    public void setStatus(VideoUploadStatus status) {
        this.status = status;
    }

    public String getStatusBadge() {
        return statusBadge;
    }

    public void setStatusBadge(String statusBadge) {
        this.statusBadge = statusBadge;
    }

    public Double getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Double progressPercent) {
        this.progressPercent = progressPercent;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getHlsUrl() {
        return hlsUrl;
    }

    public void setHlsUrl(String hlsUrl) {
        this.hlsUrl = hlsUrl;
    }

    public String getDashUrl() {
        return dashUrl;
    }

    public void setDashUrl(String dashUrl) {
        this.dashUrl = dashUrl;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
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

