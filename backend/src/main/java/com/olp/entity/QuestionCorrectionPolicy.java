package com.olp.entity;

/**
 * Policy defining what happens to marks when a question is found invalid or ambiguous (FR-I66, Step 41).
 * Uniformity enforced across all attempts once attempts start (DR-ED05).
 */
public enum QuestionCorrectionPolicy {
    AWARD_FULL_MARKS("Award full marks to all attempts"),
    DROP_AND_RESCALE("Drop invalid question and rescale total marks");

    private final String description;

    QuestionCorrectionPolicy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static QuestionCorrectionPolicy fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return AWARD_FULL_MARKS;
        }
        for (QuestionCorrectionPolicy p : values()) {
            if (p.name().equalsIgnoreCase(value.trim())) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown question correction policy: " + value +
                ". Valid options are: AWARD_FULL_MARKS, DROP_AND_RESCALE");
    }
}
