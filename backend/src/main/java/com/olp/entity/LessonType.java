package com.olp.entity;

/**
 * Supported lesson types for course structure (FR-I06).
 * Extensible for interactive materials, quizzes, and SCORM/xAPI packages.
 */
public enum LessonType {
    VIDEO,
    MATERIAL,
    QUIZ,
    ASSIGNMENT;

    public static LessonType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Lesson type cannot be blank");
        }
        for (LessonType type : LessonType.values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid lesson type: " + value + ". Allowed: VIDEO, MATERIAL, QUIZ, ASSIGNMENT");
    }
}

