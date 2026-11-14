package com.olp.exception;

/**
 * Thrown when an assignment submission arrives after the deadline and the late policy is REJECT (FR-I68, Step 28).
 * Extends IllegalStateException (HTTP 409 Conflict) for consistent API error reporting.
 */
public class AssignmentLateException extends IllegalStateException {

    public AssignmentLateException(String message) {
        super(message);
    }
}

