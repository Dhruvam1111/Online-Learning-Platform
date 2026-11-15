package com.olp.controller;

import com.olp.dto.ExamAttemptReviewHistoryResponse;
import com.olp.dto.ExamAttemptReviewRequest;
import com.olp.dto.ExamAttemptReviewResponse;
import com.olp.dto.FlaggedAttemptSummaryResponse;
import com.olp.entity.ExamAttemptReviewStatus;
import com.olp.entity.User;
import com.olp.service.ExamAttemptReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for instructor exam attempt integrity reviews, flagging, and escalation (FR-I69, Step 56).
 */
@RestController
@RequestMapping("/api/instructor/exams/attempts")
public class ExamAttemptReviewController {

    private final ExamAttemptReviewService reviewService;

    @Autowired
    public ExamAttemptReviewController(ExamAttemptReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Get flagged or reviewed attempts across courses or filtered by course/exam/status.
     */
    @GetMapping("/flagged")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<FlaggedAttemptSummaryResponse>> getFlaggedAttempts(
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) Integer examId,
            @RequestParam(required = false) ExamAttemptReviewStatus status,
            Authentication authentication) {
        User currentUser = reviewService.resolveUser(authentication);
        List<FlaggedAttemptSummaryResponse> response = reviewService.getFlaggedAttempts(currentUser, courseId, examId, status);
        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed review information, audit history, and marks for a specific attempt.
     */
    @GetMapping("/{attemptId}/review")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ExamAttemptReviewResponse> getAttemptReviewDetails(
            @PathVariable Integer attemptId,
            Authentication authentication) {
        User currentUser = reviewService.resolveUser(authentication);
        ExamAttemptReviewResponse response = reviewService.getAttemptReviewDetails(currentUser, attemptId);
        return ResponseEntity.ok(response);
    }

    /**
     * Perform a review action on a flagged attempt (FLAG, CLEAR, ESCALATE, UPDATE_NOTES).
     * Enforces DR-07: recommendations only; marks are never altered.
     */
    @PostMapping("/{attemptId}/review")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ExamAttemptReviewResponse> reviewAttempt(
            @PathVariable Integer attemptId,
            @Valid @RequestBody ExamAttemptReviewRequest request,
            Authentication authentication) {
        User currentUser = reviewService.resolveUser(authentication);
        ExamAttemptReviewResponse response = reviewService.reviewAttempt(currentUser, attemptId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the append-only review history for a specific attempt.
     */
    @GetMapping("/{attemptId}/review/history")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<ExamAttemptReviewHistoryResponse>> getReviewHistory(
            @PathVariable Integer attemptId,
            Authentication authentication) {
        User currentUser = reviewService.resolveUser(authentication);
        List<ExamAttemptReviewHistoryResponse> response = reviewService.getReviewHistory(currentUser, attemptId);
        return ResponseEntity.ok(response);
    }
}
