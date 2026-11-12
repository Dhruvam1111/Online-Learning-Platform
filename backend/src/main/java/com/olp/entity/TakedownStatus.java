package com.olp.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TakedownStatus {
    NONE,
    FLAGGED,
    RESPONDED,
    REPLACED,
    REMOVED;

    @JsonCreator
    public static TakedownStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NONE;
        }
        String normalized = value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (TakedownStatus status : values()) {
            if (status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid takedown status: '" + value + "'");
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}

