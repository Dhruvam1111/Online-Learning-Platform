package com.olp.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LicenseType {
    ORIGINAL_CREATOR,
    CREATIVE_COMMONS,
    COMMERCIAL_LICENSE,
    PUBLIC_DOMAIN,
    EDUCATIONAL_FAIR_USE;

    @JsonCreator
    public static LicenseType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("License type cannot be blank");
        }
        String normalized = value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (LicenseType type : values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid license type: '" + value + "'. Valid types are ORIGINAL_CREATOR, CREATIVE_COMMONS, COMMERCIAL_LICENSE, PUBLIC_DOMAIN, and EDUCATIONAL_FAIR_USE");
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}

