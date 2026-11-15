package com.olp.entity;

/**
 * Review and escalation status for flagged exam attempts (FR-I69, Step 56).
 */
public enum ExamAttemptReviewStatus {
    NONE,
    PENDING,
    CLEARED,
    ESCALATED
}
