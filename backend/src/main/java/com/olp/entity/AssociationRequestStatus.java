package com.olp.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status lifecycle for course association requests (FR-I58).
 */
public enum AssociationRequestStatus {
    PENDING,
    APPROVED,
    REJECTED;

    @JsonCreator
    public static AssociationRequestStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Association request status cannot be blank");
        }
        String normalized = value.trim().toUpperCase();
        for (AssociationRequestStatus status : values()) {
            if (status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException(
                "Invalid association request status: '" + value + "'. Valid statuses are PENDING, APPROVED, and REJECTED");
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}

