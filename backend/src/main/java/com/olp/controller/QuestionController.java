package com.olp.controller;

import com.olp.dto.ApiResponse;
import com.olp.dto.QuestionRequest;
import com.olp.dto.QuestionResponse;
import com.olp.entity.User;
import com.olp.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for quiz-independent Question CRUD, type validation,
 * and manual-grading routing for DESCRIPTIVE questions (FR-I21).
 */
@RestController
@RequestMapping({"/api/instructor", "/api/instructors"})
@PreAuthorize("hasRole('instructor') or hasRole('admin')")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping("/questions")
    public ResponseEntity<QuestionResponse> createQuestion(
            @Valid @RequestBody QuestionRequest request,
            Authentication authentication) {
        User currentUser = questionService.resolveUser(authentication);
        QuestionResponse response = questionService.createQuestion(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/courses/{courseId}/questions")
    public ResponseEntity<QuestionResponse> createQuestionForCourse(
            @PathVariable Integer courseId,
            @Valid @RequestBody QuestionRequest request,
            Authentication authentication) {
        User currentUser = questionService.resolveUser(authentication);
        request.setCourseId(courseId);
        QuestionResponse response = questionService.createQuestion(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/questions")
    public ResponseEntity<List<QuestionResponse>> getQuestions(
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean requiresManualGrading,
            Authentication authentication) {
        User currentUser = questionService.resolveUser(authentication);
        return ResponseEntity.ok(questionService.getQuestions(currentUser, courseId, type, requiresManualGrading));
    }

    @GetMapping("/courses/{courseId}/questions")
    public ResponseEntity<List<QuestionResponse>> getQuestionsByCourse(
            @PathVariable Integer courseId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean requiresManualGrading,
            Authentication authentication) {
        User currentUser = questionService.resolveUser(authentication);
        return ResponseEntity.ok(questionService.getQuestions(currentUser, courseId, type, requiresManualGrading));
    }

    @GetMapping("/questions/{questionId}")
    public ResponseEntity<QuestionResponse> getQuestionById(
            @PathVariable Integer questionId,
            Authentication authentication) {
        User currentUser = questionService.resolveUser(authentication);
        return ResponseEntity.ok(questionService.getQuestionById(currentUser, questionId));
    }

    @PutMapping("/questions/{questionId}")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable Integer questionId,
            @Valid @RequestBody QuestionRequest request,
            Authentication authentication) {
        User currentUser = questionService.resolveUser(authentication);
        return ResponseEntity.ok(questionService.updateQuestion(currentUser, questionId, request));
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse> deleteQuestion(
            @PathVariable Integer questionId,
            Authentication authentication) {
        User currentUser = questionService.resolveUser(authentication);
        return ResponseEntity.ok(questionService.deleteQuestion(currentUser, questionId));
    }

    @GetMapping("/questions/manual-grading")
    public ResponseEntity<List<QuestionResponse>> getManualGradingQuestions(
            @RequestParam(required = false) Integer courseId,
            Authentication authentication) {
        User currentUser = questionService.resolveUser(authentication);
        return ResponseEntity.ok(questionService.getManualGradingQuestions(currentUser, courseId));
    }

    @PostMapping("/questions/{questionId}/manual-grading")
    public ResponseEntity<QuestionResponse> sendForManualGrading(
            @PathVariable Integer questionId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        User currentUser = questionService.resolveUser(authentication);
        String rubric = body != null ? body.get("gradingRubric") : null;
        return ResponseEntity.ok(questionService.sendForManualGrading(currentUser, questionId, rubric));
    }
}

