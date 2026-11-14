package com.olp.controller;

import com.olp.dto.ApiResponse;
import com.olp.dto.AvailableCourseAssociationResponse;
import com.olp.dto.CourseAssociationRequestDto;
import com.olp.dto.CourseAssociationRequestResponse;
import com.olp.dto.CourseAssociationReviewRequest;
import com.olp.dto.CourseInstructorResponse;
import com.olp.entity.AssociationRequestStatus;
import com.olp.entity.CourseInstructorRole;
import com.olp.entity.User;
import com.olp.service.AdminService;
import com.olp.service.CourseAssociationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseAssociationControllerTest {

    @Mock
    private CourseAssociationService courseAssociationService;

    @Mock
    private AdminService adminService;

    @InjectMocks
    private CourseAssociationController instructorController;

    private AdminController adminController;

    private User instructorUser;
    private User adminUser;
    private Authentication instructorAuth;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        instructorUser = new User("Prof. James", "james@olp.com", "hash", "instructor", "active");
        instructorUser.setId(2);
        instructorAuth = new UsernamePasswordAuthenticationToken(instructorUser, null);

        adminUser = new User("Admin", "admin@olp.com", "hash", "admin", "active");
        adminUser.setId(1);
        adminAuth = new UsernamePasswordAuthenticationToken(adminUser, null);

        adminController = new AdminController(adminService, null, courseAssociationService);
    }

    @Test
    void testControllers_HaveRequiredPreAuthorizeAnnotations() {
        PreAuthorize instPreAuth = CourseAssociationController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(instPreAuth);
        assertEquals("hasRole('instructor')", instPreAuth.value());

        PreAuthorize adminPreAuth = AdminController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(adminPreAuth);
        assertEquals("hasRole('admin')", adminPreAuth.value());
    }

    @Test
    void testSearchAvailableCourses_Returns200() {
        when(courseAssociationService.resolveUser(instructorAuth)).thenReturn(instructorUser);
        AvailableCourseAssociationResponse item = new AvailableCourseAssociationResponse(
                10, "Databases", "SQL fundamentals", "Databases", "PUBLISHED",
                1, "Dr. Sarah", false, "AVAILABLE", null);
        when(courseAssociationService.searchAvailableCourses(instructorUser, "Data", null))
                .thenReturn(List.of(item));

        ResponseEntity<List<AvailableCourseAssociationResponse>> response =
                instructorController.searchAvailableCourses(instructorAuth, "Data", null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
        assertEquals("AVAILABLE", Objects.requireNonNull(response.getBody()).get(0).getAssociationStatus());
    }

    @Test
    void testSubmitAssociationRequest_BodyEndpoint_Returns201() {
        when(courseAssociationService.resolveUser(instructorAuth)).thenReturn(instructorUser);
        CourseAssociationRequestDto dto = new CourseAssociationRequestDto(10, "Join request");
        CourseAssociationRequestResponse created = new CourseAssociationRequestResponse();
        created.setId(50);
        created.setCourseId(10);
        created.setStatus(AssociationRequestStatus.PENDING);

        when(courseAssociationService.submitAssociationRequest(instructorUser, null, dto)).thenReturn(created);

        ResponseEntity<CourseAssociationRequestResponse> response =
                instructorController.submitAssociationRequest(instructorAuth, dto);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(AssociationRequestStatus.PENDING, Objects.requireNonNull(response.getBody()).getStatus());
    }

    @Test
    void testSubmitCourseAssociationRequest_PathEndpoint_Returns201() {
        when(courseAssociationService.resolveUser(instructorAuth)).thenReturn(instructorUser);
        CourseAssociationRequestDto dto = new CourseAssociationRequestDto(null, "Let me join");
        CourseAssociationRequestResponse created = new CourseAssociationRequestResponse();
        created.setId(51);
        created.setCourseId(10);
        created.setStatus(AssociationRequestStatus.PENDING);

        when(courseAssociationService.submitAssociationRequest(instructorUser, 10, dto)).thenReturn(created);

        ResponseEntity<CourseAssociationRequestResponse> response =
                instructorController.submitCourseAssociationRequest(instructorAuth, 10, dto);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(51, Objects.requireNonNull(response.getBody()).getId());
    }

    @Test
    void testGetAndCancelMyRequests_Returns200() {
        when(courseAssociationService.resolveUser(instructorAuth)).thenReturn(instructorUser);
        CourseAssociationRequestResponse req = new CourseAssociationRequestResponse();
        req.setId(50);
        req.setStatus(AssociationRequestStatus.PENDING);

        when(courseAssociationService.getMyRequests(instructorUser, "PENDING")).thenReturn(List.of(req));
        when(courseAssociationService.getMyRequestById(instructorUser, 50)).thenReturn(req);

        assertEquals(1, Objects.requireNonNull(instructorController.getMyRequests(instructorAuth, "PENDING").getBody()).size());
        assertEquals(50, Objects.requireNonNull(instructorController.getMyRequestById(instructorAuth, 50).getBody()).getId());

        ResponseEntity<ApiResponse> cancelled = instructorController.cancelMyRequest(instructorAuth, 50);
        assertEquals(200, cancelled.getStatusCode().value());
        assertTrue(Objects.requireNonNull(cancelled.getBody()).isSuccess());
        verify(courseAssociationService).cancelMyRequest(instructorUser, 50);
    }

    @Test
    void testAdminAssociationRequestEndpoints_ApproveRejectAndList() {
        when(courseAssociationService.resolveUser(adminAuth)).thenReturn(adminUser);

        CourseAssociationRequestResponse pending = new CourseAssociationRequestResponse();
        pending.setId(50);
        pending.setStatus(AssociationRequestStatus.PENDING);
        when(courseAssociationService.getRequestsForAdmin(adminUser, "PENDING")).thenReturn(List.of(pending));

        ResponseEntity<List<CourseAssociationRequestResponse>> listResp =
                adminController.getAssociationRequests("PENDING", adminAuth);
        assertEquals(200, listResp.getStatusCode().value());
        assertEquals(1, Objects.requireNonNull(listResp.getBody()).size());

        CourseAssociationReviewRequest approveBody = new CourseAssociationReviewRequest("Welcome!");
        CourseAssociationRequestResponse approved = new CourseAssociationRequestResponse();
        approved.setId(50);
        approved.setStatus(AssociationRequestStatus.APPROVED);
        when(courseAssociationService.approveRequest(adminUser, 50, approveBody)).thenReturn(approved);

        ResponseEntity<CourseAssociationRequestResponse> approveResp =
                adminController.approveAssociationRequest(50, approveBody, adminAuth);
        assertEquals(200, approveResp.getStatusCode().value());
        assertEquals(AssociationRequestStatus.APPROVED, Objects.requireNonNull(approveResp.getBody()).getStatus());

        CourseAssociationReviewRequest rejectBody = new CourseAssociationReviewRequest("Full team");
        CourseAssociationRequestResponse rejected = new CourseAssociationRequestResponse();
        rejected.setId(51);
        rejected.setStatus(AssociationRequestStatus.REJECTED);
        when(courseAssociationService.rejectRequest(adminUser, 51, rejectBody)).thenReturn(rejected);

        ResponseEntity<CourseAssociationRequestResponse> rejectResp =
                adminController.rejectAssociationRequest(51, rejectBody, adminAuth);
        assertEquals(200, rejectResp.getStatusCode().value());
        assertEquals(AssociationRequestStatus.REJECTED, Objects.requireNonNull(rejectResp.getBody()).getStatus());
    }

    @Test
    void testAdminGetCourseInstructors_Returns200WithOrderedTeam() {
        when(courseAssociationService.resolveUser(adminAuth)).thenReturn(adminUser);
        List<CourseInstructorResponse> team = List.of(
                new CourseInstructorResponse(1, "Dr. Sarah", "sarah@olp.com", CourseInstructorRole.OWNER,
                        "Ph.D.", "DB", "Bio", null, null),
                new CourseInstructorResponse(2, "Prof. James", "james@olp.com", CourseInstructorRole.CO_INSTRUCTOR,
                        "M.Sc.", "SQL", "Bio 2", null, null)
        );
        when(courseAssociationService.getCourseInstructorsForAdmin(adminUser, 10)).thenReturn(team);

        ResponseEntity<List<CourseInstructorResponse>> response =
                adminController.getCourseInstructors(10, adminAuth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, Objects.requireNonNull(response.getBody()).size());
        assertEquals(CourseInstructorRole.OWNER, Objects.requireNonNull(response.getBody()).get(0).getRole());
        assertEquals(CourseInstructorRole.CO_INSTRUCTOR, Objects.requireNonNull(response.getBody()).get(1).getRole());
    }
}

