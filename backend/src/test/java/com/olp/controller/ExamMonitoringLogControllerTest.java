package com.olp.controller;

import com.olp.dto.ExamAttemptMonitoringSummaryResponse;
import com.olp.dto.ExamMonitoringTimelineResponse;
import com.olp.dto.MonitoringEventResponse;
import com.olp.dto.RecordMonitoringEventRequest;
import com.olp.entity.MonitoringEventType;
import com.olp.entity.MonitoringSeverity;
import com.olp.entity.User;
import com.olp.service.ExamMonitoringLogService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamMonitoringLogControllerTest {

    @Mock
    private ExamMonitoringLogService monitoringLogService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ExamMonitoringLogController controller;

    private User instructor;
    private User student;

    @BeforeEach
    void setUp() {
        instructor = new User();
        instructor.setId(10);
        instructor.setName("Professor Smith");
        instructor.setEmail("smith@olp.com");
        instructor.setRole("INSTRUCTOR");

        student = new User();
        student.setId(101);
        student.setName("Alice Student");
        student.setEmail("alice@olp.com");
        student.setRole("STUDENT");
    }

    @Test
    @DisplayName("getExamMonitoringLogs: returns 200 OK with attempt summaries list")
    void testGetExamMonitoringLogs() {
        when(monitoringLogService.resolveUser(authentication)).thenReturn(instructor);

        ExamAttemptMonitoringSummaryResponse summary = new ExamAttemptMonitoringSummaryResponse(
                501, 20, "Midterm", 101, "Alice Student", "alice@olp.com",
                1, "SUBMITTED", "PENDING", true, 3, 1, 4L, "HIGH",
                LocalDateTime.now().minusHours(1), LocalDateTime.now()
        );
        when(monitoringLogService.getExamAttemptMonitoringList(instructor, 20, false))
                .thenReturn(List.of(summary));

        ResponseEntity<List<ExamAttemptMonitoringSummaryResponse>> response =
                controller.getExamMonitoringLogs(20, false, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(501, response.getBody().get(0).getAttemptId());
        assertEquals("HIGH", response.getBody().get(0).getHighestSeverity());
    }

    @Test
    @DisplayName("getAttemptMonitoringTimeline: returns 200 OK with chronological timeline")
    void testGetAttemptMonitoringTimeline() {
        when(monitoringLogService.resolveUser(authentication)).thenReturn(instructor);

        MonitoringEventResponse ev = new MonitoringEventResponse(
                1, 501, MonitoringEventType.TAB_SWITCH, MonitoringSeverity.MEDIUM,
                LocalDateTime.now(), 5, null, "Tab switched"
        );
        ExamMonitoringTimelineResponse timeline = new ExamMonitoringTimelineResponse(
                501, 20, "Midterm", 101, "Alice Student", "alice@olp.com",
                1, "SUBMITTED", "PENDING", true, 3, 1, 1L, 0L, 1L, 0L, 0L, List.of(ev)
        );

        when(monitoringLogService.getAttemptTimeline(instructor, 501)).thenReturn(timeline);

        ResponseEntity<ExamMonitoringTimelineResponse> response =
                controller.getAttemptMonitoringTimeline(501, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(501, response.getBody().getAttemptId());
        assertEquals(1, response.getBody().getEvents().size());
        assertEquals(MonitoringEventType.TAB_SWITCH, response.getBody().getEvents().get(0).getEventType());
    }

    @Test
    @DisplayName("recordMonitoringEvent: returns 200 OK with recorded event")
    void testRecordMonitoringEvent() {
        when(monitoringLogService.resolveUser(authentication)).thenReturn(student);

        RecordMonitoringEventRequest req = new RecordMonitoringEventRequest(
                MonitoringEventType.FULLSCREEN_EXIT, MonitoringSeverity.MEDIUM,
                LocalDateTime.now(), 2, null, "Exited fullscreen"
        );
        MonitoringEventResponse res = new MonitoringEventResponse(
                10, 501, MonitoringEventType.FULLSCREEN_EXIT, MonitoringSeverity.MEDIUM,
                LocalDateTime.now(), 2, null, "Exited fullscreen"
        );

        when(monitoringLogService.recordMonitoringEvent(eq(student), eq(501), any(RecordMonitoringEventRequest.class)))
                .thenReturn(res);

        ResponseEntity<MonitoringEventResponse> response =
                controller.recordMonitoringEvent(501, req, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().getId());
        assertEquals(MonitoringEventType.FULLSCREEN_EXIT, response.getBody().getEventType());
    }
}
