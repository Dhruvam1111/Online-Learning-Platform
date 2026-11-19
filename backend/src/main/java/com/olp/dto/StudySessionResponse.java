package com.olp.dto;

import java.time.OffsetDateTime;

public record StudySessionResponse(
        Integer id, Integer courseId, String courseTitle, String title,
        String notes, OffsetDateTime startsAt, OffsetDateTime endsAt, boolean completed
) {}
