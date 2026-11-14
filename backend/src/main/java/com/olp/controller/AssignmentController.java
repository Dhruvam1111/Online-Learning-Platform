package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller handling assignments, submissions, server-side deadline enforcement,
 * and grading queue hand-off (FR-I68, Step 28).
 */
@RestController
@RequestMapping("/api")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    // =========================================================================
    // INSTRUCTOR ASSIGNMENT MANAGEMENT ENDPOINTS
    // =========================================================================

    @PostMapping({"/instructor/assignments", "/assignments"})
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<AssignmentResponse> createAssignment(
            @Valid @RequestBody AssignmentCreateRequest request,
            Authentication authentication) {
        User currentUser = assignmentService.resolveUser(authentication);
        AssignmentResponse response = assignmentService.createAssignment(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping({"/instructor/assignments/{assignmentId}", "/assignments/{assignmentId}"})
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @PathVariable Integer assignmentId,
            @Valid @RequestBody AssignmentUpdateRequest request,
            Authentication authentication) {
        User currentUser = assignmentService.resolveUser(authentication);
        AssignmentResponse response = assignmentService.updateAssignment(currentUser, assignmentId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/instructor/assignments/{assignmentId}/submissions", "/assignments/{assignmentId}/submissions"})
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<List<AssignmentSubmissionResponse>> getAssignmentSubmissions(
            @PathVariable Integer assignmentId,
            Authentication authentication) {
        User currentUser = assignmentService.resolveUser(authentication);
        List<AssignmentSubmissionResponse> responses = assignmentService.getAssignmentSubmissions(currentUser, assignmentId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping({"/instructor/assignments/{assignmentId}/submissions/{submissionId}/grade",
                  "/assignments/{assignmentId}/submissions/{submissionId}/grade"})
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<AssignmentSubmissionResponse> gradeSubmission(
            @PathVariable Integer assignmentId,
            @PathVariable Integer submissionId,
            @Valid @RequestBody AssignmentGradeRequest request,
            Authentication authentication) {
        User currentUser = assignmentService.resolveUser(authentication);
        AssignmentSubmissionResponse response = assignmentService.gradeSubmission(
                currentUser, submissionId, request.getAwardedMarks(), request.getFeedback());
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // STUDENT & GENERAL COURSE ASSIGNMENT ENDPOINTS
    // =========================================================================

    @GetMapping("/courses/{courseId}/assignments")
    public ResponseEntity<List<AssignmentResponse>> getCourseAssignments(
            @PathVariable Integer courseId,
            Authentication authentication) {
        User currentUser = assignmentService.resolveUser(authentication);
        List<AssignmentResponse> responses = assignmentService.getCourseAssignments(currentUser, courseId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/assignments/{assignmentId}")
    public ResponseEntity<AssignmentResponse> getAssignment(
            @PathVariable Integer assignmentId,
            Authentication authentication) {
        User currentUser = assignmentService.resolveUser(authentication);
        AssignmentResponse response = assignmentService.getAssignment(currentUser, assignmentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assignments/{assignmentId}/submissions")
    @PreAuthorize("hasRole('student')")
    public ResponseEntity<AssignmentSubmissionResponse> submitAssignment(
            @PathVariable Integer assignmentId,
            @Valid @RequestBody AssignmentSubmissionRequest request,
            Authentication authentication) {
        User currentUser = assignmentService.resolveUser(authentication);
        AssignmentSubmissionResponse response = assignmentService.submitAssignment(currentUser, assignmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/assignments/{assignmentId}/my-submission")
    @PreAuthorize("hasRole('student')")
    public ResponseEntity<AssignmentSubmissionResponse> getMySubmission(
            @PathVariable Integer assignmentId,
            Authentication authentication) {
        User currentUser = assignmentService.resolveUser(authentication);
        AssignmentSubmissionResponse response = assignmentService.getMySubmission(currentUser, assignmentId);
        return ResponseEntity.ok(response);
    }
}

