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
    private Integer orderIndex;
    private LocalDateTime deletedAt;
    private LocalDateTime releaseAt;
    private Boolean isReleased;
    private String timeZone;
    private String timeZoneLabel;
    private String releaseAtDisplay;
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
        resp.setOrderIndex(entity.getOrderIndex());
        resp.setDeletedAt(entity.getDeletedAt());
        resp.setReleaseAt(entity.getReleaseAt());
        resp.setIsReleased(entity.getIsReleased());
        resp.setTimeZone(entity.getTimeZone());
        resp.computeReleaseDisplay();
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

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public LocalDateTime getReleaseAt() { return releaseAt; }
    public void setReleaseAt(LocalDateTime releaseAt) {
        this.releaseAt = releaseAt;
        computeReleaseDisplay();
    }

    public Boolean getIsReleased() { return isReleased; }
    public void setIsReleased(Boolean isReleased) { this.isReleased = isReleased; }

    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
        computeReleaseDisplay();
    }

    public String getTimeZoneLabel() { return timeZoneLabel; }
    public void setTimeZoneLabel(String timeZoneLabel) { this.timeZoneLabel = timeZoneLabel; }

    public String getReleaseAtDisplay() { return releaseAtDisplay; }
    public void setReleaseAtDisplay(String releaseAtDisplay) { this.releaseAtDisplay = releaseAtDisplay; }

    public void computeReleaseDisplay() {
        if (this.releaseAt == null) {
            this.timeZoneLabel = "IST";
            this.releaseAtDisplay = "Immediate";
            return;
        }
        java.time.ZoneId zoneId;
        try {
            if ("IST".equalsIgnoreCase(this.timeZone)) {
                zoneId = java.time.ZoneId.of("Asia/Kolkata");
            } else if ("UTC".equalsIgnoreCase(this.timeZone) || "Z".equalsIgnoreCase(this.timeZone)) {
                zoneId = java.time.ZoneOffset.UTC;
            } else if (this.timeZone != null && !this.timeZone.isBlank()) {
                zoneId = java.time.ZoneId.of(this.timeZone);
            } else {
                zoneId = java.time.ZoneId.of("Asia/Kolkata");
            }
        } catch (Exception e) {
            zoneId = java.time.ZoneId.of("Asia/Kolkata");
        }

        java.time.ZonedDateTime zdt = this.releaseAt.atZone(java.time.ZoneOffset.UTC).withZoneSameInstant(zoneId);
        String shortLabel = zoneId.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH);
        if ("UTC".equalsIgnoreCase(zoneId.getId())) {
            shortLabel = "UTC";
        } else if ("Asia/Kolkata".equalsIgnoreCase(zoneId.getId()) || "Asia/Calcutta".equalsIgnoreCase(zoneId.getId())) {
            shortLabel = "IST";
        }
        this.timeZoneLabel = shortLabel;
        this.releaseAtDisplay = zdt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " " + shortLabel;
    }
}

