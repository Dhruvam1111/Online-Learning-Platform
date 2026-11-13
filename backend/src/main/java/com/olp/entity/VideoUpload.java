package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity tracking chunked, resumable video lecture uploads and transcoding status (FR-I13, Step 13).
 */
@Entity
@Table(name = "video_uploads")
public class VideoUpload {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;

    @Column(name = "total_bytes", nullable = false)
    private Long totalBytes;

    @Column(name = "received_bytes", nullable = false)
    private Long receivedBytes = 0L;

    @Column(name = "chunk_size", nullable = false)
    private Long chunkSize;

    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VideoUploadStatus status = VideoUploadStatus.INITIATED;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Column(name = "hls_url", length = 500)
    private String hlsUrl;

    @Column(name = "dash_url", length = 500)
    private String dashUrl;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public VideoUpload() {}

    public VideoUpload(String id, Lesson lesson, User uploader, String filename, String fileType,
                       Long totalBytes, Long chunkSize, Integer totalChunks) {
        this.id = id;
        this.lesson = lesson;
        this.uploader = uploader;
        this.filename = filename;
        this.fileType = fileType;
        this.totalBytes = totalBytes;
        this.chunkSize = chunkSize;
        this.totalChunks = totalChunks;
        this.receivedBytes = 0L;
        this.status = VideoUploadStatus.INITIATED;
        this.expiresAt = LocalDateTime.now().plusDays(2);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.receivedBytes == null) {
            this.receivedBytes = 0L;
        }
        if (this.status == null) {
            this.status = VideoUploadStatus.INITIATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public User getUploader() {
        return uploader;
    }

    public void setUploader(User uploader) {
        this.uploader = uploader;
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

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
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

