package com.olp.controller;

import com.olp.dto.ApplyQuestionCorrectionRequest;
import com.olp.dto.ApplyQuestionCorrectionResponse;
import com.olp.entity.User;
import com.olp.service.QuestionCorrectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for applying approved question corrections to results across student attempts (FR-I37, Step 54).
 */
@RestController
@RequestMapping("/api/instructor/reported-questions")
public class QuestionCorrectionController {

    private final QuestionCorrectionService questionCorrectionService;

    @Autowired
    public QuestionCorrectionController(QuestionCorrectionService questionCorrectionService) {
        this.questionCorrectionService = questionCorrectionService;
    }

    /**
     * Apply approved correction to all student attempts for a reported question (FR-I37, Step 54).
     * Enforces idempotency: calling repeatedly yields no duplicate modifications.
     */
    @PostMapping("/{reportedQuestionId}/apply-correction")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApplyQuestionCorrectionResponse> applyCorrection(
            @PathVariable Integer reportedQuestionId,
            @RequestBody(required = false) ApplyQuestionCorrectionRequest request,
            Authentication authentication) {
        User instructor = questionCorrectionService.resolveUser(authentication);
        ApplyQuestionCorrectionResponse response = questionCorrectionService.applyCorrection(
                instructor, reportedQuestionId, request);
        return ResponseEntity.ok(response);
    }
}
