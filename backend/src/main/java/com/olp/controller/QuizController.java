package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Quiz creation, management, and ordered Quiz-Question mapping (FR-I19).
 */
@RestController
@RequestMapping({"/api/instructor", "/api/instructors"})
@PreAuthorize("hasRole('instructor') or hasRole('admin')")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    // =========================================================================
    // QUIZ CRUD ENDPOINTS
    // =========================================================================

    @PostMapping("/quizzes")
    public ResponseEntity<QuizResponse> createQuiz(
            @Valid @RequestBody QuizRequest request,
            Authentication authentication) {
        User currentUser = quizService.resolveUser(authentication);
        QuizResponse response = quizService.createQuiz(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/courses/{courseId}/quizzes")
    public ResponseEntity<QuizResponse> createQuizForCourse(
            @PathVariable Integer courseId,
            @Valid @RequestBody QuizRequest request,
            Authentication authentication) {
        User currentUser = quizService.resolveUser(authentication);
        request.setCourseId(courseId);
        QuizResponse response = quizService.createQuiz(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/quizzes")
    public ResponseEntity<List<QuizResponse>> getMyQuizzes(
            @RequestParam(required = false) String status,
            Authentication authentication) {
        User currentUser = quizService.resolveUser(authentication);
        return ResponseEntity.ok(quizService.getMyQuizzes(currentUser, status));
    }

    @GetMapping("/courses/{courseId}/quizzes")
    public ResponseEntity<List<QuizResponse>> getQuizzesByCourse(
            @PathVariable Integer courseId,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        User currentUser = quizService.resolveUser(authentication);
        return ResponseEntity.ok(quizService.getQuizzesByCourse(currentUser, courseId, status));
    }

    @GetMapping("/courses/{courseId}/modules/{moduleId}/quizzes")
    public ResponseEntity<List<QuizResponse>> getQuizzesByModule(
            @PathVariable Integer courseId,
            @PathVariable Integer moduleId,
            Authentication authentication) {
        User currentUser = quizService.resolveUser(authentication);
        return ResponseEntity.ok(quizService.getQuizzesByModule(currentUser, courseId, moduleId));
    }

    @GetMapping("/quizzes/{quizId}")
    public ResponseEntity<QuizResponse> getQuizById(
            @PathVariable Integer quizId,
            Authentication authentication) {
        User currentUser = quizService.resolveUser(authentication);
        return ResponseEntity.ok(quizService.getQuizById(currentUser, quizId));
    }

    @PutMapping("/quizzes/{quizId}")
    public ResponseEntity<QuizResponse> updateQuiz(
            @PathVariable Integer quizId,
            @Valid @RequestBody QuizRequest request,
            Authentication authentication) {
        User currentUser = quizService.resolveUser(authentication);
        return ResponseEntity.ok(quizService.updateQuiz(currentUser, quizId, request));
    }

    @DeleteMapping("/quizzes/{quizId}")
    public ResponseEntity<ApiResponse> deleteQuiz(
            @PathVariable Integer quizId,
            Authentication authentication) {
        User currentUser = quizService.resolveUser(authentication);
        return ResponseEntity.ok(quizService.deleteQuiz(currentUser, quizId));
    }

    // =========================================================================
    // QUIZ SETTINGS ENDPOINTS (FR-I20)
    // =========================================================================

    @GetMapping("/quizzes/{quizId}/settings")
    public ResponseEntity<QuizSettingsResponse> getQuizSettings(
            @PathVariable Integer quizId,
            Authentication authentication) {
        User currentUser = quizService.resolveUser(authentication);
        return ResponseEntity.ok(quizService.getQuizSettings(currentUser, quizId));
    }

    @PostMapping("/quizzes/{quizId}/settings")
    public ResponseEntity<QuizSettingsResponse> createOrConfigureQuizSettings(
            @PathVariable Integer quizId,
            @Valid @RequestBody QuizSettingsRequest request,
            Authentication authentication) {
        User currentUser = quizService.resolveUser(authentication);
        return ResponseEntity.ok(quizService.configureQuizSettings(currentUser, quizId, request));
    }

    @PutMapping("/quizzes/{quizId}/settings")
    public ResponseEntity<QuizSettingsResponse> updateQuizSettings(
            @PathVariable Integer quizId,
            @Valid @RequestBody QuizSettingsRequest request,
            Authentication authentication) {
        User currentUser = quizService.resolveUser(authentication);
        return ResponseEntity.ok(quizService.configureQuizSettings(currentUser, quizId, request));
    }

    // =========================================================================
    // QUIZ-QUESTION MAPPING & ORDERING ENDPOINTS
    // =========================================================================

    @GetMapping("/quizzes/{quizId}/questions")
    public ResponseEntity<List<QuizQuestionResponse>> getQuizQuestions(
            @PathVariable Integer quizId,
            Authentication authentication) {
        User currentUser = quizService.resolveUser(authentication);
        return ResponseEntity.ok(quizService.getQuizQuestions(currentUser, quizId));
    }

    @PostMapping("/quizzes/{quizId}/questions")
    public ResponseEntity<QuizQuestionResponse> addQuestionToQuiz(
            @PathVariable Integer quizId,
            @Valid @RequestBody QuizQuestionRequest request,
            Authentication authentication) {
        User currentUser = quizService.resolveUser(authentication);
        QuizQuestionResponse response = quizService.addQuestionToQuiz(currentUser, quizId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/quizzes/{quizId}/questions/{quizQuestionId}")
    public ResponseEntity<QuizQuestionResponse> updateQuizQuestion(
            @PathVariable Integer quizId,
            @PathVariable Integer quizQuestionId,
            @Valid @RequestBody QuizQuestionRequest request,
            Authentication authentication) {
        User currentUser = quizService.resolveUser(authentication);
        return ResponseEntity.ok(quizService.updateQuizQuestion(currentUser, quizId, quizQuestionId, request));
    }

    @DeleteMapping("/quizzes/{quizId}/questions/{quizQuestionId}")
    public ResponseEntity<ApiResponse> removeQuestionFromQuiz(
            @PathVariable Integer quizId,
            @PathVariable Integer quizQuestionId,
            Authentication authentication) {
        User currentUser = quizService.resolveUser(authentication);
        return ResponseEntity.ok(quizService.removeQuestionFromQuiz(currentUser, quizId, quizQuestionId));
    }

    @PutMapping("/quizzes/{quizId}/questions/reorder")
    public ResponseEntity<List<QuizQuestionResponse>> reorderQuizQuestions(
            @PathVariable Integer quizId,
            @Valid @RequestBody ReorderStructureRequest request,
            Authentication authentication) {
        User currentUser = quizService.resolveUser(authentication);
        return ResponseEntity.ok(quizService.reorderQuizQuestions(currentUser, quizId, request));
    }
}

