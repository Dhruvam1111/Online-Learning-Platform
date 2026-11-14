package com.olp.service;

import com.olp.dto.DescriptiveSubmissionResponse;
import com.olp.dto.GradeSubmissionRequest;
import com.olp.dto.GradeSubmissionResponse;
import com.olp.dto.PageResponse;
import com.olp.dto.PublishResultResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test Suite for FR-I30: Grade Descriptive Submissions.
 * Tests grading queue ordering, filtering, authorization (DR-05),
 * marks bounds validation, EVALUATED_UNPUBLISHED status handling,
 * and result publishing workflows.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DescriptiveGradingServiceTest {

    @Mock
    private QuizAttemptAnswerRepository quizAttemptAnswerRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseInstructorRepository courseInstructorRepository;
    @Mock
    private UserRepository userRepository;

    private DescriptiveGradingService gradingService;

    private User instructor;
    private User otherInstructor;
    private User admin;
    private User student;
    private Course course;
    private Quiz quiz;
    private QuizAttempt attempt;
    private Question question;
    private QuizAttemptAnswer answer;

    @BeforeEach
    void setUp() {
        gradingService = new DescriptiveGradingService(
                quizAttemptAnswerRepository,
                quizAttemptRepository,
                quizRepository,
                courseRepository,
                courseInstructorRepository,
                userRepository
        );

        instructor = new User("Dr. Turing", "turing@olp.com", "hash", "instructor", "active");
        instructor.setId(10);

        otherInstructor = new User("Dr. Lovelace", "lovelace@olp.com", "hash", "instructor", "active");
        otherInstructor.setId(20);

        admin = new User("System Admin", "admin@olp.com", "hash", "admin", "active");
        admin.setId(1);

        student = new User("Alice Learner", "alice@olp.com", "hash", "student", "active");
        student.setId(100);

        course = new Course(instructor, "Advanced Algorithms", "Deep dive into algorithms", CourseStatus.PUBLISHED);
        course.setId(50);
        course.setDeleted(false);

        quiz = new Quiz(course, null, null, instructor, "Midterm Exam", "Midterm descriptive exam", null, QuizStatus.PUBLISHED);
        quiz.setId(200);
        quiz.setTotalMarks(BigDecimal.valueOf(100.0));
        quiz.setPassingMarks(BigDecimal.valueOf(50.0));
        quiz.setTimeLimitMinutes(60);

        attempt = new QuizAttempt(quiz, student, 1, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1));
        attempt.setId(300);
        attempt.setStatus(QuizAttemptStatus.SUBMITTED);
        attempt.setObtainedMarks(BigDecimal.ZERO);

        question = new Question(course, instructor, "Explain Dijkstra's Algorithm", QuestionType.DESCRIPTIVE,
                null, null, BigDecimal.valueOf(20.0), BigDecimal.ZERO, BigDecimal.ZERO, "Rubric detail", true);
        question.setId(400);

        answer = new QuizAttemptAnswer(attempt, question, "Dijkstra finds shortest path using priority queue",
                BigDecimal.ZERO, BigDecimal.valueOf(20.0), EvaluationStatus.MANUAL_GRADING_REQUIRED,
                null, LocalDateTime.now().minusMinutes(45));
        answer.setId(500);

        when(courseRepository.findById(50)).thenReturn(Optional.of(course));
        when(courseRepository.findByInstructorIdOrderByIdAsc(10)).thenReturn(List.of(course));
        when(courseInstructorRepository.findByInstructorId(10)).thenReturn(Collections.emptyList());
        when(quizRepository.findByIdWithDetails(200)).thenReturn(Optional.of(quiz));
        when(quizAttemptAnswerRepository.findByIdWithDetails(500)).thenReturn(Optional.of(answer));
        when(quizAttemptAnswerRepository.save(any(QuizAttemptAnswer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // =========================================================================
    // 1. Grading Queue Tests
    // =========================================================================

    @Test
    @DisplayName("Should retrieve grading queue for instructor courses with ungraded first")
    void testGetGradingQueue_Success() {
        QuizAttemptAnswer ungradedAns = answer;
        QuizAttemptAnswer evaluatedAns = new QuizAttemptAnswer(attempt, question, "Second submission",
                BigDecimal.valueOf(15.0), BigDecimal.valueOf(20.0), EvaluationStatus.EVALUATED_UNPUBLISHED,
                "Good explanation", LocalDateTime.now().minusMinutes(30));
        evaluatedAns.setId(501);

        when(quizAttemptAnswerRepository.findDescriptiveGradingQueue(
                eq(Set.of(50)), isNull(), isNull(), isNull()))
                .thenReturn(List.of(ungradedAns, evaluatedAns));

        List<DescriptiveSubmissionResponse> result = gradingService.getGradingQueue(instructor, null, null, null);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(500, result.get(0).getAnswerId());
        assertEquals("MANUAL_GRADING_REQUIRED", result.get(0).getEvaluationStatus());
        assertEquals(501, result.get(1).getAnswerId());
        assertEquals("EVALUATED_UNPUBLISHED", result.get(1).getEvaluationStatus());
    }

    @Test
    @DisplayName("Should support paginated grading queue query")
    void testGetGradingQueuePaged_Success() {
        Page<QuizAttemptAnswer> page = new PageImpl<>(List.of(answer), PageRequest.of(0, 10), 1);
        when(quizAttemptAnswerRepository.findDescriptiveGradingQueuePaged(
                eq(Set.of(50)), eq(50), eq(200), eq(EvaluationStatus.MANUAL_GRADING_REQUIRED), any(PageRequest.class)))
                .thenReturn(page);

        PageResponse<DescriptiveSubmissionResponse> response = gradingService.getGradingQueuePaged(
                instructor, 50, 200, "UNGRADED", 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(500, response.getContent().get(0).getAnswerId());
        assertEquals("Alice Learner", response.getContent().get(0).getStudentName());
    }

    @Test
    @DisplayName("Should reject unassociated instructor from viewing course queue (DR-05)")
    void testGetGradingQueue_AccessDeniedForUnassociatedInstructor() {
        assertThrows(AccessDeniedException.class,
                () -> gradingService.getGradingQueue(otherInstructor, 50, null, null));
    }

    @Test
    @DisplayName("Should reject inactive instructor")
    void testGetGradingQueue_AccessDeniedForInactiveInstructor() {
        User inactiveInstructor = new User("Inactive", "inactive@olp.com", "hash", "instructor", "pending");
        inactiveInstructor.setId(30);

        assertThrows(AccessDeniedException.class,
                () -> gradingService.getGradingQueue(inactiveInstructor, 50, null, null));
    }

    @Test
    @DisplayName("Admin can view queue across all courses")
    void testGetGradingQueue_AdminSuccess() {
        when(courseRepository.findAll()).thenReturn(List.of(course));
        when(quizAttemptAnswerRepository.findDescriptiveGradingQueue(
                anySet(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(answer));

        List<DescriptiveSubmissionResponse> result = gradingService.getGradingQueue(admin, null, null, null);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // =========================================================================
    // 2. Grade Submissions Tests
    // =========================================================================

    @Test
    @DisplayName("Should grade submission with EVALUATED_UNPUBLISHED by default")
    void testGradeSubmission_UnpublishedByDefault() {
        GradeSubmissionRequest request = new GradeSubmissionRequest(BigDecimal.valueOf(18.0), "Excellent answer");

        GradeSubmissionResponse response = gradingService.gradeSubmission(instructor, 500, request);

        assertNotNull(response);
        assertEquals("Submission graded successfully with status EVALUATED_UNPUBLISHED", response.getMessage());
        assertNotNull(response.getSubmission());
        assertEquals(BigDecimal.valueOf(18.0), response.getSubmission().getAwardedMarks());
        assertEquals("EVALUATED_UNPUBLISHED", response.getSubmission().getEvaluationStatus());
        assertEquals("Excellent answer", response.getSubmission().getFeedback());
        assertEquals(10, response.getSubmission().getGraderId());
        assertEquals("Dr. Turing", response.getSubmission().getGraderName());

        assertEquals(EvaluationStatus.EVALUATED_UNPUBLISHED, answer.getEvaluationStatus());
        assertEquals(BigDecimal.valueOf(18.0), answer.getAwardedMarks());
        assertEquals("Excellent answer", answer.getFeedback());
        assertEquals(instructor, answer.getGrader());
        assertNotNull(answer.getEvaluatedAt());

        // Attempt total marks must NOT be recalculated for unpublished grades
        assertEquals(BigDecimal.ZERO, attempt.getObtainedMarks());
    }

    @Test
    @DisplayName("Should immediately publish when publish flag is set to true")
    void testGradeSubmission_ImmediatePublish() {
        GradeSubmissionRequest request = new GradeSubmissionRequest(BigDecimal.valueOf(15.0), "Well structured", true);

        when(quizAttemptAnswerRepository.findByAttemptId(300)).thenReturn(List.of(answer));

        GradeSubmissionResponse response = gradingService.gradeSubmission(instructor, 500, request);

        assertNotNull(response);
        assertEquals("Submission graded and published successfully", response.getMessage());
        assertEquals("PUBLISHED", response.getSubmission().getEvaluationStatus());

        assertEquals(EvaluationStatus.PUBLISHED, answer.getEvaluationStatus());
        // Recalculated attempt total marks
        assertEquals(BigDecimal.valueOf(15.0), attempt.getObtainedMarks());
        verify(quizAttemptRepository).save(attempt);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when awarded marks is negative")
    void testGradeSubmission_NegativeMarks_ThrowsException() {
        GradeSubmissionRequest request = new GradeSubmissionRequest(BigDecimal.valueOf(-5.0), "Negative");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> gradingService.gradeSubmission(instructor, 500, request));
        assertTrue(ex.getMessage().contains("negative"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when awarded marks exceeds maximum marks")
    void testGradeSubmission_ExceedsMaxMarks_ThrowsException() {
        GradeSubmissionRequest request = new GradeSubmissionRequest(BigDecimal.valueOf(25.0), "Too high");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> gradingService.gradeSubmission(instructor, 500, request));
        assertTrue(ex.getMessage().contains("cannot exceed maximum marks"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when awarded marks is null")
    void testGradeSubmission_NullMarks_ThrowsException() {
        GradeSubmissionRequest request = new GradeSubmissionRequest(null, "No marks");

        assertThrows(IllegalArgumentException.class,
                () -> gradingService.gradeSubmission(instructor, 500, request));
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when unassociated instructor attempts grading")
    void testGradeSubmission_UnassociatedInstructor_ThrowsAccessDenied() {
        GradeSubmissionRequest request = new GradeSubmissionRequest(BigDecimal.valueOf(10.0), "Valid");

        assertThrows(AccessDeniedException.class,
                () -> gradingService.gradeSubmission(otherInstructor, 500, request));
    }

    // =========================================================================
    // 3. Result Publishing Tests
    // =========================================================================

    @Test
    @DisplayName("Should publish single answer result and recalculate attempt score")
    void testPublishAnswerResult_Success() {
        answer.setEvaluationStatus(EvaluationStatus.EVALUATED_UNPUBLISHED);
        answer.setAwardedMarks(BigDecimal.valueOf(18.0));
        when(quizAttemptAnswerRepository.findByAttemptId(300)).thenReturn(List.of(answer));

        PublishResultResponse response = gradingService.publishAnswerResult(instructor, 500);

        assertNotNull(response);
        assertEquals(1, response.getPublishedCount());
        assertEquals(EvaluationStatus.PUBLISHED, answer.getEvaluationStatus());
        assertEquals(BigDecimal.valueOf(18.0), attempt.getObtainedMarks());
        verify(quizAttemptRepository).save(attempt);
    }

    @Test
    @DisplayName("Publishing already published answer returns idempotent message with 0 count")
    void testPublishAnswerResult_AlreadyPublished() {
        answer.setEvaluationStatus(EvaluationStatus.PUBLISHED);

        PublishResultResponse response = gradingService.publishAnswerResult(instructor, 500);

        assertNotNull(response);
        assertEquals(0, response.getPublishedCount());
        assertTrue(response.getMessage().contains("already published"));
        verify(quizAttemptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should publish all evaluated unpublished submissions for a quiz")
    void testPublishQuizResults_Success() {
        QuizAttemptAnswer ans1 = answer;
        ans1.setEvaluationStatus(EvaluationStatus.EVALUATED_UNPUBLISHED);
        ans1.setAwardedMarks(BigDecimal.valueOf(16.0));

        QuizAttempt attempt2 = new QuizAttempt(quiz, student, 2, LocalDateTime.now(), LocalDateTime.now());
        attempt2.setId(301);
        QuizAttemptAnswer ans2 = new QuizAttemptAnswer(attempt2, question, "Another student answer",
                BigDecimal.valueOf(14.0), BigDecimal.valueOf(20.0), EvaluationStatus.EVALUATED_UNPUBLISHED,
                "Good", LocalDateTime.now());
        ans2.setId(502);

        when(quizAttemptAnswerRepository.findEvaluatedUnpublishedByQuizId(200))
                .thenReturn(List.of(ans1, ans2));
        when(quizAttemptAnswerRepository.findByAttemptId(300)).thenReturn(List.of(ans1));
        when(quizAttemptAnswerRepository.findByAttemptId(301)).thenReturn(List.of(ans2));

        PublishResultResponse response = gradingService.publishQuizResults(instructor, 200);

        assertNotNull(response);
        assertEquals(2, response.getPublishedCount());
        assertEquals(EvaluationStatus.PUBLISHED, ans1.getEvaluationStatus());
        assertEquals(EvaluationStatus.PUBLISHED, ans2.getEvaluationStatus());
        assertEquals(BigDecimal.valueOf(16.0), attempt.getObtainedMarks());
        assertEquals(BigDecimal.valueOf(14.0), attempt2.getObtainedMarks());
        verify(quizAttemptRepository, times(2)).save(any(QuizAttempt.class));
    }

    @Test
    @DisplayName("Should publish all evaluated unpublished submissions for a specific attempt")
    void testPublishAttemptResults_Success() {
        answer.setEvaluationStatus(EvaluationStatus.EVALUATED_UNPUBLISHED);
        answer.setAwardedMarks(BigDecimal.valueOf(19.0));

        when(quizAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));
        when(quizAttemptAnswerRepository.findEvaluatedUnpublishedByAttemptId(300)).thenReturn(List.of(answer));
        when(quizAttemptAnswerRepository.findByAttemptId(300)).thenReturn(List.of(answer));

        PublishResultResponse response = gradingService.publishAttemptResults(instructor, 300);

        assertNotNull(response);
        assertEquals(1, response.getPublishedCount());
        assertEquals(EvaluationStatus.PUBLISHED, answer.getEvaluationStatus());
        assertEquals(BigDecimal.valueOf(19.0), attempt.getObtainedMarks());
        verify(quizAttemptRepository).save(attempt);
    }

    // =========================================================================
    // 4. Attempt Score Recalculation & Exclusion Tests
    // =========================================================================

    @Test
    @DisplayName("Score recalculation sums PUBLISHED and auto-graded, excluding UNPUBLISHED")
    void testRecalculateAttemptScore_ExcludesUnpublished() {
        // Auto-graded correct answer: 10 marks
        Question mcqQ = new Question(course, instructor, "MCQ", QuestionType.MULTIPLE_CHOICE,
                "[]", "A", BigDecimal.valueOf(10.0), BigDecimal.ZERO, BigDecimal.ZERO, null, false);
        mcqQ.setId(401);
        QuizAttemptAnswer mcqAns = new QuizAttemptAnswer(attempt, mcqQ, "A",
                BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0), EvaluationStatus.CORRECT, "Correct", LocalDateTime.now());

        // Published descriptive answer: 15 marks
        QuizAttemptAnswer pubAns = new QuizAttemptAnswer(attempt, question, "Published ans",
                BigDecimal.valueOf(15.0), BigDecimal.valueOf(20.0), EvaluationStatus.PUBLISHED, "Good", LocalDateTime.now());

        // Unpublished descriptive answer: 12 marks (must NOT be counted)
        QuizAttemptAnswer unpubAns = new QuizAttemptAnswer(attempt, question, "Unpub ans",
                BigDecimal.valueOf(12.0), BigDecimal.valueOf(20.0), EvaluationStatus.EVALUATED_UNPUBLISHED, "Draft", LocalDateTime.now());

        when(quizAttemptAnswerRepository.findByAttemptId(300)).thenReturn(List.of(mcqAns, pubAns, unpubAns));

        gradingService.recalculateAttemptScore(attempt);

        // Total should be 10 + 15 = 25 (excluding the 12 marks)
        assertEquals(BigDecimal.valueOf(25.0), attempt.getObtainedMarks());
    }
}
