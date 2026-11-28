package com.olp.dto;

import com.olp.util.DateTimeParseUtils;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
        return DateTimeParseUtils.resolveZoneIdOrDefaultIst(timeZone);
    }

    /**
     * Parses the requested releaseAt string in the target time zone and converts it to UTC.
     */
    public LocalDateTime resolveUtcReleaseAt() {
        return DateTimeParseUtils.parseToUtc(releaseAt, timeZone);
    }
}
