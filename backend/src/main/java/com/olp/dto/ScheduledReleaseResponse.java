package com.olp.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Response DTO representing the scheduled release configuration of a content item (FR-I18, Step 20, DR-ED08).
 * Stores release date-time in UTC and surfaces formatted time in the instructor's zone with zone label.
 */
public class ScheduledReleaseResponse {

    private Integer id;
    private String contentType; // "MATERIAL" or "LESSON"
    private String title;
    private Integer courseId;
    private Integer lessonId;
    private LocalDateTime releaseAt; // stored in UTC
    private Boolean isReleased;
    private String timeZone;
    private String timeZoneLabel;
    private String releaseAtDisplay;

    public ScheduledReleaseResponse() {}

    public ScheduledReleaseResponse(Integer id, String contentType, String title, Integer courseId,
                                    Integer lessonId, LocalDateTime releaseAtUtc, Boolean isReleased,
                                    String timeZone) {
        this.id = id;
        this.contentType = contentType;
        this.title = title;
        this.courseId = courseId;
        this.lessonId = lessonId;
        this.releaseAt = releaseAtUtc;
        this.isReleased = isReleased;
        this.timeZone = (timeZone != null && !timeZone.isBlank()) ? timeZone : "Asia/Kolkata";
        computeDisplayFields();
    }

    private void computeDisplayFields() {
        if (this.releaseAt == null) {
            this.timeZoneLabel = "IST";
            this.releaseAtDisplay = "Immediate";
            return;
        }

        ZoneId zoneId;
        try {
            if ("IST".equalsIgnoreCase(this.timeZone)) {
                zoneId = ZoneId.of("Asia/Kolkata");
            } else if ("UTC".equalsIgnoreCase(this.timeZone) || "Z".equalsIgnoreCase(this.timeZone)) {
                zoneId = ZoneOffset.UTC;
            } else {
                zoneId = ZoneId.of(this.timeZone);
            }
        } catch (Exception e) {
            zoneId = ZoneId.of("Asia/Kolkata");
        }

        ZonedDateTime zdt = this.releaseAt.atZone(ZoneOffset.UTC).withZoneSameInstant(zoneId);
        String shortLabel = zoneId.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        if ("UTC".equalsIgnoreCase(zoneId.getId()) || "Z".equalsIgnoreCase(zoneId.getId())) {
            shortLabel = "UTC";
        } else if ("Asia/Kolkata".equalsIgnoreCase(zoneId.getId()) || "Asia/Calcutta".equalsIgnoreCase(zoneId.getId())) {
            shortLabel = "IST";
        }
        this.timeZoneLabel = shortLabel;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.releaseAtDisplay = zdt.format(formatter) + " " + shortLabel;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public LocalDateTime getReleaseAt() {
        return releaseAt;
    }

    public void setReleaseAt(LocalDateTime releaseAt) {
        this.releaseAt = releaseAt;
        computeDisplayFields();
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
        computeDisplayFields();
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
}
