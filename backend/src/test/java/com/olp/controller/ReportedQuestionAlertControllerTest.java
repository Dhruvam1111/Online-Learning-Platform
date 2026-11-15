package com.olp.controller;

import com.olp.dto.QuestionReportRequest;
import com.olp.dto.ReportedQuestionInboxResponse;
import com.olp.entity.AssessmentType;
import com.olp.entity.ReportedQuestionStatus;
import com.olp.entity.User;
import com.olp.service.ReportedQuestionAlertService;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportedQuestionAlertController (FR-I35, Step 52).
 */
@ExtendWith(MockitoExtension.class)
class ReportedQuestionAlertControllerTest {

    @Mock
    private ReportedQuestionAlertService alertService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ReportedQuestionAlertController controller;

    private User student;
    private User instructor;
    private ReportedQuestionInboxResponse sampleResponse;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(10);
        student.setEmail("student@example.com");
        student.setName("Student Dan");
        student.setRole("STUDENT");

        instructor = new User();
        instructor.setId(20);
        instructor.setEmail("prof@example.com");
        instructor.setName("Prof Dan");
        instructor.setRole("INSTRUCTOR");

        sampleResponse = new ReportedQuestionInboxResponse();
        sampleResponse.setId(1);
        sampleResponse.setAssessmentType(AssessmentType.EXAM);
        sampleResponse.setAssessmentId(101);
        sampleResponse.setAssessmentTitle("Midterm Exam");
        sampleResponse.setQuestionId(501);
        sampleResponse.setCourseId(12);
        sampleResponse.setCourseTitle("Algorithms");
        sampleResponse.setReportCount(1);
        sampleResponse.setStatus(ReportedQuestionStatus.PENDING);
    }

    @Test
    @DisplayName("POST reportQuestion returns 201 Created with response body")
    void reportQuestion_ReturnsCreated() {
        QuestionReportRequest request = new QuestionReportRequest("Typo in question text", "Option A is repeated", 999);
        when(alertService.resolveUser(authentication)).thenReturn(student);
        when(alertService.reportQuestion(eq(student), eq(AssessmentType.EXAM), eq(101), eq(501), any(QuestionReportRequest.class)))
                .thenReturn(sampleResponse);

        ResponseEntity<ReportedQuestionInboxResponse> result = controller.reportQuestion(
                AssessmentType.EXAM, 101, 501, request, authentication);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getId());
        assertEquals(ReportedQuestionStatus.PENDING, result.getBody().getStatus());
    }

    @Test
    @DisplayName("GET getInstructorInbox returns 200 OK with inbox list")
    void getInstructorInbox_ReturnsOk() {
        when(alertService.resolveUser(authentication)).thenReturn(instructor);
        when(alertService.getInstructorInbox(instructor, 12, ReportedQuestionStatus.PENDING))
                .thenReturn(List.of(sampleResponse));

        ResponseEntity<List<ReportedQuestionInboxResponse>> result = controller.getInstructorInbox(
                12, ReportedQuestionStatus.PENDING, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
        assertEquals(501, result.getBody().get(0).getQuestionId());
    }

    @Test
    @DisplayName("POST escalateUnresolvedReports returns 200 OK with escalated count")
    void escalateUnresolvedReports_ReturnsOk() {
        when(alertService.resolveUser(authentication)).thenReturn(instructor);
        when(alertService.escalateUnresolvedReports()).thenReturn(3);

        ResponseEntity<Map<String, Object>> result = controller.escalateUnresolvedReports(authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(3, result.getBody().get("escalatedCount"));
        assertEquals("Escalation check completed", result.getBody().get("message"));
    }
}
