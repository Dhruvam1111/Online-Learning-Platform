package com.olp.dto;

import com.olp.entity.Lesson;
import java.time.LocalDateTime;

public class LessonResponse {

    private Integer id;
    private Integer sectionId;
    private String title;
    private String lessonType;
    private String content;
    private String contentUrl;
    private Integer durationMinutes;
    private Integer orderIndex;
    private String metadata;
    private LocalDateTime releaseAt;
    private Boolean isReleased;
    private String timeZone;
    private String timeZoneLabel;
    private String releaseAtDisplay;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LessonResponse() {}

    public LessonResponse(Integer id, Integer sectionId, String title, String lessonType,
                          String content, String contentUrl, Integer durationMinutes,
                          Integer orderIndex, String metadata, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.sectionId = sectionId;
        this.title = title;
        this.lessonType = lessonType;
        this.content = content;
        this.contentUrl = contentUrl;
        this.durationMinutes = durationMinutes;
        this.orderIndex = orderIndex;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static LessonResponse fromEntity(Lesson lesson) {
        if (lesson == null) return null;

        LessonResponse resp = new LessonResponse(
                lesson.getId(),
                lesson.getSection() != null ? lesson.getSection().getId() : null,
                lesson.getTitle(),
                lesson.getLessonType() != null ? lesson.getLessonType().name() : null,
                lesson.getContent(),
                lesson.getContentUrl(),
                lesson.getDurationMinutes(),
                lesson.getOrderIndex(),
                lesson.getMetadata(),
                lesson.getCreatedAt(),
                lesson.getUpdatedAt()
        );
        resp.setReleaseAt(lesson.getReleaseAt());
        resp.setIsReleased(lesson.getIsReleased());
        resp.setTimeZone(lesson.getTimeZone());
        resp.computeReleaseDisplay();
        return resp;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSectionId() {
        return sectionId;
    }

    public void setSectionId(Integer sectionId) {
        this.sectionId = sectionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLessonType() {
        return lessonType;
    }

    public void setLessonType(String lessonType) {
        this.lessonType = lessonType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
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

    public LocalDateTime getReleaseAt() {
        return releaseAt;
    }

    public void setReleaseAt(LocalDateTime releaseAt) {
        this.releaseAt = releaseAt;
        computeReleaseDisplay();
    }

    public Boolean getIsReleased() {
        return isReleased;
    }

    public void setIsReleased(Boolean released) {
        isReleased = released;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
        computeReleaseDisplay();
    }

    public String getTimeZoneLabel() {
        return timeZoneLabel;
    }

    public void setTimeZoneLabel(String timeZoneLabel) {
        this.timeZoneLabel = timeZoneLabel;
    }

    public String getReleaseAtDisplay() {
        return releaseAtDisplay;
    }

    public void setReleaseAtDisplay(String releaseAtDisplay) {
        this.releaseAtDisplay = releaseAtDisplay;
    }

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
        if ("UTC".equalsIgnoreCase(zoneId.getId()) || "Z".equalsIgnoreCase(zoneId.getId())) {
            shortLabel = "UTC";
        } else if ("Asia/Kolkata".equalsIgnoreCase(zoneId.getId()) || "Asia/Calcutta".equalsIgnoreCase(zoneId.getId())) {
            shortLabel = "IST";
        }
        this.timeZoneLabel = shortLabel;
        this.releaseAtDisplay = zdt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " " + shortLabel;
    }
}

