package com.olp.service;

import com.olp.dto.ExamCreateRequest;
import com.olp.dto.ExamResponse;
import com.olp.dto.ExamUpdateRequest;
import com.olp.dto.QuestionCorrectionPolicyRequest;
import com.olp.dto.QuestionCorrectionPolicyResponse;
import com.olp.entity.Course;
import com.olp.entity.Exam;
import com.olp.entity.QuestionCorrectionPolicy;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Question Correction Policy (FR-I66, Step 41).
 * Verifies default policy, custom policy configuration, lock enforcement once attempts start (DR-ED05),
 * conflict 409 status on post-attempt edits, and read-only exposure for FR-I37.
 */
@ExtendWith(MockitoExtension.class)
class ExamCorrectionPolicyTest {

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
    private User admin;
    private User student;
    private User otherInstructor;
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
                eventPublisher
        );

        Clock fixedClock = Clock.fixed(baseNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        examService.setClock(fixedClock);

        instructor = new User("Instructor Alice", "alice@olp.com", "hash", "instructor", "active");
        instructor.setId(1);

        admin = new User("Admin User", "admin@olp.com", "hash", "admin", "active");
        admin.setId(2);

        student = new User("Student Bob", "bob@olp.com", "hash", "student", "active");
        student.setId(3);

        otherInstructor = new User("Other Prof", "other@olp.com", "hash", "instructor", "active");
        otherInstructor.setId(4);

        course = new Course();
        course.setId(100);
        course.setTitle("Distributed Systems");
        course.setInstructor(instructor);

        exam = new Exam();
        exam.setId(10);
        exam.setCourse(course);
        exam.setCreatedBy(instructor);
        exam.setTitle("Final Exam");
        exam.setStartWindow(baseNow.plusHours(1));
        exam.setEndWindow(baseNow.plusHours(3));
        exam.setDurationMinutes(60);
        exam.setMaxMarks(new BigDecimal("100.00"));
        exam.setQuestionCorrectionPolicy(QuestionCorrectionPolicy.AWARD_FULL_MARKS);

        lenient().when(courseRepository.findById(100)).thenReturn(Optional.of(course));
    }

    @Test
    @DisplayName("Exam creation defaults to AWARD_FULL_MARKS policy when unspecified")
    void testDefaultPolicyOnExamCreation() {
        ExamCreateRequest request = new ExamCreateRequest();
        request.setCourseId(100);
        request.setTitle("Midterm Assessment");
        request.setStartWindow(baseNow.plusDays(1));
        request.setEndWindow(baseNow.plusDays(1).plusHours(2));
        request.setDurationMinutes(90);

        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 1)).thenReturn(false);
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> {
            Exam saved = invocation.getArgument(0);
            saved.setId(50);
            return saved;
        });

        ExamResponse response = examService.createExam(instructor, request);

        assertNotNull(response);
        assertEquals(QuestionCorrectionPolicy.AWARD_FULL_MARKS, response.getQuestionCorrectionPolicy());
        verify(examRepository, times(1)).save(any(Exam.class));
    }

    @Test
    @DisplayName("Exam creation persists DROP_AND_RESCALE policy when explicitly specified")
    void testCustomPolicyOnExamCreation() {
        ExamCreateRequest request = new ExamCreateRequest();
        request.setCourseId(100);
        request.setTitle("Algorithm Challenge");
        request.setStartWindow(baseNow.plusDays(2));
        request.setEndWindow(baseNow.plusDays(2).plusHours(3));
        request.setDurationMinutes(120);
        request.setQuestionCorrectionPolicy(QuestionCorrectionPolicy.DROP_AND_RESCALE);

        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 1)).thenReturn(false);
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> {
            Exam saved = invocation.getArgument(0);
            saved.setId(51);
            return saved;
        });

        ExamResponse response = examService.createExam(instructor, request);

        assertNotNull(response);
        assertEquals(QuestionCorrectionPolicy.DROP_AND_RESCALE, response.getQuestionCorrectionPolicy());
    }

    @Test
    @DisplayName("Get correction policy when zero attempts exist returns unlocked status")
    void testGetCorrectionPolicyUnlocked() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 1)).thenReturn(false);
        when(examAttemptRepository.countByExamId(10)).thenReturn(0L);

        QuestionCorrectionPolicyResponse response = examService.getCorrectionPolicy(instructor, 10);

        assertNotNull(response);
        assertEquals(10, response.getExamId());
        assertEquals("Final Exam", response.getExamTitle());
        assertEquals(QuestionCorrectionPolicy.AWARD_FULL_MARKS, response.getPolicy());
        assertFalse(response.getIsLocked());
        assertEquals(0L, response.getAttemptCount());
        assertTrue(response.getMessage().contains("unlocked"));
    }

    @Test
    @DisplayName("Get correction policy when attempts exist returns locked status (DR-ED05)")
    void testGetCorrectionPolicyLocked() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 1)).thenReturn(false);
        when(examAttemptRepository.countByExamId(10)).thenReturn(5L);

        QuestionCorrectionPolicyResponse response = examService.getCorrectionPolicy(instructor, 10);

        assertNotNull(response);
        assertTrue(response.getIsLocked());
        assertEquals(5L, response.getAttemptCount());
        assertTrue(response.getMessage().contains("locked"));
    }

    @Test
    @DisplayName("Update correction policy succeeds when zero attempts exist")
    void testUpdateCorrectionPolicySuccessWhenNoAttempts() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 1)).thenReturn(false);
        when(examAttemptRepository.countByExamId(10)).thenReturn(0L);
        when(examRepository.save(any(Exam.class))).thenReturn(exam);

        QuestionCorrectionPolicyRequest request = new QuestionCorrectionPolicyRequest(QuestionCorrectionPolicy.DROP_AND_RESCALE);
        QuestionCorrectionPolicyResponse response = examService.updateCorrectionPolicy(instructor, 10, request);

        assertNotNull(response);
        assertEquals(QuestionCorrectionPolicy.DROP_AND_RESCALE, response.getPolicy());
        assertFalse(response.getIsLocked());
        assertEquals(QuestionCorrectionPolicy.DROP_AND_RESCALE, exam.getQuestionCorrectionPolicy());
        verify(examRepository, times(1)).save(exam);
    }

    @Test
    @DisplayName("Update correction policy throws 409 Conflict when attempts already exist (DR-ED05)")
    void testUpdateCorrectionPolicyFailsWhenAttemptsExist() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 1)).thenReturn(false);
        when(examAttemptRepository.countByExamId(10)).thenReturn(2L);

        QuestionCorrectionPolicyRequest request = new QuestionCorrectionPolicyRequest(QuestionCorrectionPolicy.DROP_AND_RESCALE);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                examService.updateCorrectionPolicy(instructor, 10, request)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("locked") || ex.getReason().contains("DR-ED05"));
        verify(examRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update correction policy with same value when attempts exist is idempotent")
    void testUpdateCorrectionPolicySameValueWhenAttemptsExist() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 1)).thenReturn(false);
        when(examAttemptRepository.countByExamId(10)).thenReturn(3L);
        when(examRepository.save(any(Exam.class))).thenReturn(exam);

        // Same policy already set on exam
        QuestionCorrectionPolicyRequest request = new QuestionCorrectionPolicyRequest(QuestionCorrectionPolicy.AWARD_FULL_MARKS);
        QuestionCorrectionPolicyResponse response = examService.updateCorrectionPolicy(instructor, 10, request);

        assertNotNull(response);
        assertEquals(QuestionCorrectionPolicy.AWARD_FULL_MARKS, response.getPolicy());
        assertTrue(response.getIsLocked());
    }

    @Test
    @DisplayName("updateExam throws 409 Conflict when attempting to change policy with existing attempts")
    void testUpdateExamWithPolicyConflictWhenAttemptsExist() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 1)).thenReturn(false);
        when(examAttemptRepository.countByExamId(10)).thenReturn(1L);

        ExamUpdateRequest request = new ExamUpdateRequest();
        request.setQuestionCorrectionPolicy(QuestionCorrectionPolicy.DROP_AND_RESCALE);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                examService.updateExam(instructor, 10, request)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(examRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateExam updates policy successfully when zero attempts exist")
    void testUpdateExamWithPolicySuccessWhenNoAttempts() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 1)).thenReturn(false);
        when(examAttemptRepository.countByExamId(10)).thenReturn(0L);
        when(examRepository.save(any(Exam.class))).thenAnswer(i -> i.getArgument(0));

        ExamUpdateRequest request = new ExamUpdateRequest();
        request.setQuestionCorrectionPolicy(QuestionCorrectionPolicy.DROP_AND_RESCALE);

        ExamResponse response = examService.updateExam(instructor, 10, request);

        assertNotNull(response);
        assertEquals(QuestionCorrectionPolicy.DROP_AND_RESCALE, response.getQuestionCorrectionPolicy());
        assertEquals(QuestionCorrectionPolicy.DROP_AND_RESCALE, exam.getQuestionCorrectionPolicy());
    }

    @Test
    @DisplayName("Updating correction policy with null throws IllegalArgumentException")
    void testUpdateCorrectionPolicyNullPolicyThrows() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 1)).thenReturn(false);

        QuestionCorrectionPolicyRequest request = new QuestionCorrectionPolicyRequest(null);

        assertThrows(IllegalArgumentException.class, () ->
                examService.updateCorrectionPolicy(instructor, 10, request)
        );
    }

    @Test
    @DisplayName("Unauthorized instructor cannot modify question correction policy")
    void testUnauthorizedInstructorCannotUpdatePolicy() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 4)).thenReturn(false);

        QuestionCorrectionPolicyRequest request = new QuestionCorrectionPolicyRequest(QuestionCorrectionPolicy.DROP_AND_RESCALE);

        assertThrows(AccessDeniedException.class, () ->
                examService.updateCorrectionPolicy(otherInstructor, 10, request)
        );
    }

    @Test
    @DisplayName("Student cannot modify question correction policy")
    void testStudentCannotUpdatePolicy() {
        QuestionCorrectionPolicyRequest request = new QuestionCorrectionPolicyRequest(QuestionCorrectionPolicy.DROP_AND_RESCALE);

        assertThrows(AccessDeniedException.class, () ->
                examService.updateCorrectionPolicy(student, 10, request)
        );
    }

    @Test
    @DisplayName("Admin can update policy when no attempts exist")
    void testAdminCanUpdatePolicy() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.countByExamId(10)).thenReturn(0L);
        when(examRepository.save(any(Exam.class))).thenReturn(exam);

        QuestionCorrectionPolicyRequest request = new QuestionCorrectionPolicyRequest(QuestionCorrectionPolicy.DROP_AND_RESCALE);
        QuestionCorrectionPolicyResponse response = examService.updateCorrectionPolicy(admin, 10, request);

        assertNotNull(response);
        assertEquals(QuestionCorrectionPolicy.DROP_AND_RESCALE, response.getPolicy());
    }

    @Test
    @DisplayName("getCorrectionPolicyForCorrection exposes policy directly for FR-I37")
    void testGetCorrectionPolicyForCorrectionReadDirectly() {
        when(examRepository.findById(10)).thenReturn(Optional.of(exam));

        QuestionCorrectionPolicy policy = examService.getCorrectionPolicyForCorrection(10);

        assertEquals(QuestionCorrectionPolicy.AWARD_FULL_MARKS, policy);
    }

    @Test
    @DisplayName("Non-existent exam throws ResourceNotFoundException")
    void testNonExistentExamThrowsNotFound() {
        when(examRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                examService.getCorrectionPolicy(instructor, 999)
        );
    }
}
