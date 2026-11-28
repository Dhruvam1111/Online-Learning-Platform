package com.olp.dto;

import java.time.OffsetDateTime;

/** Suggestions are read-only. The student decides whether to schedule them. */
public record StudySuggestionResponse(
        Integer courseId, String courseTitle, String sourceType, Integer sourceId,
        String title, OffsetDateTime dueAt, int suggestedMinutes, String reason
) {}
