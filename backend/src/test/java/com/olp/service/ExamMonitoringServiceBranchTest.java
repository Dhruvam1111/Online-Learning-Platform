package com.olp.service;

import com.olp.dto.ExamConsentRequest;
import com.olp.dto.ExamConsentResponse;
import com.olp.dto.ExamMonitoringConfigRequest;
import com.olp.dto.ExamMonitoringConfigResponse;
import com.olp.entity.Course;
import com.olp.entity.Exam;
import com.olp.entity.ExamAttempt;
import com.olp.entity.ExamMonitoringConfig;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.ExamAttemptRepository;
import com.olp.repository.ExamMonitoringConfigRepository;
import com.olp.repository.ExamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests targeting branch coverage for ExamMonitoringService (FR-I26, Step 40).
 */
@ExtendWith(MockitoExtension.class)
class ExamMonitoringServiceBranchTest {

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
    private Course course;
    private Exam exam;

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

        instructor = new User("Instructor", "instructor@olp.com", "hash", "instructor", "active");
        instructor.setId(1);

        student = new User("Student", "student@olp.com", "hash", "student", "active");
        student.setId(2);

        course = new Course();
        course.setId(100);
        course.setInstructor(instructor);

        exam = new Exam();
        exam.setId(10);
        exam.setTitle("Proctored Exam");
        exam.setCourse(course);
        exam.setRequireEnrollment(true);

        lenient().when(courseRepository.findById(100)).thenReturn(Optional.of(course));
    }

    @Test
    @DisplayName("saveMonitoringConfig validates thresholds, policies, and updates existing records")
    void testSaveMonitoringConfigValidationAndExistingUpdate() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));

        // null request
        assertThrows(IllegalArgumentException.class, () ->
                monitoringService.saveMonitoringConfig(instructor, 10, null)
        );

        // invalid tab switch limit
        ExamMonitoringConfigRequest r1 = new ExamMonitoringConfigRequest();
        r1.setTabSwitchLimit(55);
        assertThrows(IllegalArgumentException.class, () ->
                monitoringService.saveMonitoringConfig(instructor, 10, r1)
        );

        r1.setTabSwitchLimit(-1);
        assertThrows(IllegalArgumentException.class, () ->
                monitoringService.saveMonitoringConfig(instructor, 10, r1)
        );

        // invalid fullscreen exit limit
        r1.setTabSwitchLimit(3);
        r1.setFullscreenExitLimit(55);
        assertThrows(IllegalArgumentException.class, () ->
                monitoringService.saveMonitoringConfig(instructor, 10, r1)
        );

        // invalid unsupported browser policy
        r1.setFullscreenExitLimit(2);
        r1.setUnsupportedBrowserPolicy("INVALID_POLICY");
        assertThrows(IllegalArgumentException.class, () ->
                monitoringService.saveMonitoringConfig(instructor, 10, r1)
        );

        // Existing config update path
        ExamMonitoringConfig existing = new ExamMonitoringConfig(exam, true, 3, true, 2, false, false, false, true, "WARN", "Custom");
        existing.setId(50);
        when(monitoringConfigRepository.findByExamId(10)).thenReturn(Optional.of(existing));
        when(monitoringConfigRepository.save(any(ExamMonitoringConfig.class))).thenAnswer(i -> i.getArgument(0));

        ExamMonitoringConfigRequest updateReq = new ExamMonitoringConfigRequest();
        updateReq.setTabSwitchDetection(false);
        updateReq.setTabSwitchLimit(5);
        updateReq.setRequireFullscreen(false);
        updateReq.setFullscreenExitLimit(4);
        updateReq.setRequireScreenShare(true);
        updateReq.setRequireWebcam(true);
        updateReq.setRequireMicrophone(true);
        updateReq.setRequireStudentConsent(false);
        updateReq.setUnsupportedBrowserPolicy("BLOCK");
        updateReq.setConsentText("Updated text");

        ExamMonitoringConfigResponse res = monitoringService.saveMonitoringConfig(instructor, 10, updateReq);

        assertNotNull(res);
        assertFalse(res.getTabSwitchDetection());
        assertEquals(5, res.getTabSwitchLimit());
        assertTrue(res.getRequireScreenShare());
        assertTrue(res.getRequireWebcam());
        assertTrue(res.getRequireMicrophone());
        assertEquals("BLOCK", res.getUnsupportedBrowserPolicy());
        assertEquals("Updated text", res.getConsentText());
    }

    @Test
    @DisplayName("getMonitoringConfig verifies enrollment and produces fallback warnings")
    void testGetMonitoringConfigBranches() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));

        // Student not enrolled
        when(enrollmentRepository.existsByStudentIdAndCourseId(2, 100)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () ->
                monitoringService.getMonitoringConfig(student, 10, true)
        );

        // Student enrolled, browser lacking fullscreen with policy BLOCK
        when(enrollmentRepository.existsByStudentIdAndCourseId(2, 100)).thenReturn(true);
        ExamMonitoringConfig blockConfig = new ExamMonitoringConfig(exam, true, 3, true, 2, false, false, false, true, "BLOCK", null);
        when(monitoringConfigRepository.findByExamId(10)).thenReturn(Optional.of(blockConfig));

        ExamMonitoringConfigResponse blockRes = monitoringService.getMonitoringConfig(student, 10, false);
        assertNotNull(blockRes);
        assertTrue(blockRes.getFullscreenSupportWarning().startsWith("BLOCKED"));

        // Browser lacking fullscreen with policy WARN
        ExamMonitoringConfig warnConfig = new ExamMonitoringConfig(exam, true, 3, true, 2, false, false, false, true, "WARN", null);
        when(monitoringConfigRepository.findByExamId(10)).thenReturn(Optional.of(warnConfig));

        ExamMonitoringConfigResponse warnRes = monitoringService.getMonitoringConfig(student, 10, false);
        assertNotNull(warnRes);
        assertTrue(warnRes.getFullscreenSupportWarning().startsWith("WARNING"));
    }

    @Test
    @DisplayName("recordStudentConsent branches for unauthenticated, declined, blocked, and active attempt updates")
    void testRecordStudentConsentBranches() {
        // Unauthenticated
        assertThrows(InvalidCredentialsException.class, () ->
                monitoringService.recordStudentConsent(null, 10, new ExamConsentRequest())
        );

        when(examRepository.findById(10)).thenReturn(Optional.of(exam));

        // Student not enrolled
        when(enrollmentRepository.existsByStudentIdAndCourseId(2, 100)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () ->
                monitoringService.recordStudentConsent(student, 10, new ExamConsentRequest())
        );

        // Student enrolled, consent declined
        when(enrollmentRepository.existsByStudentIdAndCourseId(2, 100)).thenReturn(true);
        ExamConsentRequest declinedReq = new ExamConsentRequest();
        declinedReq.setConsentGiven(false);

        ExamConsentResponse declinedRes = monitoringService.recordStudentConsent(student, 10, declinedReq);
        assertFalse(declinedRes.getConsentGiven());
        assertTrue(declinedRes.getMessage().contains("declined"));

        // Consent granted, but unsupported browser and policy BLOCK
        ExamMonitoringConfig blockConfig = new ExamMonitoringConfig(exam, true, 3, true, 2, false, false, false, true, "BLOCK", null);
        when(monitoringConfigRepository.findByExamId(10)).thenReturn(Optional.of(blockConfig));

        ExamConsentRequest blockReq = new ExamConsentRequest();
        blockReq.setConsentGiven(true);
        blockReq.setBrowserSupportsFullscreen(false);

        ExamConsentResponse blockRes = monitoringService.recordStudentConsent(student, 10, blockReq);
        assertTrue(blockRes.getBlocked());
        assertTrue(blockRes.getMessage().contains("Blocked"));

        // Consent granted, unsupported browser and policy WARN, updates active attempt
        ExamMonitoringConfig warnConfig = new ExamMonitoringConfig(exam, true, 3, true, 2, false, false, false, true, "WARN", null);
        when(monitoringConfigRepository.findByExamId(10)).thenReturn(Optional.of(warnConfig));

        ExamAttempt active = new ExamAttempt(exam, student, 1, baseNow, baseNow.plusHours(1));
        when(examAttemptRepository.findActiveAttempts(10, 2)).thenReturn(List.of(active));

        ExamConsentRequest warnReq = new ExamConsentRequest();
        warnReq.setConsentGiven(true);
        warnReq.setBrowserSupportsFullscreen(false);

        ExamConsentResponse warnRes = monitoringService.recordStudentConsent(student, 10, warnReq);
        assertTrue(warnRes.getConsentGiven());
        assertFalse(warnRes.getBlocked());
        assertNotNull(warnRes.getWarning());
        assertTrue(active.getConsentGiven());
        verify(examAttemptRepository, times(1)).save(active);
    }
}
