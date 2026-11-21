package com.olp.dto;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

/** API times accept explicit UTC offsets and are stored normalized to UTC. */
public record StudySessionRequest(
        @NotNull Integer courseId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String notes,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt
) {}
