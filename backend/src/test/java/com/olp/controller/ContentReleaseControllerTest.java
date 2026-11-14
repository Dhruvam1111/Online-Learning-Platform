package com.olp.controller;

import com.olp.dto.ScheduleContentReleaseRequest;
import com.olp.dto.ScheduledReleaseResponse;
import com.olp.entity.User;
import com.olp.service.ScheduledContentReleaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentReleaseControllerTest {

    @Mock
    private ScheduledContentReleaseService releaseService;

    @InjectMocks
    private ContentReleaseController controller;

    private User instructor;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        instructor = new User("Dr. Chen", "chen@olp.com", "hash", "instructor");
        instructor.setId(10);
        authentication = new UsernamePasswordAuthenticationToken(instructor, null);
    }

    @Test
    @DisplayName("PUT /api/instructor/materials/{id}/schedule-release returns 200 OK")
    void testScheduleMaterialRelease() {
        ScheduleContentReleaseRequest req = new ScheduleContentReleaseRequest("2028-12-01 10:00:00", "Asia/Kolkata");
        ScheduledReleaseResponse mockResponse = new ScheduledReleaseResponse(
                500, "MATERIAL", "Lecture Notes", 100, 400,
                LocalDateTime.of(2028, 12, 1, 4, 30, 0), false, "Asia/Kolkata");

        when(releaseService.resolveUser(authentication)).thenReturn(instructor);
        when(releaseService.scheduleMaterialRelease(eq(instructor), eq(500), any(ScheduleContentReleaseRequest.class)))
                .thenReturn(mockResponse);

        ResponseEntity<ScheduledReleaseResponse> response = controller.scheduleMaterialRelease(500, req, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getId());
        assertEquals("MATERIAL", response.getBody().getContentType());
        assertFalse(response.getBody().getIsReleased());
        assertEquals("IST", response.getBody().getTimeZoneLabel());
        verify(releaseService, times(1)).scheduleMaterialRelease(instructor, 500, req);
    }

    @Test
    @DisplayName("GET /api/instructor/materials/{id}/schedule-release returns 200 OK")
    void testGetMaterialReleaseSchedule() {
        ScheduledReleaseResponse mockResponse = new ScheduledReleaseResponse(
                500, "MATERIAL", "Lecture Notes", 100, 400,
                LocalDateTime.of(2028, 12, 1, 4, 30, 0), false, "Asia/Kolkata");

        when(releaseService.resolveUser(authentication)).thenReturn(instructor);
        when(releaseService.getMaterialReleaseSchedule(instructor, 500)).thenReturn(mockResponse);

        ResponseEntity<ScheduledReleaseResponse> response = controller.getMaterialReleaseSchedule(500, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getId());
        verify(releaseService, times(1)).getMaterialReleaseSchedule(instructor, 500);
    }

    @Test
    @DisplayName("PUT /api/instructor/lessons/{lessonId}/schedule-release returns 200 OK")
    void testScheduleLessonRelease() {
        ScheduleContentReleaseRequest req = new ScheduleContentReleaseRequest("2028-06-15T09:00:00", "UTC");
        ScheduledReleaseResponse mockResponse = new ScheduledReleaseResponse(
                400, "LESSON", "Raft Consensus", 100, 400,
                LocalDateTime.of(2028, 6, 15, 9, 0, 0), false, "UTC");

        when(releaseService.resolveUser(authentication)).thenReturn(instructor);
        when(releaseService.scheduleLessonRelease(eq(instructor), eq(400), any(ScheduleContentReleaseRequest.class)))
                .thenReturn(mockResponse);

        ResponseEntity<ScheduledReleaseResponse> response = controller.scheduleLessonRelease(400, req, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getId());
        assertEquals("LESSON", response.getBody().getContentType());
        verify(releaseService, times(1)).scheduleLessonRelease(instructor, 400, req);
    }

    @Test
    @DisplayName("GET /api/instructor/lessons/{lessonId}/schedule-release returns 200 OK")
    void testGetLessonReleaseSchedule() {
        ScheduledReleaseResponse mockResponse = new ScheduledReleaseResponse(
                400, "LESSON", "Raft Consensus", 100, 400,
                LocalDateTime.of(2028, 6, 15, 9, 0, 0), false, "UTC");

        when(releaseService.resolveUser(authentication)).thenReturn(instructor);
        when(releaseService.getLessonReleaseSchedule(instructor, 400)).thenReturn(mockResponse);

        ResponseEntity<ScheduledReleaseResponse> response = controller.getLessonReleaseSchedule(400, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getId());
        verify(releaseService, times(1)).getLessonReleaseSchedule(instructor, 400);
    }

    @Test
    @DisplayName("POST /api/instructor/content/scan-releases returns 200 OK with published count")
    void testScanAndPublishReleases() {
        when(releaseService.resolveUser(authentication)).thenReturn(instructor);
        when(releaseService.triggerContentReleaseScan()).thenReturn(3);

        ResponseEntity<Map<String, Object>> response = controller.scanAndPublishReleases(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals(3, response.getBody().get("publishedCount"));
        verify(releaseService, times(1)).triggerContentReleaseScan();
    }
}

