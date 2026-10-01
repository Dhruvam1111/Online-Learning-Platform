package com.onlinelearning.model;

public enum Role {
    STUDENT("student"),
    INSTRUCTOR("instructor");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Role fromString(String text) {
        if (text == null) {
            return null;
        }
        for (Role role : Role.values()) {
            if (role.value.equalsIgnoreCase(text) || role.name().equalsIgnoreCase(text)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + text + ". Allowed roles: student, instructor");
    }
}
