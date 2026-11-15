package com.olp.controller;

import com.olp.dto.CertificateResponse;
import com.olp.dto.PageResponse;
import com.olp.entity.User;
import com.olp.service.CourseCompletionService;
import com.olp.service.InstructorCourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Controller Unit Test Suite for FR-I57: View Certificates Issued.
 * Tests REST endpoint routing, role-based access control annotations,
 * query parameter handling (filtering & pagination), read-only safety,
 * and public verification endpoints (DR-02).
 */
@ExtendWith(MockitoExtension.class)
class InstructorCertificateControllerTest {

    @Mock
    private CourseCompletionService courseCompletionService;

    @Mock
    private InstructorCourseService instructorCourseService;

    @InjectMocks
    private InstructorCertificateController controller;

    @InjectMocks
    private CertificateController publicCertificateController;

    private User instructor;
    private Authentication auth;
    private CertificateResponse certResponse;

    @BeforeEach
    void setUp() {
        instructor = new User("Dr. Banner", "banner@olp.com", "hash", "instructor", "active");
        instructor.setId(10);
        auth = new UsernamePasswordAuthenticationToken(instructor, null);

        certResponse = new CertificateResponse();
        certResponse.setId(1);
        certResponse.setCertificateId(1);
        certResponse.setCourseId(100);
        certResponse.setCourseName("Advanced Physics");
        certResponse.setStudentId(50);
        certResponse.setLearnerId(50);
        certResponse.setStudentName("Peter Parker");
        certResponse.setLearnerName("Peter Parker");
        certResponse.setStudentEmail("peter@olp.com");
        certResponse.setLearnerEmail("peter@olp.com");
        certResponse.setCertificateCode("OLP-100-50-PHYSICS1");
        certResponse.setIssuedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        certResponse.setIssueDate(LocalDateTime.of(2026, 4, 1, 10, 0));
        certResponse.setVerificationUrl("/api/certificates/verify/OLP-100-50-PHYSICS1");
        certResponse.setPublicVerificationUrl("/api/certificates/verify/OLP-100-50-PHYSICS1");
        certResponse.setValid(true);
        certResponse.setStatus("VERIFIED");
    }

    @Test
    @DisplayName("Security Annotation: Controller is restricted to instructor or admin roles")
    void testSecurityAnnotation_RestrictedToInstructorOrAdmin() {
        PreAuthorize preAuth = InstructorCertificateController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuth, "InstructorCertificateController must have @PreAuthorize");
        assertTrue(preAuth.value().contains("hasRole('instructor')") || preAuth.value().contains("hasRole('admin')"),
                "Must enforce instructor or admin authorization");
    }

    @Test
    @DisplayName("Read-Only Enforcement: Controller has zero POST, PUT, PATCH, or DELETE endpoints")
    void testReadOnlySafety_NoMutatingMethods() {
        Method[] methods = InstructorCertificateController.class.getDeclaredMethods();
        for (Method method : methods) {
            assertFalse(method.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class),
                    "No POST mappings allowed: certificate records must be strictly read-only");
            assertFalse(method.isAnnotationPresent(org.springframework.web.bind.annotation.PutMapping.class),
                    "No PUT mappings allowed: certificate records must be strictly read-only");
            assertFalse(method.isAnnotationPresent(org.springframework.web.bind.annotation.DeleteMapping.class),
                    "No DELETE mappings allowed: certificate records must be strictly read-only");
            assertFalse(method.isAnnotationPresent(org.springframework.web.bind.annotation.PatchMapping.class),
                    "No PATCH mappings allowed: certificate records must be strictly read-only");
        }
    }

    @Test
    @DisplayName("GET /api/instructor/certificates (unpaginated) returns List<CertificateResponse>")
    void testGetIssuedCertificates_Unpaginated_ReturnsList() {
        when(instructorCourseService.resolveUser(auth)).thenReturn(instructor);
        when(courseCompletionService.getIssuedCertificatesList(eq(instructor), isNull(), isNull(), isNull()))
                .thenReturn(List.of(certResponse));

        ResponseEntity<?> response = controller.getIssuedCertificates(
                auth, null, null, null, null, null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List<?>);
        @SuppressWarnings("unchecked")
        List<CertificateResponse> list = (List<CertificateResponse>) Objects.requireNonNull(response.getBody());
        assertEquals(1, list.size());
        assertEquals("OLP-100-50-PHYSICS1", list.get(0).getCertificateCode());
        assertEquals("Peter Parker", list.get(0).getLearnerName());
    }

    @Test
    @DisplayName("GET /api/instructor/certificates (paginated) returns PageResponse<CertificateResponse>")
    void testGetIssuedCertificates_Paginated_ReturnsPageResponse() {
        when(instructorCourseService.resolveUser(auth)).thenReturn(instructor);
        PageResponse<CertificateResponse> pageResp = new PageResponse<>(
                List.of(certResponse), 0, 10, 1, 1, true);
        when(courseCompletionService.getIssuedCertificates(eq(instructor), eq(100), anyString(), anyString(), eq(0), eq(10)))
                .thenReturn(pageResp);

        ResponseEntity<?> response = controller.getIssuedCertificates(
                auth, 100, "2026-01-01", null, "2026-12-31", null, 0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof PageResponse<?>);
        @SuppressWarnings("unchecked")
        PageResponse<CertificateResponse> body = (PageResponse<CertificateResponse>) Objects.requireNonNull(response.getBody());
        assertEquals(1, body.getTotalElements());
        assertEquals(1, body.getContent().size());
        assertEquals("Advanced Physics", body.getContent().get(0).getCourseName());
    }

    @Test
    @DisplayName("GET /api/instructor/certificates supports alias parameters fromDate and toDate")
    void testGetIssuedCertificates_AliasDateParameters() {
        when(instructorCourseService.resolveUser(auth)).thenReturn(instructor);
        when(courseCompletionService.getIssuedCertificatesList(eq(instructor), isNull(), eq("2026-03-01"), eq("2026-04-30")))
                .thenReturn(List.of(certResponse));

        ResponseEntity<?> response = controller.getIssuedCertificates(
                auth, null, null, "2026-03-01", null, "2026-04-30", null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(courseCompletionService).getIssuedCertificatesList(instructor, null, "2026-03-01", "2026-04-30");
    }

    @Test
    @DisplayName("GET /api/instructor/certificates/paged explicitly returns PageResponse")
    void testGetIssuedCertificatesPaged_Endpoint() {
        when(instructorCourseService.resolveUser(auth)).thenReturn(instructor);
        PageResponse<CertificateResponse> pageResp = new PageResponse<>(
                List.of(certResponse), 0, 5, 1, 1, true);
        when(courseCompletionService.getIssuedCertificates(eq(instructor), isNull(), isNull(), isNull(), eq(0), eq(5)))
                .thenReturn(pageResp);

        ResponseEntity<PageResponse<CertificateResponse>> response = controller.getIssuedCertificatesPaged(
                auth, null, null, null, null, null, 0, 5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, Objects.requireNonNull(response.getBody()).getTotalElements());
        assertEquals(5, Objects.requireNonNull(response.getBody()).getPageSize());
    }

    @Test
    @DisplayName("GET /api/instructor/certificates/all explicitly returns unpaginated List")
    void testGetIssuedCertificatesAll_Endpoint() {
        when(instructorCourseService.resolveUser(auth)).thenReturn(instructor);
        when(courseCompletionService.getIssuedCertificatesList(eq(instructor), eq(100), isNull(), isNull()))
                .thenReturn(List.of(certResponse));

        ResponseEntity<List<CertificateResponse>> response = controller.getIssuedCertificatesAll(
                auth, 100, null, null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
    }

    @Test
    @DisplayName("Public Verification: GET /api/certificates/verify/{code} returns verified details (DR-02)")
    void testPublicVerification_Path_Success() {
        when(courseCompletionService.verifyCertificate("OLP-100-50-PHYSICS1"))
                .thenReturn(certResponse);

        ResponseEntity<CertificateResponse> response = publicCertificateController
                .verifyCertificatePath("OLP-100-50-PHYSICS1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(Objects.requireNonNull(response.getBody()).isValid());
        assertEquals("VERIFIED", Objects.requireNonNull(response.getBody()).getStatus());
        assertEquals("OLP-100-50-PHYSICS1", Objects.requireNonNull(response.getBody()).getCertificateCode());
        assertEquals("/api/certificates/verify/OLP-100-50-PHYSICS1", Objects.requireNonNull(response.getBody()).getVerificationUrl());
    }

    @Test
    @DisplayName("Public Verification: GET /api/certificates/{code} alias returns verified details")
    void testPublicVerification_DirectAlias_Success() {
        when(courseCompletionService.verifyCertificate("OLP-100-50-PHYSICS1"))
                .thenReturn(certResponse);

        ResponseEntity<CertificateResponse> response = publicCertificateController
                .getCertificateByCode("OLP-100-50-PHYSICS1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Peter Parker", Objects.requireNonNull(response.getBody()).getLearnerName());
    }

    @Test
    @DisplayName("Public Verification: GET /api/certificates/verify?code=... query returns verified details")
    void testPublicVerification_QueryParam_Success() {
        when(courseCompletionService.verifyCertificate("OLP-100-50-PHYSICS1"))
                .thenReturn(certResponse);

        ResponseEntity<CertificateResponse> response = publicCertificateController
                .verifyCertificateQuery("OLP-100-50-PHYSICS1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Advanced Physics", Objects.requireNonNull(response.getBody()).getCourseName());
    }
}
