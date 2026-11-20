package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.ExamActivityLogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for student examination activity logging, proctoring events, anti-cheating telemetry,
 * screen-sharing permissions, and randomized questions
 * (FR-S52, FR-S53, FR-S54, FR-S55, FR-S56, FR-S57, FR-S58, FR-S59, FR-S60, FR-S61).
 */
@RestController
@RequestMapping("/api/student/exams/attempts/{attemptId}")
@PreAuthorize("hasRole('student') or hasRole('instructor') or hasRole('admin')")
public class StudentExamMonitoringController {

    private final ExamActivityLogService examActivityLogService;

    public StudentExamMonitoringController(ExamActivityLogService examActivityLogService) {
        this.examActivityLogService = examActivityLogService;
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return user;
    }

    @PostMapping("/events")
    public ResponseEntity<ExamMonitoringEventResponse> recordEvent(
            @PathVariable Integer attemptId,
            @Valid @RequestBody ExamMonitoringEventRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        ExamMonitoringEventResponse response = examActivityLogService.recordMonitoringEvent(user, attemptId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/screen-share-consent")
    public ResponseEntity<ApiResponse> recordScreenShareConsent(
            @PathVariable Integer attemptId,
            @Valid @RequestBody ScreenShareConsentRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        examActivityLogService.recordScreenShareConsent(user, attemptId, request);
        return ResponseEntity.ok(new ApiResponse(true, "Screen sharing consent recorded successfully"));
    }

    @GetMapping("/events")
    public ResponseEntity<List<ExamMonitoringLogDto>> getAttemptLogs(
            @PathVariable Integer attemptId,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(examActivityLogService.getAttemptLogs(user, attemptId));
    }

    @GetMapping("/questions")
    public ResponseEntity<List<ExamQuestionItemDto>> getAttemptQuestions(
            @PathVariable Integer attemptId,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(examActivityLogService.getStudentAttemptQuestions(user, attemptId));
    }
}
