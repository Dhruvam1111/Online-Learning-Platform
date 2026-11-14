package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.GradingAlertType;
import com.olp.entity.User;
import com.olp.service.PendingGradingAlertService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PendingGradingAlertController (FR-I33, Step 50).
 */
@ExtendWith(MockitoExtension.class)
class PendingGradingAlertControllerTest {

    @Mock
    private PendingGradingAlertService service;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PendingGradingAlertController controller;

    private User instructor;
    private PendingGradingCountResponse countResponse;
    private CoursePendingDetailsResponse detailsResponse;
    private PendingGradingAlertResponse alertResponse;

    @BeforeEach
    void setUp() {
        instructor = new User();
        instructor.setId(10);
        instructor.setEmail("instructor@example.com");
        instructor.setName("Instructor Jane");
        instructor.setRole("INSTRUCTOR");

        CoursePendingGradingSummary summary = new CoursePendingGradingSummary(
                101, "Software Architecture", 5, 3, 2);
        countResponse = new PendingGradingCountResponse(5, 3, 2, List.of(summary));

        detailsResponse = new CoursePendingDetailsResponse(
                101, "Software Architecture", 5, 3, 2, List.of(), List.of());

        alertResponse = new PendingGradingAlertResponse();
        alertResponse.setId(1);
        alertResponse.setInstructorId(10);
        alertResponse.setCourseId(101);
        alertResponse.setCourseTitle("Software Architecture");
        alertResponse.setAlertType(GradingAlertType.NEW_SUBMISSION);
        alertResponse.setPendingCount(5);
        alertResponse.setTitle("New Submission Awaiting Grading");
        alertResponse.setMessage("Student Alice submitted Homework 1");
        alertResponse.setIsRead(false);
        alertResponse.setSentAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Get pending grading count badge returns OK with counts")
    void testGetPendingGradingCount() {
        when(service.resolveUser(authentication)).thenReturn(instructor);
        when(service.getPendingGradingCount(instructor)).thenReturn(countResponse);

        ResponseEntity<PendingGradingCountResponse> response = controller.getPendingGradingCount(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().getTotalPendingCount());
        assertEquals(3, response.getBody().getDescriptiveQuizPendingCount());
        assertEquals(2, response.getBody().getAssignmentPendingCount());
        assertEquals(1, response.getBody().getCourses().size());
        verify(service).getPendingGradingCount(instructor);
    }

    @Test
    @DisplayName("Get course pending details returns OK with breakdown lists")
    void testGetCoursePendingDetails() {
        when(service.resolveUser(authentication)).thenReturn(instructor);
        when(service.getCoursePendingDetails(instructor, 101)).thenReturn(detailsResponse);

        ResponseEntity<CoursePendingDetailsResponse> response = controller.getCoursePendingDetails(101, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(101, response.getBody().getCourseId());
        assertEquals(5, response.getBody().getTotalPending());
        verify(service).getCoursePendingDetails(instructor, 101);
    }

    @Test
    @DisplayName("Get instructor alerts with unreadOnly flag")
    void testGetInstructorAlerts() {
        when(service.resolveUser(authentication)).thenReturn(instructor);
        when(service.getInstructorAlerts(instructor, true)).thenReturn(List.of(alertResponse));

        ResponseEntity<List<PendingGradingAlertResponse>> response = controller.getInstructorAlerts(true, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("New Submission Awaiting Grading", response.getBody().get(0).getTitle());
        verify(service).getInstructorAlerts(instructor, true);
    }

    @Test
    @DisplayName("Mark alert as read returns updated response")
    void testMarkAlertAsRead() {
        alertResponse.setIsRead(true);
        when(service.resolveUser(authentication)).thenReturn(instructor);
        when(service.markAlertAsRead(instructor, 1)).thenReturn(alertResponse);

        ResponseEntity<PendingGradingAlertResponse> response = controller.markAlertAsRead(1, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getIsRead());
        verify(service).markAlertAsRead(instructor, 1);
    }

    @Test
    @DisplayName("Send daily reminder digests returns count")
    void testSendDailyReminderDigests() {
        when(service.sendDailyReminderDigests()).thenReturn(3);

        ResponseEntity<Map<String, Object>> response = controller.sendDailyReminderDigests();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().get("digestsSent"));
        verify(service).sendDailyReminderDigests();
    }

    @Test
    @DisplayName("Trigger submission alert returns generated notifications")
    void testTriggerSubmissionAlert() {
        when(service.triggerNewSubmissionAlert(101, "Essay 1", "Bob"))
                .thenReturn(List.of(alertResponse));

        ResponseEntity<List<PendingGradingAlertResponse>> response = controller.triggerSubmissionAlert(
                101, "Essay 1", "Bob");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(service).triggerNewSubmissionAlert(101, "Essay 1", "Bob");
    }
}
