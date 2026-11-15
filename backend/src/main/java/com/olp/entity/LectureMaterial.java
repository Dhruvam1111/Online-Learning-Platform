package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing learning and supplementary materials attached to lessons, modules, or lectures (FR-S14, FR-I14, FR-I17).
 */
@Entity
@Table(name = "lecture_materials")
public class LectureMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "lecture_id")
    private Integer lectureId;

    @Column(name = "lesson_id")
    private Integer lessonId;

    @Column(name = "module_id")
    private Integer moduleId;

    @Column(name = "uploader_id")
    private Integer uploaderId;

    @Column(nullable = false)
    private String title;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType; // pdf, notes, slides, docx, pptx

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "is_downloadable", nullable = false)
    private Boolean isDownloadable = false;

    @Column(name = "scan_status", length = 30)
    private String scanStatus = "CLEAN";

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 1;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "release_at")
    private LocalDateTime releaseAt;

    @Column(name = "is_released", nullable = false)
    private Boolean isReleased = true;

    @Column(name = "time_zone", length = 50)
    private String timeZone = "Asia/Kolkata";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public LectureMaterial() {}

    public LectureMaterial(Integer lectureId, String title, String fileUrl, String fileType, Boolean isDownloadable) {
        this.lectureId = lectureId;
        this.title = title;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.isDownloadable = isDownloadable != null ? isDownloadable : false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public LectureMaterial(Integer lessonId, Integer moduleId, Integer lectureId, Integer uploaderId,
                           String title, String originalFilename, String fileUrl, String fileType,
                           String mimeType, Long fileSizeBytes, Boolean isDownloadable) {
        this.lessonId = lessonId;
        this.moduleId = moduleId;
        this.lectureId = lectureId;
        this.uploaderId = uploaderId;
        this.title = title;
        this.originalFilename = originalFilename;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.mimeType = mimeType;
        this.fileSizeBytes = fileSizeBytes;
        this.isDownloadable = isDownloadable != null ? isDownloadable : false;
        this.scanStatus = "CLEAN";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.isDownloadable == null) {
            this.isDownloadable = false;
        }
        if (this.scanStatus == null) {
            this.scanStatus = "CLEAN";
        }
        if (this.orderIndex == null) {
            this.orderIndex = 1;
        }
        if (this.isReleased == null) {
            this.isReleased = true;
        }
        if (this.timeZone == null || this.timeZone.isBlank()) {
            this.timeZone = "Asia/Kolkata";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getLectureId() { return lectureId; }
    public void setLectureId(Integer lectureId) { this.lectureId = lectureId; }

    public Integer getLessonId() { return lessonId; }
    public void setLessonId(Integer lessonId) { this.lessonId = lessonId; }

    public Integer getModuleId() { return moduleId; }
    public void setModuleId(Integer moduleId) { this.moduleId = moduleId; }

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

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex != null ? orderIndex : 1; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public LocalDateTime getReleaseAt() { return releaseAt; }
    public void setReleaseAt(LocalDateTime releaseAt) { this.releaseAt = releaseAt; }

    public Boolean getIsReleased() { return isReleased; }
    public void setIsReleased(Boolean isReleased) { this.isReleased = isReleased; }

    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

    public boolean isDeleted() { return deletedAt != null; }

    public boolean isAvailableToStudent() {
        if (this.deletedAt != null) {
            return false;
        }
        if (Boolean.TRUE.equals(this.isReleased)) {
            return true;
        }
        if (this.releaseAt == null) {
            return true;
        }
        return !this.releaseAt.isAfter(LocalDateTime.now(java.time.ZoneOffset.UTC));
    }
}
