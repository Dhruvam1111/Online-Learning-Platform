package com.olp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.AnswerFeedbackRequest;
import com.olp.dto.AnswerFeedbackResponse;
import com.olp.dto.DescriptiveSubmissionResponse;
import com.olp.dto.GradeSubmissionRequest;
import com.olp.dto.GradeSubmissionResponse;
import com.olp.dto.PageResponse;
import com.olp.dto.PublishResultResponse;
import com.olp.dto.SubmissionFeedbackRequest;
import com.olp.dto.SubmissionFeedbackResponse;
import com.olp.entity.User;
import com.olp.exception.GlobalExceptionHandler;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.service.DescriptiveGradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private InstructorGradingController controller;

    private User instructor;
    private Authentication auth;
    private DescriptiveSubmissionResponse submissionResponse;

    @BeforeEach
    void setUp() {
        controller = new InstructorGradingController(descriptiveGradingService);

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
        List<DescriptiveSubmissionResponse> list = (List<DescriptiveSubmissionResponse>) entity.getBody();
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
        PageResponse<DescriptiveSubmissionResponse> body = (PageResponse<DescriptiveSubmissionResponse>) entity.getBody();
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
        assertNotNull(entity.getBody());
        assertEquals(1, entity.getBody().getContent().size());
    }

    @Test
    @DisplayName("GET /api/instructor/grading/{answerId} - retrieves submission details")
    void testGetSubmissionDetails() {
        when(descriptiveGradingService.getSubmissionDetails(instructor, 500))
                .thenReturn(submissionResponse);

        ResponseEntity<DescriptiveSubmissionResponse> entity = controller.getSubmissionDetails(auth, 500);

        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertNotNull(entity.getBody());
        assertEquals(500, entity.getBody().getAnswerId());
        assertEquals("Alice Learner", entity.getBody().getStudentName());
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
        assertNotNull(entity.getBody());
        assertEquals(BigDecimal.valueOf(17.5), entity.getBody().getSubmission().getAwardedMarks());
        assertEquals("EVALUATED_UNPUBLISHED", entity.getBody().getSubmission().getEvaluationStatus());
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
        assertNotNull(entity.getBody());
        assertEquals(1, entity.getBody().getPublishedCount());
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
        assertNotNull(entity.getBody());
        assertEquals(3, entity.getBody().getPublishedCount());
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
        assertNotNull(entity.getBody());
        assertEquals(1, entity.getBody().getPublishedCount());
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
        assertNotNull(entity.getBody());
        assertEquals("Good point made", entity.getBody().getFeedback());
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
        assertNotNull(entity.getBody());
        assertEquals("Great overall test score", entity.getBody().getOverallFeedback());
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
        assertNotNull(entity.getBody());
        assertEquals("Detailed feedback comments", entity.getBody().getOverallFeedback());
    }

    @Test
    @DisplayName("GET /api/instructor/grading - defaults missing page to 0 or missing size to 20 when only one param is supplied")
    void testGetGradingQueue_PartialPaginationParamsDefaults() {
        PageResponse<DescriptiveSubmissionResponse> pageResponse = new PageResponse<>(
                List.of(submissionResponse), 2, 20, 41L, 3, false);
        when(descriptiveGradingService.getGradingQueuePaged(instructor, 50, null, null, 2, 20))
                .thenReturn(pageResponse);

        ResponseEntity<?> onlyPage = controller.getGradingQueue(auth, 50, null, null, 2, null);
        assertEquals(HttpStatus.OK, onlyPage.getStatusCode());
        verify(descriptiveGradingService).getGradingQueuePaged(instructor, 50, null, null, 2, 20);

        PageResponse<DescriptiveSubmissionResponse> sizeOnlyResponse = new PageResponse<>(
                List.of(submissionResponse), 0, 5, 1L, 1, true);
        when(descriptiveGradingService.getGradingQueuePaged(instructor, null, null, null, 0, 5))
                .thenReturn(sizeOnlyResponse);

        ResponseEntity<?> onlySize = controller.getGradingQueue(auth, null, null, null, null, 5);
        assertEquals(HttpStatus.OK, onlySize.getStatusCode());
        verify(descriptiveGradingService).getGradingQueuePaged(instructor, null, null, null, 0, 5);
    }

    // =========================================================================
    // Standalone MockMvc Contract, Validation (@Valid) & GlobalExceptionHandler Tests
    // =========================================================================

    @Nested
    @DisplayName("MockMvc HTTP Validation & Exception Handling Contract")
    class MockMvcContractTests {

        private MockMvc mockMvc;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @BeforeEach
        void setUpMockMvc() {
            mockMvc = MockMvcBuilders
                    .standaloneSetup(controller)
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();
        }

        @Test
        @DisplayName("GET /api/instructors/grading/queue - supports plural base mapping")
        void testPluralBasePathMapping() throws Exception {
            when(descriptiveGradingService.getGradingQueue(instructor, null, null, null))
                    .thenReturn(List.of(submissionResponse));

            mockMvc.perform(get("/api/instructors/grading/queue").principal(auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].answerId").value(500));
        }

        @Test
        @DisplayName("POST /api/instructor/grading/{answerId} - rejects null awardedMarks with 400 Bad Request")
        void testGradeSubmission_NullMarks_Returns400() throws Exception {
            String body = "{\"feedback\":\"Missing score\"}";

            mockMvc.perform(post("/api/instructor/grading/500")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(Objects.requireNonNull(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("awardedMarks: Awarded marks are required"));

            verify(descriptiveGradingService, never()).gradeSubmission(any(), anyInt(), any());
        }

        @Test
        @DisplayName("POST /api/instructor/grading/{answerId} - rejects negative awardedMarks with 400 Bad Request")
        void testGradeSubmission_NegativeMarks_Returns400() throws Exception {
            GradeSubmissionRequest req = new GradeSubmissionRequest(BigDecimal.valueOf(-2.5), "Invalid");

            mockMvc.perform(post("/api/instructor/grading/500")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("awardedMarks: Awarded marks cannot be negative"));

            verify(descriptiveGradingService, never()).gradeSubmission(any(), anyInt(), any());
        }

        @Test
        @DisplayName("PUT /api/instructor/grading/submissions/{answerId} - supports PUT on alias path")
        void testGradeSubmission_PutAliasRoute_Returns200() throws Exception {
            GradeSubmissionRequest req = new GradeSubmissionRequest(BigDecimal.valueOf(18.0), "Solid");
            submissionResponse.setAwardedMarks(BigDecimal.valueOf(18.0));
            GradeSubmissionResponse serviceRes = new GradeSubmissionResponse("Graded", submissionResponse);
            when(descriptiveGradingService.gradeSubmission(eq(instructor), eq(500), any(GradeSubmissionRequest.class)))
                    .thenReturn(serviceRes);

            mockMvc.perform(put("/api/instructor/grading/submissions/500")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.submission.awardedMarks").value(18.0));
        }

        @Test
        @DisplayName("POST /api/instructor/grading/answers/{answerId}/feedback - rejects blank feedback with 400 Bad Request")
        void testAddAnswerFeedback_Blank_Returns400() throws Exception {
            String body = "{\"feedback\":\"   \"}";

            mockMvc.perform(post("/api/instructor/grading/answers/500/feedback")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(Objects.requireNonNull(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("feedback: Feedback comment cannot be empty"));

            verify(descriptiveGradingService, never()).addOrUpdateAnswerFeedback(any(), anyInt(), any());
        }

        @Test
        @DisplayName("POST /api/instructor/grading/answers/{answerId}/feedback - rejects feedback exceeding 2000 chars")
        void testAddAnswerFeedback_ExceedsMaxLength_Returns400() throws Exception {
            AnswerFeedbackRequest req = new AnswerFeedbackRequest("a".repeat(2001), false);

            mockMvc.perform(post("/api/instructor/grading/answers/500/feedback")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("feedback: Feedback comment must not exceed 2000 characters"));

            verify(descriptiveGradingService, never()).addOrUpdateAnswerFeedback(any(), anyInt(), any());
        }

        @Test
        @DisplayName("PUT /api/instructor/grading/{answerId}/feedback - accepts @JsonAlias('comment')")
        void testUpdateAnswerFeedback_JsonAliasAndPut_Returns200() throws Exception {
            AnswerFeedbackResponse res = new AnswerFeedbackResponse();
            res.setAnswerId(500);
            res.setFeedback("Updated via comment alias");
            when(descriptiveGradingService.addOrUpdateAnswerFeedback(eq(instructor), eq(500), any(AnswerFeedbackRequest.class)))
                    .thenReturn(res);

            mockMvc.perform(put("/api/instructor/grading/500/feedback")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"comment\":\"Updated via comment alias\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.feedback").value("Updated via comment alias"));
        }

        @Test
        @DisplayName("POST /api/instructor/grading/attempts/{attemptId}/feedback - rejects empty and >4000 char feedback")
        void testAddSubmissionFeedback_Validation_Returns400() throws Exception {
            mockMvc.perform(post("/api/instructor/grading/attempts/300/feedback")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"feedback\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("feedback: Overall feedback comment cannot be empty"));

            SubmissionFeedbackRequest tooLong = new SubmissionFeedbackRequest("x".repeat(4001), false);
            mockMvc.perform(post("/api/instructor/grading/attempts/300/feedback")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(Objects.requireNonNull(objectMapper.writeValueAsString(tooLong))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("feedback: Overall feedback comment must not exceed 4000 characters"));

            verify(descriptiveGradingService, never()).addOrUpdateSubmissionFeedback(any(), anyInt(), any());
        }

        @Test
        @DisplayName("PUT /api/instructor/grading/attempts/{attemptId}/overall-feedback - accepts @JsonAlias('overallFeedback')")
        void testUpdateSubmissionFeedback_JsonAliasAndPut_Returns200() throws Exception {
            SubmissionFeedbackResponse res = new SubmissionFeedbackResponse();
            res.setAttemptId(300);
            res.setOverallFeedback("Strong reasoning overall");
            when(descriptiveGradingService.addOrUpdateSubmissionFeedback(eq(instructor), eq(300), any(SubmissionFeedbackRequest.class)))
                    .thenReturn(res);

            mockMvc.perform(put("/api/instructor/grading/attempts/300/overall-feedback")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"overallFeedback\":\"Strong reasoning overall\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.overallFeedback").value("Strong reasoning overall"));
        }

        @Test
        @DisplayName("Malformed JSON returns 400 Bad Request")
        void testMalformedJson_Returns400() throws Exception {
            mockMvc.perform(post("/api/instructor/grading/500")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{bad-json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Malformed request body"));
        }

        @Test
        @DisplayName("GlobalExceptionHandler maps NotFound(404), Forbidden(403), Conflict(409), BadRequest(400), Unauthorized(401)")
        void testServiceExceptionsMapToExpectedHttpStatuses() throws Exception {
            when(descriptiveGradingService.getSubmissionDetails(instructor, 999))
                    .thenThrow(new ResourceNotFoundException("Submission answer not found with id: 999"));
            mockMvc.perform(get("/api/instructor/grading/999").principal(auth))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            when(descriptiveGradingService.getSubmissionFeedback(instructor, 300))
                    .thenThrow(new AccessDeniedException("Access denied"));
            mockMvc.perform(get("/api/instructor/grading/attempts/300/feedback").principal(auth))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));

            when(descriptiveGradingService.addOrUpdateAnswerFeedback(eq(instructor), eq(500), any()))
                    .thenThrow(new IllegalStateException("Cannot modify feedback: answer results have already been published"));
            mockMvc.perform(post("/api/instructor/grading/answers/500/feedback")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"feedback\":\"Late edit\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));

            when(descriptiveGradingService.gradeSubmission(eq(instructor), eq(500), any()))
                    .thenThrow(new IllegalArgumentException("Awarded marks (25.0) cannot exceed maximum marks (20.0)"));
            mockMvc.perform(post("/api/instructor/grading/500")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"awardedMarks\":25.0}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            when(descriptiveGradingService.resolveUser(null))
                    .thenThrow(new InvalidCredentialsException("User is not authenticated"));
            mockMvc.perform(get("/api/instructor/grading/queue"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }
    }
}
