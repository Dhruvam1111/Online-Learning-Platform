package com.olp.entity;

/**
 * Lifecycle status for quizzes (FR-I19).
 */
public enum QuizStatus {
    DRAFT,
    PUBLISHED;

    public static QuizStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Quiz status cannot be blank");
        }
        for (QuizStatus status : QuizStatus.values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException(
                "Invalid quiz status: " + value + ". Allowed: DRAFT, PUBLISHED");
    }
}

