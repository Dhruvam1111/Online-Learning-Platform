package com.olp.controller;

import com.olp.dto.AffectedAttemptsCountResponse;
import com.olp.dto.ReportedQuestionDetailResponse;
import com.olp.dto.ReportedQuestionInboxResponse;
import com.olp.dto.ReportedQuestionReviewRequest;
import com.olp.entity.AssessmentType;
import com.olp.entity.ReportedQuestionStatus;
import com.olp.entity.User;
import com.olp.service.ReportedQuestionReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for instructor review of reported questions (FR-I36, Step 53).
 */
@RestController
@RequestMapping("/api/instructor/reported-questions")
public class ReportedQuestionReviewController {

    private final ReportedQuestionReviewService reviewService;

    @Autowired
    public ReportedQuestionReviewController(ReportedQuestionReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * List reported questions with filters for course, assessment type, assessment ID, and status (FR-I36, Step 53).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<ReportedQuestionInboxResponse>> getReportedQuestions(
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) AssessmentType assessmentType,
            @RequestParam(required = false) Integer assessmentId,
            @RequestParam(required = false) ReportedQuestionStatus status,
            Authentication authentication) {
        User instructor = reviewService.resolveUser(authentication);
        List<ReportedQuestionInboxResponse> list = reviewService.getReportedQuestions(
                instructor, courseId, assessmentType, assessmentId, status);
        return ResponseEntity.ok(list);
    }

    /**
     * Get report details for a specific reported question including all student reports (FR-I36, Step 53).
     */
    @GetMapping("/{reportedQuestionId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ReportedQuestionDetailResponse> getReportDetails(
            @PathVariable Integer reportedQuestionId,
            Authentication authentication) {
        User instructor = reviewService.resolveUser(authentication);
        ReportedQuestionDetailResponse details = reviewService.getReportDetails(instructor, reportedQuestionId);
        return ResponseEntity.ok(details);
    }

    /**
     * Preview count of student attempts that would be affected before taking an action (FR-I36, Step 53).
     */
    @GetMapping("/{reportedQuestionId}/affected-attempts")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<AffectedAttemptsCountResponse> getAffectedAttemptsCount(
            @PathVariable Integer reportedQuestionId,
            Authentication authentication) {
        User instructor = reviewService.resolveUser(authentication);
        AffectedAttemptsCountResponse response = reviewService.getAffectedAttemptsCount(instructor, reportedQuestionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Execute review action (CORRECT, INVALIDATE, DISMISS) with mandatory comment (FR-I36, Step 53).
     */
    @PostMapping("/{reportedQuestionId}/review")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ReportedQuestionDetailResponse> reviewReportedQuestion(
            @PathVariable Integer reportedQuestionId,
            @Valid @RequestBody ReportedQuestionReviewRequest request,
            Authentication authentication) {
        User instructor = reviewService.resolveUser(authentication);
        ReportedQuestionDetailResponse response = reviewService.reviewReportedQuestion(
                instructor, reportedQuestionId, request);
        return ResponseEntity.ok(response);
    }
}
