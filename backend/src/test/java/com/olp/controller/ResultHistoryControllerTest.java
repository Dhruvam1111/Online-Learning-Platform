package com.olp.controller;

import com.olp.dto.CreateResultVersionRequest;
import com.olp.dto.ResultVersionResponse;
import com.olp.entity.AssessmentType;
import com.olp.entity.User;
import com.olp.service.ResultHistoryService;
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
 * Unit tests for ResultHistoryController (FR-I74, Step 49).
 */
@ExtendWith(MockitoExtension.class)
class ResultHistoryControllerTest {

    @Mock
    private ResultHistoryService resultHistoryService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ResultHistoryController controller;

    private User instructor;
    private ResultVersionResponse sampleResponse;

    @BeforeEach
    void setUp() {
        instructor = new User();
        instructor.setId(2);
        instructor.setEmail("instructor@example.com");
        instructor.setName("Instructor Jane");
        instructor.setRole("INSTRUCTOR");

        sampleResponse = new ResultVersionResponse();
        sampleResponse.setId(1);
        sampleResponse.setCourseId(10);
        sampleResponse.setAssessmentType(AssessmentType.EXAM);
        sampleResponse.setAssessmentId(101);
        sampleResponse.setAssessmentTitle("Midterm Exam");
        sampleResponse.setStudentId(50);
        sampleResponse.setStudentName("Student Bob");
        sampleResponse.setStudentEmail("bob@example.com");
        sampleResponse.setAttemptId(500);
        sampleResponse.setOldScore(BigDecimal.valueOf(75.00));
        sampleResponse.setNewScore(BigDecimal.valueOf(85.00));
        sampleResponse.setOldValue("75.00");
        sampleResponse.setNewValue("85.00");
        sampleResponse.setReason("Grade adjustment after question correction review");
        sampleResponse.setActorId(2);
        sampleResponse.setActorName("Instructor Jane");
        sampleResponse.setActorEmail("instructor@example.com");
        sampleResponse.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Get result history with specific student and assessment filters")
    void testGetResultHistory_WithFilters() {
        when(resultHistoryService.resolveUser(authentication)).thenReturn(instructor);
        when(resultHistoryService.getResultHistory(
                eq(instructor), eq(10), eq(50), eq(AssessmentType.EXAM), eq(101)))
                .thenReturn(List.of(sampleResponse));

        ResponseEntity<List<ResultVersionResponse>> response = controller.getResultHistory(
                10, 50, AssessmentType.EXAM, 101, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("85.00", response.getBody().get(0).getNewValue());
        assertEquals("Grade adjustment after question correction review", response.getBody().get(0).getReason());
        verify(resultHistoryService).getResultHistory(instructor, 10, 50, AssessmentType.EXAM, 101);
    }

    @Test
    @DisplayName("Get result history with no optional filters")
    void testGetResultHistory_NoFilters() {
        when(resultHistoryService.resolveUser(authentication)).thenReturn(instructor);
        when(resultHistoryService.getResultHistory(
                eq(instructor), eq(10), isNull(), isNull(), isNull()))
                .thenReturn(List.of(sampleResponse));

        ResponseEntity<List<ResultVersionResponse>> response = controller.getResultHistory(
                10, null, null, null, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(resultHistoryService).getResultHistory(instructor, 10, null, null, null);
    }

    @Test
    @DisplayName("Record a new result version change returns CREATED")
    void testRecordResultVersion_Success() {
        CreateResultVersionRequest request = new CreateResultVersionRequest(
                AssessmentType.EXAM,
                101,
                "Midterm Exam",
                50,
                500,
                BigDecimal.valueOf(75.00),
                BigDecimal.valueOf(85.00),
                "75.00",
                "85.00",
                "Grade adjustment after question correction review"
        );

        when(resultHistoryService.resolveUser(authentication)).thenReturn(instructor);
        when(resultHistoryService.recordResultVersion(eq(instructor), eq(10), any(CreateResultVersionRequest.class)))
                .thenReturn(sampleResponse);

        ResponseEntity<ResultVersionResponse> response = controller.recordResultVersion(10, request, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getId());
        assertEquals("85.00", response.getBody().getNewValue());
        verify(resultHistoryService).recordResultVersion(instructor, 10, request);
    }
}
