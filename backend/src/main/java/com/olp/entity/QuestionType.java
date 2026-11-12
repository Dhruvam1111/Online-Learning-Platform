package com.olp.entity;

/**
 * Extensible question types supporting future question formats and marking schemes (FR-I19, FR-I21, FR-I24).
 */
public enum QuestionType {
    MULTIPLE_CHOICE,
    MULTIPLE_SELECT,
    TRUE_FALSE,
    NUMERICAL,
    DESCRIPTIVE,
    SHORT_ANSWER;

    public static QuestionType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Question type cannot be blank");
        }
        for (QuestionType type : QuestionType.values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Invalid question type: " + value
                        + ". Allowed: MULTIPLE_CHOICE, MULTIPLE_SELECT, TRUE_FALSE, NUMERICAL, DESCRIPTIVE, SHORT_ANSWER");
    }
}

