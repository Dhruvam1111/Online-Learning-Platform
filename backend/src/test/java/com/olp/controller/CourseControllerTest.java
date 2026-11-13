package com.olp.controller;

import com.olp.dto.CoursePreviewResponse;
import com.olp.dto.CoursePricingResponse;
import com.olp.dto.EnrollmentResponse;
import com.olp.entity.User;
import com.olp.service.CourseCatalogService;
import com.olp.service.CoursePreviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock
    private CourseCatalogService courseCatalogService;

    @Mock
    private CoursePreviewService coursePreviewService;

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
        assertEquals(new BigDecimal("1180.00"), response.getBody().getFinalPayableAmount());
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
        assertTrue(response.getBody().success());
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
        assertTrue(response.getBody().isPreview());
        verify(coursePreviewService).previewCourseAsStudent(student, 101);
    }
}

