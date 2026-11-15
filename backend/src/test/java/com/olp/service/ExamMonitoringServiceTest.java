package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExamMonitoringService (FR-I26, Step 40).
 * Tests monitoring config persistence, threshold validation, instructor access control,
 * student read permissions, consent verification (DR-06), and browser fallback warnings (NFR-25).
 */
@ExtendWith(MockitoExtension.class)
class ExamMonitoringServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamMonitoringConfigRepository monitoringConfigRepository;

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private ExamMonitoringService monitoringService;

    private User instructor;
    private User student;
    private User unenrolledStudent;
    private User unauthorizedInstructor;
    private Course course;
    private Exam exam;
    private ExamMonitoringConfig config;

    private final LocalDateTime baseNow = LocalDateTime.of(2026, 11, 20, 10, 0, 0);

    @BeforeEach
    void setUp() {
        monitoringService = new ExamMonitoringService(
                examRepository,
                monitoringConfigRepository,
                examAttemptRepository,
                courseRepository,
                courseInstructorRepository,
                enrollmentRepository
        );

        Clock fixedClock = Clock.fixed(baseNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        monitoringService.setClock(fixedClock);

        instructor = new User("Dr. Proctor", "proctor@olp.com", "hash", "instructor", "active");
        instructor.setId(10);

        unauthorizedInstructor = new User("Dr. Rogue", "rogue@olp.com", "hash", "instructor", "active");
        unauthorizedInstructor.setId(99);

        student = new User("Alice Student", "alice@olp.com", "hash", "student", "active");
        student.setId(20);

        unenrolledStudent = new User("Eve Outsider", "eve@olp.com", "hash", "student", "active");
        unenrolledStudent.setId(30);

        course = new Course();
        course.setId(100);
        course.setTitle("Operating Systems & Security");
        course.setInstructor(instructor);

        exam = new Exam();
        exam.setId(1);
        exam.setTitle("OS Midterm Exam");
        exam.setCourse(course);
        exam.setCreatedBy(instructor);
        exam.setRequireEnrollment(true);

        config = new ExamMonitoringConfig(
                exam,
                true,   // tabSwitchDetection
                3,      // tabSwitchLimit
                true,   // requireFullscreen
                2,      // fullscreenExitLimit
                false,  // requireScreenShare
                true,   // requireWebcam
                false,  // requireMicrophone
                true,   // requireStudentConsent
                "WARN", // unsupportedBrowserPolicy
                "Custom consent text for academic integrity."
        );
        config.setId(1);
    }

    @Test
    void testSaveMonitoringConfig_Success() {
        when(examRepository.findById(1)).thenReturn(Optional.of(exam));
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(monitoringConfigRepository.findByExamId(1)).thenReturn(Optional.of(config));
        when(monitoringConfigRepository.save(any(ExamMonitoringConfig.class))).thenAnswer(i -> i.getArgument(0));

        ExamMonitoringConfigRequest request = new ExamMonitoringConfigRequest();
        request.setTabSwitchDetection(true);
        request.setTabSwitchLimit(5);
        request.setRequireFullscreen(true);
        request.setFullscreenExitLimit(3);
        request.setRequireScreenShare(true);
        request.setRequireWebcam(true);
        request.setUnsupportedBrowserPolicy("BLOCK");
        request.setConsentText("Updated integrity terms.");

        ExamMonitoringConfigResponse response = monitoringService.saveMonitoringConfig(instructor, 1, request);

        assertNotNull(response);
        assertEquals(1, response.getExamId());
        assertEquals(5, response.getTabSwitchLimit());
        assertEquals(3, response.getFullscreenExitLimit());
        assertTrue(response.getRequireScreenShare());
        assertEquals("BLOCK", response.getUnsupportedBrowserPolicy());
        assertEquals("Updated integrity terms.", response.getConsentText());
    }

    @Test
    void testSaveMonitoringConfig_InvalidTabSwitchThreshold_ThrowsIllegalArgument() {
        when(examRepository.findById(1)).thenReturn(Optional.of(exam));
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));

        ExamMonitoringConfigRequest request = new ExamMonitoringConfigRequest();
        request.setTabSwitchLimit(-5); // invalid!

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> monitoringService.saveMonitoringConfig(instructor, 1, request));
        assertTrue(ex.getMessage().contains("Tab switch limit"));
    }

    @Test
    void testSaveMonitoringConfig_InvalidFullscreenExitThreshold_ThrowsIllegalArgument() {
        when(examRepository.findById(1)).thenReturn(Optional.of(exam));
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));

        ExamMonitoringConfigRequest request = new ExamMonitoringConfigRequest();
        request.setFullscreenExitLimit(100); // exceeds 50!

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> monitoringService.saveMonitoringConfig(instructor, 1, request));
        assertTrue(ex.getMessage().contains("Fullscreen exit limit"));
    }

    @Test
    void testSaveMonitoringConfig_InvalidPolicy_ThrowsIllegalArgument() {
        when(examRepository.findById(1)).thenReturn(Optional.of(exam));
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));

        ExamMonitoringConfigRequest request = new ExamMonitoringConfigRequest();
        request.setUnsupportedBrowserPolicy("INVALID_POLICY");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> monitoringService.saveMonitoringConfig(instructor, 1, request));
        assertTrue(ex.getMessage().contains("Unsupported browser policy"));
    }

    @Test
    void testSaveMonitoringConfig_UnauthorizedInstructor_ThrowsAccessDenied() {
        when(examRepository.findById(1)).thenReturn(Optional.of(exam));
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 99)).thenReturn(false);

        ExamMonitoringConfigRequest request = new ExamMonitoringConfigRequest();

        assertThrows(AccessDeniedException.class,
                () -> monitoringService.saveMonitoringConfig(unauthorizedInstructor, 1, request));
        verify(monitoringConfigRepository, never()).save(any());
    }

    @Test
    void testGetMonitoringConfig_StudentEnrolled_ReturnsConfig() {
        when(examRepository.findById(1)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);
        when(monitoringConfigRepository.findByExamId(1)).thenReturn(Optional.of(config));

        ExamMonitoringConfigResponse response = monitoringService.getMonitoringConfig(student, 1, true);

        assertNotNull(response);
        assertEquals(1, response.getExamId());
        assertEquals("OS Midterm Exam", response.getExamTitle());
        assertEquals(3, response.getTabSwitchLimit());
        assertNull(response.getFullscreenSupportWarning());
    }

    @Test
    void testGetMonitoringConfig_StudentNotEnrolled_ThrowsAccessDenied() {
        when(examRepository.findById(1)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(30, 100)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> monitoringService.getMonitoringConfig(unenrolledStudent, 1, true));
    }

    @Test
    void testGetMonitoringConfig_UnsupportedBrowser_WarnPolicy() {
        config.setUnsupportedBrowserPolicy("WARN");
        when(examRepository.findById(1)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);
        when(monitoringConfigRepository.findByExamId(1)).thenReturn(Optional.of(config));

        ExamMonitoringConfigResponse response = monitoringService.getMonitoringConfig(student, 1, false);

        assertNotNull(response);
        assertNotNull(response.getFullscreenSupportWarning());
        assertTrue(response.getFullscreenSupportWarning().contains("WARNING"));
    }

    @Test
    void testGetMonitoringConfig_UnsupportedBrowser_BlockPolicy() {
        config.setUnsupportedBrowserPolicy("BLOCK");
        when(examRepository.findById(1)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);
        when(monitoringConfigRepository.findByExamId(1)).thenReturn(Optional.of(config));

        ExamMonitoringConfigResponse response = monitoringService.getMonitoringConfig(student, 1, false);

        assertNotNull(response);
        assertNotNull(response.getFullscreenSupportWarning());
        assertTrue(response.getFullscreenSupportWarning().contains("BLOCKED"));
    }

    @Test
    void testRecordConsent_ConsentGiven_Success() {
        when(examRepository.findById(1)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);
        when(monitoringConfigRepository.findByExamId(1)).thenReturn(Optional.of(config));

        ExamAttempt active = new ExamAttempt(exam, student, 1, baseNow, baseNow.plusHours(1));
        when(examAttemptRepository.findActiveAttempts(1, 20)).thenReturn(List.of(active));
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(i -> i.getArgument(0));

        ExamConsentRequest request = new ExamConsentRequest(true, true);
        ExamConsentResponse response = monitoringService.recordStudentConsent(student, 1, request);

        assertNotNull(response);
        assertTrue(response.getConsentGiven());
        assertFalse(response.getBlocked());
        assertEquals(baseNow, response.getConsentGivenAt());
        assertTrue(active.getConsentGiven());
        assertEquals(baseNow, active.getConsentGivenAt());
    }

    @Test
    void testRecordConsent_ConsentDeclined_Blocked() {
        when(examRepository.findById(1)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);
        when(monitoringConfigRepository.findByExamId(1)).thenReturn(Optional.of(config));

        ExamConsentRequest request = new ExamConsentRequest(false, true);
        ExamConsentResponse response = monitoringService.recordStudentConsent(student, 1, request);

        assertNotNull(response);
        assertFalse(response.getConsentGiven());
        assertTrue(response.getBlocked());
        assertTrue(response.getMessage().contains("Consent was declined"));
    }

    @Test
    void testRecordConsent_UnsupportedBrowser_BlockPolicy_Blocked() {
        config.setUnsupportedBrowserPolicy("BLOCK");
        when(examRepository.findById(1)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);
        when(monitoringConfigRepository.findByExamId(1)).thenReturn(Optional.of(config));

        ExamConsentRequest request = new ExamConsentRequest(true, false); // lacks fullscreen!
        ExamConsentResponse response = monitoringService.recordStudentConsent(student, 1, request);

        assertNotNull(response);
        assertTrue(response.getBlocked());
        assertTrue(response.getMessage().contains("Blocked"));
    }
}
