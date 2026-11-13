package com.olp.dto;

import java.time.LocalDateTime;

/**
 * Response DTO after publishing graded descriptive assessment results (FR-I28, FR-I30, FR-I34).
 */
public class PublishResultResponse {

    private String message;
    private Integer publishedCount;
    private Integer quizId;
    private Integer attemptId;
    private LocalDateTime publishedAt;

    public PublishResultResponse() {}

    public PublishResultResponse(String message, Integer publishedCount, Integer quizId, Integer attemptId) {
        this.message = message;
        this.publishedCount = publishedCount;
        this.quizId = quizId;
        this.attemptId = attemptId;
        this.publishedAt = LocalDateTime.now();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getPublishedCount() {
        return publishedCount;
    }

    public void setPublishedCount(Integer publishedCount) {
        this.publishedCount = publishedCount;
    }

    public Integer getQuizId() {
        return quizId;
    }

    public void setQuizId(Integer quizId) {
        this.quizId = quizId;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
