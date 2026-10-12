package com.olp.controller;

import com.olp.dto.AdminCourseResponse;
import com.olp.dto.AdminUserResponse;
import com.olp.dto.PageResponse;
import com.olp.dto.RejectCourseRequest;
import com.olp.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private AdminUserResponse sampleAdminResponse;
    private AdminCourseResponse sampleCourseResponse;

    @BeforeEach
    void setUp() {
        sampleAdminResponse = new AdminUserResponse(
                1, "Test User", "test@olp.com", "instructor", "pending", LocalDateTime.now()
        );
        sampleCourseResponse = new AdminCourseResponse(
                10, "Spring Boot", "Desc", 2, "Dr. Chen", "chen@olp.com", "pending_review", null, LocalDateTime.now()
        );
    }

    @Test
    void testGetUsers() {
        PageResponse<AdminUserResponse> pageResponse = new PageResponse<>(
                List.of(sampleAdminResponse), 0, 10, 1, 1, true
        );
        when(adminService.getUsers(any(), any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(pageResponse);

        ResponseEntity<PageResponse<AdminUserResponse>> response = adminController.getUsers(
                "instructor", "pending", "test", 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getContent().size());
    }

    @Test
    void testGetPendingInstructors() {
        PageResponse<AdminUserResponse> pageResponse = new PageResponse<>(
                List.of(sampleAdminResponse), 0, 10, 1, 1, true
        );
        when(adminService.getPendingInstructors(0, 10)).thenReturn(pageResponse);

        ResponseEntity<PageResponse<AdminUserResponse>> response = adminController.getPendingInstructors(0, 10);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getContent().size());
    }

    @Test
    void testApproveInstructor() {
        sampleAdminResponse.setAccountStatus("active");
        when(adminService.approveInstructor(1)).thenReturn(sampleAdminResponse);

        ResponseEntity<AdminUserResponse> response = adminController.approveInstructor(1);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("active", response.getBody().getAccountStatus());
    }

    @Test
    void testRejectInstructor() {
        sampleAdminResponse.setAccountStatus("rejected");
        when(adminService.rejectInstructor(1)).thenReturn(sampleAdminResponse);

        ResponseEntity<AdminUserResponse> response = adminController.rejectInstructor(1);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("rejected", response.getBody().getAccountStatus());
    }

    @Test
    void testSuspendUser() {
        sampleAdminResponse.setAccountStatus("suspended");
        when(adminService.suspendUser(1)).thenReturn(sampleAdminResponse);

        ResponseEntity<AdminUserResponse> response = adminController.suspendUser(1);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("suspended", response.getBody().getAccountStatus());
    }

    @Test
    void testActivateUser() {
        sampleAdminResponse.setAccountStatus("active");
        when(adminService.activateUser(1)).thenReturn(sampleAdminResponse);

        ResponseEntity<AdminUserResponse> response = adminController.activateUser(1);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("active", response.getBody().getAccountStatus());
    }

    @Test
    void testDeleteUser() {
        doNothing().when(adminService).deleteUser(1);

        var response = adminController.deleteUser(1);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        verify(adminService, times(1)).deleteUser(1);
    }

    @Test
    void testGetCourses() {
        PageResponse<AdminCourseResponse> pageResponse = new PageResponse<>(
                List.of(sampleCourseResponse), 0, 10, 1, 1, true
        );
        when(adminService.getCourses(any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(pageResponse);

        ResponseEntity<PageResponse<AdminCourseResponse>> response = adminController.getCourses(
                "pending_review", "Spring", 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getContent().size());
    }

    @Test
    void testGetPendingCourses() {
        PageResponse<AdminCourseResponse> pageResponse = new PageResponse<>(
                List.of(sampleCourseResponse), 0, 10, 1, 1, true
        );
        when(adminService.getPendingCourses(0, 10)).thenReturn(pageResponse);

        ResponseEntity<PageResponse<AdminCourseResponse>> response = adminController.getPendingCourses(0, 10);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getContent().size());
    }

    @Test
    void testApproveCourse() {
        sampleCourseResponse.setStatus("published");
        when(adminService.approveCourse(10)).thenReturn(sampleCourseResponse);

        ResponseEntity<AdminCourseResponse> response = adminController.approveCourse(10);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("published", response.getBody().getStatus());
    }

    @Test
    void testRejectCourse() {
        sampleCourseResponse.setStatus("rejected");
        sampleCourseResponse.setRejectionReason("Audio quality needs improvement");
        RejectCourseRequest request = new RejectCourseRequest("Audio quality needs improvement");
        when(adminService.rejectCourse(eq(10), any(RejectCourseRequest.class))).thenReturn(sampleCourseResponse);

        ResponseEntity<AdminCourseResponse> response = adminController.rejectCourse(10, request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("rejected", response.getBody().getStatus());
        assertEquals("Audio quality needs improvement", response.getBody().getRejectionReason());
    }

    @Test
    void testUnpublishCourse() {
        sampleCourseResponse.setStatus("draft");
        when(adminService.unpublishCourse(10)).thenReturn(sampleCourseResponse);

        ResponseEntity<AdminCourseResponse> response = adminController.unpublishCourse(10);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("draft", response.getBody().getStatus());
    }

    @Test
    void testDeleteCourse() {
        doNothing().when(adminService).deleteCourse(10);

        var response = adminController.deleteCourse(10);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        verify(adminService, times(1)).deleteCourse(10);
    }
}
