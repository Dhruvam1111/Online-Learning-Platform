package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.StudentAiLearningService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for student AI learning assistant, automated summaries, explanations,
 * weak topic detection, and personalized study recommendations
 * (FR-S71, FR-S72, FR-S73, FR-S74, FR-S75, FR-S76, NFR-04).
 */
@RestController
@RequestMapping("/api/student/ai")
@PreAuthorize("hasRole('student') or hasRole('instructor') or hasRole('admin')")
public class StudentAiAssistantController {

    private final StudentAiLearningService aiLearningService;

    public StudentAiAssistantController(StudentAiLearningService aiLearningService) {
        this.aiLearningService = aiLearningService;
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return user;
    }

    @PostMapping("/ask")
    public ResponseEntity<AiAskResponse> askAssistant(
            @Valid @RequestBody AiAskRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(aiLearningService.askAssistant(user, request));
    }

    @PostMapping("/summarize")
    public ResponseEntity<AiSummaryResponse> generateSummary(
            @Valid @RequestBody AiSummaryRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(aiLearningService.generateSummary(user, request));
    }

    @GetMapping("/questions/{questionId}/explanation")
    public ResponseEntity<QuestionExplanationResponse> getQuestionExplanation(
            @PathVariable Integer questionId,
            @RequestParam(required = false) Integer attemptId,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(aiLearningService.getQuestionExplanation(user, questionId, attemptId));
    }

    @GetMapping("/courses/{courseId}/weak-topics")
    public ResponseEntity<WeakTopicsResponse> getWeakTopics(
            @PathVariable Integer courseId,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(aiLearningService.identifyWeakTopics(user, courseId));
    }

    @GetMapping("/courses/{courseId}/recommendations")
    public ResponseEntity<ResourceRecommendationResponse> getRecommendations(
            @PathVariable Integer courseId,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(aiLearningService.recommendLearningResources(user, courseId));
    }
}
