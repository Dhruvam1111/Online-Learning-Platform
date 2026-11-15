package com.olp.controller;

import com.olp.dto.AssessmentPublishStatusResponse;
import com.olp.dto.PublishAssessmentResultsRequest;
import com.olp.dto.WithholdAssessmentResultsRequest;
import com.olp.entity.AssessmentType;
import com.olp.entity.User;
import com.olp.service.AssessmentPublishService;
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
 * Unit tests for AssessmentPublishController (FR-I34, Step 51).
 */
@ExtendWith(MockitoExtension.class)
class AssessmentPublishControllerTest {

    @Mock
    private AssessmentPublishService assessmentPublishService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AssessmentPublishController controller;

    private User instructor;
    private AssessmentPublishStatusResponse sampleResponse;

    @BeforeEach
    void setUp() {
        instructor = new User();
        instructor.setId(10);
        instructor.setEmail("instructor@example.com");
        instructor.setName("Instructor Jane");
        instructor.setRole("INSTRUCTOR");

        sampleResponse = new AssessmentPublishStatusResponse(
                AssessmentType.EXAM,
                100,
                "Midterm Exam",
                1,
                true,
                false,
                LocalDateTime.now(),
                20,
                20,
                0,
                true,
                20,
                "Exam results published and locked successfully"
        );
    }

    @Test
    @DisplayName("GET status returns OK with publication status response")
    void testGetPublishStatus_Success() {
        when(assessmentPublishService.resolveUser(authentication)).thenReturn(instructor);
        when(assessmentPublishService.parseAssessmentType("EXAM")).thenReturn(AssessmentType.EXAM);
        when(assessmentPublishService.getPublishStatus(instructor, AssessmentType.EXAM, 100))
                .thenReturn(sampleResponse);

        ResponseEntity<AssessmentPublishStatusResponse> response =
                controller.getPublishStatus("EXAM", 100, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Midterm Exam", response.getBody().getAssessmentTitle());
        assertTrue(response.getBody().getResultsPublished());
        assertFalse(response.getBody().getResultsWithheld());
        verify(assessmentPublishService).getPublishStatus(instructor, AssessmentType.EXAM, 100);
    }

    @Test
    @DisplayName("POST publish with request body returns OK")
    void testPublishResults_WithBody() {
        PublishAssessmentResultsRequest req = new PublishAssessmentResultsRequest(true, true);
        when(assessmentPublishService.resolveUser(authentication)).thenReturn(instructor);
        when(assessmentPublishService.parseAssessmentType("assignment")).thenReturn(AssessmentType.ASSIGNMENT);
        when(assessmentPublishService.publishResults(instructor, AssessmentType.ASSIGNMENT, 200, req))
                .thenReturn(sampleResponse);

        ResponseEntity<AssessmentPublishStatusResponse> response =
                controller.publishResults("assignment", 200, req, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(assessmentPublishService).publishResults(instructor, AssessmentType.ASSIGNMENT, 200, req);
    }

    @Test
    @DisplayName("POST publish with null request body succeeds with defaults")
    void testPublishResults_NullBody() {
        when(assessmentPublishService.resolveUser(authentication)).thenReturn(instructor);
        when(assessmentPublishService.parseAssessmentType("quiz")).thenReturn(AssessmentType.QUIZ);
        when(assessmentPublishService.publishResults(eq(instructor), eq(AssessmentType.QUIZ), eq(300), any()))
                .thenReturn(sampleResponse);

        ResponseEntity<AssessmentPublishStatusResponse> response =
                controller.publishResults("quiz", 300, null, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(assessmentPublishService).publishResults(eq(instructor), eq(AssessmentType.QUIZ), eq(300), any());
    }

    @Test
    @DisplayName("POST withhold with request body returns OK")
    void testWithholdResults_WithBody() {
        WithholdAssessmentResultsRequest req = new WithholdAssessmentResultsRequest("Audit in progress");
        AssessmentPublishStatusResponse withheldResponse = new AssessmentPublishStatusResponse(
                AssessmentType.ASSIGNMENT, 200, "Project 1", 1, false, true, null, 10, 10, 0, false, 0, "Withheld");

        when(assessmentPublishService.resolveUser(authentication)).thenReturn(instructor);
        when(assessmentPublishService.parseAssessmentType("ASSIGNMENT")).thenReturn(AssessmentType.ASSIGNMENT);
        when(assessmentPublishService.withholdResults(instructor, AssessmentType.ASSIGNMENT, 200, req))
                .thenReturn(withheldResponse);

        ResponseEntity<AssessmentPublishStatusResponse> response =
                controller.withholdResults("ASSIGNMENT", 200, req, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getResultsWithheld());
        assertFalse(response.getBody().getResultsPublished());
        verify(assessmentPublishService).withholdResults(instructor, AssessmentType.ASSIGNMENT, 200, req);
    }

    @Test
    @DisplayName("POST withhold with null request body succeeds")
    void testWithholdResults_NullBody() {
        when(assessmentPublishService.resolveUser(authentication)).thenReturn(instructor);
        when(assessmentPublishService.parseAssessmentType("EXAM")).thenReturn(AssessmentType.EXAM);
        when(assessmentPublishService.withholdResults(eq(instructor), eq(AssessmentType.EXAM), eq(100), any()))
                .thenReturn(sampleResponse);

        ResponseEntity<AssessmentPublishStatusResponse> response =
                controller.withholdResults("EXAM", 100, null, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(assessmentPublishService).withholdResults(eq(instructor), eq(AssessmentType.EXAM), eq(100), any());
    }
}
