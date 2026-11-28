package com.olp.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeParseUtilsTest {

    @Test
    void resolveZoneIdOrDefaultIst_HandlesAliasesNullAndInvalidZones() {
        ZoneId ist = ZoneId.of("Asia/Kolkata");
        assertEquals(ist, DateTimeParseUtils.resolveZoneIdOrDefaultIst(null));
        assertEquals(ist, DateTimeParseUtils.resolveZoneIdOrDefaultIst("  "));
        assertEquals(ist, DateTimeParseUtils.resolveZoneIdOrDefaultIst("IST"));
        assertEquals(ZoneOffset.UTC, DateTimeParseUtils.resolveZoneIdOrDefaultIst("UTC"));
        assertEquals(ZoneOffset.UTC, DateTimeParseUtils.resolveZoneIdOrDefaultIst("z"));
        assertEquals(ZoneId.of("America/New_York"), DateTimeParseUtils.resolveZoneIdOrDefaultIst("America/New_York"));
        assertEquals(ist, DateTimeParseUtils.resolveZoneIdOrDefaultIst("Invalid/Timezone"));
    }

    @Test
    void parseToUtc_ConvertsVariousSupportedFormatsAccurately() {
        assertEquals(LocalDateTime.of(2026, 12, 1, 10, 0, 0),
                DateTimeParseUtils.parseToUtc("2026-12-01T10:00:00Z", "IST"));

        assertEquals(LocalDateTime.of(2026, 12, 1, 4, 30, 0),
                DateTimeParseUtils.parseToUtc("2026-12-01T10:00:00", "IST"));

        assertEquals(LocalDateTime.of(2026, 12, 1, 4, 30, 0),
                DateTimeParseUtils.parseToUtc("2026-12-01 10:00:00", "Asia/Kolkata"));

        assertEquals(LocalDateTime.of(2026, 12, 1, 0, 0, 0),
                DateTimeParseUtils.parseToUtc("2026-12-01", "UTC"));
    }

    @Test
    void parseToUtc_RejectsBlankOrInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> DateTimeParseUtils.parseToUtc(null, "UTC"));
        assertThrows(IllegalArgumentException.class, () -> DateTimeParseUtils.parseToUtc("   ", "UTC"));
        assertThrows(IllegalArgumentException.class, () -> DateTimeParseUtils.parseToUtc("not-a-date", "UTC"));
    }

    @Test
    void parseFlexibleDateTime_HandlesEmptyDateBoundsAndFormats() {
        assertNull(DateTimeParseUtils.parseFlexibleDateTime(null, false));
        assertNull(DateTimeParseUtils.parseFlexibleDateTime(" ", true));

        LocalDateTime start = DateTimeParseUtils.parseFlexibleDateTime("2026-11-28", false);
        assertEquals(LocalDateTime.of(2026, 11, 28, 0, 0, 0), start);

        LocalDateTime end = DateTimeParseUtils.parseFlexibleDateTime("2026-11-28", true);
        assertEquals(2026, end.getYear());
        assertEquals(23, end.getHour());
        assertEquals(59, end.getMinute());

        assertEquals(LocalDateTime.of(2026, 11, 28, 14, 30, 0),
                DateTimeParseUtils.parseFlexibleDateTime("2026-11-28 14:30:00", false));

        assertThrows(IllegalArgumentException.class,
                () -> DateTimeParseUtils.parseFlexibleDateTime("28/11/2026", false));
    }
}
