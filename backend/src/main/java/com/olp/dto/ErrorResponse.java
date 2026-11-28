package com.olp.dto;

import org.slf4j.MDC;

import java.time.LocalDateTime;

public class ErrorResponse {

    private int status;
    private String error;
    private String traceId;
    private LocalDateTime timestamp;

    public ErrorResponse(int status, String error) {
        this(status, error, MDC.get("traceId"));
    }

    public ErrorResponse(int status, String error, String traceId) {
        this.status = status;
        this.error = error;
        this.traceId = traceId;
        this.timestamp = LocalDateTime.now();
    }

    // ===== Getters =====

    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return error; }
    public String getTraceId() { return traceId; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
