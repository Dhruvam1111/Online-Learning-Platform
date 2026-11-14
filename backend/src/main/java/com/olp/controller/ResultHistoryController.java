package com.olp.controller;

import com.olp.dto.CreateResultVersionRequest;
import com.olp.dto.ResultVersionResponse;
import com.olp.entity.AssessmentType;
import com.olp.entity.User;
import com.olp.service.ResultHistoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for viewing and appending result change history (FR-I74, Step 49).
 * Strictly read-only for history consumers; no edit or delete paths exist (DR-ED04).
 */
@RestController
@RequestMapping("/api")
public class ResultHistoryController {

    private final ResultHistoryService resultHistoryService;

    public ResultHistoryController(ResultHistoryService resultHistoryService) {
        this.resultHistoryService = resultHistoryService;
    }

    /**
     * Get result change history for a course, with optional filters by student and assessment.
     */
    @GetMapping("/instructor/courses/{courseId}/result-history")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<ResultVersionResponse>> getResultHistory(
            @PathVariable Integer courseId,
            @RequestParam(required = false) Integer studentId,
            @RequestParam(required = false) AssessmentType assessmentType,
            @RequestParam(required = false) Integer assessmentId,
            Authentication authentication) {
        User currentUser = resultHistoryService.resolveUser(authentication);
        List<ResultVersionResponse> history = resultHistoryService.getResultHistory(
                currentUser, courseId, studentId, assessmentType, assessmentId);
        return ResponseEntity.ok(history);
    }

    /**
     * Record a new result version change (for overrides, corrections, and result publication adjustments).
     */
    @PostMapping("/instructor/courses/{courseId}/result-history")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ResultVersionResponse> recordResultVersion(
            @PathVariable Integer courseId,
            @Valid @RequestBody CreateResultVersionRequest request,
            Authentication authentication) {
        User currentUser = resultHistoryService.resolveUser(authentication);
        ResultVersionResponse response = resultHistoryService.recordResultVersion(currentUser, courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
