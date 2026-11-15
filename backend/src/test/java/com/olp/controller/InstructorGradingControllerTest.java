package com.olp.controller;

import com.olp.dto.AnswerFeedbackRequest;
import com.olp.dto.AnswerFeedbackResponse;
import com.olp.dto.DescriptiveSubmissionResponse;
import com.olp.dto.GradeOverrideRequest;
import com.olp.dto.GradeOverrideResponse;
import com.olp.dto.GradeSubmissionRequest;
import com.olp.dto.GradeSubmissionResponse;
import com.olp.dto.GradingResultVersionResponse;
import com.olp.dto.PageResponse;
import com.olp.dto.PublishResultResponse;
import com.olp.dto.SubmissionFeedbackRequest;
import com.olp.dto.SubmissionFeedbackResponse;
import com.olp.entity.User;
import com.olp.service.DescriptiveGradingService;
import com.olp.service.GradingResultVersioningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Controller Unit Test Suite for FR-I30: Grade Descriptive Submissions.
 * Tests REST endpoint routing, query parameter handling (filtering & pagination),
 * role-based access control annotations, and grading/publishing responses.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InstructorGradingControllerTest {

    @Mock
    private DescriptiveGradingService descriptiveGradingService;
    @Mock
    private GradingResultVersioningService gradingResultVersioningService;

    private InstructorGradingController controller;

    private User instructor;
    private Authentication auth;
    private DescriptiveSubmissionResponse submissionResponse;

    @BeforeEach
    void setUp() {
        controller = new InstructorGradingController(descriptiveGradingService, gradingResultVersioningService);

        instructor = new User("Dr. Turing", "turing@olp.com", "hash", "instructor", "active");
        instructor.setId(10);
        auth = new UsernamePasswordAuthenticationToken(instructor, null);

        submissionResponse = new DescriptiveSubmissionResponse();
        submissionResponse.setAnswerId(500);
        submissionResponse.setAttemptId(300);
        submissionResponse.setQuizId(200);
        submissionResponse.setQuizTitle("Midterm Exam");
        submissionResponse.setCourseId(50);
        submissionResponse.setCourseTitle("Advanced Algorithms");
        submissionResponse.setStudentId(100);
        submissionResponse.setStudentName("Alice Learner");
        submissionResponse.setStudentEmail("alice@olp.com");
        submissionResponse.setSubmittedAnswer("Dijkstra finds shortest path");
        submissionResponse.setAwardedMarks(BigDecimal.ZERO);
        submissionResponse.setMaxMarks(BigDecimal.valueOf(20.0));
        submissionResponse.setEvaluationStatus("MANUAL_GRADING_REQUIRED");
        submissionResponse.setSubmittedAt(LocalDateTime.now().minusHours(1));

        when(descriptiveGradingService.resolveUser(auth)).thenReturn(instructor);
    }

    @Test
    @DisplayName("PreAuthorize security configuration covers instructor and admin")
    void testSecurityAnnotations() {
        PreAuthorize classPreAuthorize = InstructorGradingController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(classPreAuthorize, "InstructorGradingController must have class-level @PreAuthorize");
        assertTrue(classPreAuthorize.value().contains("instructor"), "Must permit instructor");
        assertTrue(classPreAuthorize.value().contains("admin"), "Must permit admin");
    }

    @Test
    @DisplayName("GET /api/instructor/grading - returns list when no pagination params are provided")
    void testGetGradingQueue_ListResponse() {
        when(descriptiveGradingService.getGradingQueue(instructor, 50, 200, "UNGRADED"))
                .thenReturn(List.of(submissionResponse));

        ResponseEntity<?> entity = controller.getGradingQueue(auth, 50, 200, "UNGRADED", null, null);

        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertTrue(entity.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<DescriptiveSubmissionResponse> list = (List<DescriptiveSubmissionResponse>) Objects.requireNonNull(entity.getBody());
        assertEquals(1, list.size());
        assertEquals(500, list.get(0).getAnswerId());
    }

    @Test
    @DisplayName("GET /api/instructor/grading - returns page when pagination params are provided")
    void testGetGradingQueue_PagedResponse() {
        PageResponse<DescriptiveSubmissionResponse> pageResponse = new PageResponse<>(
                List.of(submissionResponse), 0, 10, 1L, 1, true);

        when(descriptiveGradingService.getGradingQueuePaged(instructor, 50, null, null, 0, 10))
                .thenReturn(pageResponse);

        ResponseEntity<?> entity = controller.getGradingQueue(auth, 50, null, null, 0, 10);

        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertTrue(entity.getBody() instanceof PageResponse);
        @SuppressWarnings("unchecked")
        PageResponse<DescriptiveSubmissionResponse> body = (PageResponse<DescriptiveSubmissionResponse>) Objects.requireNonNull(entity.getBody());
        assertEquals(1, body.getContent().size());
        assertEquals(500, body.getContent().get(0).getAnswerId());
    }

    @Test
    @DisplayName("GET /api/instructor/grading/queue/paged - explicit paginated endpoint")
    void testGetGradingQueuePaged_Endpoint() {
        PageResponse<DescriptiveSubmissionResponse> pageResponse = new PageResponse<>(
                List.of(submissionResponse), 0, 20, 1L, 1, true);

        when(descriptiveGradingService.getGradingQueuePaged(instructor, null, null, null, 0, 20))
                .thenReturn(pageResponse);

        ResponseEntity<PageResponse<DescriptiveSubmissionResponse>> entity =
                controller.getGradingQueuePaged(auth, null, null, null, 0, 20);

        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        PageResponse<DescriptiveSubmissionResponse> body = Objects.requireNonNull(entity.getBody());
        assertEquals(1, body.getContent().size());
    }

    @Test
    @DisplayName("GET /api/instructor/grading/{answerId} - retrieves submission details")
    void testGetSubmissionDetails() {
        when(descriptiveGradingService.getSubmissionDetails(instructor, 500))
                .thenReturn(submissionResponse);

        ResponseEntity<DescriptiveSubmissionResponse> entity = controller.getSubmissionDetails(auth, 500);

        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        DescriptiveSubmissionResponse body = Objects.requireNonNull(entity.getBody());
        assertEquals(500, body.getAnswerId());
        assertEquals("Alice Learner", body.getStudentName());
    }

    @Test
    @DisplayName("POST /api/instructor/grading/{answerId} - evaluates submission successfully")
    void testGradeSubmission() {
        GradeSubmissionRequest request = new GradeSubmissionRequest(BigDecimal.valueOf(17.5), "Great job");
        DescriptiveSubmissionResponse gradedDto = submissionResponse;
        gradedDto.setAwardedMarks(BigDecimal.valueOf(17.5));
        gradedDto.setEvaluationStatus("EVALUATED_UNPUBLISHED");
        GradeSubmissionResponse serviceResponse = new GradeSubmissionResponse(
                "Submission graded successfully with status EVALUATED_UNPUBLISHED", gradedDto);

        when(descriptiveGradingService.gradeSubmission(instructor, 500, request))
                .thenReturn(serviceResponse);

        ResponseEntity<GradeSubmissionResponse> entity = controller.gradeSubmission(auth, 500, request);

        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        GradeSubmissionResponse body = Objects.requireNonNull(entity.getBody());
        assertEquals(BigDecimal.valueOf(17.5), Objects.requireNonNull(body.getSubmission()).getAwardedMarks());
        assertEquals("EVALUATED_UNPUBLISHED", Objects.requireNonNull(body.getSubmission()).getEvaluationStatus());
    }

    @Test
    @DisplayName("POST /api/instructor/grading/{answerId}/publish - publishes single submission result")
    void testPublishAnswerResult() {
        PublishResultResponse pubResponse = new PublishResultResponse("Graded submission published successfully", 1, 200, 300);

        when(descriptiveGradingService.publishAnswerResult(instructor, 500))
                .thenReturn(pubResponse);

        ResponseEntity<PublishResultResponse> entity = controller.publishAnswerResult(auth, 500);

        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        PublishResultResponse body = Objects.requireNonNull(entity.getBody());
        assertEquals(1, body.getPublishedCount());
    }

    @Test
    @DisplayName("POST /api/instructor/grading/quizzes/{quizId}/publish - publishes all quiz results")
    void testPublishQuizResults() {
        PublishResultResponse pubResponse = new PublishResultResponse("Published 3 evaluated submissions", 3, 200, null);

        when(descriptiveGradingService.publishQuizResults(instructor, 200))
                .thenReturn(pubResponse);

        ResponseEntity<PublishResultResponse> entity = controller.publishQuizResults(auth, 200);

        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        PublishResultResponse body = Objects.requireNonNull(entity.getBody());
        assertEquals(3, body.getPublishedCount());
    }

    @Test
    @DisplayName("POST /api/instructor/grading/attempts/{attemptId}/publish - publishes attempt results")
    void testPublishAttemptResults() {
        PublishResultResponse pubResponse = new PublishResultResponse("Published 1 evaluated submission", 1, 200, 300);

        when(descriptiveGradingService.publishAttemptResults(instructor, 300))
                .thenReturn(pubResponse);

        ResponseEntity<PublishResultResponse> entity = controller.publishAttemptResults(auth, 300);

        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        PublishResultResponse body = Objects.requireNonNull(entity.getBody());
        assertEquals(1, body.getPublishedCount());
    }

    @Test
    @DisplayName("POST /api/instructor/grading/answers/{answerId}/feedback - adds or updates answer feedback")
    void testAddOrUpdateAnswerFeedback() {
        AnswerFeedbackRequest req = new AnswerFeedbackRequest("Good point made", false);
        AnswerFeedbackResponse res = new AnswerFeedbackResponse();
        res.setAnswerId(500);
        res.setFeedback("Good point made");
        res.setMessage("Answer feedback saved successfully");

        when(descriptiveGradingService.addOrUpdateAnswerFeedback(instructor, 500, req))
                .thenReturn(res);

        ResponseEntity<AnswerFeedbackResponse> entity = controller.addOrUpdateAnswerFeedback(auth, 500, req);

        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        AnswerFeedbackResponse body = Objects.requireNonNull(entity.getBody());
        assertEquals("Good point made", body.getFeedback());
    }

    @Test
    @DisplayName("POST /api/instructor/grading/attempts/{attemptId}/feedback - adds or updates submission feedback")
    void testAddOrUpdateSubmissionFeedback() {
        SubmissionFeedbackRequest req = new SubmissionFeedbackRequest("Great overall test score", false);
        SubmissionFeedbackResponse res = new SubmissionFeedbackResponse();
        res.setAttemptId(300);
        res.setOverallFeedback("Great overall test score");
        res.setMessage("Submission feedback saved successfully");

        when(descriptiveGradingService.addOrUpdateSubmissionFeedback(instructor, 300, req))
                .thenReturn(res);

        ResponseEntity<SubmissionFeedbackResponse> entity = controller.addOrUpdateSubmissionFeedback(auth, 300, req);

        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        SubmissionFeedbackResponse body = Objects.requireNonNull(entity.getBody());
        assertEquals("Great overall test score", body.getOverallFeedback());
    }

    @Test
    @DisplayName("GET /api/instructor/grading/attempts/{attemptId}/feedback - retrieves submission feedback")
    void testGetSubmissionFeedback() {
        SubmissionFeedbackResponse res = new SubmissionFeedbackResponse();
        res.setAttemptId(300);
        res.setOverallFeedback("Detailed feedback comments");
        res.setMessage("Submission feedback retrieved successfully");

        when(descriptiveGradingService.getSubmissionFeedback(instructor, 300))
                .thenReturn(res);

        ResponseEntity<SubmissionFeedbackResponse> entity = controller.getSubmissionFeedback(auth, 300);

        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        SubmissionFeedbackResponse body = Objects.requireNonNull(entity.getBody());
        assertEquals("Detailed feedback comments", body.getOverallFeedback());
    }

    // =========================================================================
    // FR-I32: Grade Override & Result Versioning Controller Tests
    // =========================================================================

    @Test
    @DisplayName("POST /api/instructor/grading/answers/{answerId}/override - overrides answer mark")
    void testOverrideAnswerGrade_Success() {
        GradeOverrideRequest req = new GradeOverrideRequest(BigDecimal.valueOf(18.0), "Bonus marks awarded for alternative proof");
        GradeOverrideResponse res = new GradeOverrideResponse();
        res.setVersionId(1);
        res.setVersionNumber(1);
        res.setAnswerId(500);
        res.setNewMarks(BigDecimal.valueOf(18.0));
        res.setJustification("Bonus marks awarded for alternative proof");

        when(gradingResultVersioningService.overrideAnswerGrade(instructor, 500, req)).thenReturn(res);

        ResponseEntity<GradeOverrideResponse> entity = controller.overrideAnswerGrade(auth, 500, req);

        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        GradeOverrideResponse body = Objects.requireNonNull(entity.getBody());
        assertEquals(BigDecimal.valueOf(18.0), body.getNewMarks());
        assertEquals("Bonus marks awarded for alternative proof", body.getJustification());
    }

    @Test
    @DisplayName("POST /api/instructor/grading/attempts/{attemptId}/override - overrides attempt mark")
    void testOverrideAttemptGrade_Success() {
        GradeOverrideRequest req = new GradeOverrideRequest(BigDecimal.valueOf(85.0), "Curve adjustment applied");
        GradeOverrideResponse res = new GradeOverrideResponse();
        res.setVersionId(2);
        res.setVersionNumber(1);
        res.setAttemptId(300);
        res.setNewMarks(BigDecimal.valueOf(85.0));
        res.setJustification("Curve adjustment applied");

        when(gradingResultVersioningService.overrideAttemptGrade(instructor, 300, req)).thenReturn(res);

        ResponseEntity<GradeOverrideResponse> entity = controller.overrideAttemptGrade(auth, 300, req);

        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        GradeOverrideResponse body = Objects.requireNonNull(entity.getBody());
        assertEquals(BigDecimal.valueOf(85.0), body.getNewMarks());
    }

    @Test
    @DisplayName("GET /api/instructor/grading/answers/{answerId}/versions - retrieves version history")
    void testGetAnswerVersionHistory_Success() {
        GradingResultVersionResponse v1 = new GradingResultVersionResponse();
        v1.setVersionId(1);
        v1.setVersionNumber(1);
        v1.setPreviousMarks(BigDecimal.valueOf(10.0));
        v1.setNewMarks(BigDecimal.valueOf(15.0));
        v1.setJustification("Correction for partial credit");

        when(gradingResultVersioningService.getAnswerVersionHistory(instructor, 500)).thenReturn(List.of(v1));

        ResponseEntity<List<GradingResultVersionResponse>> entity = controller.getAnswerVersionHistory(auth, 500);

        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        List<GradingResultVersionResponse> body = Objects.requireNonNull(entity.getBody());
        assertEquals(1, body.size());
        assertEquals(BigDecimal.valueOf(15.0), body.get(0).getNewMarks());
    }

    @Test
    @DisplayName("GET /api/instructor/grading/attempts/{attemptId}/versions - retrieves attempt version history")
    void testGetAttemptVersionHistory_Success() {
        GradingResultVersionResponse v1 = new GradingResultVersionResponse();
        v1.setVersionId(2);
        v1.setVersionNumber(1);
        v1.setPreviousMarks(BigDecimal.valueOf(70.0));
        v1.setNewMarks(BigDecimal.valueOf(80.0));
        v1.setJustification("Overall grade scale adjustment");

        when(gradingResultVersioningService.getAttemptVersionHistory(instructor, 300)).thenReturn(List.of(v1));

        ResponseEntity<List<GradingResultVersionResponse>> entity = controller.getAttemptVersionHistory(auth, 300);

        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        List<GradingResultVersionResponse> body = Objects.requireNonNull(entity.getBody());
        assertEquals(1, body.size());
        assertEquals(BigDecimal.valueOf(80.0), body.get(0).getNewMarks());
    }

    @Test
    @DisplayName("POST /api/instructor/grading/override - unified endpoint routes by answerId")
    void testOverrideGrade_UnifiedRoute() {
        GradeOverrideRequest req = new GradeOverrideRequest(BigDecimal.valueOf(17.0), "Updated via unified route");
        req.setAnswerId(500);

        GradeOverrideResponse res = new GradeOverrideResponse();
        res.setAnswerId(500);
        res.setNewMarks(BigDecimal.valueOf(17.0));

        when(gradingResultVersioningService.overrideAnswerGrade(instructor, 500, req)).thenReturn(res);

        ResponseEntity<GradeOverrideResponse> entity = controller.overrideGrade(auth, req);

        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        GradeOverrideResponse body = Objects.requireNonNull(entity.getBody());
        assertEquals(BigDecimal.valueOf(17.0), body.getNewMarks());
    }
}
