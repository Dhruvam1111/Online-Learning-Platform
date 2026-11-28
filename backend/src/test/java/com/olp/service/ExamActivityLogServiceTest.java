package com.olp.service;

import com.olp.dto.ExamMonitoringEventRequest;
import com.olp.dto.ExamMonitoringEventResponse;
import com.olp.dto.ExamMonitoringLogDto;
import com.olp.dto.ScreenShareConsentRequest;
import com.olp.entity.Course;
import com.olp.entity.Exam;
import com.olp.entity.ExamAttempt;
import com.olp.entity.ExamMonitoringLog;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.ExamAttemptRepository;
import com.olp.repository.ExamMonitoringConfigRepository;
import com.olp.repository.ExamMonitoringLogRepository;
import com.olp.repository.ExamQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamActivityLogServiceTest {

    @Mock
    private ExamMonitoringLogRepository examMonitoringLogRepository;

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private ExamMonitoringConfigRepository monitoringConfigRepository;

    @Mock
    private ExamQuestionRepository examQuestionRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @InjectMocks
    private ExamActivityLogService examActivityLogService;

    private User student;
    private Exam exam;
    private ExamAttempt attempt;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(101);
        student.setEmail("student@example.com");

        Course course = new Course();
        course.setId(10);

        exam = new Exam();
        exam.setId(201);
        exam.setTitle("Midterm Exam");
        exam.setCourse(course);

        attempt = new ExamAttempt(exam, student, 1, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        attempt.setId(301);
    }

    @Test
    void testRecordMonitoringEvent_TabSwitch_Success() {
        when(examAttemptRepository.findById(301)).thenReturn(Optional.of(attempt));
        when(examMonitoringLogRepository.save(any(ExamMonitoringLog.class))).thenAnswer(i -> {
            ExamMonitoringLog log = i.getArgument(0);
            log.setId(1);
            return log;
        });

        ExamMonitoringEventRequest request = new ExamMonitoringEventRequest();
        request.setEventType("TAB_SWITCH");
        request.setDurationSeconds(5);
        request.setDetails("Left exam tab");

        ExamMonitoringEventResponse response = examActivityLogService.recordMonitoringEvent(student, 301, request);

        assertNotNull(response);
        assertEquals("TAB_SWITCH", response.getEventType());
        assertEquals(1, attempt.getTabSwitchCount());
        verify(examAttemptRepository).save(attempt);
        verify(examMonitoringLogRepository).save(any(ExamMonitoringLog.class));
    }

    @Test
    void testRecordMonitoringEvent_FullscreenExit_Success() {
        when(examAttemptRepository.findById(301)).thenReturn(Optional.of(attempt));
        when(examMonitoringLogRepository.save(any(ExamMonitoringLog.class))).thenAnswer(i -> {
            ExamMonitoringLog log = i.getArgument(0);
            log.setId(2);
            return log;
        });

        ExamMonitoringEventRequest request = new ExamMonitoringEventRequest();
        request.setEventType("FULLSCREEN_EXIT");
        request.setDurationSeconds(3);

        ExamMonitoringEventResponse response = examActivityLogService.recordMonitoringEvent(student, 301, request);

        assertNotNull(response);
        assertEquals(1, attempt.getFullscreenExitCount());
        verify(examAttemptRepository).save(attempt);
    }

    @Test
    void testRecordMonitoringEvent_UnauthorizedUser_ThrowsAccessDenied() {
        User otherStudent = new User();
        otherStudent.setId(999);

        when(examAttemptRepository.findById(301)).thenReturn(Optional.of(attempt));

        ExamMonitoringEventRequest request = new ExamMonitoringEventRequest();
        request.setEventType("WINDOW_BLUR");

        assertThrows(AccessDeniedException.class, () ->
                examActivityLogService.recordMonitoringEvent(otherStudent, 301, request)
        );
    }

    @Test
    void testRecordMonitoringEvent_AttemptNotFound_ThrowsResourceNotFound() {
        when(examAttemptRepository.findById(999)).thenReturn(Optional.empty());

        ExamMonitoringEventRequest request = new ExamMonitoringEventRequest();
        request.setEventType("TAB_SWITCH");

        assertThrows(ResourceNotFoundException.class, () ->
                examActivityLogService.recordMonitoringEvent(student, 999, request)
        );
    }

    @Test
    void testRecordScreenShareConsent_Success() {
        when(examAttemptRepository.findById(301)).thenReturn(Optional.of(attempt));
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(i -> i.getArgument(0));

        ScreenShareConsentRequest request = new ScreenShareConsentRequest(true);
        ExamAttempt updated = examActivityLogService.recordScreenShareConsent(student, 301, request);

        assertTrue(updated.getScreenShareConsentGiven());
        assertNotNull(updated.getScreenShareConsentAt());
        verify(examAttemptRepository).save(attempt);
        verify(examMonitoringLogRepository).save(any(ExamMonitoringLog.class));
    }

    @Test
    void testGetAttemptLogs_Success() {
        when(examAttemptRepository.findById(301)).thenReturn(Optional.of(attempt));

        ExamMonitoringLog log1 = new ExamMonitoringLog(attempt, student, exam, "TAB_SWITCH", LocalDateTime.now(), 2, "Tab lost", "WARN", "FOCUS_CHECK");
        log1.setId(10);
        when(examMonitoringLogRepository.findByAttemptIdOrderByTimestampAsc(301)).thenReturn(List.of(log1));

        List<ExamMonitoringLogDto> logs = examActivityLogService.getAttemptLogs(student, 301);

        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals("TAB_SWITCH", logs.get(0).getEventType());
    }
}
