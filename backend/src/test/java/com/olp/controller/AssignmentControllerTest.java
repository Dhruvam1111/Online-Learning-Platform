package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.AssignmentService;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AssignmentController (FR-I68, Step 28).
 * Verifies 100% controller action routing and response status mapping across all endpoints.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentControllerTest {

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AssignmentController assignmentController;

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
    @DisplayName("POST /assignments returns 201 Created on assignment creation")
    void testCreateAssignment() {
        AssignmentCreateRequest request = new AssignmentCreateRequest();
        AssignmentResponse response = new AssignmentResponse();
        response.setId(10);

        when(assignmentService.resolveUser(authentication)).thenReturn(instructor);
        when(assignmentService.createAssignment(eq(instructor), any(AssignmentCreateRequest.class))).thenReturn(response);

        ResponseEntity<AssignmentResponse> result = assignmentController.createAssignment(request, authentication);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(10, result.getBody().getId());
        verify(assignmentService, times(1)).createAssignment(instructor, request);
    }

    @Test
    @DisplayName("PUT /assignments/{id} returns 200 OK on assignment update")
    void testUpdateAssignment() {
        AssignmentUpdateRequest request = new AssignmentUpdateRequest();
        AssignmentResponse response = new AssignmentResponse();
        response.setId(10);

        when(assignmentService.resolveUser(authentication)).thenReturn(instructor);
        when(assignmentService.updateAssignment(eq(instructor), eq(10), any(AssignmentUpdateRequest.class))).thenReturn(response);

        ResponseEntity<AssignmentResponse> result = assignmentController.updateAssignment(10, request, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(10, result.getBody().getId());
    }

    @Test
    @DisplayName("GET /assignments/{id}/submissions returns 200 OK with list of submissions")
    void testGetAssignmentSubmissions() {
        AssignmentSubmissionResponse subResponse = new AssignmentSubmissionResponse();
        subResponse.setId(101);

        when(assignmentService.resolveUser(authentication)).thenReturn(instructor);
        when(assignmentService.getAssignmentSubmissions(instructor, 10)).thenReturn(List.of(subResponse));

        ResponseEntity<List<AssignmentSubmissionResponse>> result = assignmentController.getAssignmentSubmissions(10, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
        assertEquals(101, result.getBody().get(0).getId());
    }

    @Test
    @DisplayName("POST /assignments/{id}/submissions/{subId}/grade returns 200 OK on grading submission")
    void testGradeSubmission() {
        AssignmentGradeRequest gradeRequest = new AssignmentGradeRequest();
        gradeRequest.setAwardedMarks(new BigDecimal("95.00"));
        gradeRequest.setFeedback("Excellent work");

        AssignmentSubmissionResponse response = new AssignmentSubmissionResponse();
        response.setId(101);
        response.setAwardedMarks(new BigDecimal("95.00"));

        when(assignmentService.resolveUser(authentication)).thenReturn(instructor);
        when(assignmentService.gradeSubmission(instructor, 101, new BigDecimal("95.00"), "Excellent work")).thenReturn(response);

        ResponseEntity<AssignmentSubmissionResponse> result = assignmentController.gradeSubmission(10, 101, gradeRequest, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(new BigDecimal("95.00"), result.getBody().getAwardedMarks());
    }

    @Test
    @DisplayName("GET /courses/{courseId}/assignments returns 200 OK with course assignments")
    void testGetCourseAssignments() {
        AssignmentResponse response = new AssignmentResponse();
        response.setId(10);

        when(assignmentService.resolveUser(authentication)).thenReturn(student);
        when(assignmentService.getCourseAssignments(student, 50)).thenReturn(List.of(response));

        ResponseEntity<List<AssignmentResponse>> result = assignmentController.getCourseAssignments(50, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    @DisplayName("GET /assignments/{id} returns 200 OK with assignment details")
    void testGetAssignment() {
        AssignmentResponse response = new AssignmentResponse();
        response.setId(10);

        when(assignmentService.resolveUser(authentication)).thenReturn(student);
        when(assignmentService.getAssignment(student, 10)).thenReturn(response);

        ResponseEntity<AssignmentResponse> result = assignmentController.getAssignment(10, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(10, result.getBody().getId());
    }

    @Test
    @DisplayName("POST /assignments/{id}/submissions returns 201 Created on student submission")
    void testSubmitAssignment() {
        AssignmentSubmissionRequest request = new AssignmentSubmissionRequest();
        AssignmentSubmissionResponse response = new AssignmentSubmissionResponse();
        response.setId(101);

        when(assignmentService.resolveUser(authentication)).thenReturn(student);
        when(assignmentService.submitAssignment(student, 10, request)).thenReturn(response);

        ResponseEntity<AssignmentSubmissionResponse> result = assignmentController.submitAssignment(10, request, authentication);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(101, result.getBody().getId());
    }

    @Test
    @DisplayName("GET /assignments/{id}/my-submission returns 200 OK with student submission")
    void testGetMySubmission() {
        AssignmentSubmissionResponse response = new AssignmentSubmissionResponse();
        response.setId(101);

        when(assignmentService.resolveUser(authentication)).thenReturn(student);
        when(assignmentService.getMySubmission(student, 10)).thenReturn(response);

        ResponseEntity<AssignmentSubmissionResponse> result = assignmentController.getMySubmission(10, authentication);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(101, result.getBody().getId());
    }
}
