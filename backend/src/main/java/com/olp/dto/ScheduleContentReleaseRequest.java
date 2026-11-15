package com.olp.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Request DTO for configuring scheduled release date/time on course content (FR-I18, Step 20, DR-ED08).
 * Accepts ISO-8601 or standard datetime string and timezone (defaults to Asia/Kolkata / IST).
 */
public class ScheduleContentReleaseRequest {

    @NotBlank(message = "Release date-time is required")
    private String releaseAt;

    private String timeZone = "Asia/Kolkata";

    public ScheduleContentReleaseRequest() {}

    public ScheduleContentReleaseRequest(String releaseAt, String timeZone) {
        this.releaseAt = releaseAt;
        this.timeZone = (timeZone != null && !timeZone.isBlank()) ? timeZone : "Asia/Kolkata";
    }

    public String getReleaseAt() {
        return releaseAt;
    }

    public void setReleaseAt(String releaseAt) {
        this.releaseAt = releaseAt;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = (timeZone != null && !timeZone.isBlank()) ? timeZone : "Asia/Kolkata";
    }

    /**
     * Resolves and normalizes the target ZoneId, falling back to Asia/Kolkata (IST).
     */
    public ZoneId resolveZoneId() {
        if (timeZone == null || timeZone.isBlank()) {
            return ZoneId.of("Asia/Kolkata");
        }
        try {
            if ("IST".equalsIgnoreCase(timeZone.trim())) {
                return ZoneId.of("Asia/Kolkata");
            }
            if ("UTC".equalsIgnoreCase(timeZone.trim()) || "Z".equalsIgnoreCase(timeZone.trim())) {
                return ZoneOffset.UTC;
            }
            return ZoneId.of(timeZone.trim());
        } catch (Exception e) {
            return ZoneId.of("Asia/Kolkata");
        }
    }

    /**
     * Parses the requested releaseAt string in the target time zone and converts it to UTC.
     */
    public LocalDateTime resolveUtcReleaseAt() {
        if (releaseAt == null || releaseAt.isBlank()) {
            throw new IllegalArgumentException("Release date-time cannot be empty");
        }
        String cleaned = releaseAt.trim();
        ZoneId zone = resolveZoneId();

        // 1. Try ISO Offset/Zoned format: e.g. 2026-12-01T10:00:00Z or 2026-12-01T10:00:00+05:30
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(cleaned, DateTimeFormatter.ISO_DATE_TIME);
            return zdt.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {}

        // 2. Try ISO Local format: 2026-12-01T10:00:00
        try {
            LocalDateTime ldt = LocalDateTime.parse(cleaned, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return ldt.atZone(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {}

        // 3. Try standard pattern: yyyy-MM-dd HH:mm:ss
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime ldt = LocalDateTime.parse(cleaned, fmt);
            return ldt.atZone(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {}

        // 4. Try standard date-only: yyyy-MM-dd
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            java.time.LocalDate ld = java.time.LocalDate.parse(cleaned, fmt);
            LocalDateTime ldt = ld.atStartOfDay();
            return ldt.atZone(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {}

        throw new IllegalArgumentException("Invalid date-time format for releaseAt: '" + releaseAt +
                "'. Supported formats: ISO-8601 (yyyy-MM-dd'T'HH:mm:ss) or 'yyyy-MM-dd HH:mm:ss'");
    }
}
