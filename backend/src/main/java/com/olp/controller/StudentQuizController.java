package com.olp.controller;

import com.olp.dto.QuizAttemptResponse;
import com.olp.dto.QuizEvaluationSummary;
import com.olp.dto.QuizSettingsResponse;
import com.olp.dto.QuizSubmissionRequest;
import com.olp.entity.User;
import com.olp.service.QuizAttemptService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Student-facing REST endpoints for reading quiz settings, availability, attempt limits,
 * and server-enforced timed quiz attempts (FR-I20, DR-ED06, S19, S22, S24).
 */
@RestController
@RequestMapping({"/api/student/quizzes", "/api/students/quizzes"})
@PreAuthorize("hasRole('student')")
public class StudentQuizController {

    private final QuizAttemptService quizAttemptService;

    public StudentQuizController(QuizAttemptService quizAttemptService) {
        this.quizAttemptService = quizAttemptService;
    }

    @GetMapping("/{quizId}/settings")
    public ResponseEntity<QuizSettingsResponse> getStudentQuizSettings(
            @PathVariable Integer quizId,
            Authentication authentication) {
        User currentUser = quizAttemptService.resolveUser(authentication);
        return ResponseEntity.ok(quizAttemptService.getStudentQuizSettings(currentUser, quizId));
    }

    @PostMapping("/{quizId}/attempts")
    public ResponseEntity<QuizAttemptResponse> startQuizAttempt(
            @PathVariable Integer quizId,
            Authentication authentication) {
        User currentUser = quizAttemptService.resolveUser(authentication);
        QuizAttemptResponse response = quizAttemptService.startQuizAttempt(currentUser, quizId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{quizId}/attempts")
    public ResponseEntity<List<QuizAttemptResponse>> getStudentQuizAttempts(
            @PathVariable Integer quizId,
            Authentication authentication) {
        User currentUser = quizAttemptService.resolveUser(authentication);
        return ResponseEntity.ok(quizAttemptService.getStudentQuizAttempts(currentUser, quizId));
    }

    @GetMapping("/{quizId}/attempts/{attemptId}")
    public ResponseEntity<QuizAttemptResponse> getQuizAttemptStatus(
            @PathVariable Integer quizId,
            @PathVariable Integer attemptId,
            Authentication authentication) {
        User currentUser = quizAttemptService.resolveUser(authentication);
        return ResponseEntity.ok(quizAttemptService.getQuizAttemptStatus(currentUser, quizId, attemptId));
    }

    @PostMapping("/{quizId}/attempts/{attemptId}/submit")
    public ResponseEntity<QuizAttemptResponse> submitQuizAttempt(
            @PathVariable Integer quizId,
            @PathVariable Integer attemptId,
            @RequestBody(required = false) QuizSubmissionRequest submissionRequest,
            Authentication authentication) {
        User currentUser = quizAttemptService.resolveUser(authentication);
        return ResponseEntity.ok(quizAttemptService.submitQuizAttempt(currentUser, quizId, attemptId, submissionRequest));
    }

    @GetMapping("/{quizId}/attempts/{attemptId}/evaluation")
    public ResponseEntity<QuizEvaluationSummary> getAttemptEvaluation(
            @PathVariable Integer quizId,
            @PathVariable Integer attemptId,
            Authentication authentication) {
        User currentUser = quizAttemptService.resolveUser(authentication);
        return ResponseEntity.ok(quizAttemptService.getAttemptEvaluation(currentUser, quizId, attemptId));
    }
}

