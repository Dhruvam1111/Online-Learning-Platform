package com.olp.service;

import com.olp.dto.ExamAttemptMonitoringSummaryResponse;
import com.olp.dto.ExamMonitoringTimelineResponse;
import com.olp.dto.MonitoringEventResponse;
import com.olp.dto.RecordMonitoringEventRequest;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamMonitoringLogServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private ExamMonitoringEventRepository monitoringEventRepository;

    @Mock
    private ExamMonitoringConfigRepository monitoringConfigRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExamMonitoringLogService service;

    private User admin;
    private User primaryInstructor;
    private User coInstructor;
    private User unauthorizedInstructor;
    private User student1;
    private User student2;

    private Course course;
    private Exam exam;
    private ExamMonitoringConfig config;
    private ExamAttempt attempt1;
    private ExamAttempt attempt2;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1);
        admin.setRole("ADMIN");
        admin.setEmail("admin@olp.com");
        admin.setName("Admin User");

        primaryInstructor = new User();
        primaryInstructor.setId(2);
        primaryInstructor.setRole("INSTRUCTOR");
        primaryInstructor.setEmail("prof@olp.com");
        primaryInstructor.setName("Primary Professor");

        coInstructor = new User();
        coInstructor.setId(3);
        coInstructor.setRole("INSTRUCTOR");
        coInstructor.setEmail("co@olp.com");
        coInstructor.setName("Co Instructor");

        unauthorizedInstructor = new User();
        unauthorizedInstructor.setId(4);
        unauthorizedInstructor.setRole("INSTRUCTOR");
        unauthorizedInstructor.setEmail("stranger@olp.com");
        unauthorizedInstructor.setName("Unauthorized Instructor");

        student1 = new User();
        student1.setId(101);
        student1.setRole("STUDENT");
        student1.setEmail("student1@olp.com");
        student1.setName("Alice Student");

        student2 = new User();
        student2.setId(102);
        student2.setRole("STUDENT");
        student2.setEmail("student2@olp.com");
        student2.setName("Bob Student");

        course = new Course();
        course.setId(10);
        course.setTitle("Distributed Systems");
        course.setInstructor(primaryInstructor);

        exam = new Exam();
        exam.setId(20);
        exam.setTitle("Midterm Examination");
        exam.setCourse(course);

        config = new ExamMonitoringConfig();
        config.setId(30);
        config.setExam(exam);
        config.setTabSwitchDetection(true);
        config.setTabSwitchLimit(2);
        config.setRequireFullscreen(true);
        config.setFullscreenExitLimit(1);

        attempt1 = new ExamAttempt();
        attempt1.setId(1001);
        attempt1.setExam(exam);
        attempt1.setStudent(student1);
        attempt1.setAttemptNumber(1);
        attempt1.setStatus("SUBMITTED");
        attempt1.setReviewStatus(ExamAttemptReviewStatus.NONE);
        attempt1.setTabSwitchCount(0);
        attempt1.setFullscreenExitCount(0);
        attempt1.setStartedAt(LocalDateTime.now().minusHours(2));
        attempt1.setSubmittedAt(LocalDateTime.now().minusHours(1));

        attempt2 = new ExamAttempt();
        attempt2.setId(1002);
        attempt2.setExam(exam);
        attempt2.setStudent(student2);
        attempt2.setAttemptNumber(1);
        attempt2.setStatus("SUBMITTED");
        attempt2.setReviewStatus(ExamAttemptReviewStatus.PENDING);
        attempt2.setTabSwitchCount(3);
        attempt2.setFullscreenExitCount(1);
        attempt2.setStartedAt(LocalDateTime.now().minusHours(2));
        attempt2.setSubmittedAt(LocalDateTime.now().minusHours(1));
    }

    @Test
    @DisplayName("getExamAttemptMonitoringList: returns all attempts when flaggedOnly is false")
    void testGetExamAttemptMonitoringList_AllAttempts() {
        when(examRepository.findById(20)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.findByExamId(20)).thenReturn(List.of(attempt1, attempt2));

        ExamMonitoringEvent event = new ExamMonitoringEvent(
                attempt2, MonitoringEventType.TAB_SWITCH, MonitoringSeverity.HIGH,
                LocalDateTime.now().minusMinutes(30), 5, "{\"target\":\"browser\"}", "Switched window"
        );
        when(monitoringEventRepository.findByAttemptIdOrderByEventTimestampAsc(1001)).thenReturn(Collections.emptyList());
        when(monitoringEventRepository.findByAttemptIdOrderByEventTimestampAsc(1002)).thenReturn(List.of(event));

        List<ExamAttemptMonitoringSummaryResponse> results = service.getExamAttemptMonitoringList(primaryInstructor, 20, false);

        assertNotNull(results);
        assertEquals(2, results.size());

        ExamAttemptMonitoringSummaryResponse first = results.get(0);
        assertEquals(1001, first.getAttemptId());
        assertFalse(first.getIsFlagged());
        assertEquals(0, first.getTotalEventsCount());
        assertNull(first.getHighestSeverity());

        ExamAttemptMonitoringSummaryResponse second = results.get(1);
        assertEquals(1002, second.getAttemptId());
        assertTrue(second.getIsFlagged());
        assertEquals(1, second.getTotalEventsCount());
        assertEquals("HIGH", second.getHighestSeverity());
    }

    @Test
    @DisplayName("getExamAttemptMonitoringList: filters only flagged attempts when flaggedOnly is true")
    void testGetExamAttemptMonitoringList_FlaggedOnly() {
        when(examRepository.findById(20)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.findByExamId(20)).thenReturn(List.of(attempt1, attempt2));

        ExamMonitoringEvent event = new ExamMonitoringEvent(
                attempt2, MonitoringEventType.TAB_SWITCH, MonitoringSeverity.MEDIUM,
                LocalDateTime.now().minusMinutes(30), 5, null, null
        );
        when(monitoringEventRepository.findByAttemptIdOrderByEventTimestampAsc(1001)).thenReturn(Collections.emptyList());
        when(monitoringEventRepository.findByAttemptIdOrderByEventTimestampAsc(1002)).thenReturn(List.of(event));

        List<ExamAttemptMonitoringSummaryResponse> results = service.getExamAttemptMonitoringList(primaryInstructor, 20, true);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(1002, results.get(0).getAttemptId());
    }

    @Test
    @DisplayName("getExamAttemptMonitoringList: allows co-instructor and admin access")
    void testGetExamAttemptMonitoringList_CoInstructorAndAdmin() {
        when(examRepository.findById(20)).thenReturn(Optional.of(exam));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 3)).thenReturn(true);
        when(examAttemptRepository.findByExamId(20)).thenReturn(Collections.emptyList());

        List<ExamAttemptMonitoringSummaryResponse> coResults = service.getExamAttemptMonitoringList(coInstructor, 20, false);
        assertNotNull(coResults);

        List<ExamAttemptMonitoringSummaryResponse> adminResults = service.getExamAttemptMonitoringList(admin, 20, false);
        assertNotNull(adminResults);
    }

    @Test
    @DisplayName("getExamAttemptMonitoringList: throws AccessDeniedException for unauthorized instructor or student")
    void testGetExamAttemptMonitoringList_Unauthorized() {
        when(examRepository.findById(20)).thenReturn(Optional.of(exam));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 4)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                service.getExamAttemptMonitoringList(unauthorizedInstructor, 20, false));

        assertThrows(AccessDeniedException.class, () ->
                service.getExamAttemptMonitoringList(student1, 20, false));
    }

    @Test
    @DisplayName("getExamAttemptMonitoringList: throws ResourceNotFoundException when exam not found")
    void testGetExamAttemptMonitoringList_ExamNotFound() {
        when(examRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.getExamAttemptMonitoringList(primaryInstructor, 999, false));
    }

    @Test
    @DisplayName("getExamAttemptMonitoringList: throws IllegalArgumentException when examId is null")
    void testGetExamAttemptMonitoringList_NullExamId() {
        assertThrows(IllegalArgumentException.class, () ->
                service.getExamAttemptMonitoringList(primaryInstructor, null, false));
    }

    @Test
    @DisplayName("getAttemptTimeline: returns chronological events and severity counters")
    void testGetAttemptTimeline_Success() {
        when(examAttemptRepository.findById(1002)).thenReturn(Optional.of(attempt2));

        LocalDateTime now = LocalDateTime.now();
        ExamMonitoringEvent e1 = new ExamMonitoringEvent(attempt2, MonitoringEventType.FOCUS_LOST, MonitoringSeverity.LOW, now.minusMinutes(40), 10, null, "Blur");
        e1.setId(1);
        ExamMonitoringEvent e2 = new ExamMonitoringEvent(attempt2, MonitoringEventType.TAB_SWITCH, MonitoringSeverity.MEDIUM, now.minusMinutes(30), 15, null, "Tab switched");
        e2.setId(2);
        ExamMonitoringEvent e3 = new ExamMonitoringEvent(attempt2, MonitoringEventType.SCREEN_SHARE_STOPPED, MonitoringSeverity.HIGH, now.minusMinutes(20), 5, null, "Stopped share");
        e3.setId(3);
        ExamMonitoringEvent e4 = new ExamMonitoringEvent(attempt2, MonitoringEventType.DEVICE_DISCONNECTED, MonitoringSeverity.CRITICAL, now.minusMinutes(10), 30, null, "Unplugged");
        e4.setId(4);

        when(monitoringEventRepository.findByAttemptIdOrderByEventTimestampAsc(1002)).thenReturn(List.of(e1, e2, e3, e4));

        ExamMonitoringTimelineResponse timeline = service.getAttemptTimeline(primaryInstructor, 1002);

        assertNotNull(timeline);
        assertEquals(1002, timeline.getAttemptId());
        assertEquals(20, timeline.getExamId());
        assertEquals("Midterm Examination", timeline.getExamTitle());
        assertEquals(4, timeline.getTotalEventsCount());
        assertEquals(1, timeline.getLowSeverityCount());
        assertEquals(1, timeline.getMediumSeverityCount());
        assertEquals(1, timeline.getHighSeverityCount());
        assertEquals(1, timeline.getCriticalSeverityCount());
        assertEquals(4, timeline.getEvents().size());
        assertEquals(MonitoringEventType.FOCUS_LOST, timeline.getEvents().get(0).getEventType());
        assertEquals(MonitoringEventType.DEVICE_DISCONNECTED, timeline.getEvents().get(3).getEventType());
    }

    @Test
    @DisplayName("getAttemptTimeline: throws AccessDeniedException when caller is not authorized")
    void testGetAttemptTimeline_Unauthorized() {
        when(examAttemptRepository.findById(1002)).thenReturn(Optional.of(attempt2));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 4)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                service.getAttemptTimeline(unauthorizedInstructor, 1002));
    }

    @Test
    @DisplayName("getAttemptTimeline: throws ResourceNotFoundException when attempt not found")
    void testGetAttemptTimeline_NotFound() {
        when(examAttemptRepository.findById(9999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.getAttemptTimeline(primaryInstructor, 9999));
    }

    @Test
    @DisplayName("getAttemptTimeline: throws IllegalArgumentException when attemptId is null")
    void testGetAttemptTimeline_NullAttemptId() {
        assertThrows(IllegalArgumentException.class, () ->
                service.getAttemptTimeline(primaryInstructor, null));
    }

    @Test
    @DisplayName("recordMonitoringEvent: records TAB_SWITCH, increments counter, flags attempt on limit breach")
    void testRecordMonitoringEvent_TabSwitchBreach() {
        when(examAttemptRepository.findById(1001)).thenReturn(Optional.of(attempt1));
        when(monitoringConfigRepository.findByExamId(20)).thenReturn(Optional.of(config));
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(monitoringEventRepository.save(any(ExamMonitoringEvent.class))).thenAnswer(inv -> {
            ExamMonitoringEvent e = inv.getArgument(0);
            e.setId(501);
            return e;
        });

        // First tab switch (count becomes 1, limit is 2)
        RecordMonitoringEventRequest req1 = new RecordMonitoringEventRequest(
                MonitoringEventType.TAB_SWITCH, null, LocalDateTime.now(), 4, "{\"dest\":\"notepad\"}", "Tab switch 1"
        );
        MonitoringEventResponse resp1 = service.recordMonitoringEvent(student1, 1001, req1);
        assertNotNull(resp1);
        assertEquals(501, resp1.getId());
        assertEquals(1, attempt1.getTabSwitchCount());
        assertEquals(ExamAttemptReviewStatus.NONE, attempt1.getReviewStatus());

        // Second tab switch (count becomes 2, breaches limit 2 -> flags attempt)
        RecordMonitoringEventRequest req2 = new RecordMonitoringEventRequest(
                MonitoringEventType.TAB_SWITCH, null, LocalDateTime.now(), 6, "{\"dest\":\"browser\"}", "Tab switch 2"
        );
        service.recordMonitoringEvent(student1, 1001, req2);
        assertEquals(2, attempt1.getTabSwitchCount());
        assertEquals(ExamAttemptReviewStatus.PENDING, attempt1.getReviewStatus());
        assertNotNull(attempt1.getMonitoringFlags());
        assertTrue(attempt1.getMonitoringFlags().contains("TAB_SWITCH"));
    }

    @Test
    @DisplayName("recordMonitoringEvent: records FULLSCREEN_EXIT and increments counter")
    void testRecordMonitoringEvent_FullscreenExit() {
        when(examAttemptRepository.findById(1001)).thenReturn(Optional.of(attempt1));
        when(monitoringConfigRepository.findByExamId(20)).thenReturn(Optional.of(config));
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(monitoringEventRepository.save(any(ExamMonitoringEvent.class))).thenAnswer(inv -> {
            ExamMonitoringEvent e = inv.getArgument(0);
            e.setId(502);
            return e;
        });

        RecordMonitoringEventRequest req = new RecordMonitoringEventRequest(
                MonitoringEventType.FULLSCREEN_EXIT, null, LocalDateTime.now(), 12, null, "Exited fullscreen"
        );
        service.recordMonitoringEvent(student1, 1001, req);

        assertEquals(1, attempt1.getFullscreenExitCount());
        // config limit for fullscreen exit is 1 -> breach!
        assertEquals(ExamAttemptReviewStatus.PENDING, attempt1.getReviewStatus());
    }

    @Test
    @DisplayName("recordMonitoringEvent: flags attempt when HIGH or CRITICAL severity event recorded")
    void testRecordMonitoringEvent_HighSeverity() {
        when(examAttemptRepository.findById(1001)).thenReturn(Optional.of(attempt1));
        when(monitoringConfigRepository.findByExamId(20)).thenReturn(Optional.of(config));
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(monitoringEventRepository.save(any(ExamMonitoringEvent.class))).thenAnswer(inv -> {
            ExamMonitoringEvent e = inv.getArgument(0);
            e.setId(503);
            return e;
        });

        RecordMonitoringEventRequest req = new RecordMonitoringEventRequest(
                MonitoringEventType.SCREEN_SHARE_STOPPED, MonitoringSeverity.HIGH, LocalDateTime.now(), 2, null, "Stopped stream"
        );
        service.recordMonitoringEvent(student1, 1001, req);

        assertEquals(ExamAttemptReviewStatus.PENDING, attempt1.getReviewStatus());
        assertTrue(attempt1.getMonitoringFlags().contains("SCREEN_SHARE_STOPPED"));
    }

    @Test
    @DisplayName("recordMonitoringEvent: throws AccessDeniedException when another student attempts recording")
    void testRecordMonitoringEvent_WrongStudent() {
        when(examAttemptRepository.findById(1001)).thenReturn(Optional.of(attempt1));

        RecordMonitoringEventRequest req = new RecordMonitoringEventRequest(
                MonitoringEventType.TAB_SWITCH, null, LocalDateTime.now(), 2, null, null
        );

        assertThrows(AccessDeniedException.class, () ->
                service.recordMonitoringEvent(student2, 1001, req));
    }

    @Test
    @DisplayName("recordMonitoringEvent: allows admin to record event for student attempt")
    void testRecordMonitoringEvent_AdminAllowed() {
        when(examAttemptRepository.findById(1001)).thenReturn(Optional.of(attempt1));
        when(monitoringConfigRepository.findByExamId(20)).thenReturn(Optional.of(config));
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(monitoringEventRepository.save(any(ExamMonitoringEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordMonitoringEventRequest req = new RecordMonitoringEventRequest(
                MonitoringEventType.BROWSER_WARNING, null, LocalDateTime.now(), 0, null, "Admin logged warning"
        );

        assertDoesNotThrow(() -> service.recordMonitoringEvent(admin, 1001, req));
    }

    @Test
    @DisplayName("recordMonitoringEvent: throws IllegalArgumentException on null attemptId or invalid request")
    void testRecordMonitoringEvent_ValidationFailures() {
        RecordMonitoringEventRequest req = new RecordMonitoringEventRequest();

        assertThrows(IllegalArgumentException.class, () ->
                service.recordMonitoringEvent(student1, null, req));

        assertThrows(IllegalArgumentException.class, () ->
                service.recordMonitoringEvent(student1, 1001, null));

        assertThrows(IllegalArgumentException.class, () ->
                service.recordMonitoringEvent(student1, 1001, req));
    }

    @Test
    @DisplayName("calculateHighestSeverity: evaluates correct hierarchy and empty cases")
    void testCalculateHighestSeverity() {
        assertNull(service.calculateHighestSeverity(null));
        assertNull(service.calculateHighestSeverity(Collections.emptyList()));

        ExamMonitoringEvent low = new ExamMonitoringEvent(attempt1, MonitoringEventType.FOCUS_LOST, MonitoringSeverity.LOW, LocalDateTime.now(), 0, null, null);
        ExamMonitoringEvent med = new ExamMonitoringEvent(attempt1, MonitoringEventType.TAB_SWITCH, MonitoringSeverity.MEDIUM, LocalDateTime.now(), 0, null, null);
        ExamMonitoringEvent high = new ExamMonitoringEvent(attempt1, MonitoringEventType.SCREEN_SHARE_STOPPED, MonitoringSeverity.HIGH, LocalDateTime.now(), 0, null, null);
        ExamMonitoringEvent crit = new ExamMonitoringEvent(attempt1, MonitoringEventType.DEVICE_DISCONNECTED, MonitoringSeverity.CRITICAL, LocalDateTime.now(), 0, null, null);

        assertEquals("LOW", service.calculateHighestSeverity(List.of(low)));
        assertEquals("MEDIUM", service.calculateHighestSeverity(List.of(low, med)));
        assertEquals("HIGH", service.calculateHighestSeverity(List.of(low, med, high)));
        assertEquals("CRITICAL", service.calculateHighestSeverity(List.of(low, med, high, crit)));
    }

    @Test
    @DisplayName("calculateIsFlagged: evaluates CLEARED, PENDING, ESCALATED, counters, and event severities")
    void testCalculateIsFlagged() {
        assertFalse(service.calculateIsFlagged(null, null));

        ExamAttempt a = new ExamAttempt();
        a.setReviewStatus(ExamAttemptReviewStatus.CLEARED);
        a.setTabSwitchCount(5);
        assertFalse(service.calculateIsFlagged(a, null));

        a.setReviewStatus(ExamAttemptReviewStatus.ESCALATED);
        assertTrue(service.calculateIsFlagged(a, null));

        a.setReviewStatus(ExamAttemptReviewStatus.NONE);
        a.setTabSwitchCount(0);
        a.setFullscreenExitCount(0);
        a.setMonitoringFlags(null);
        assertFalse(service.calculateIsFlagged(a, Collections.emptyList()));

        a.setMonitoringFlags("Suspicious behaviour");
        assertTrue(service.calculateIsFlagged(a, Collections.emptyList()));

        a.setMonitoringFlags("");
        ExamMonitoringEvent high = new ExamMonitoringEvent(a, MonitoringEventType.SCREEN_SHARE_STOPPED, MonitoringSeverity.HIGH, LocalDateTime.now(), 0, null, null);
        assertTrue(service.calculateIsFlagged(a, List.of(high)));
    }

    @Test
    @DisplayName("defaultSeverityFor: maps event types appropriately")
    void testDefaultSeverityFor() {
        assertEquals(MonitoringSeverity.LOW, service.defaultSeverityFor(null));
        assertEquals(MonitoringSeverity.HIGH, service.defaultSeverityFor(MonitoringEventType.SCREEN_SHARE_STOPPED));
        assertEquals(MonitoringSeverity.HIGH, service.defaultSeverityFor(MonitoringEventType.DEVICE_DISCONNECTED));
        assertEquals(MonitoringSeverity.MEDIUM, service.defaultSeverityFor(MonitoringEventType.TAB_SWITCH));
        assertEquals(MonitoringSeverity.MEDIUM, service.defaultSeverityFor(MonitoringEventType.FULLSCREEN_EXIT));
        assertEquals(MonitoringSeverity.MEDIUM, service.defaultSeverityFor(MonitoringEventType.BROWSER_WARNING));
        assertEquals(MonitoringSeverity.LOW, service.defaultSeverityFor(MonitoringEventType.FOCUS_LOST));
        assertEquals(MonitoringSeverity.LOW, service.defaultSeverityFor(MonitoringEventType.FOCUS_REGAINED));
    }

    @Test
    @DisplayName("resolveUser: resolves User principal, email String, auth name, and throws on failure")
    void testResolveUser() {
        Authentication authUser = new UsernamePasswordAuthenticationToken(primaryInstructor, null);
        assertSame(primaryInstructor, service.resolveUser(authUser));

        when(userRepository.findByEmail("prof@olp.com")).thenReturn(Optional.of(primaryInstructor));
        Authentication authEmail = new UsernamePasswordAuthenticationToken("prof@olp.com", null);
        assertSame(primaryInstructor, service.resolveUser(authEmail));

        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(null));

        Authentication invalidAuth = new UsernamePasswordAuthenticationToken(12345, null);
        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(invalidAuth));
    }

    @Test
    @DisplayName("isInstructorOfCourse: covers null checks, admin role, primary instructor, and co-instructor")
    void testIsInstructorOfCourse() {
        assertFalse(service.isInstructorOfCourse(null, course));
        assertFalse(service.isInstructorOfCourse(primaryInstructor, null));
        assertTrue(service.isInstructorOfCourse(admin, course));
        assertTrue(service.isInstructorOfCourse(primaryInstructor, course));

        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 3)).thenReturn(true);
        assertTrue(service.isInstructorOfCourse(coInstructor, course));

        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 4)).thenReturn(false);
        assertFalse(service.isInstructorOfCourse(unauthorizedInstructor, course));
    }
}
