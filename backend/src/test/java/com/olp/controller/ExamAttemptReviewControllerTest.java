package com.olp.controller;

import com.olp.dto.ExamAttemptReviewHistoryResponse;
import com.olp.dto.ExamAttemptReviewRequest;
import com.olp.dto.ExamAttemptReviewResponse;
import com.olp.dto.FlaggedAttemptSummaryResponse;
import com.olp.entity.ExamAttemptReviewStatus;
import com.olp.entity.User;
import com.olp.service.ExamAttemptReviewService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExamAttemptReviewController (FR-I69, Step 56).
 */
@ExtendWith(MockitoExtension.class)
class ExamAttemptReviewControllerTest {

    @Mock
    private ExamAttemptReviewService reviewService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ExamAttemptReviewController controller;

    private User instructor;
    private ExamAttemptReviewResponse sampleReviewResponse;
    private FlaggedAttemptSummaryResponse sampleSummary;

    @BeforeEach
    void setUp() {
        instructor = new User();
        instructor.setId(10);
        instructor.setEmail("instructor@example.com");
        instructor.setName("Instructor Jane");
        instructor.setRole("INSTRUCTOR");

        sampleReviewResponse = new ExamAttemptReviewResponse();
        sampleReviewResponse.setAttemptId(101);
        sampleReviewResponse.setExamId(201);
        sampleReviewResponse.setExamTitle("Final Exam");
        sampleReviewResponse.setCourseId(301);
        sampleReviewResponse.setStudentId(401);
        sampleReviewResponse.setStudentName("Student Alice");
        sampleReviewResponse.setReviewStatus(ExamAttemptReviewStatus.PENDING);
        sampleReviewResponse.setReviewNotes("Excessive tab switches detected");
        sampleReviewResponse.setObtainedMarks(new BigDecimal("88.00"));
        sampleReviewResponse.setTotalMarks(new BigDecimal("100.00"));
        sampleReviewResponse.setMessage("Attempt flagged for proctoring review");

        sampleSummary = new FlaggedAttemptSummaryResponse(
                101, 201, "Final Exam", 301, 401, "Student Alice",
                1, 4, 1, "TAB_SWITCH_LIMIT_EXCEEDED",
                ExamAttemptReviewStatus.PENDING, "Excessive tab switches detected",
                10, "Instructor Jane", LocalDateTime.now(), null, null
        );
    }

    @Test
    @DisplayName("GET /flagged returns list of flagged attempts")
    void testGetFlaggedAttempts() {
        when(reviewService.resolveUser(authentication)).thenReturn(instructor);
        when(reviewService.getFlaggedAttempts(instructor, 301, 201, ExamAttemptReviewStatus.PENDING))
                .thenReturn(List.of(sampleSummary));

        ResponseEntity<List<FlaggedAttemptSummaryResponse>> response =
                controller.getFlaggedAttempts(301, 201, ExamAttemptReviewStatus.PENDING, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(101, response.getBody().get(0).getAttemptId());
        verify(reviewService).getFlaggedAttempts(instructor, 301, 201, ExamAttemptReviewStatus.PENDING);
    }

    @Test
    @DisplayName("GET /{attemptId}/review returns attempt review details")
    void testGetAttemptReviewDetails() {
        when(reviewService.resolveUser(authentication)).thenReturn(instructor);
        when(reviewService.getAttemptReviewDetails(instructor, 101)).thenReturn(sampleReviewResponse);

        ResponseEntity<ExamAttemptReviewResponse> response =
                controller.getAttemptReviewDetails(101, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ExamAttemptReviewStatus.PENDING, response.getBody().getReviewStatus());
        assertEquals(new BigDecimal("88.00"), response.getBody().getObtainedMarks());
        verify(reviewService).getAttemptReviewDetails(instructor, 101);
    }

    @Test
    @DisplayName("POST /{attemptId}/review performs review action successfully")
    void testReviewAttempt() {
        ExamAttemptReviewRequest req = new ExamAttemptReviewRequest("ESCALATE", "Multiple integrity flags", "Integrity compromise", "DISQUALIFY_ATTEMPT");
        when(reviewService.resolveUser(authentication)).thenReturn(instructor);
        when(reviewService.reviewAttempt(instructor, 101, req)).thenReturn(sampleReviewResponse);

        ResponseEntity<ExamAttemptReviewResponse> response =
                controller.reviewAttempt(101, req, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(reviewService).reviewAttempt(instructor, 101, req);
    }

    @Test
    @DisplayName("GET /{attemptId}/review/history returns append-only audit history")
    void testGetReviewHistory() {
        ExamAttemptReviewHistoryResponse historyEntry = new ExamAttemptReviewHistoryResponse(
                1, 101, "NONE", "PENDING", "FLAG", "Initial flag", null, 10, "Instructor Jane", LocalDateTime.now()
        );
        when(reviewService.resolveUser(authentication)).thenReturn(instructor);
        when(reviewService.getReviewHistory(instructor, 101)).thenReturn(List.of(historyEntry));

        ResponseEntity<List<ExamAttemptReviewHistoryResponse>> response =
                controller.getReviewHistory(101, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("FLAG", response.getBody().get(0).getAction());
        verify(reviewService).getReviewHistory(instructor, 101);
    }
}
