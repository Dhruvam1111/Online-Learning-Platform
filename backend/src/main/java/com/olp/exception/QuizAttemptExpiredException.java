package com.olp.exception;

/**
 * Thrown when a quiz attempt exceeds its server-side timer deadline (DR-ED06).
 * Extends IllegalStateException (HTTP 409) so @Transactional(noRollbackFor = QuizAttemptExpiredException.class)
 * can persist the EXPIRED status cleanly while reporting the conflict to the client.
 */
public class QuizAttemptExpiredException extends IllegalStateException {

    public QuizAttemptExpiredException(String message) {
        super(message);
    }
}

