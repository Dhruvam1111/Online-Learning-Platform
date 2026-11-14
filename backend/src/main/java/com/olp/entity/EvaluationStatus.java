package com.olp.entity;

/**
 * Lifecycle status of an individual question evaluation (FR-I22, FR-I30).
 */
public enum EvaluationStatus {
    CORRECT,
    INCORRECT,
    PARTIALLY_CORRECT,
    MANUAL_GRADING_REQUIRED,
    EVALUATED_UNPUBLISHED,
    PUBLISHED,
    UNANSWERED,
    INVALID_SUBMISSION
}
