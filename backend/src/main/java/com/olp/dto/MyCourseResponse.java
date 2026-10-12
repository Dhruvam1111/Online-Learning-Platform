package com.olp.dto;

import java.time.LocalDateTime;

public record MyCourseResponse(
        Integer courseId,
        String title,
        String instructorName,
        LocalDateTime enrolledAt
) {}