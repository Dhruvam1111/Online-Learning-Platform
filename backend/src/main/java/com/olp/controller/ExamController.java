package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for examination creation, scheduling, eligibility verification,
 * and exam start lifecycle (FR-I25, Step 38).
 */
@RestController
@RequestMapping("/api")
public class ExamController {

    private final ExamService examService;
    private final com.olp.service.ExamRandomizationService examRandomizationService;
    private final com.olp.service.ExamMonitoringService examMonitoringService;

    public ExamController(ExamService examService,
                          com.olp.service.ExamRandomizationService examRandomizationService,
                          com.olp.service.ExamMonitoringService examMonitoringService) {
        this.examService = examService;
        this.examRandomizationService = examRandomizationService;
        this.examMonitoringService = examMonitoringService;
    }

    // =========================================================================
    // INSTRUCTOR EXAM MANAGEMENT ENDPOINTS
    // =========================================================================

    @PostMapping({"/instructor/exams", "/exams"})
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<ExamResponse> createExam(
            @Valid @RequestBody ExamCreateRequest request,
            Authentication authentication) {
        User currentUser = examService.resolveUser(authentication);
        ExamResponse response = examService.createExam(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping({"/instructor/exams/{examId}", "/exams/{examId}"})
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<ExamResponse> updateExam(
            @PathVariable Integer examId,
            @Valid @RequestBody ExamUpdateRequest request,
            Authentication authentication) {
        User currentUser = examService.resolveUser(authentication);
        ExamResponse response = examService.updateExam(currentUser, examId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/instructor/exams/{examId}/questions", "/exams/{examId}/questions"})
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<List<ExamQuestionItemDto>> linkQuestions(
            @PathVariable Integer examId,
            @RequestBody List<Integer> questionIds,
            Authentication authentication) {
        User currentUser = examService.resolveUser(authentication);
        List<ExamQuestionItemDto> response = examService.linkQuestionsToExam(currentUser, examId, questionIds);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/instructor/exams/{examId}/blueprint", "/exams/{examId}/blueprint"})
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<List<ExamBlueprintItemDto>> configureBlueprint(
            @PathVariable Integer examId,
            @Valid @RequestBody ExamBlueprintRequest request,
            Authentication authentication) {
        User currentUser = examService.resolveUser(authentication);
        List<ExamBlueprintItemDto> response = examRandomizationService.configureBlueprint(currentUser, examId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/instructor/exams/{examId}/blueprint", "/exams/{examId}/blueprint"})
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<List<ExamBlueprintItemDto>> getBlueprint(
            @PathVariable Integer examId,
            Authentication authentication) {
        User currentUser = examService.resolveUser(authentication);
        List<ExamBlueprintItemDto> response = examRandomizationService.getBlueprint(currentUser, examId);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/instructor/exams/{examId}/preview-sets", "/exams/{examId}/preview-sets"})
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<ExamSetGenerationResponse> previewExamSets(
            @PathVariable Integer examId,
            @RequestParam(required = false) Long seed,
            @RequestParam(required = false) Integer numSets,
            Authentication authentication) {
        User currentUser = examService.resolveUser(authentication);
        ExamSetGenerationResponse response = examRandomizationService.previewExamSets(currentUser, examId, seed, numSets);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/exams/{examId}/sets")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<ExamSetGenerationResponse> getExamSets(
            @PathVariable Integer examId,
            @RequestParam(required = false) Long seed,
            @RequestParam(required = false) Integer numSets,
            Authentication authentication) {
        User currentUser = examService.resolveUser(authentication);
        ExamSetGenerationResponse response = examRandomizationService.previewExamSets(currentUser, examId, seed, numSets);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // COURSE & EXAM QUERY ENDPOINTS
    // =========================================================================

    @GetMapping("/courses/{courseId}/exams")
    public ResponseEntity<List<ExamResponse>> getCourseExams(
            @PathVariable Integer courseId,
            Authentication authentication) {
        User currentUser = examService.resolveUser(authentication);
        List<ExamResponse> responses = examService.getCourseExams(currentUser, courseId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/exams/{examId}")
    public ResponseEntity<ExamResponse> getExam(
            @PathVariable Integer examId,
            Authentication authentication) {
        User currentUser = examService.resolveUser(authentication);
        ExamResponse response = examService.getExam(currentUser, examId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // STUDENT EXAM ELIGIBILITY & ATTEMPT LIFECYCLE ENDPOINTS
    // =========================================================================

    @GetMapping("/exams/{examId}/eligibility")
    public ResponseEntity<ExamEligibilityResponse> checkEligibility(
            @PathVariable Integer examId,
            Authentication authentication) {
        User currentUser = examService.resolveUser(authentication);
        ExamEligibilityResponse response = examService.checkEligibility(currentUser, examId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/exams/{examId}/start")
    public ResponseEntity<ExamStartResponse> startExam(
            @PathVariable Integer examId,
            Authentication authentication) {
        User currentUser = examService.resolveUser(authentication);
        ExamStartResponse response = examService.startExam(currentUser, examId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // PROCTORING & MONITORING RULES ENDPOINTS (FR-I26, Step 40)
    // =========================================================================

    @PutMapping({"/instructor/exams/{examId}/monitoring", "/exams/{examId}/monitoring"})
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<ExamMonitoringConfigResponse> saveMonitoringConfig(
            @PathVariable Integer examId,
            @Valid @RequestBody ExamMonitoringConfigRequest request,
            Authentication authentication) {
        User currentUser = examService.resolveUser(authentication);
        ExamMonitoringConfigResponse response = examMonitoringService.saveMonitoringConfig(currentUser, examId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/exams/{examId}/monitoring")
    public ResponseEntity<ExamMonitoringConfigResponse> getMonitoringConfig(
            @PathVariable Integer examId,
            @RequestParam(required = false, defaultValue = "true") Boolean browserSupportsFullscreen,
            Authentication authentication) {
        User currentUser = examService.resolveUser(authentication);
        ExamMonitoringConfigResponse response = examMonitoringService.getMonitoringConfig(currentUser, examId, browserSupportsFullscreen);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/exams/{examId}/consent")
    public ResponseEntity<ExamConsentResponse> recordStudentConsent(
            @PathVariable Integer examId,
            @Valid @RequestBody ExamConsentRequest request,
            Authentication authentication) {
        User currentUser = examService.resolveUser(authentication);
        ExamConsentResponse response = examMonitoringService.recordStudentConsent(currentUser, examId, request);
        return ResponseEntity.ok(response);
    }
}

