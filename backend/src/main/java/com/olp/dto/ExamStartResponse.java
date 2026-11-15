package com.olp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response returned to the student upon starting an examination attempt (FR-I25, Step 38).
 * Includes the server-authoritative timer parameters (DR-ED06).
 */
public class ExamStartResponse {

    private Integer attemptId;
    private Integer examId;
    private String title;
    private Integer attemptNumber;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private Long remainingSeconds;
    private String status;
    private String setLabel;
    private Long randomSeed;
    private LocalDateTime serverTime;
    private Integer durationMinutes;
    private BigDecimal totalMarks;
    private String instructions;
    private List<ExamQuestionItemDto> questions = new ArrayList<>();

    public ExamStartResponse() {}

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer getExamId() {
        return examId;
    }

    public void setExamId(Integer examId) {
        this.examId = examId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getServerTime() {
        return serverTime;
    }

    public void setServerTime(LocalDateTime serverTime) {
        this.serverTime = serverTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public BigDecimal getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(BigDecimal totalMarks) {
        this.totalMarks = totalMarks;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public List<ExamQuestionItemDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<ExamQuestionItemDto> questions) {
        this.questions = questions;
    }

    public String getSetLabel() {
        return setLabel;
    }

    public void setSetLabel(String setLabel) {
        this.setLabel = setLabel;
    }

    public Long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(Long randomSeed) {
        this.randomSeed = randomSeed;
    }
}

