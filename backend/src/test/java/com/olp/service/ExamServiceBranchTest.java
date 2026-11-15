package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.ExamIneligibleException;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests targeting branch coverage for ExamService (FR-I25, FR-I27).
 */
@ExtendWith(MockitoExtension.class)
class ExamServiceBranchTest {

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

    @Mock
    private ExamBlueprintRepository examBlueprintRepository;

    @Mock
    private ExamRandomizationService examRandomizationService;

    private ExamService examService;

    private User instructor;
    private User student;
    private User admin;
    private Course course;
    private Exam exam;

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
                eventPublisher,
                examBlueprintRepository,
                examRandomizationService
        );

        Clock fixedClock = Clock.fixed(baseNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        examService.setClock(fixedClock);

        instructor = new User("Instructor", "instructor@olp.com", "hash", "instructor", "active");
        instructor.setId(1);

        student = new User("Student", "student@olp.com", "hash", "student", "active");
        student.setId(2);

        admin = new User("Admin", "admin@olp.com", "hash", "admin", "active");
        admin.setId(3);

        course = new Course();
        course.setId(100);
        course.setInstructor(instructor);

        exam = new Exam();
        exam.setId(10);
        exam.setCourse(course);
        exam.setCreatedBy(instructor);
        exam.setTitle("Exam A");
        exam.setStartWindow(baseNow.minusHours(1));
        exam.setEndWindow(baseNow.plusHours(2));
        exam.setDurationMinutes(60);
        exam.setMaxMarks(new BigDecimal("100.00"));
        exam.setMaxAttempts(2);
        exam.setRequireEnrollment(true);
        exam.setRequirePrerequisites(false);
        exam.setMinProgressPercentage(BigDecimal.ZERO);

        lenient().when(courseRepository.findById(100)).thenReturn(Optional.of(course));
    }

    @Test
    @DisplayName("checkEligibility returns NOT_ENROLLED when enrollment required and student not enrolled")
    void testCheckEligibilityNotEnrolled() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(2, 100)).thenReturn(false);

        ExamEligibilityResponse res = examService.checkEligibility(student, 10);

        assertFalse(res.getIsEligible());
        assertEquals("NOT_ENROLLED", res.getDenialReason());
    }

    @Test
    @DisplayName("checkEligibility returns PREREQUISITE_UNMET when prerequisite course progress is below 100%")
    void testCheckEligibilityPrerequisiteUnmet() {
        exam.setRequirePrerequisites(true);
        Course requiredCourse = new Course();
        requiredCourse.setId(101);

        CoursePrerequisite prereq = new CoursePrerequisite();
        prereq.setPrerequisiteType("COURSE");
        prereq.setRequiredCourse(requiredCourse);
        prereq.setTitle("Intro Course");

        CourseProgressResponse reqProgress = new CourseProgressResponse();
        reqProgress.setPercentage(65.0);

        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(2, 100)).thenReturn(true);
        when(coursePrerequisiteRepository.findByCourseIdOrderByIdAsc(100)).thenReturn(List.of(prereq));
        when(courseProgressService.getCourseProgress(2, 101)).thenReturn(reqProgress);

        ExamEligibilityResponse res = examService.checkEligibility(student, 10);

        assertFalse(res.getIsEligible());
        assertEquals("PREREQUISITE_UNMET", res.getDenialReason());
        assertTrue(res.getDenialMessage().contains("Intro Course"));
    }

    @Test
    @DisplayName("checkEligibility returns BELOW_MIN_PROGRESS when student progress is insufficient")
    void testCheckEligibilityBelowMinProgress() {
        exam.setMinProgressPercentage(new BigDecimal("75.00"));

        CourseProgressResponse prog = new CourseProgressResponse();
        prog.setPercentage(50.0);

        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(2, 100)).thenReturn(true);
        when(courseProgressService.getCourseProgress(2, 100)).thenReturn(prog);

        ExamEligibilityResponse res = examService.checkEligibility(student, 10);

        assertFalse(res.getIsEligible());
        assertEquals("BELOW_MIN_PROGRESS", res.getDenialReason());
    }

    @Test
    @DisplayName("checkEligibility returns BEFORE_START_WINDOW when exam has not opened yet")
    void testCheckEligibilityBeforeStartWindow() {
        exam.setStartWindow(baseNow.plusHours(2));
        exam.setEndWindow(baseNow.plusHours(4));

        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(2, 100)).thenReturn(true);

        ExamEligibilityResponse res = examService.checkEligibility(student, 10);

        assertFalse(res.getIsEligible());
        assertEquals("BEFORE_START_WINDOW", res.getDenialReason());
    }

    @Test
    @DisplayName("checkEligibility returns AFTER_END_WINDOW when exam window has closed")
    void testCheckEligibilityAfterEndWindow() {
        exam.setStartWindow(baseNow.minusHours(4));
        exam.setEndWindow(baseNow.minusHours(1));

        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(2, 100)).thenReturn(true);

        ExamEligibilityResponse res = examService.checkEligibility(student, 10);

        assertFalse(res.getIsEligible());
        assertEquals("AFTER_END_WINDOW", res.getDenialReason());
    }

    @Test
    @DisplayName("checkEligibility returns MAX_ATTEMPTS_EXHAUSTED when attempt limit reached")
    void testCheckEligibilityMaxAttemptsExhausted() {
        exam.setMaxAttempts(1);

        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.existsByStudentIdAndCourseId(2, 100)).thenReturn(true);
        when(examAttemptRepository.countByExamIdAndStudentId(10, 2)).thenReturn(1L);

        ExamEligibilityResponse res = examService.checkEligibility(student, 10);

        assertFalse(res.getIsEligible());
        assertEquals("MAX_ATTEMPTS_EXHAUSTED", res.getDenialReason());
    }

    @Test
    @DisplayName("startExam resumes ongoing attempt if not expired")
    void testStartExamResumesActiveAttempt() {
        ExamAttempt active = new ExamAttempt(exam, student, 1, baseNow.minusMinutes(10), baseNow.plusMinutes(50));
        active.setId(99);

        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.findActiveAttempts(10, 2)).thenReturn(List.of(active));
        when(examQuestionRepository.findByExamIdOrderByOrderIndexAsc(10)).thenReturn(Collections.emptyList());

        ExamStartResponse res = examService.startExam(student, 10);

        assertNotNull(res);
        assertEquals(99, res.getAttemptId());
        assertEquals("IN_PROGRESS", res.getStatus());
        verify(examAttemptRepository, never()).save(any(ExamAttempt.class));
    }

    @Test
    @DisplayName("startExam marks active attempt as EXPIRED when timer has expired")
    void testStartExamMarksActiveAttemptExpired() {
        ExamAttempt expiredAttempt = new ExamAttempt(exam, student, 1, baseNow.minusHours(2), baseNow.minusMinutes(10));
        expiredAttempt.setId(98);

        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.findActiveAttempts(10, 2)).thenReturn(List.of(expiredAttempt));
        when(enrollmentRepository.existsByStudentIdAndCourseId(2, 100)).thenReturn(true);
        when(examAttemptRepository.countByExamIdAndStudentId(10, 2)).thenReturn(1L);
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(i -> {
            ExamAttempt a = i.getArgument(0);
            if (a.getId() == null) a.setId(100);
            return a;
        });

        ExamStartResponse res = examService.startExam(student, 10);

        assertNotNull(res);
        assertEquals(100, res.getAttemptId());
        assertEquals(2, res.getAttemptNumber());
        assertEquals("EXPIRED", expiredAttempt.getStatus());
    }

    @Test
    @DisplayName("startExam throws ExamIneligibleException when eligibility verification fails")
    void testStartExamThrowsWhenIneligible() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.findActiveAttempts(10, 2)).thenReturn(Collections.emptyList());
        when(enrollmentRepository.existsByStudentIdAndCourseId(2, 100)).thenReturn(false);

        assertThrows(ExamIneligibleException.class, () ->
                examService.startExam(student, 10)
        );
    }

    @Test
    @DisplayName("startExam generates randomized set when isRandomized is true")
    void testStartExamWithRandomization() {
        exam.setIsRandomized(true);
        exam.setRandomSeed(12345L);
        exam.setTotalSets(3);

        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.findActiveAttempts(10, 2)).thenReturn(Collections.emptyList());
        when(enrollmentRepository.existsByStudentIdAndCourseId(2, 100)).thenReturn(true);
        when(examAttemptRepository.countByExamIdAndStudentId(10, 2)).thenReturn(0L);
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(i -> {
            ExamAttempt a = i.getArgument(0);
            a.setId(101);
            return a;
        });

        ExamQuestion eq1 = new ExamQuestion();
        eq1.setId(1);
        Question q = new Question();
        q.setId(501);
        q.setOptionsJson("[\"A\",\"B\"]");
        eq1.setQuestion(q);

        when(examBlueprintRepository.findByExamIdOrderByOrderIndexAsc(10)).thenReturn(Collections.emptyList());
        when(examQuestionRepository.findByExamIdOrderByOrderIndexAsc(10)).thenReturn(List.of(eq1));
        when(examRandomizationService.shuffleOptions(any(), anyLong()))
                .thenReturn(new ExamRandomizationService.ShuffledOptionsResult("[\"B\",\"A\"]", List.of(1, 0)));

        ExamStartResponse res = examService.startExam(student, 10);

        assertNotNull(res);
        assertNotNull(res.getSetLabel());
        assertEquals(1, res.getQuestions().size());
    }

    @Test
    @DisplayName("linkQuestionsToExam handles empty list and validates questions exist")
    void testLinkQuestionsToExam() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));

        // Empty list
        List<ExamQuestionItemDto> res1 = examService.linkQuestionsToExam(instructor, 10, Collections.emptyList());
        assertTrue(res1.isEmpty());

        // Question not found
        when(questionRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                examService.linkQuestionsToExam(instructor, 10, List.of(999))
        );
    }

    @Test
    @DisplayName("updateExam validates boundary conditions on timing and marks")
    void testUpdateExamValidationBoundaries() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));

        // End before start
        ExamUpdateRequest r1 = new ExamUpdateRequest();
        r1.setStartWindow(baseNow.plusHours(3));
        r1.setEndWindow(baseNow.plusHours(1));
        assertThrows(IllegalArgumentException.class, () -> examService.updateExam(instructor, 10, r1));

        // Duration <= 0
        ExamUpdateRequest r2 = new ExamUpdateRequest();
        r2.setDurationMinutes(0);
        assertThrows(IllegalArgumentException.class, () -> examService.updateExam(instructor, 10, r2));

        // Max marks < 0
        ExamUpdateRequest r3 = new ExamUpdateRequest();
        r3.setMaxMarks(new BigDecimal("-1.00"));
        assertThrows(IllegalArgumentException.class, () -> examService.updateExam(instructor, 10, r3));

        // Passing marks < 0
        ExamUpdateRequest r4 = new ExamUpdateRequest();
        r4.setPassingMarks(new BigDecimal("-5.00"));
        assertThrows(IllegalArgumentException.class, () -> examService.updateExam(instructor, 10, r4));

        // Passing marks > max marks
        ExamUpdateRequest r5 = new ExamUpdateRequest();
        r5.setPassingMarks(new BigDecimal("150.00"));
        assertThrows(IllegalArgumentException.class, () -> examService.updateExam(instructor, 10, r5));

        // Max attempts < 1
        ExamUpdateRequest r6 = new ExamUpdateRequest();
        r6.setMaxAttempts(0);
        assertThrows(IllegalArgumentException.class, () -> examService.updateExam(instructor, 10, r6));

        // Progress percentage invalid
        ExamUpdateRequest r7 = new ExamUpdateRequest();
        r7.setMinProgressPercentage(new BigDecimal("105.00"));
        assertThrows(IllegalArgumentException.class, () -> examService.updateExam(instructor, 10, r7));
    }

    @Test
    @DisplayName("getExam verifies enrollment for students and course ownership for instructors")
    void testGetExamAccessControl() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));

        // Student not enrolled
        when(enrollmentRepository.existsByStudentIdAndCourseId(2, 100)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> examService.getExam(student, 10));

        // Admin can access without checks
        ExamResponse adminRes = examService.getExam(admin, 10);
        assertNotNull(adminRes);

        // Instructor associated can access
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 1)).thenReturn(false);
        ExamResponse instRes = examService.getExam(instructor, 10);
        assertNotNull(instRes);
    }

    @Test
    @DisplayName("resolveUser handles authentication variants properly")
    void testResolveUser() {
        // Null auth
        assertThrows(InvalidCredentialsException.class, () -> examService.resolveUser(null));

        // User directly in principal
        Authentication auth1 = new UsernamePasswordAuthenticationToken(student, "credentials");
        assertEquals(student, examService.resolveUser(auth1));

        // Email in principal name
        Authentication auth2 = mock(Authentication.class);
        when(auth2.getPrincipal()).thenReturn("bob@olp.com");
        when(userRepository.findByEmail("bob@olp.com")).thenReturn(Optional.of(student));
        assertEquals(student, examService.resolveUser(auth2));

        // Unknown email
        Authentication auth3 = mock(Authentication.class);
        when(auth3.getPrincipal()).thenReturn("unknown@olp.com");
        when(userRepository.findByEmail("unknown@olp.com")).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class, () -> examService.resolveUser(auth3));
    }
}
