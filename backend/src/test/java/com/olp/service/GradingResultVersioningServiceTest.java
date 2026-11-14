package com.olp.service;

import com.olp.dto.GradeOverrideRequest;
import com.olp.dto.GradeOverrideResponse;
import com.olp.dto.GradingResultVersionResponse;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test Suite for FR-I32: Override Grades with Justification and Result Versioning (DR-01, DR-ED04).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GradingResultVersioningServiceTest {

    @Mock
    private GradingResultVersionRepository gradingResultVersionRepository;
    @Mock
    private QuizAttemptAnswerRepository quizAttemptAnswerRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseInstructorRepository courseInstructorRepository;
    @Mock
    private UserRepository userRepository;

    private GradingResultVersioningService versioningService;

    private User instructor;
    private User otherInstructor;
    private User student;
    private Course course;
    private Quiz quiz;
    private QuizAttempt attempt;
    private Question question;
    private QuizAttemptAnswer answer;

    @BeforeEach
    void setUp() {
        versioningService = new GradingResultVersioningService(
                gradingResultVersionRepository,
                quizAttemptAnswerRepository,
                quizAttemptRepository,
                courseRepository,
                courseInstructorRepository,
                userRepository
        );

        instructor = new User("Prof. Smith", "smith@olp.com", "hash", "instructor", "active");
        instructor.setId(10);

        otherInstructor = new User("Dr. Brown", "brown@olp.com", "hash", "instructor", "active");
        otherInstructor.setId(20);

        student = new User("Alice", "alice@olp.com", "hash", "student", "active");
        student.setId(100);

        course = new Course(instructor, "CS101", "Computer Science 101", CourseStatus.PUBLISHED);
        course.setId(1);
        course.setDeleted(false);

        quiz = new Quiz(course, null, null, instructor, "Midterm Exam", "Midterm exam", null, QuizStatus.PUBLISHED);
        quiz.setId(200);
        quiz.setTotalMarks(BigDecimal.valueOf(100.0));
        quiz.setPassingMarks(BigDecimal.valueOf(70.0));
        quiz.setTimeLimitMinutes(60);

        LocalDateTime now = LocalDateTime.now();
        attempt = new QuizAttempt(quiz, student, 1, now.minusMinutes(45), now.minusMinutes(15));
        attempt.setId(300);
        attempt.setStatus(QuizAttemptStatus.SUBMITTED);
        attempt.setObtainedMarks(BigDecimal.valueOf(10.0));

        question = new Question(course, instructor, "Explain encapsulation", QuestionType.DESCRIPTIVE,
                null, null, BigDecimal.valueOf(20.0), BigDecimal.ZERO, BigDecimal.ZERO, "Rubric", true);
        question.setId(400);

        answer = new QuizAttemptAnswer(attempt, question, "Encapsulation bundles data and methods.",
                BigDecimal.valueOf(10.0), BigDecimal.valueOf(20.0), EvaluationStatus.PARTIALLY_CORRECT, "Initial feedback", now);
        answer.setId(500);

        when(gradingResultVersionRepository.save(any(GradingResultVersion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(quizAttemptAnswerRepository.save(any(QuizAttemptAnswer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // =========================================================================
    // 1. Answer-Level Grade Override Tests (FR-I32)
    // =========================================================================

    @Test
    @DisplayName("Should successfully override answer grade and create version 1 record")
    void testOverrideAnswerGrade_Success() {
        when(quizAttemptAnswerRepository.findByIdWithDetails(500)).thenReturn(Optional.of(answer));
        when(gradingResultVersionRepository.countByAnswerId(500)).thenReturn(0);
        when(quizAttemptAnswerRepository.findByAttemptId(300)).thenReturn(List.of(answer));

        GradeOverrideRequest req = new GradeOverrideRequest(BigDecimal.valueOf(18.0), "Awarding full credit for rigorous alternative explanation");

        GradeOverrideResponse res = versioningService.overrideAnswerGrade(instructor, 500, req);

        assertNotNull(res);
        assertEquals(1, res.getVersionNumber());
        assertEquals(BigDecimal.valueOf(10.0), res.getPreviousMarks());
        assertEquals(BigDecimal.valueOf(18.0), res.getNewMarks());
        assertEquals(BigDecimal.valueOf(20.0), res.getMaxMarks());
        assertEquals("Awarding full credit for rigorous alternative explanation", res.getJustification());
        assertEquals(instructor.getId(), res.getInstructorId());
        assertTrue(res.getMessage().contains("version 1"));

        // Verify underlying entity updates
        assertEquals(BigDecimal.valueOf(18.0), answer.getAwardedMarks());
        assertTrue(answer.isOverridden());
        assertEquals(instructor, answer.getGrader());
        verify(gradingResultVersionRepository).save(any(GradingResultVersion.class));
        verify(quizAttemptAnswerRepository).save(answer);
        verify(quizAttemptRepository).save(attempt);
    }

    @Test
    @DisplayName("Should create sequential version 2 when overridden a second time")
    void testOverrideAnswerGrade_SequentialVersionNumber() {
        answer.setAwardedMarks(BigDecimal.valueOf(15.0));
        when(quizAttemptAnswerRepository.findByIdWithDetails(500)).thenReturn(Optional.of(answer));
        when(gradingResultVersionRepository.countByAnswerId(500)).thenReturn(1);
        when(quizAttemptAnswerRepository.findByAttemptId(300)).thenReturn(List.of(answer));

        GradeOverrideRequest req = new GradeOverrideRequest(BigDecimal.valueOf(20.0), "Correction after rubric re-evaluation");

        GradeOverrideResponse res = versioningService.overrideAnswerGrade(instructor, 500, req);

        assertNotNull(res);
        assertEquals(2, res.getVersionNumber());
        assertEquals(BigDecimal.valueOf(15.0), res.getPreviousMarks());
        assertEquals(BigDecimal.valueOf(20.0), res.getNewMarks());
    }

    @Test
    @DisplayName("Should reject override if justification is missing or blank")
    void testOverrideAnswerGrade_RequiresMandatoryJustification() {
        GradeOverrideRequest emptyReq = new GradeOverrideRequest(BigDecimal.valueOf(15.0), "");
        GradeOverrideRequest whitespaceReq = new GradeOverrideRequest(BigDecimal.valueOf(15.0), "   ");
        GradeOverrideRequest nullReq = new GradeOverrideRequest(BigDecimal.valueOf(15.0), null);

        assertThrows(IllegalArgumentException.class, () -> versioningService.overrideAnswerGrade(instructor, 500, emptyReq));
        assertThrows(IllegalArgumentException.class, () -> versioningService.overrideAnswerGrade(instructor, 500, whitespaceReq));
        assertThrows(IllegalArgumentException.class, () -> versioningService.overrideAnswerGrade(instructor, 500, nullReq));
    }

    @Test
    @DisplayName("Should reject negative marks for answer override")
    void testOverrideAnswerGrade_RejectsNegativeMarks() {
        when(quizAttemptAnswerRepository.findByIdWithDetails(500)).thenReturn(Optional.of(answer));
        GradeOverrideRequest req = new GradeOverrideRequest(BigDecimal.valueOf(-1.0), "Penalty applied");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> versioningService.overrideAnswerGrade(instructor, 500, req));
        assertTrue(ex.getMessage().contains("negative"));
    }

    @Test
    @DisplayName("Should reject marks exceeding maximum allowed marks")
    void testOverrideAnswerGrade_RejectsExceedingMaxMarks() {
        when(quizAttemptAnswerRepository.findByIdWithDetails(500)).thenReturn(Optional.of(answer));
        GradeOverrideRequest req = new GradeOverrideRequest(BigDecimal.valueOf(25.0), "Extra bonus marks");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> versioningService.overrideAnswerGrade(instructor, 500, req));
        assertTrue(ex.getMessage().contains("cannot exceed maximum marks"));
    }

    @Test
    @DisplayName("Should prevent duplicate override if new marks are identical to existing marks")
    void testOverrideAnswerGrade_PreventsDuplicateOverride() {
        answer.setAwardedMarks(BigDecimal.valueOf(10.0));
        when(quizAttemptAnswerRepository.findByIdWithDetails(500)).thenReturn(Optional.of(answer));

        GradeOverrideRequest req = new GradeOverrideRequest(BigDecimal.valueOf(10.0), "Re-affirming same score");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> versioningService.overrideAnswerGrade(instructor, 500, req));
        assertTrue(ex.getMessage().contains("identical"));
        verify(gradingResultVersionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject override if instructor is not associated with course (DR-05)")
    void testOverrideAnswerGrade_AccessDeniedForUnassociatedInstructor() {
        when(quizAttemptAnswerRepository.findByIdWithDetails(500)).thenReturn(Optional.of(answer));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(1, 20)).thenReturn(false);

        GradeOverrideRequest req = new GradeOverrideRequest(BigDecimal.valueOf(15.0), "Valid reason");

        assertThrows(AccessDeniedException.class,
                () -> versioningService.overrideAnswerGrade(otherInstructor, 500, req));
    }

    @Test
    @DisplayName("Should reject override for student role")
    void testOverrideAnswerGrade_AccessDeniedForStudent() {
        GradeOverrideRequest req = new GradeOverrideRequest(BigDecimal.valueOf(15.0), "Self-adjustment");

        assertThrows(AccessDeniedException.class,
                () -> versioningService.overrideAnswerGrade(student, 500, req));
    }

    // =========================================================================
    // 2. Attempt-Level Grade Override Tests (FR-I32)
    // =========================================================================

    @Test
    @DisplayName("Should successfully override attempt-level grade with mandatory justification")
    void testOverrideAttemptGrade_Success() {
        when(quizAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));
        when(gradingResultVersionRepository.countByAttemptIdAndAnswerIsNull(300)).thenReturn(0);

        GradeOverrideRequest req = new GradeOverrideRequest(BigDecimal.valueOf(88.0), "Course-wide curve applied after review");

        GradeOverrideResponse res = versioningService.overrideAttemptGrade(instructor, 300, req);

        assertNotNull(res);
        assertEquals(1, res.getVersionNumber());
        assertEquals(BigDecimal.valueOf(10.0), res.getPreviousMarks());
        assertEquals(BigDecimal.valueOf(88.0), res.getNewMarks());
        assertEquals("Course-wide curve applied after review", res.getJustification());
        assertEquals(BigDecimal.valueOf(88.0), attempt.getObtainedMarks());
        assertTrue(attempt.isOverridden());
        verify(gradingResultVersionRepository).save(any(GradingResultVersion.class));
        verify(quizAttemptRepository).save(attempt);
    }

    @Test
    @DisplayName("Should reject attempt override if marks exceed quiz total marks")
    void testOverrideAttemptGrade_RejectsExceedingQuizTotalMarks() {
        when(quizAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));
        GradeOverrideRequest req = new GradeOverrideRequest(BigDecimal.valueOf(105.0), "Above maximum");

        assertThrows(IllegalArgumentException.class,
                () -> versioningService.overrideAttemptGrade(instructor, 300, req));
    }

    // =========================================================================
    // 3. Reusable recordVersion & History Tests (DR-01, DR-ED04)
    // =========================================================================

    @Test
    @DisplayName("recordVersion correctly persists generic result version")
    void testRecordVersion_Success() {
        GradingResultVersion v = versioningService.recordVersion(
                attempt, answer, BigDecimal.valueOf(5.0), BigDecimal.valueOf(10.0), BigDecimal.valueOf(20.0),
                "Approved dispute credit", instructor, "INCORRECT", "PARTIALLY_CORRECT");

        assertNotNull(v);
        assertEquals("Approved dispute credit", v.getJustification());
        assertEquals(BigDecimal.valueOf(5.0), v.getPreviousMarks());
        assertEquals(BigDecimal.valueOf(10.0), v.getNewMarks());
        verify(gradingResultVersionRepository).save(any(GradingResultVersion.class));
    }

    @Test
    @DisplayName("getAnswerVersionHistory retrieves all versions in ascending version order")
    void testGetAnswerVersionHistory_Success() {
        GradingResultVersion v1 = new GradingResultVersion(attempt, answer, 1, BigDecimal.valueOf(5.0),
                BigDecimal.valueOf(10.0), BigDecimal.valueOf(20.0), "Dispute 1", instructor, "INCORRECT", "PARTIAL", LocalDateTime.now());
        GradingResultVersion v2 = new GradingResultVersion(attempt, answer, 2, BigDecimal.valueOf(10.0),
                BigDecimal.valueOf(15.0), BigDecimal.valueOf(20.0), "Dispute 2", instructor, "PARTIAL", "CORRECT", LocalDateTime.now());

        when(quizAttemptAnswerRepository.findByIdWithDetails(500)).thenReturn(Optional.of(answer));
        when(gradingResultVersionRepository.findByAnswerIdOrderByVersionNumberAsc(500)).thenReturn(List.of(v1, v2));

        List<GradingResultVersionResponse> history = versioningService.getAnswerVersionHistory(instructor, 500);

        assertNotNull(history);
        assertEquals(2, history.size());
        assertEquals(1, history.get(0).getVersionNumber());
        assertEquals(2, history.get(1).getVersionNumber());
        assertEquals("Dispute 1", history.get(0).getJustification());
        assertEquals("Dispute 2", history.get(1).getJustification());
    }
}
