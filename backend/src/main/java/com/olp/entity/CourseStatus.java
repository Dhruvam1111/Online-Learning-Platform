package com.olp.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CourseStatus {
    DRAFT,
    PUBLISHED;

    @JsonCreator
    public static CourseStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Course status cannot be blank");
        }
        for (CourseStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid course status: '" + value + "'. Valid statuses are DRAFT and PUBLISHED");
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
