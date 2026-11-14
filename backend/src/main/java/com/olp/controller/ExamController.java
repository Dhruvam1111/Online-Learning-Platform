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

    public ExamController(ExamService examService) {
        this.examService = examService;
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
}

