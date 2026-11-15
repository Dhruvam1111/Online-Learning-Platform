package com.olp.dto;

import java.time.LocalDateTime;

/**
 * Response DTO representing an entry in the append-only review history of an exam attempt (FR-I69, Step 56).
 */
public class ExamAttemptReviewHistoryResponse {

    private Integer id;
    private Integer attemptId;
    private String previousStatus;
    private String newStatus;
    private String action;
    private String notes;
    private String recommendation;
    private Integer reviewedById;
    private String reviewedByName;
    private LocalDateTime createdAt;

    public ExamAttemptReviewHistoryResponse() {}

    public ExamAttemptReviewHistoryResponse(Integer id, Integer attemptId, String previousStatus,
                                            String newStatus, String action, String notes,
                                            String recommendation, Integer reviewedById,
                                            String reviewedByName, LocalDateTime createdAt) {
        this.id = id;
        this.attemptId = attemptId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.action = action;
        this.notes = notes;
        this.recommendation = recommendation;
        this.reviewedById = reviewedById;
        this.reviewedByName = reviewedByName;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public Integer getReviewedById() {
        return reviewedById;
    }

    public void setReviewedById(Integer reviewedById) {
        this.reviewedById = reviewedById;
    }

    public String getReviewedByName() {
        return reviewedByName;
    }

    public void setReviewedByName(String reviewedByName) {
        this.reviewedByName = reviewedByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
