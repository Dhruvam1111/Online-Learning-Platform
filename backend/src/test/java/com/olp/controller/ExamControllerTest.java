package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.QuestionCorrectionPolicy;
import com.olp.entity.User;
import com.olp.service.ExamMonitoringService;
import com.olp.service.ExamRandomizationService;
import com.olp.service.ExamService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExamController (FR-I25, FR-I27, FR-I26, FR-I66).
 * Verifies 100% controller action routing and response status mapping across all endpoints.
 */
@ExtendWith(MockitoExtension.class)
class ExamControllerTest {

    @Mock
    private ExamService examService;

    @Mock
    private ExamRandomizationService examRandomizationService;

    @Mock
    private ExamMonitoringService examMonitoringService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ExamController examController;

    private User instructor;
    private User student;

    @BeforeEach
    void setUp() {
        instructor = new User("Instructor", "instructor@olp.com", "hash", "instructor", "active");
        instructor.setId(1);

        student = new User("Student", "student@olp.com", "hash", "student", "active");
        student.setId(2);
    }

    @Test
    @DisplayName("POST /exams returns 201 Created on exam creation")
    void testCreateExam() {
        ExamCreateRequest request = new ExamCreateRequest();
        request.setTitle("Midterm");
        ExamResponse response = new ExamResponse();
        response.setId(10);
        response.setTitle("Midterm");

        when(examService.resolveUser(authentication)).thenReturn(instructor);
        when(examService.createExam(eq(instructor), any(ExamCreateRequest.class))).thenReturn(response);

        ResponseEntity<ExamResponse> result = examController.createExam(request, authentication);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(10, result.getBody().getId());
        verify(examService, times(1)).createExam(instructor, request);
    }

    @Test
    @DisplayName("PUT /exams/{id} returns 200 OK on exam update")
    void testUpdateExam() {
        ExamUpdateRequest request = new ExamUpdateRequest();
        ExamResponse response = new ExamResponse();
        response.setId(10);

        when(examService.resolveUser(authentication)).thenReturn(instructor);
        when(examService.updateExam(eq(instructor), eq(10), any(ExamUpdateRequest.class))).thenReturn(response);

        ResponseEntity<ExamResponse> result = examController.updateExam(10, request, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(10, result.getBody().getId());
    }

    @Test
    @DisplayName("POST /exams/{id}/questions returns 200 OK on question linking")
    void testLinkQuestions() {
        List<Integer> questionIds = List.of(101, 102);
        ExamQuestionItemDto dto = new ExamQuestionItemDto();
        dto.setQuestionId(101);

        when(examService.resolveUser(authentication)).thenReturn(instructor);
        when(examService.linkQuestionsToExam(eq(instructor), eq(10), eq(questionIds))).thenReturn(List.of(dto));

        ResponseEntity<List<ExamQuestionItemDto>> result = examController.linkQuestions(10, questionIds, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    @DisplayName("POST /exams/{id}/blueprint returns 200 OK on configuring blueprint")
    void testConfigureBlueprint() {
        ExamBlueprintRequest request = new ExamBlueprintRequest();
        ExamBlueprintItemDto item = new ExamBlueprintItemDto();
        item.setTopic("Algorithms");

        when(examService.resolveUser(authentication)).thenReturn(instructor);
        when(examRandomizationService.configureBlueprint(eq(instructor), eq(10), eq(request))).thenReturn(List.of(item));

        ResponseEntity<List<ExamBlueprintItemDto>> result = examController.configureBlueprint(10, request, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    @DisplayName("GET /exams/{id}/blueprint returns 200 OK on retrieving blueprint")
    void testGetBlueprint() {
        when(examService.resolveUser(authentication)).thenReturn(instructor);
        when(examRandomizationService.getBlueprint(instructor, 10)).thenReturn(Collections.emptyList());

        ResponseEntity<List<ExamBlueprintItemDto>> result = examController.getBlueprint(10, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    @DisplayName("POST /exams/{id}/preview-sets returns 200 OK on previewing randomized sets")
    void testPreviewExamSets() {
        ExamSetGenerationResponse response = new ExamSetGenerationResponse();
        when(examService.resolveUser(authentication)).thenReturn(instructor);
        when(examRandomizationService.previewExamSets(instructor, 10, 42L, 3)).thenReturn(response);

        ResponseEntity<ExamSetGenerationResponse> result = examController.previewExamSets(10, 42L, 3, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
    }

    @Test
    @DisplayName("GET /exams/{id}/sets returns 200 OK on getting exam sets")
    void testGetExamSets() {
        ExamSetGenerationResponse response = new ExamSetGenerationResponse();
        when(examService.resolveUser(authentication)).thenReturn(instructor);
        when(examRandomizationService.previewExamSets(instructor, 10, null, null)).thenReturn(response);

        ResponseEntity<ExamSetGenerationResponse> result = examController.getExamSets(10, null, null, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
    }

    @Test
    @DisplayName("GET /courses/{courseId}/exams returns 200 OK with course exams")
    void testGetCourseExams() {
        when(examService.resolveUser(authentication)).thenReturn(student);
        when(examService.getCourseExams(student, 100)).thenReturn(Collections.emptyList());

        ResponseEntity<List<ExamResponse>> result = examController.getCourseExams(100, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
    }

    @Test
    @DisplayName("GET /exams/{id} returns 200 OK with single exam details")
    void testGetExam() {
        ExamResponse response = new ExamResponse();
        response.setId(10);
        when(examService.resolveUser(authentication)).thenReturn(student);
        when(examService.getExam(student, 10)).thenReturn(response);

        ResponseEntity<ExamResponse> result = examController.getExam(10, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(10, result.getBody().getId());
    }

    @Test
    @DisplayName("GET /exams/{id}/eligibility returns 200 OK with eligibility details")
    void testCheckEligibility() {
        ExamEligibilityResponse response = new ExamEligibilityResponse();
        response.setIsEligible(true);

        when(examService.resolveUser(authentication)).thenReturn(student);
        when(examService.checkEligibility(student, 10)).thenReturn(response);

        ResponseEntity<ExamEligibilityResponse> result = examController.checkEligibility(10, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().getIsEligible());
    }

    @Test
    @DisplayName("POST /exams/{id}/start returns 200 OK on starting exam")
    void testStartExam() {
        ExamStartResponse response = new ExamStartResponse();
        response.setAttemptId(5);

        when(examService.resolveUser(authentication)).thenReturn(student);
        when(examService.startExam(student, 10)).thenReturn(response);

        ResponseEntity<ExamStartResponse> result = examController.startExam(10, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(5, result.getBody().getAttemptId());
    }

    @Test
    @DisplayName("PUT /exams/{id}/monitoring returns 200 OK on saving monitoring config")
    void testSaveMonitoringConfig() {
        ExamMonitoringConfigRequest request = new ExamMonitoringConfigRequest();
        ExamMonitoringConfigResponse response = new ExamMonitoringConfigResponse();

        when(examService.resolveUser(authentication)).thenReturn(instructor);
        when(examMonitoringService.saveMonitoringConfig(instructor, 10, request)).thenReturn(response);

        ResponseEntity<ExamMonitoringConfigResponse> result = examController.saveMonitoringConfig(10, request, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    @DisplayName("GET /exams/{id}/monitoring returns 200 OK with monitoring config")
    void testGetMonitoringConfig() {
        ExamMonitoringConfigResponse response = new ExamMonitoringConfigResponse();

        when(examService.resolveUser(authentication)).thenReturn(student);
        when(examMonitoringService.getMonitoringConfig(student, 10, true)).thenReturn(response);

        ResponseEntity<ExamMonitoringConfigResponse> result = examController.getMonitoringConfig(10, true, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    @DisplayName("POST /exams/{id}/consent returns 200 OK on recording consent")
    void testRecordStudentConsent() {
        ExamConsentRequest request = new ExamConsentRequest();
        ExamConsentResponse response = new ExamConsentResponse();
        response.setConsentGiven(true);

        when(examService.resolveUser(authentication)).thenReturn(student);
        when(examMonitoringService.recordStudentConsent(student, 10, request)).thenReturn(response);

        ResponseEntity<ExamConsentResponse> result = examController.recordStudentConsent(10, request, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().getConsentGiven());
    }

    @Test
    @DisplayName("GET /exams/{id}/correction-policy returns 200 OK with policy details")
    void testGetCorrectionPolicy() {
        QuestionCorrectionPolicyResponse response = new QuestionCorrectionPolicyResponse();
        response.setPolicy(QuestionCorrectionPolicy.AWARD_FULL_MARKS);

        when(examService.resolveUser(authentication)).thenReturn(instructor);
        when(examService.getCorrectionPolicy(instructor, 10)).thenReturn(response);

        ResponseEntity<QuestionCorrectionPolicyResponse> result = examController.getCorrectionPolicy(10, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(QuestionCorrectionPolicy.AWARD_FULL_MARKS, result.getBody().getPolicy());
    }

    @Test
    @DisplayName("PUT /exams/{id}/correction-policy returns 200 OK on updating policy")
    void testUpdateCorrectionPolicy() {
        QuestionCorrectionPolicyRequest request = new QuestionCorrectionPolicyRequest(QuestionCorrectionPolicy.DROP_AND_RESCALE);
        QuestionCorrectionPolicyResponse response = new QuestionCorrectionPolicyResponse();
        response.setPolicy(QuestionCorrectionPolicy.DROP_AND_RESCALE);

        when(examService.resolveUser(authentication)).thenReturn(instructor);
        when(examService.updateCorrectionPolicy(instructor, 10, request)).thenReturn(response);

        ResponseEntity<QuestionCorrectionPolicyResponse> result = examController.updateCorrectionPolicy(10, request, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(QuestionCorrectionPolicy.DROP_AND_RESCALE, result.getBody().getPolicy());
    }
}
