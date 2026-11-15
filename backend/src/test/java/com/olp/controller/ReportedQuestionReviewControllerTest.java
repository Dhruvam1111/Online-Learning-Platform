package com.olp.controller;

import com.olp.dto.AffectedAttemptsCountResponse;
import com.olp.dto.ReportedQuestionDetailResponse;
import com.olp.dto.ReportedQuestionInboxResponse;
import com.olp.dto.ReportedQuestionReviewRequest;
import com.olp.entity.AssessmentType;
import com.olp.entity.ReportedQuestionReviewAction;
import com.olp.entity.ReportedQuestionStatus;
import com.olp.entity.User;
import com.olp.service.ReportedQuestionReviewService;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportedQuestionReviewController (FR-I36, Step 53).
 */
@ExtendWith(MockitoExtension.class)
class ReportedQuestionReviewControllerTest {

    @Mock
    private ReportedQuestionReviewService reviewService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ReportedQuestionReviewController controller;

    private User instructor;
    private ReportedQuestionDetailResponse sampleDetails;

    @BeforeEach
    void setUp() {
        instructor = new User();
        instructor.setId(20);
        instructor.setEmail("instructor@example.com");
        instructor.setName("Instructor Alice");
        instructor.setRole("INSTRUCTOR");

        sampleDetails = new ReportedQuestionDetailResponse();
        sampleDetails.setId(101);
        sampleDetails.setAssessmentType(AssessmentType.EXAM);
        sampleDetails.setAssessmentId(201);
        sampleDetails.setAssessmentTitle("Final Exam");
        sampleDetails.setQuestionId(301);
        sampleDetails.setQuestionText("What is a mutex?");
        sampleDetails.setCourseId(10);
        sampleDetails.setCourseTitle("Operating Systems");
        sampleDetails.setStatus(ReportedQuestionStatus.PENDING);
        sampleDetails.setAffectedAttemptsCount(15L);
    }

    @Test
    @DisplayName("GET getReportedQuestions returns 200 OK with filtered list")
    void getReportedQuestions_ReturnsOk() {
        ReportedQuestionInboxResponse inbox = new ReportedQuestionInboxResponse();
        inbox.setId(101);

        when(reviewService.resolveUser(authentication)).thenReturn(instructor);
        when(reviewService.getReportedQuestions(instructor, 10, AssessmentType.EXAM, 201, ReportedQuestionStatus.PENDING))
                .thenReturn(List.of(inbox));

        ResponseEntity<List<ReportedQuestionInboxResponse>> response = controller.getReportedQuestions(
                10, AssessmentType.EXAM, 201, ReportedQuestionStatus.PENDING, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(101, response.getBody().get(0).getId());
    }

    @Test
    @DisplayName("GET getReportDetails returns 200 OK with detailed view")
    void getReportDetails_ReturnsOk() {
        when(reviewService.resolveUser(authentication)).thenReturn(instructor);
        when(reviewService.getReportDetails(instructor, 101)).thenReturn(sampleDetails);

        ResponseEntity<ReportedQuestionDetailResponse> response = controller.getReportDetails(101, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(101, response.getBody().getId());
        assertEquals("What is a mutex?", response.getBody().getQuestionText());
    }

    @Test
    @DisplayName("GET getAffectedAttemptsCount returns 200 OK with attempt counter")
    void getAffectedAttemptsCount_ReturnsOk() {
        AffectedAttemptsCountResponse countResp = new AffectedAttemptsCountResponse(
                101, AssessmentType.EXAM, 201, 301, 15L);

        when(reviewService.resolveUser(authentication)).thenReturn(instructor);
        when(reviewService.getAffectedAttemptsCount(instructor, 101)).thenReturn(countResp);

        ResponseEntity<AffectedAttemptsCountResponse> response = controller.getAffectedAttemptsCount(101, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(15L, response.getBody().getAffectedAttemptsCount());
    }

    @Test
    @DisplayName("POST reviewReportedQuestion returns 200 OK with updated resolution details")
    void reviewReportedQuestion_ReturnsOk() {
        ReportedQuestionReviewRequest request = new ReportedQuestionReviewRequest(
                ReportedQuestionReviewAction.CORRECT, "Corrected key");

        sampleDetails.setStatus(ReportedQuestionStatus.RESOLVED_CORRECTED);
        sampleDetails.setResolutionAction("CORRECT");
        sampleDetails.setResolutionComment("Corrected key");

        when(reviewService.resolveUser(authentication)).thenReturn(instructor);
        when(reviewService.reviewReportedQuestion(eq(instructor), eq(101), any(ReportedQuestionReviewRequest.class)))
                .thenReturn(sampleDetails);

        ResponseEntity<ReportedQuestionDetailResponse> response = controller.reviewReportedQuestion(
                101, request, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ReportedQuestionStatus.RESOLVED_CORRECTED, response.getBody().getStatus());
        assertEquals("CORRECT", response.getBody().getResolutionAction());
    }
}
