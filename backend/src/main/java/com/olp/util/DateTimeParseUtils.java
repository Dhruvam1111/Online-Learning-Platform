package com.olp.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Shared date-time parsing and timezone normalization utilities across services and DTOs.
 */
public final class DateTimeParseUtils {

    private static final ZoneId DEFAULT_IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter STANDARD_SPACE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]");

    private DateTimeParseUtils() {}

    /**
     * Resolves timezone strings (supporting aliases "IST", "UTC", "Z"), defaulting to Asia/Kolkata.
     */
    public static ZoneId resolveZoneIdOrDefaultIst(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return DEFAULT_IST_ZONE;
        }
        String trimmed = timeZone.trim();
        try {
            if ("IST".equalsIgnoreCase(trimmed)) {
                return DEFAULT_IST_ZONE;
            }
            if ("UTC".equalsIgnoreCase(trimmed) || "Z".equalsIgnoreCase(trimmed)) {
                return ZoneOffset.UTC;
            }
            return ZoneId.of(trimmed);
        } catch (Exception e) {
            return DEFAULT_IST_ZONE;
        }
    }

    /**
     * Parses a date-time string in the given zone and normalizes it to UTC LocalDateTime.
     */
    public static LocalDateTime parseToUtc(String input, String timeZone) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Release date-time cannot be empty");
        }
        String cleaned = input.trim();
        ZoneId zone = resolveZoneIdOrDefaultIst(timeZone);

        try {
            ZonedDateTime zdt = ZonedDateTime.parse(cleaned, DateTimeFormatter.ISO_DATE_TIME);
            return zdt.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {}

        try {
            LocalDateTime ldt = LocalDateTime.parse(cleaned, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return ldt.atZone(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {}

        try {
            LocalDateTime ldt = LocalDateTime.parse(cleaned, STANDARD_SPACE_FORMAT);
            return ldt.atZone(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {}

        try {
            LocalDate ld = LocalDate.parse(cleaned, DateTimeFormatter.ISO_LOCAL_DATE);
            return ld.atStartOfDay().atZone(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {}

        throw new IllegalArgumentException("Invalid date-time format for releaseAt: '" + input +
                "'. Supported formats: ISO-8601 (yyyy-MM-dd'T'HH:mm:ss) or 'yyyy-MM-dd HH:mm:ss'");
    }

    /**
     * Parses optional date or date-time range bounds into LocalDateTime in the system timezone.
     */
    public static LocalDateTime parseFlexibleDateTime(String input, boolean isEndOfDay) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        String cleaned = input.trim();
        try {
            LocalDate date = LocalDate.parse(cleaned);
            return isEndOfDay ? date.atTime(LocalTime.MAX) : date.atStartOfDay();
        } catch (DateTimeParseException ignored) {}

        try {
            return LocalDateTime.parse(cleaned);
        } catch (DateTimeParseException ignored) {}

        try {
            return LocalDateTime.parse(cleaned, STANDARD_SPACE_FORMAT);
        } catch (DateTimeParseException ignored) {}

        try {
            Instant instant = Instant.parse(cleaned);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (DateTimeParseException ignored) {}

        throw new IllegalArgumentException("Invalid date format: " + input + ". Expected yyyy-MM-dd or ISO date-time.");
    }
}
