package com.olp.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Role assigned to an instructor associated with a course (FR-I59).
 */
public enum CourseInstructorRole {
    OWNER,
    CO_INSTRUCTOR;

    @JsonCreator
    public static CourseInstructorRole fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Instructor role cannot be blank");
        }
        String normalized = value.trim().toUpperCase().replace("-", "_");
        for (CourseInstructorRole role : values()) {
            if (role.name().equalsIgnoreCase(normalized)) {
                return role;
            }
        }
        throw new IllegalArgumentException(
                "Invalid course instructor role: '" + value + "'. Valid roles are OWNER and CO_INSTRUCTOR");
    }

    /**
     * Database representation preserving compatibility with 'owner' and 'co-instructor' CHECK constraint.
     */
    public String toDatabaseValue() {
        return this == OWNER ? "owner" : "co-instructor";
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}

