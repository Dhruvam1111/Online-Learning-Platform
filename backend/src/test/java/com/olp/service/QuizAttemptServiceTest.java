package com.olp.service;

import com.olp.dto.QuizAttemptResponse;
import com.olp.dto.QuizSettingsResponse;
import com.olp.entity.*;
import com.olp.exception.QuizAttemptExpiredException;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseModuleRepository courseModuleRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private UserRepository userRepository;

    private QuizService quizService;
    private QuizAttemptService attemptService;

    private User instructor;
    private User enrolledStudent;
    private User unenrolledStudent;
    private Course course;
    private Quiz publishedQuiz;
    private Quiz draftQuiz;

    private final ZoneId zone = ZoneId.systemDefault();
    private final LocalDateTime baseTime = LocalDateTime.of(2026, 11, 12, 10, 0, 0);

    @BeforeEach
    void setUp() {
        quizService = new QuizService(
                quizRepository, questionRepository, quizQuestionRepository,
                courseRepository, courseModuleRepository, lessonRepository,
                courseInstructorRepository, userRepository);
        attemptService = new QuizAttemptService(
                quizRepository, quizQuestionRepository, quizAttemptRepository,
                enrollmentRepository, userRepository, quizService);

        setServerTime(baseTime);

        instructor = new User("Dr. Owner", "owner@olp.com", "hash", "instructor", "active");
        instructor.setId(1);

        enrolledStudent = new User("Alice", "alice@olp.com", "hash", "student", "active");
        enrolledStudent.setId(3);

        unenrolledStudent = new User("Bob", "bob@olp.com", "hash", "student", "active");
        unenrolledStudent.setId(4);

        course = new Course(instructor, "Web Dev", "Desc", CourseStatus.PUBLISHED);
        course.setId(10);

        publishedQuiz = new Quiz(course, null, null, instructor, "Midterm Quiz", "Desc", null, QuizStatus.PUBLISHED);
        publishedQuiz.setId(100);
        publishedQuiz.setTimeLimitMinutes(30);
        publishedQuiz.setMaxAttempts(2);
        publishedQuiz.setTotalMarks(new BigDecimal("10.00"));
        publishedQuiz.setAvailableFrom(LocalDateTime.of(2026, 11, 12, 9, 0, 0));
        publishedQuiz.setAvailableUntil(LocalDateTime.of(2026, 11, 12, 12, 0, 0));

        draftQuiz = new Quiz(course, null, null, instructor, "Draft Quiz", "Desc", null, QuizStatus.DRAFT);
        draftQuiz.setId(101);
    }

    private void setServerTime(LocalDateTime dateTime) {
        Clock fixed = Clock.fixed(dateTime.atZone(zone).toInstant(), zone);
        quizService.setClock(fixed);
        attemptService.setClock(fixed);
    }

    @Test
    void testGetStudentQuizSettings_SuccessAndAccessChecks() {
        when(quizRepository.findByIdWithDetails(100)).thenReturn(Optional.of(publishedQuiz));
        when(enrollmentRepository.existsByStudentIdAndCourseId(3, 10)).thenReturn(true);
        when(quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(100)).thenReturn(List.of());
        when(quizAttemptRepository.countByQuizIdAndStudentId(100, 3)).thenReturn(1L);

        QuizSettingsResponse res = attemptService.getStudentQuizSettings(enrolledStudent, 100);

        assertEquals(100, res.getQuizId());
        assertEquals(30, res.getTimeLimitMinutes());
        assertEquals(2, res.getMaxAttempts());
        assertEquals(1L, res.getAttemptsUsed());
        assertEquals(1, res.getRemainingAttempts());
        assertTrue(res.isCurrentlyAvailable());

        // Unenrolled student blocked
        when(enrollmentRepository.existsByStudentIdAndCourseId(4, 10)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> attemptService.getStudentQuizSettings(unenrolledStudent, 100));

        // Draft quiz blocked
        when(quizRepository.findByIdWithDetails(101)).thenReturn(Optional.of(draftQuiz));
        assertThrows(IllegalStateException.class, () -> attemptService.getStudentQuizSettings(enrolledStudent, 101));

        // Non-student blocked
        assertThrows(AccessDeniedException.class, () -> attemptService.getStudentQuizSettings(instructor, 100));
    }

    @Test
    void testAvailabilityWindowEnforcement_BeforeStartAndAfterEndRejected() {
        when(quizRepository.findByIdWithDetails(100)).thenReturn(Optional.of(publishedQuiz));
        when(enrollmentRepository.existsByStudentIdAndCourseId(3, 10)).thenReturn(true);

        // Before start (08:30 < 09:00)
        setServerTime(LocalDateTime.of(2026, 11, 12, 8, 30, 0));
        IllegalStateException beforeEx = assertThrows(IllegalStateException.class,
                () -> attemptService.startQuizAttempt(enrolledStudent, 100));
        assertTrue(beforeEx.getMessage().contains("not available yet"));

        // After end (12:01 > 12:00)
        setServerTime(LocalDateTime.of(2026, 11, 12, 12, 1, 0));
        IllegalStateException afterEx = assertThrows(IllegalStateException.class,
                () -> attemptService.startQuizAttempt(enrolledStudent, 100));
        assertTrue(afterEx.getMessage().contains("closed"));
    }

    @Test
    void testServerSideTimerEnforcement_StartSubmitBeforeAndAfterExpiry() {
        when(quizRepository.findByIdWithDetails(100)).thenReturn(Optional.of(publishedQuiz));
        when(enrollmentRepository.existsByStudentIdAndCourseId(3, 10)).thenReturn(true);
        when(quizAttemptRepository.findByQuizIdAndStudentIdAndStatusOrderByAttemptNumberDesc(
                100, 3, QuizAttemptStatus.IN_PROGRESS)).thenReturn(List.of());
        when(quizAttemptRepository.countByQuizIdAndStudentId(100, 3)).thenReturn(0L);
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> {
            QuizAttempt a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(500);
            }
            return a;
        });

        // Start attempt at 10:00 -> expires at 10:30 (30-min duration)
        setServerTime(baseTime);
        QuizAttemptResponse started = attemptService.startQuizAttempt(enrolledStudent, 100);
        assertEquals(500, started.getId());
        assertEquals(1, started.getAttemptNumber());
        assertEquals("IN_PROGRESS", started.getStatus());
        assertEquals(baseTime.plusMinutes(30), started.getExpiresAt());
        assertEquals(1800L, started.getRemainingSeconds());

        // Check status at 10:20 -> 600s remaining
        QuizAttempt persisted = new QuizAttempt(publishedQuiz, enrolledStudent, 1, baseTime, baseTime.plusMinutes(30));
        persisted.setId(500);
        when(quizAttemptRepository.findByIdAndQuizId(500, 100)).thenReturn(Optional.of(persisted));

        setServerTime(baseTime.plusMinutes(20));
        QuizAttemptResponse midStatus = attemptService.getQuizAttemptStatus(enrolledStudent, 100, 500);
        assertEquals("IN_PROGRESS", midStatus.getStatus());
        assertEquals(600L, midStatus.getRemainingSeconds());

        // Submit within limit (10:25) succeeds
        setServerTime(baseTime.plusMinutes(25));
        QuizAttemptResponse submitted = attemptService.submitQuizAttempt(enrolledStudent, 100, 500);
        assertEquals("SUBMITTED", submitted.getStatus());
        assertEquals(baseTime.plusMinutes(25), submitted.getSubmittedAt());

        // Second attempt started at 11:45 when window ends at 12:00 -> capped at 12:00 (15 min)
        setServerTime(LocalDateTime.of(2026, 11, 12, 11, 45, 0));
        when(quizAttemptRepository.countByQuizIdAndStudentId(100, 3)).thenReturn(1L);
        QuizAttemptResponse capped = attemptService.startQuizAttempt(enrolledStudent, 100);
        assertEquals(LocalDateTime.of(2026, 11, 12, 12, 0, 0), capped.getExpiresAt());
        assertEquals(900L, capped.getRemainingSeconds());

        // Submit attempt #2 after expiry (12:02) -> marks EXPIRED and throws QuizAttemptExpiredException
        QuizAttempt attempt2 = new QuizAttempt(
                publishedQuiz, enrolledStudent, 2,
                LocalDateTime.of(2026, 11, 12, 11, 45, 0),
                LocalDateTime.of(2026, 11, 12, 12, 0, 0));
        attempt2.setId(501);
        when(quizAttemptRepository.findByIdAndQuizId(501, 100)).thenReturn(Optional.of(attempt2));

        setServerTime(LocalDateTime.of(2026, 11, 12, 12, 2, 0));
        assertThrows(QuizAttemptExpiredException.class,
                () -> attemptService.submitQuizAttempt(enrolledStudent, 100, 501));
        assertEquals(QuizAttemptStatus.EXPIRED, attempt2.getStatus());
    }

    @Test
    void testAttemptLimitEnforcement_BlocksWhenLimitReached() {
        when(quizRepository.findByIdWithDetails(100)).thenReturn(Optional.of(publishedQuiz));
        when(enrollmentRepository.existsByStudentIdAndCourseId(3, 10)).thenReturn(true);
        when(quizAttemptRepository.findByQuizIdAndStudentIdAndStatusOrderByAttemptNumberDesc(
                100, 3, QuizAttemptStatus.IN_PROGRESS)).thenReturn(List.of());
        when(quizAttemptRepository.countByQuizIdAndStudentId(100, 3)).thenReturn(2L);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> attemptService.startQuizAttempt(enrolledStudent, 100));
        assertTrue(ex.getMessage().contains("Maximum allowed attempts (2)"));
    }

    @Test
    void testConcurrentActiveAttemptBlocked_ButExpiredAutoCleanedUp() {
        when(quizRepository.findByIdWithDetails(100)).thenReturn(Optional.of(publishedQuiz));
        when(enrollmentRepository.existsByStudentIdAndCourseId(3, 10)).thenReturn(true);

        QuizAttempt activeAttempt = new QuizAttempt(
                publishedQuiz, enrolledStudent, 1, baseTime, baseTime.plusMinutes(30));
        activeAttempt.setId(600);

        when(quizAttemptRepository.findByQuizIdAndStudentIdAndStatusOrderByAttemptNumberDesc(
                100, 3, QuizAttemptStatus.IN_PROGRESS)).thenReturn(List.of(activeAttempt));

        // Still active at +10 min -> cannot open duplicate attempt
        setServerTime(baseTime.plusMinutes(10));
        assertThrows(IllegalStateException.class, () -> attemptService.startQuizAttempt(enrolledStudent, 100));

        // After +35 min -> stale attempt auto-expired, attempt #2 starts cleanly
        setServerTime(baseTime.plusMinutes(35));
        when(quizAttemptRepository.save(activeAttempt)).thenReturn(activeAttempt);
        when(quizAttemptRepository.findByQuizIdAndStudentIdAndStatusOrderByAttemptNumberDesc(
                100, 3, QuizAttemptStatus.IN_PROGRESS))
                .thenReturn(List.of(activeAttempt))
                .thenReturn(List.of());
        when(quizAttemptRepository.countByQuizIdAndStudentId(100, 3)).thenReturn(1L);
        when(quizAttemptRepository.save(argThat(a -> a != activeAttempt))).thenAnswer(inv -> {
            QuizAttempt a = inv.getArgument(0);
            a.setId(601);
            return a;
        });

        QuizAttemptResponse second = attemptService.startQuizAttempt(enrolledStudent, 100);
        assertEquals(QuizAttemptStatus.EXPIRED, activeAttempt.getStatus());
        assertEquals(601, second.getId());
        assertEquals(2, second.getAttemptNumber());
    }
}

