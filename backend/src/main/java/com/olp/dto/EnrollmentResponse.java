package com.olp.dto;

public record EnrollmentResponse(
        boolean success,
        String message,
        Integer courseId,
        boolean enrolled
) {}
