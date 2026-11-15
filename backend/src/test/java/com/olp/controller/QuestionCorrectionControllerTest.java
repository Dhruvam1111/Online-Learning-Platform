package com.olp.controller;

import com.olp.dto.ApplyQuestionCorrectionRequest;
import com.olp.dto.ApplyQuestionCorrectionResponse;
import com.olp.entity.AssessmentType;
import com.olp.entity.QuestionCorrectionPolicy;
import com.olp.entity.User;
import com.olp.service.QuestionCorrectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QuestionCorrectionController (FR-I37, Step 54).
 */
@ExtendWith(MockitoExtension.class)
class QuestionCorrectionControllerTest {

    @Mock
    private QuestionCorrectionService questionCorrectionService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private QuestionCorrectionController controller;

    private User instructor;
    private ApplyQuestionCorrectionResponse sampleResponse;

    @BeforeEach
    void setUp() {
        instructor = new User();
        instructor.setId(20);
        instructor.setEmail("instructor@example.com");
        instructor.setName("Instructor Alan");
        instructor.setRole("INSTRUCTOR");

        sampleResponse = new ApplyQuestionCorrectionResponse();
        sampleResponse.setReportedQuestionId(10);
        sampleResponse.setAssessmentType(AssessmentType.EXAM);
        sampleResponse.setAssessmentId(101);
        sampleResponse.setQuestionId(501);
        sampleResponse.setCourseId(1);
        sampleResponse.setPolicy(QuestionCorrectionPolicy.AWARD_FULL_MARKS);
        sampleResponse.setAlreadyApplied(false);
        sampleResponse.setAffectedAttemptsCount(3);
        sampleResponse.setAppliedAt(LocalDateTime.now());
        sampleResponse.setMessage("Question correction successfully applied across 3 attempt(s).");
    }

    @Test
    @DisplayName("POST applyCorrection returns 200 OK with correction summary")
    void applyCorrection_ReturnsOk() {
        ApplyQuestionCorrectionRequest request = new ApplyQuestionCorrectionRequest(
                QuestionCorrectionPolicy.AWARD_FULL_MARKS, "Flawed question");

        when(questionCorrectionService.resolveUser(authentication)).thenReturn(instructor);
        when(questionCorrectionService.applyCorrection(eq(instructor), eq(10), any(ApplyQuestionCorrectionRequest.class)))
                .thenReturn(sampleResponse);

        ResponseEntity<ApplyQuestionCorrectionResponse> response = controller.applyCorrection(
                10, request, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().getReportedQuestionId());
        assertEquals(3, response.getBody().getAffectedAttemptsCount());
        assertFalse(response.getBody().isAlreadyApplied());
    }
}
