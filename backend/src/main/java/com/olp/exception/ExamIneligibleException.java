package com.olp.exception;

/**
 * Exception thrown when a student attempts to start an examination but fails eligibility checks (DR-ED07, FR-I25).
 * Extends IllegalStateException so it maps to HTTP 409 Conflict via GlobalExceptionHandler.
 */
public class ExamIneligibleException extends IllegalStateException {

    private final String reason;

    public ExamIneligibleException(String reason, String message) {
        super(reason + ": " + message);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}

