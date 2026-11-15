package com.olp.entity;

/**
 * Lifecycle states for video lecture uploads and transcoding pipeline (FR-I13, FR-I15).
 */
public enum VideoUploadStatus {
    INITIATED,
    UPLOADING,
    COMPLETED,
    TRANSCODING,
    READY,
    FAILED,
    CANCELLED;

    /**
     * Validates whether a state transition from this status to next is permissible.
     * Prevents invalid leaps, such as jumping from FAILED directly to READY without re-upload.
     */
    public boolean canTransitionTo(VideoUploadStatus next) {
        if (next == null) {
            return false;
        }
        if (this == next) {
            return true; // Idempotent no-op
        }
        return switch (this) {
            case INITIATED -> next == UPLOADING || next == CANCELLED || next == FAILED;
            case UPLOADING -> next == COMPLETED || next == FAILED || next == CANCELLED;
            case COMPLETED -> next == TRANSCODING || next == FAILED || next == READY;
            case TRANSCODING -> next == READY || next == FAILED;
            case READY -> false; // Terminal state for this upload job
            case FAILED -> false; // Terminal state; requires a fresh re-upload job
            case CANCELLED -> false; // Terminal state
        };
    }
}

