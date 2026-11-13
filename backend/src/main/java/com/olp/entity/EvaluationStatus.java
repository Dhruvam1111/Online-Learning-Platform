package com.olp.entity;

/**
 * Lifecycle status of an individual question evaluation (FR-I22).
 */
public enum EvaluationStatus {
    CORRECT,
    INCORRECT,
    PARTIALLY_CORRECT,
    MANUAL_GRADING_REQUIRED,
    UNANSWERED,
    INVALID_SUBMISSION
}

