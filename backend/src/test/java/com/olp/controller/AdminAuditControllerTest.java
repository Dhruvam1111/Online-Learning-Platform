package com.olp.controller;

import com.olp.dto.AdminAuditLogResponse;
import com.olp.dto.PageResponse;
import com.olp.service.AdminAuditService;
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
class AdminAuditControllerTest {

    @Mock
    private AdminAuditService adminAuditService;

    @InjectMocks
    private AdminAuditController adminAuditController;

    private AdminAuditLogResponse sampleLog;

    @BeforeEach
    void setUp() {
        sampleLog = new AdminAuditLogResponse(
                1L, 10, "Admin User", "admin@example.com",
                "COURSE_APPROVED", "COURSE", "5", "Spring Boot Masterclass",
                "Course approved and published", "PENDING_REVIEW", "PUBLISHED",
                "127.0.0.1", LocalDateTime.now()
        );
    }

    @Test
    void testGetAuditLogs_Success() {
        PageResponse<AdminAuditLogResponse> pageResponse = new PageResponse<>(
                List.of(sampleLog), 0, 10, 1L, 1, true
        );

        when(adminAuditService.getAuditLogs(
                any(), any(), any(), any(), any(), any(), eq(0), eq(10), eq("createdAt"), eq("desc")
        )).thenReturn(pageResponse);

        ResponseEntity<PageResponse<AdminAuditLogResponse>> response = adminAuditController.getAuditLogs(
                null, null, null, null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        assertEquals("COURSE_APPROVED", response.getBody().getContent().get(0).getAction());
        assertEquals("Spring Boot Masterclass", response.getBody().getContent().get(0).getTargetName());
    }

    @Test
    void testGetAuditLogs_WithFilters() {
        PageResponse<AdminAuditLogResponse> pageResponse = new PageResponse<>(
                List.of(sampleLog), 0, 5, 1L, 1, true
        );

        when(adminAuditService.getAuditLogs(
                eq(10), eq("COURSE_APPROVED"), eq("COURSE"), any(), any(), eq("Spring"), eq(0), eq(5), eq("createdAt"), eq("desc")
        )).thenReturn(pageResponse);

        ResponseEntity<PageResponse<AdminAuditLogResponse>> response = adminAuditController.getAuditLogs(
                10, "COURSE_APPROVED", "COURSE", null, null, "Spring", 0, 5, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getContent().size());
        verify(adminAuditService, times(1)).getAuditLogs(
                eq(10), eq("COURSE_APPROVED"), eq("COURSE"), any(), any(), eq("Spring"), eq(0), eq(5), eq("createdAt"), eq("desc")
        );
    }
}
