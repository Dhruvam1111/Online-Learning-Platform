package com.olp.entity;

/**
 * Status of a reported question lifecycle (FR-I35 Step 52, FR-I36 Step 53).
 */
public enum ReportedQuestionStatus {
    PENDING,
    UNDER_REVIEW,
    RESOLVED_CORRECTED,
    RESOLVED_INVALIDATED,
    DISMISSED,
    ESCALATED
}
