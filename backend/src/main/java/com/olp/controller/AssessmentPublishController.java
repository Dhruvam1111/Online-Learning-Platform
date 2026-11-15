package com.olp.controller;

import com.olp.dto.AssessmentPublishStatusResponse;
import com.olp.dto.PublishAssessmentResultsRequest;
import com.olp.dto.WithholdAssessmentResultsRequest;
import com.olp.entity.AssessmentType;
import com.olp.entity.User;
import com.olp.service.AssessmentPublishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for instructor assessment result publication, withholding, and status checking (FR-I34, Step 51).
 */
@RestController
@RequestMapping("/api/instructor/assessments")
public class AssessmentPublishController {

    private final AssessmentPublishService assessmentPublishService;

    @Autowired
    public AssessmentPublishController(AssessmentPublishService assessmentPublishService) {
        this.assessmentPublishService = assessmentPublishService;
    }

    /**
     * Get result publication state and grading progress for an assessment.
     */
    @GetMapping("/{assessmentType}/{assessmentId}/results/status")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<AssessmentPublishStatusResponse> getPublishStatus(
            @PathVariable String assessmentType,
            @PathVariable Integer assessmentId,
            Authentication authentication) {
        User currentUser = assessmentPublishService.resolveUser(authentication);
        AssessmentType type = assessmentPublishService.parseAssessmentType(assessmentType);
        AssessmentPublishStatusResponse response = assessmentPublishService.getPublishStatus(currentUser, type, assessmentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Publish results to students, locking evaluated records (DR-01).
     * Requires explicit confirmation if ungraded items remain.
     */
    @PostMapping("/{assessmentType}/{assessmentId}/results/publish")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<AssessmentPublishStatusResponse> publishResults(
            @PathVariable String assessmentType,
            @PathVariable Integer assessmentId,
            @RequestBody(required = false) PublishAssessmentResultsRequest request,
            Authentication authentication) {
        User currentUser = assessmentPublishService.resolveUser(authentication);
        AssessmentType type = assessmentPublishService.parseAssessmentType(assessmentType);
        AssessmentPublishStatusResponse response = assessmentPublishService.publishResults(currentUser, type, assessmentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Withhold assessment results from students.
     */
    @PostMapping("/{assessmentType}/{assessmentId}/results/withhold")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<AssessmentPublishStatusResponse> withholdResults(
            @PathVariable String assessmentType,
            @PathVariable Integer assessmentId,
            @RequestBody(required = false) WithholdAssessmentResultsRequest request,
            Authentication authentication) {
        User currentUser = assessmentPublishService.resolveUser(authentication);
        AssessmentType type = assessmentPublishService.parseAssessmentType(assessmentType);
        AssessmentPublishStatusResponse response = assessmentPublishService.withholdResults(currentUser, type, assessmentId, request);
        return ResponseEntity.ok(response);
    }
}
