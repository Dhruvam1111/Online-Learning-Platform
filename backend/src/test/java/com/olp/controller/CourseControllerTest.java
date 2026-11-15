package com.olp.controller;

import com.olp.dto.CoursePreviewResponse;
import com.olp.dto.CoursePricingResponse;
import com.olp.dto.CourseReviewStatusResponse;
import com.olp.dto.CourseSubmissionRequest;
import com.olp.dto.CourseSubmissionResponse;
import com.olp.dto.EnrollmentResponse;
import com.olp.entity.User;
import com.olp.service.CourseCatalogService;
import com.olp.service.CoursePreviewService;
import com.olp.service.CourseSubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock
    private CourseCatalogService courseCatalogService;

    @Mock
    private CoursePreviewService coursePreviewService;

    @Mock
    private CourseSubmissionService courseSubmissionService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CourseController controller;

    private User student;

    @BeforeEach
    void setUp() {
        student = new User("Alice", "alice@olp.com", "hash", "student", "active");
        student.setId(5);
    }

    @Test
    void testGetCoursePricing_Returns200WithPricingDetails() {
        CoursePricingResponse pricing = new CoursePricingResponse();
        pricing.setCourseId(101);
        pricing.setCourseTitle("Java Masterclass");
        pricing.setBasePrice(new BigDecimal("1000.00"));
        pricing.setFinalPayableAmount(new BigDecimal("1180.00"));
        pricing.setFree(false);

        when(courseCatalogService.getCoursePricing(101)).thenReturn(pricing);

        ResponseEntity<CoursePricingResponse> response = controller.getCoursePricing(101);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertSame(pricing, response.getBody());
        assertEquals(new BigDecimal("1180.00"), Objects.requireNonNull(response.getBody()).getFinalPayableAmount());
        verify(courseCatalogService).getCoursePricing(101);
    }

    @Test
    void testEnrollCourse_Returns200WithEnrollmentResponse() {
        when(courseCatalogService.resolveUser(authentication)).thenReturn(student);
        EnrollmentResponse enrollResp = new EnrollmentResponse(true, "Successfully enrolled", 101, true);
        when(courseCatalogService.enrollCourse(student, 101)).thenReturn(enrollResp);

        ResponseEntity<EnrollmentResponse> response = controller.enrollCourse(101, authentication);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(Objects.requireNonNull(response.getBody()).success());
        verify(courseCatalogService).enrollCourse(student, 101);
    }

    @Test
    void testPreviewCourse_Returns200WithPreviewDetails() {
        when(courseCatalogService.resolveUser(authentication)).thenReturn(student);
        CoursePreviewResponse preview = new CoursePreviewResponse();
        preview.setId(101);
        preview.setTitle("Java Masterclass");
        preview.setPreview(true);
        preview.setReadOnly(true);
        when(coursePreviewService.previewCourseAsStudent(student, 101)).thenReturn(preview);

        ResponseEntity<CoursePreviewResponse> response = controller.previewCourse(101, authentication);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertSame(preview, response.getBody());
        assertTrue(Objects.requireNonNull(response.getBody()).isPreview());
        verify(coursePreviewService).previewCourseAsStudent(student, 101);
    }

    @Test
    void testSubmitCourse_Returns200WithSubmissionResponse() {
        when(courseCatalogService.resolveUser(authentication)).thenReturn(student);
        CourseSubmissionRequest request = new CourseSubmissionRequest("Ready");
        CourseSubmissionResponse subResp = new CourseSubmissionResponse();
        subResp.setId(1);
        subResp.setCourseId(101);
        subResp.setStatus("PENDING_REVIEW");
        when(courseSubmissionService.submitCourseForReview(eq(student), eq(101), any(CourseSubmissionRequest.class)))
                .thenReturn(subResp);

        ResponseEntity<CourseSubmissionResponse> response = controller.submitCourse(101, request, authentication);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertSame(subResp, response.getBody());
        verify(courseSubmissionService).submitCourseForReview(eq(student), eq(101), any(CourseSubmissionRequest.class));
    }

    @Test
    void testGetSubmissions_Returns200WithList() {
        when(courseCatalogService.resolveUser(authentication)).thenReturn(student);
        CourseSubmissionResponse subResp = new CourseSubmissionResponse();
        subResp.setId(1);
        when(courseSubmissionService.getSubmissionHistory(student, 101)).thenReturn(List.of(subResp));

        ResponseEntity<List<CourseSubmissionResponse>> response = controller.getSubmissions(101, authentication);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
        verify(courseSubmissionService).getSubmissionHistory(student, 101);
    }

    @Test
    void testGetReviewStatus_Returns200WithStatus() {
        when(courseCatalogService.resolveUser(authentication)).thenReturn(student);
        CourseReviewStatusResponse statusResp = new CourseReviewStatusResponse();
        statusResp.setCourseId(101);
        statusResp.setReviewStatus("CHANGES_REQUESTED");
        statusResp.setCanResubmit(true);
        when(courseSubmissionService.getReviewStatus(student, 101)).thenReturn(statusResp);

        ResponseEntity<CourseReviewStatusResponse> response = controller.getReviewStatus(101, authentication);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("CHANGES_REQUESTED", Objects.requireNonNull(response.getBody()).getReviewStatus());
        assertTrue(Objects.requireNonNull(response.getBody()).isCanResubmit());
        verify(courseSubmissionService).getReviewStatus(student, 101);
    }

    @Test
    void testResubmitCourse_Returns200WithSubmissionResponse() {
        when(courseCatalogService.resolveUser(authentication)).thenReturn(student);
        CourseSubmissionRequest request = new CourseSubmissionRequest("Fixed requested items");
        CourseSubmissionResponse subResp = new CourseSubmissionResponse();
        subResp.setId(2);
        subResp.setCourseId(101);
        subResp.setStatus("PENDING_REVIEW");
        when(courseSubmissionService.resubmitCourse(eq(student), eq(101), any(CourseSubmissionRequest.class)))
                .thenReturn(subResp);

        ResponseEntity<CourseSubmissionResponse> response = controller.resubmitCourse(101, request, authentication);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertSame(subResp, response.getBody());
        assertEquals("PENDING_REVIEW", Objects.requireNonNull(response.getBody()).getStatus());
        verify(courseSubmissionService).resubmitCourse(eq(student), eq(101), any(CourseSubmissionRequest.class));
    }
}

