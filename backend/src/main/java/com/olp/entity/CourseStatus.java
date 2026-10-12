package com.olp.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CourseStatus {
    DRAFT,
    PENDING_REVIEW,
    PUBLISHED,
    REJECTED;

    @JsonCreator
    public static CourseStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Course status cannot be blank");
        }
        String normalized = value.trim().toUpperCase().replace("-", "_");
        for (CourseStatus status : values()) {
            if (status.name().equalsIgnoreCase(normalized) || status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid course status: '" + value + "'. Valid statuses are DRAFT, PENDING_REVIEW, PUBLISHED, and REJECTED");
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
