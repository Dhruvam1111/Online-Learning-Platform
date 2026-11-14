package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.event.ExamScheduledEvent;
import com.olp.exception.ExamIneligibleException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
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
 * Unit tests for ExamService (FR-I25, Step 38).
 * Tests server-side eligibility verification (DR-ED07), server-authoritative timer enforcement (DR-ED06),
 * notification event publishing, question linking, and attempt management.
 */
@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamQuestionRepository examQuestionRepository;

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CoursePrerequisiteRepository coursePrerequisiteRepository;

    @Mock
    private CourseProgressService courseProgressService;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ExamService examService;

    private User instructor;
    private User student;
    private User unauthorizedInstructor;
    private Course course;
    private Exam standardExam;

    private final LocalDateTime baseNow = LocalDateTime.of(2026, 11, 20, 10, 0, 0);

    @BeforeEach
    void setUp() {
        examService = new ExamService(
                examRepository,
                examQuestionRepository,
                examAttemptRepository,
                courseRepository,
                courseInstructorRepository,
                enrollmentRepository,
                coursePrerequisiteRepository,
                courseProgressService,
                questionRepository,
                userRepository,
                eventPublisher
        );

        Clock fixedClock = Clock.fixed(baseNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        examService.setClock(fixedClock);

        instructor = new User("Prof. Turing", "turing@olp.com", "hash", "instructor", "active");
        instructor.setId(10);

        unauthorizedInstructor = new User("Dr. Unknown", "unknown@olp.com", "hash", "instructor", "active");
        unauthorizedInstructor.setId(99);

        student = new User("Student Bob", "bob@olp.com", "hash", "student", "active");
        student.setId(20);

        course = new Course();
        course.setId(100);
        course.setTitle("Distributed Systems");
        course.setInstructor(instructor);

        standardExam = new Exam(
                course,
                instructor,
                "Midterm Examination",
                "Covers Modules 1-4",
                "No calculators allowed",
                baseNow.minusHours(1), // started at 09:00
                baseNow.plusHours(5),  // ends at 15:00
                60,                    // 60 minutes duration
                "UTC",
                new BigDecimal("100.00"),
                new BigDecimal("40.00"),
                1,                     // 1 attempt max
                new BigDecimal("75.00"), // 75% progress required
                true,                  // require prereqs
                true,                  // require enrollment
                "SCHEDULED"
        );
        standardExam.setId(1);
    }

    @Test
    void testCreateExam_Success_PublishesEvent() {
        ExamCreateRequest request = new ExamCreateRequest();
        request.setCourseId(100);
        request.setTitle("Final Exam");
        request.setDescription("Comprehensive final");
        request.setStartWindow(baseNow.plusDays(1));
        request.setEndWindow(baseNow.plusDays(1).plusHours(4));
        request.setDurationMinutes(90);
        request.setMaxMarks(new BigDecimal("100.00"));
        request.setQuestionIds(List.of(201, 202));

        Question q1 = new Question();
        q1.setId(201);
        q1.setQuestionText("What is Paxos?");
        q1.setDefaultMarks(new BigDecimal("10.00"));

        Question q2 = new Question();
        q2.setId(202);
        q2.setQuestionText("Explain Raft consensus.");
        q2.setDefaultMarks(new BigDecimal("15.00"));

        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(questionRepository.findById(201)).thenReturn(Optional.of(q1));
        when(questionRepository.findById(202)).thenReturn(Optional.of(q2));
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> {
            Exam e = invocation.getArgument(0);
            e.setId(5);
            return e;
        });

        ExamResponse response = examService.createExam(instructor, request);

        assertNotNull(response);
        assertEquals(5, response.getId());
        assertEquals("Final Exam", response.getTitle());
        assertEquals(90, response.getDurationMinutes());

        verify(examQuestionRepository, times(2)).save(any(ExamQuestion.class));

        ArgumentCaptor<ExamScheduledEvent> eventCaptor = ArgumentCaptor.forClass(ExamScheduledEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        ExamScheduledEvent event = eventCaptor.getValue();
        assertEquals(5, event.getExamId());
        assertEquals(100, event.getCourseId());
        assertEquals("Final Exam", event.getTitle());
        assertEquals(90, event.getDurationMinutes());
    }

    @Test
    void testCreateExam_UnauthorizedInstructor_ThrowsAccessDenied() {
        ExamCreateRequest request = new ExamCreateRequest();
        request.setCourseId(100);
        request.setTitle("Unauthorized Exam");
        request.setStartWindow(baseNow.plusDays(1));
        request.setEndWindow(baseNow.plusDays(1).plusHours(2));

        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 99)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> examService.createExam(unauthorizedInstructor, request));
        verify(examRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testCreateExam_InvalidWindow_ThrowsIllegalArgument() {
        ExamCreateRequest request = new ExamCreateRequest();
        request.setCourseId(100);
        request.setTitle("Invalid Time Exam");
        request.setStartWindow(baseNow.plusHours(2));
        request.setEndWindow(baseNow.plusHours(1)); // end before start!

        assertThrows(IllegalArgumentException.class, () -> examService.createExam(instructor, request));
    }

    @Test
    void testEligibility_Eligible() {
        when(examRepository.findById(1)).thenReturn(Optional.of(standardExam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);
        when(coursePrerequisiteRepository.findByCourseIdOrderByIdAsc(100)).thenReturn(Collections.emptyList());
        when(courseProgressService.getCourseProgress(20, 100))
                .thenReturn(new CourseProgressResponse(100, 8, 10, 80.0, List.of()));
        when(examAttemptRepository.countByExamIdAndStudentId(1, 20)).thenReturn(0L);

        ExamEligibilityResponse eligibility = examService.checkEligibility(student, 1);

        assertTrue(eligibility.getIsEligible());
        assertNull(eligibility.getDenialReason());
        assertEquals(1, eligibility.getExamId());
        assertEquals(20, eligibility.getStudentId());
        assertEquals(0, eligibility.getAttemptsUsed());
    }

    @Test
    void testEligibility_Denial_NotEnrolled() {
        when(examRepository.findById(1)).thenReturn(Optional.of(standardExam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(false);

        ExamEligibilityResponse eligibility = examService.checkEligibility(student, 1);

        assertFalse(eligibility.getIsEligible());
        assertEquals("NOT_ENROLLED", eligibility.getDenialReason());
        assertTrue(eligibility.getDenialMessage().contains("not enrolled"));
    }

    @Test
    void testEligibility_Denial_BelowMinProgress() {
        when(examRepository.findById(1)).thenReturn(Optional.of(standardExam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);
        when(coursePrerequisiteRepository.findByCourseIdOrderByIdAsc(100)).thenReturn(Collections.emptyList());
        when(courseProgressService.getCourseProgress(20, 100))
                .thenReturn(new CourseProgressResponse(100, 5, 10, 50.0, List.of()));

        ExamEligibilityResponse eligibility = examService.checkEligibility(student, 1);

        assertFalse(eligibility.getIsEligible());
        assertEquals("BELOW_MIN_PROGRESS", eligibility.getDenialReason());
        assertTrue(eligibility.getDenialMessage().contains("below required minimum"));
    }

    @Test
    void testEligibility_Denial_PrerequisiteUnmet() {
        Course reqCourse = new Course();
        reqCourse.setId(50);
        reqCourse.setTitle("Introduction to Operating Systems");

        CoursePrerequisite prereq = new CoursePrerequisite(course, "COURSE", reqCourse, "OS Prerequisite", "Must complete OS");

        when(examRepository.findById(1)).thenReturn(Optional.of(standardExam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);
        when(coursePrerequisiteRepository.findByCourseIdOrderByIdAsc(100)).thenReturn(List.of(prereq));
        when(courseProgressService.getCourseProgress(20, 50))
                .thenReturn(new CourseProgressResponse(50, 4, 10, 40.0, List.of())); // only 40%

        ExamEligibilityResponse eligibility = examService.checkEligibility(student, 1);

        assertFalse(eligibility.getIsEligible());
        assertEquals("PREREQUISITE_UNMET", eligibility.getDenialReason());
        assertTrue(eligibility.getDenialMessage().contains("Prerequisite course incomplete"));
    }

    @Test
    void testEligibility_Denial_BeforeStartWindow() {
        standardExam.setStartWindow(baseNow.plusHours(2));
        standardExam.setEndWindow(baseNow.plusHours(6));

        when(examRepository.findById(1)).thenReturn(Optional.of(standardExam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);
        when(coursePrerequisiteRepository.findByCourseIdOrderByIdAsc(100)).thenReturn(Collections.emptyList());
        when(courseProgressService.getCourseProgress(20, 100))
                .thenReturn(new CourseProgressResponse(100, 10, 10, 100.0, List.of()));

        ExamEligibilityResponse eligibility = examService.checkEligibility(student, 1);

        assertFalse(eligibility.getIsEligible());
        assertEquals("BEFORE_START_WINDOW", eligibility.getDenialReason());
        assertTrue(eligibility.getDenialMessage().contains("has not opened yet"));
    }

    @Test
    void testEligibility_Denial_AfterEndWindow() {
        standardExam.setStartWindow(baseNow.minusHours(4));
        standardExam.setEndWindow(baseNow.minusHours(1)); // closed at 09:00

        when(examRepository.findById(1)).thenReturn(Optional.of(standardExam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);
        when(coursePrerequisiteRepository.findByCourseIdOrderByIdAsc(100)).thenReturn(Collections.emptyList());
        when(courseProgressService.getCourseProgress(20, 100))
                .thenReturn(new CourseProgressResponse(100, 10, 10, 100.0, List.of()));

        ExamEligibilityResponse eligibility = examService.checkEligibility(student, 1);

        assertFalse(eligibility.getIsEligible());
        assertEquals("AFTER_END_WINDOW", eligibility.getDenialReason());
        assertTrue(eligibility.getDenialMessage().contains("closed at"));
    }

    @Test
    void testEligibility_Denial_MaxAttemptsExhausted() {
        when(examRepository.findById(1)).thenReturn(Optional.of(standardExam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);
        when(coursePrerequisiteRepository.findByCourseIdOrderByIdAsc(100)).thenReturn(Collections.emptyList());
        when(courseProgressService.getCourseProgress(20, 100))
                .thenReturn(new CourseProgressResponse(100, 10, 10, 100.0, List.of()));
        when(examAttemptRepository.countByExamIdAndStudentId(1, 20)).thenReturn(1L);

        ExamEligibilityResponse eligibility = examService.checkEligibility(student, 1);

        assertFalse(eligibility.getIsEligible());
        assertEquals("MAX_ATTEMPTS_EXHAUSTED", eligibility.getDenialReason());
        assertTrue(eligibility.getDenialMessage().contains("exhausted"));
    }

    @Test
    void testStartExam_Success_EnforcesServerAuthoritativeTimer() {
        when(examRepository.findById(1)).thenReturn(Optional.of(standardExam));
        when(examAttemptRepository.findActiveAttempts(1, 20)).thenReturn(Collections.emptyList());
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);
        when(coursePrerequisiteRepository.findByCourseIdOrderByIdAsc(100)).thenReturn(Collections.emptyList());
        when(courseProgressService.getCourseProgress(20, 100))
                .thenReturn(new CourseProgressResponse(100, 10, 10, 100.0, List.of()));
        when(examAttemptRepository.countByExamIdAndStudentId(1, 20)).thenReturn(0L);

        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(invocation -> {
            ExamAttempt a = invocation.getArgument(0);
            a.setId(101);
            return a;
        });

        ExamStartResponse response = examService.startExam(student, 1);

        assertNotNull(response);
        assertEquals(101, response.getAttemptId());
        assertEquals(1, response.getExamId());
        assertEquals(baseNow, response.getStartedAt());
        // Duration is 60 minutes, baseNow is 10:00, endWindow is 15:00 -> expiresAt is 11:00
        assertEquals(baseNow.plusMinutes(60), response.getExpiresAt());
        assertEquals(3600L, response.getRemainingSeconds());
        assertEquals("IN_PROGRESS", response.getStatus());
    }

    @Test
    void testStartExam_ClampsExpiresAtToEndWindow() {
        // Exam duration is 60 minutes, but window ends in 30 minutes (at 10:30)
        standardExam.setEndWindow(baseNow.plusMinutes(30));

        when(examRepository.findById(1)).thenReturn(Optional.of(standardExam));
        when(examAttemptRepository.findActiveAttempts(1, 20)).thenReturn(Collections.emptyList());
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);
        when(coursePrerequisiteRepository.findByCourseIdOrderByIdAsc(100)).thenReturn(Collections.emptyList());
        when(courseProgressService.getCourseProgress(20, 100))
                .thenReturn(new CourseProgressResponse(100, 10, 10, 100.0, List.of()));
        when(examAttemptRepository.countByExamIdAndStudentId(1, 20)).thenReturn(0L);

        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(invocation -> {
            ExamAttempt a = invocation.getArgument(0);
            a.setId(102);
            return a;
        });

        ExamStartResponse response = examService.startExam(student, 1);

        assertNotNull(response);
        // Clamped to endWindow (10:30) instead of 11:00!
        assertEquals(baseNow.plusMinutes(30), response.getExpiresAt());
        assertEquals(1800L, response.getRemainingSeconds());
    }

    @Test
    void testStartExam_Ineligible_ThrowsExamIneligibleException() {
        when(examRepository.findById(1)).thenReturn(Optional.of(standardExam));
        when(examAttemptRepository.findActiveAttempts(1, 20)).thenReturn(Collections.emptyList());
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(false);

        ExamIneligibleException ex = assertThrows(ExamIneligibleException.class, () -> examService.startExam(student, 1));
        assertEquals("NOT_ENROLLED", ex.getReason());
        verify(examAttemptRepository, never()).save(any());
    }

    @Test
    void testStartExam_ResumesActiveAttempt() {
        ExamAttempt active = new ExamAttempt(standardExam, student, 1, baseNow.minusMinutes(15), baseNow.plusMinutes(45));
        active.setId(999);

        when(examRepository.findById(1)).thenReturn(Optional.of(standardExam));
        when(examAttemptRepository.findActiveAttempts(1, 20)).thenReturn(List.of(active));

        ExamStartResponse response = examService.startExam(student, 1);

        assertNotNull(response);
        assertEquals(999, response.getAttemptId());
        assertEquals(1, response.getAttemptNumber());
        assertEquals(active.getExpiresAt(), response.getExpiresAt());
        assertEquals(45 * 60L, response.getRemainingSeconds());
        verify(examAttemptRepository, never()).save(any());
    }

    @Test
    void testUpdateExam_Success() {
        when(examRepository.findById(1)).thenReturn(Optional.of(standardExam));
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExamUpdateRequest request = new ExamUpdateRequest();
        request.setTitle("Updated Title");
        request.setDurationMinutes(75);
        request.setMaxMarks(new BigDecimal("120.00"));

        ExamResponse response = examService.updateExam(instructor, 1, request);

        assertNotNull(response);
        assertEquals("Updated Title", response.getTitle());
        assertEquals(75, response.getDurationMinutes());
        assertEquals(new BigDecimal("120.00"), response.getMaxMarks());
    }

    @Test
    void testLinkQuestionsToExam_Success() {
        when(examRepository.findById(1)).thenReturn(Optional.of(standardExam));
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));

        Question q = new Question();
        q.setId(301);
        q.setQuestionText("Explain Two-Phase Commit.");
        q.setDefaultMarks(new BigDecimal("5.00"));

        when(questionRepository.findById(301)).thenReturn(Optional.of(q));
        when(examQuestionRepository.save(any(ExamQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ExamQuestionItemDto> linked = examService.linkQuestionsToExam(instructor, 1, List.of(301));

        assertEquals(1, linked.size());
        assertEquals(301, linked.get(0).getQuestionId());
        assertEquals("Explain Two-Phase Commit.", linked.get(0).getQuestionText());
        assertEquals(1, linked.get(0).getOrderIndex());
        verify(examQuestionRepository).deleteByExamId(1);
    }
}

