package com.olp.service;

import com.olp.dto.AnswerFeedbackRequest;
import com.olp.dto.AnswerFeedbackResponse;
import com.olp.dto.DescriptiveSubmissionResponse;
import com.olp.dto.GradeSubmissionRequest;
import com.olp.dto.GradeSubmissionResponse;
import com.olp.dto.PageResponse;
import com.olp.dto.PublishResultResponse;
import com.olp.dto.SubmissionFeedbackRequest;
import com.olp.dto.SubmissionFeedbackResponse;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

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

    // =========================================================================
    // 5. FR-I31 Feedback Management & Immutability Tests
    // =========================================================================

    @Test
    @DisplayName("Should successfully add or update answer-level feedback before publish")
    void testAddAnswerFeedback_BeforePublish_Success() {
        answer.setEvaluationStatus(EvaluationStatus.EVALUATED_UNPUBLISHED);
        when(quizAttemptAnswerRepository.findByIdWithDetails(500)).thenReturn(Optional.of(answer));
        when(quizAttemptAnswerRepository.save(any(QuizAttemptAnswer.class))).thenAnswer(inv -> inv.getArgument(0));

        AnswerFeedbackRequest req = new AnswerFeedbackRequest("Excellent explanation with good depth", false);
        AnswerFeedbackResponse res = gradingService.addOrUpdateAnswerFeedback(instructor, 500, req);

        assertNotNull(res);
        assertEquals("Excellent explanation with good depth", res.getFeedback());
        assertEquals(instructor.getId(), res.getGraderId());
        assertNotNull(res.getFeedbackAt());
        assertFalse(res.isPublished());
        verify(quizAttemptAnswerRepository).save(answer);
    }

    @Test
    @DisplayName("Should prevent modifying answer feedback when results are already published")
    void testAddAnswerFeedback_AfterPublish_ThrowsIllegalStateException() {
        answer.setEvaluationStatus(EvaluationStatus.PUBLISHED);
        when(quizAttemptAnswerRepository.findByIdWithDetails(500)).thenReturn(Optional.of(answer));

        AnswerFeedbackRequest req = new AnswerFeedbackRequest("Changed my mind", false);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> gradingService.addOrUpdateAnswerFeedback(instructor, 500, req));
        assertTrue(ex.getMessage().contains("already been published"));
        verify(quizAttemptAnswerRepository, never()).save(answer);
    }

    @Test
    @DisplayName("Should prevent grading when answer results are already published")
    void testGradeSubmission_AfterPublish_ThrowsIllegalStateException() {
        answer.setEvaluationStatus(EvaluationStatus.PUBLISHED);
        when(quizAttemptAnswerRepository.findByIdWithDetails(500)).thenReturn(Optional.of(answer));

        GradeSubmissionRequest req = new GradeSubmissionRequest(BigDecimal.valueOf(18.0), "Updated", false);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> gradingService.gradeSubmission(instructor, 500, req));
        assertTrue(ex.getMessage().contains("already been published"));
    }

    @Test
    @DisplayName("Should successfully add or update submission-level overall feedback")
    void testAddSubmissionFeedback_Success() {
        attempt.setIsPublished(false);
        when(quizAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmissionFeedbackRequest req = new SubmissionFeedbackRequest("Great overall test performance", false);
        SubmissionFeedbackResponse res = gradingService.addOrUpdateSubmissionFeedback(instructor, 300, req);

        assertNotNull(res);
        assertEquals("Great overall test performance", res.getOverallFeedback());
        assertEquals(instructor.getId(), res.getGraderId());
        assertNotNull(res.getFeedbackAt());
        assertFalse(res.isPublished());
        verify(quizAttemptRepository).save(attempt);
    }

    @Test
    @DisplayName("Should prevent modifying submission feedback after results are published")
    void testAddSubmissionFeedback_AfterPublish_ThrowsIllegalStateException() {
        attempt.setIsPublished(true);
        when(quizAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));

        SubmissionFeedbackRequest req = new SubmissionFeedbackRequest("Editing overall feedback", false);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> gradingService.addOrUpdateSubmissionFeedback(instructor, 300, req));
        assertTrue(ex.getMessage().contains("already been published"));
        verify(quizAttemptRepository, never()).save(attempt);
    }

    @Test
    @DisplayName("Student cannot view unpublished submission feedback")
    void testGetSubmissionFeedback_Student_Unpublished_ThrowsAccessDenied() {
        attempt.setIsPublished(false);
        attempt.setOverallFeedback("Secret draft feedback");
        when(quizAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));

        assertThrows(AccessDeniedException.class,
                () -> gradingService.getSubmissionFeedback(student, 300));
    }

    @Test
    @DisplayName("Student cannot view submission feedback when quiz feedback is disabled (FR-I23)")
    void testGetSubmissionFeedback_Student_FeedbackDisabled_ThrowsAccessDenied() {
        quiz.setFeedbackReleased(false);
        attempt.setIsPublished(true);
        attempt.setOverallFeedback("Hidden feedback");
        when(quizAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));

        assertThrows(AccessDeniedException.class,
                () -> gradingService.getSubmissionFeedback(student, 300));
    }

    @Test
    @DisplayName("Student can view submission feedback when results are published and feedback released")
    void testGetSubmissionFeedback_Student_PublishedAndEnabled_Success() {
        quiz.setFeedbackReleased(true);
        attempt.setIsPublished(true);
        attempt.setOverallFeedback("Awesome job!");
        when(quizAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));

        SubmissionFeedbackResponse res = gradingService.getSubmissionFeedback(student, 300);

        assertNotNull(res);
        assertEquals("Awesome job!", res.getOverallFeedback());
        assertTrue(res.isPublished());
    }

    // =========================================================================
    // 6. Authentication, User Resolution & Role Edge Cases
    // =========================================================================

    @Test
    @DisplayName("resolveUser resolves User principal, String email principal, and rejects invalid/null inputs")
    void testResolveUser_AllCases() {
        Authentication userAuth = new UsernamePasswordAuthenticationToken(instructor, null);
        assertSame(instructor, gradingService.resolveUser(userAuth));

        when(userRepository.findByEmail("turing@olp.com")).thenReturn(Optional.of(instructor));
        Authentication emailAuth = new UsernamePasswordAuthenticationToken("turing@olp.com", null);
        assertSame(instructor, gradingService.resolveUser(emailAuth));

        when(userRepository.findByEmail("missing@olp.com")).thenReturn(Optional.empty());
        Authentication missingEmailAuth = new UsernamePasswordAuthenticationToken("missing@olp.com", null);
        assertThrows(InvalidCredentialsException.class, () -> gradingService.resolveUser(missingEmailAuth));

        Authentication invalidPrincipalAuth = new UsernamePasswordAuthenticationToken(12345, null);
        assertThrows(InvalidCredentialsException.class, () -> gradingService.resolveUser(invalidPrincipalAuth));

        Authentication nullPrincipalAuth = mock(Authentication.class);
        when(nullPrincipalAuth.getPrincipal()).thenReturn(null);
        assertThrows(InvalidCredentialsException.class, () -> gradingService.resolveUser(nullPrincipalAuth));
        assertThrows(InvalidCredentialsException.class, () -> gradingService.resolveUser(null));
    }

    @Test
    @DisplayName("Null currentUser or student role rejected across grading and publishing operations")
    void testValidateInstructor_NullAndStudentRejected() {
        GradeSubmissionRequest req = new GradeSubmissionRequest(BigDecimal.TEN, "OK");

        assertThrows(InvalidCredentialsException.class,
                () -> gradingService.getGradingQueue(null, null, null, null));
        assertThrows(InvalidCredentialsException.class,
                () -> gradingService.gradeSubmission(null, 500, req));
        assertThrows(InvalidCredentialsException.class,
                () -> gradingService.publishQuizResults(null, 200));

        assertThrows(AccessDeniedException.class,
                () -> gradingService.getGradingQueue(student, 50, null, null));
        assertThrows(AccessDeniedException.class,
                () -> gradingService.gradeSubmission(student, 500, req));
        assertThrows(AccessDeniedException.class,
                () -> gradingService.publishAttemptResults(student, 300));
    }

    @Test
    @DisplayName("Admin bypasses active instructor account status check")
    void testValidateInstructor_AdminBypassesAccountStatusCheck() {
        User pendingAdmin = new User("Pending Admin", "padmin@olp.com", "hash", "admin", "pending");
        pendingAdmin.setId(2);
        when(courseRepository.findAll()).thenReturn(List.of(course));
        when(quizAttemptAnswerRepository.findDescriptiveGradingQueue(anySet(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(answer));

        List<DescriptiveSubmissionResponse> result = gradingService.getGradingQueue(pendingAdmin, null, null, null);
        assertEquals(1, result.size());
    }

    // =========================================================================
    // 7. Course Association, Deleted Course, Quiz Filtering & Pagination Boundaries
    // =========================================================================

    @Test
    @DisplayName("Co-instructor associated via CourseInstructorRepository can view queue, details, and grade")
    void testCoInstructor_AuthorizedAccess_Success() {
        CourseInstructor coAssoc = new CourseInstructor(course, otherInstructor, CourseInstructorRole.CO_INSTRUCTOR);
        when(courseRepository.findByInstructorIdOrderByIdAsc(20)).thenReturn(Collections.emptyList());
        when(courseInstructorRepository.findByInstructorId(20)).thenReturn(List.of(coAssoc));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(50, 20)).thenReturn(true);
        when(quizAttemptAnswerRepository.findDescriptiveGradingQueue(eq(Set.of(50)), isNull(), isNull(), isNull()))
                .thenReturn(List.of(answer));

        List<DescriptiveSubmissionResponse> queue = gradingService.getGradingQueue(otherInstructor, null, null, null);
        assertEquals(1, queue.size());

        GradeSubmissionResponse graded = gradingService.gradeSubmission(
                otherInstructor, 500, new GradeSubmissionRequest(BigDecimal.valueOf(14.0), "Co-instructor grade"));
        assertEquals(BigDecimal.valueOf(14.0), graded.getSubmission().getAwardedMarks());
        assertEquals(20, graded.getSubmission().getGraderId());
    }

    @Test
    @DisplayName("Instructor with no courses receives empty list and empty page without hitting answer repository")
    void testGetGradingQueue_NoCourses_ReturnsEmpty() {
        when(courseRepository.findByInstructorIdOrderByIdAsc(20)).thenReturn(Collections.emptyList());
        when(courseInstructorRepository.findByInstructorId(20)).thenReturn(Collections.emptyList());

        List<DescriptiveSubmissionResponse> listResult = gradingService.getGradingQueue(otherInstructor, null, null, null);
        assertTrue(listResult.isEmpty());

        PageResponse<DescriptiveSubmissionResponse> pagedResult =
                gradingService.getGradingQueuePaged(otherInstructor, null, null, null, 0, 10);
        assertTrue(pagedResult.getContent().isEmpty());
        assertEquals(0L, pagedResult.getTotalElements());
        verifyNoInteractions(quizAttemptAnswerRepository);
    }

    @Test
    @DisplayName("Deleted course or missing courseId throws ResourceNotFoundException; deleted excluded from unfiltered list")
    void testGetGradingQueue_DeletedAndMissingCourse() {
        when(courseRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> gradingService.getGradingQueue(instructor, 999, null, null));

        Course deletedCourse = new Course(instructor, "Old Course", "Deleted", CourseStatus.ARCHIVED);
        deletedCourse.setId(51);
        deletedCourse.setDeleted(true);
        when(courseRepository.findById(51)).thenReturn(Optional.of(deletedCourse));

        assertThrows(ResourceNotFoundException.class,
                () -> gradingService.getGradingQueue(instructor, 51, null, null));

        // When courseId is null, deleted courses are filtered out
        when(courseRepository.findByInstructorIdOrderByIdAsc(10)).thenReturn(List.of(course, deletedCourse));
        when(quizAttemptAnswerRepository.findDescriptiveGradingQueue(eq(Set.of(50)), isNull(), isNull(), isNull()))
                .thenReturn(List.of(answer));

        List<DescriptiveSubmissionResponse> result = gradingService.getGradingQueue(instructor, null, null, null);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Quiz filtering rejects non-existent quizId or quiz belonging to unassociated course")
    void testGetGradingQueue_QuizValidationErrors() {
        when(quizRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> gradingService.getGradingQueue(instructor, null, 999, null));

        Course otherCourse = new Course(otherInstructor, "Other Course", "Desc", CourseStatus.PUBLISHED);
        otherCourse.setId(99);
        Quiz otherQuiz = new Quiz(otherCourse, null, null, otherInstructor, "Other Quiz", "Desc", null, QuizStatus.PUBLISHED);
        otherQuiz.setId(299);
        when(quizRepository.findByIdWithDetails(299)).thenReturn(Optional.of(otherQuiz));

        assertThrows(AccessDeniedException.class,
                () -> gradingService.getGradingQueue(instructor, null, 299, null));
    }

    @Test
    @DisplayName("Status parsing supports aliases (ALL, PENDING, GRADED, PUBLISHED) and rejects invalid status")
    void testGetGradingQueue_StatusFiltersAndInvalidStatus() {
        when(quizAttemptAnswerRepository.findDescriptiveGradingQueue(eq(Set.of(50)), isNull(), isNull(), isNull()))
                .thenReturn(List.of());
        when(quizAttemptAnswerRepository.findDescriptiveGradingQueue(eq(Set.of(50)), isNull(), isNull(), eq(EvaluationStatus.MANUAL_GRADING_REQUIRED)))
                .thenReturn(List.of());
        when(quizAttemptAnswerRepository.findDescriptiveGradingQueue(eq(Set.of(50)), isNull(), isNull(), eq(EvaluationStatus.EVALUATED_UNPUBLISHED)))
                .thenReturn(List.of());
        when(quizAttemptAnswerRepository.findDescriptiveGradingQueue(eq(Set.of(50)), isNull(), isNull(), eq(EvaluationStatus.PUBLISHED)))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> gradingService.getGradingQueue(instructor, null, null, "   "));
        assertDoesNotThrow(() -> gradingService.getGradingQueue(instructor, null, null, "all"));
        assertDoesNotThrow(() -> gradingService.getGradingQueue(instructor, null, null, " pending "));
        assertDoesNotThrow(() -> gradingService.getGradingQueue(instructor, null, null, "GRADED"));
        assertDoesNotThrow(() -> gradingService.getGradingQueue(instructor, null, null, "published"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> gradingService.getGradingQueue(instructor, null, null, "NOT_A_STATUS"));
        assertTrue(ex.getMessage().contains("Invalid grading status"));
    }

    @Test
    @DisplayName("Paged grading queue clamps negative page to 0 and zero/negative size to 1")
    void testGetGradingQueuePaged_ClampsNegativePageAndZeroSize() {
        Page<QuizAttemptAnswer> page = new PageImpl<>(List.of(answer), PageRequest.of(0, 1), 1);
        when(quizAttemptAnswerRepository.findDescriptiveGradingQueuePaged(
                eq(Set.of(50)), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        gradingService.getGradingQueuePaged(instructor, null, null, null, -5, 0);

        verify(quizAttemptAnswerRepository).findDescriptiveGradingQueuePaged(
                eq(Set.of(50)), isNull(), isNull(), isNull(),
                argThat(p -> p.getPageNumber() == 0 && p.getPageSize() == 1));
    }

    @Test
    @DisplayName("getSubmissionDetails returns full DTO details and validates null/missing/unassociated/corrupted records")
    void testGetSubmissionDetails_SuccessAndErrors() {
        DescriptiveSubmissionResponse dto = gradingService.getSubmissionDetails(instructor, 500);
        assertEquals(500, dto.getAnswerId());
        assertEquals("alice@olp.com", dto.getStudentEmail());
        assertEquals("Midterm Exam", dto.getQuizTitle());
        assertEquals("Advanced Algorithms", dto.getCourseTitle());
        assertEquals("Rubric detail", dto.getGradingRubric());
        assertFalse(dto.isGraded());
        assertFalse(dto.isPublished());

        assertThrows(IllegalArgumentException.class,
                () -> gradingService.getSubmissionDetails(instructor, null));

        when(quizAttemptAnswerRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> gradingService.getSubmissionDetails(instructor, 999));

        assertThrows(AccessDeniedException.class,
                () -> gradingService.getSubmissionDetails(otherInstructor, 500));

        // Corrupted relationship without attempt
        QuizAttemptAnswer corrupted = new QuizAttemptAnswer();
        corrupted.setId(599);
        when(quizAttemptAnswerRepository.findByIdWithDetails(599)).thenReturn(Optional.of(corrupted));
        assertThrows(IllegalStateException.class,
                () -> gradingService.getSubmissionDetails(instructor, 599));
    }

    @Test
    @DisplayName("Service works cleanly when optional CourseInstructorRepository is null")
    void testNullCourseInstructorRepository_FallbackWorks() {
        DescriptiveGradingService fallbackService = new DescriptiveGradingService(
                quizAttemptAnswerRepository, quizAttemptRepository, quizRepository,
                courseRepository, null, userRepository);

        DescriptiveSubmissionResponse details = fallbackService.getSubmissionDetails(instructor, 500);
        assertEquals(500, details.getAnswerId());

        assertThrows(AccessDeniedException.class,
                () -> fallbackService.getSubmissionDetails(otherInstructor, 500));
    }

    // =========================================================================
    // 8. Grading Bounds, Boundary Values & Re-grading Edge Cases
    // =========================================================================

    @Test
    @DisplayName("Boundary marks: 0.0 and exact maxMarks (20.0) succeed; -0.01 and 20.01 fail")
    void testGradeSubmission_ExactBoundaries() {
        GradeSubmissionResponse zeroRes = gradingService.gradeSubmission(
                instructor, 500, new GradeSubmissionRequest(BigDecimal.ZERO, "No relevant content"));
        assertEquals(BigDecimal.ZERO, zeroRes.getSubmission().getAwardedMarks());
        assertTrue(zeroRes.getSubmission().isGraded());
        assertFalse(zeroRes.getSubmission().isPublished());

        GradeSubmissionResponse maxRes = gradingService.gradeSubmission(
                instructor, 500, new GradeSubmissionRequest(BigDecimal.valueOf(20.0), "Full credit"));
        assertEquals(BigDecimal.valueOf(20.0), maxRes.getSubmission().getAwardedMarks());

        assertThrows(IllegalArgumentException.class,
                () -> gradingService.gradeSubmission(instructor, 500,
                        new GradeSubmissionRequest(new BigDecimal("-0.01"), "Just below zero")));

        assertThrows(IllegalArgumentException.class,
                () -> gradingService.gradeSubmission(instructor, 500,
                        new GradeSubmissionRequest(new BigDecimal("20.01"), "Just above max")));
    }

    @Test
    @DisplayName("Null maxMarks on answer defaults to zero during validation")
    void testGradeSubmission_NullMaxMarksDefaultsToZero() {
        answer.setMaxMarks(null);

        GradeSubmissionResponse zeroRes = gradingService.gradeSubmission(
                instructor, 500, new GradeSubmissionRequest(BigDecimal.ZERO, "Zero max"));
        assertEquals(BigDecimal.ZERO, zeroRes.getSubmission().getAwardedMarks());

        assertThrows(IllegalArgumentException.class,
                () -> gradingService.gradeSubmission(instructor, 500,
                        new GradeSubmissionRequest(BigDecimal.ONE, "Exceeds default zero")));
    }

    @Test
    @DisplayName("Re-grading an EVALUATED_UNPUBLISHED answer updates marks and feedback before publish")
    void testGradeSubmission_RegradeBeforePublish_Success() {
        gradingService.gradeSubmission(instructor, 500,
                new GradeSubmissionRequest(BigDecimal.valueOf(12.0), "Initial pass"));

        GradeSubmissionResponse updated = gradingService.gradeSubmission(otherInstructorOrAdmin(admin), 500,
                new GradeSubmissionRequest(BigDecimal.valueOf(16.5), "Revised evaluation"));

        assertEquals(BigDecimal.valueOf(16.5), updated.getSubmission().getAwardedMarks());
        assertEquals("Revised evaluation", updated.getSubmission().getFeedback());
        assertEquals("EVALUATED_UNPUBLISHED", updated.getSubmission().getEvaluationStatus());
        assertEquals(1, updated.getSubmission().getGraderId());
    }

    private User otherInstructorOrAdmin(User user) {
        return user;
    }

    @Test
    @DisplayName("Status='PUBLISHED' string triggers immediate publication even when publish boolean is false")
    void testGradeSubmission_StatusStringPublished_TriggersPublish() {
        GradeSubmissionRequest req = new GradeSubmissionRequest();
        req.setAwardedMarks(BigDecimal.valueOf(19.0));
        req.setFeedback("Excellent");
        req.setPublish(false);
        req.setStatus("published");
        when(quizAttemptAnswerRepository.findByAttemptId(300)).thenReturn(List.of(answer));

        GradeSubmissionResponse res = gradingService.gradeSubmission(instructor, 500, req);

        assertEquals("PUBLISHED", res.getSubmission().getEvaluationStatus());
        assertTrue(res.getSubmission().isPublished());
        assertEquals(BigDecimal.valueOf(19.0), attempt.getObtainedMarks());
        assertTrue(attempt.isPublished());
    }

    @Test
    @DisplayName("gradeSubmission throws on null answerId, null request, or missing answer")
    void testGradeSubmission_NullAndNotFoundErrors() {
        GradeSubmissionRequest validReq = new GradeSubmissionRequest(BigDecimal.TEN, "OK");

        assertThrows(IllegalArgumentException.class,
                () -> gradingService.gradeSubmission(instructor, null, validReq));
        assertThrows(IllegalArgumentException.class,
                () -> gradingService.gradeSubmission(instructor, 500, null));

        when(quizAttemptAnswerRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> gradingService.gradeSubmission(instructor, 999, validReq));
    }

    // =========================================================================
    // 9. Publishing & Attempt Score Recalculation Edge Cases
    // =========================================================================

    @Test
    @DisplayName("publishAnswerResult rejects missing answer or unassociated instructor")
    void testPublishAnswerResult_Errors() {
        when(quizAttemptAnswerRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> gradingService.publishAnswerResult(instructor, 999));

        assertThrows(AccessDeniedException.class,
                () -> gradingService.publishAnswerResult(otherInstructor, 500));
    }

    @Test
    @DisplayName("publishQuizResults handles null, missing quiz, unassociated instructor, and 0 unpublished submissions")
    void testPublishQuizResults_EdgeAndNegativeCases() {
        assertThrows(IllegalArgumentException.class,
                () -> gradingService.publishQuizResults(instructor, null));

        when(quizRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> gradingService.publishQuizResults(instructor, 999));

        assertThrows(AccessDeniedException.class,
                () -> gradingService.publishQuizResults(otherInstructor, 200));

        when(quizAttemptAnswerRepository.findEvaluatedUnpublishedByQuizId(200)).thenReturn(Collections.emptyList());
        PublishResultResponse emptyRes = gradingService.publishQuizResults(instructor, 200);
        assertEquals(0, emptyRes.getPublishedCount());
        verify(quizAttemptRepository, never()).save(any());
    }

    @Test
    @DisplayName("publishAttemptResults validates null, missing attempt, and unassociated instructor")
    void testPublishAttemptResults_EdgeAndNegativeCases() {
        assertThrows(IllegalArgumentException.class,
                () -> gradingService.publishAttemptResults(instructor, null));

        when(quizAttemptRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> gradingService.publishAttemptResults(instructor, 999));

        when(quizAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));
        assertThrows(AccessDeniedException.class,
                () -> gradingService.publishAttemptResults(otherInstructor, 300));
    }

    @Test
    @DisplayName("recalculateAttemptScore handles null attempt, empty answers, PARTIALLY_CORRECT, and auto-publishing attempt")
    void testRecalculateAttemptScore_ComprehensiveCases() {
        // Safe null check
        assertDoesNotThrow(() -> gradingService.recalculateAttemptScore(null));

        // Empty answers sets score to 0 and keeps isPublished false
        attempt.setIsPublished(false);
        when(quizAttemptAnswerRepository.findByAttemptId(300)).thenReturn(Collections.emptyList());
        gradingService.recalculateAttemptScore(attempt);
        assertEquals(BigDecimal.ZERO, attempt.getObtainedMarks());
        assertFalse(attempt.isPublished());

        // Pending MANUAL_GRADING_REQUIRED keeps isPublished false while summing PARTIALLY_CORRECT
        QuizAttemptAnswer partialAns = new QuizAttemptAnswer(attempt, question, "Partial",
                BigDecimal.valueOf(5.5), BigDecimal.valueOf(10.0), EvaluationStatus.PARTIALLY_CORRECT, null, LocalDateTime.now());
        QuizAttemptAnswer manualAns = new QuizAttemptAnswer(attempt, question, "Pending",
                null, BigDecimal.valueOf(10.0), EvaluationStatus.MANUAL_GRADING_REQUIRED, null, LocalDateTime.now());
        when(quizAttemptAnswerRepository.findByAttemptId(300)).thenReturn(List.of(partialAns, manualAns));

        gradingService.recalculateAttemptScore(attempt);
        assertEquals(BigDecimal.valueOf(5.5), attempt.getObtainedMarks());
        assertFalse(attempt.isPublished());

        // When pending answer is published with null marks, total remains 5.5 and attempt becomes published
        manualAns.setEvaluationStatus(EvaluationStatus.PUBLISHED);
        gradingService.recalculateAttemptScore(attempt);
        assertEquals(BigDecimal.valueOf(5.5), attempt.getObtainedMarks());
        assertTrue(attempt.isPublished());
    }

    // =========================================================================
    // 10. FR-I31 Feedback Validation, Editing, Publish & Visibility Edge Cases
    // =========================================================================

    @Test
    @DisplayName("addOrUpdateAnswerFeedback edits prior feedback, preserves evaluatedAt, publishes when requested, and handles errors")
    void testAddOrUpdateAnswerFeedback_ComprehensiveEdgeCases() {
        assertThrows(IllegalArgumentException.class,
                () -> gradingService.addOrUpdateAnswerFeedback(instructor, null, new AnswerFeedbackRequest("Text")));
        assertThrows(IllegalArgumentException.class,
                () -> gradingService.addOrUpdateAnswerFeedback(instructor, 500, null));

        when(quizAttemptAnswerRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> gradingService.addOrUpdateAnswerFeedback(instructor, 999, new AnswerFeedbackRequest("Text")));

        assertThrows(AccessDeniedException.class,
                () -> gradingService.addOrUpdateAnswerFeedback(otherInstructor, 500, new AnswerFeedbackRequest("Text")));

        // First edit: evaluatedAt was null -> gets populated
        answer.setEvaluatedAt(null);
        answer.setEvaluationStatus(EvaluationStatus.EVALUATED_UNPUBLISHED);
        AnswerFeedbackResponse first = gradingService.addOrUpdateAnswerFeedback(
                instructor, 500, new AnswerFeedbackRequest("First draft feedback", false));
        assertNotNull(answer.getEvaluatedAt());
        LocalDateTime originalEvalTime = answer.getEvaluatedAt();
        assertEquals("First draft feedback", first.getFeedback());
        assertFalse(first.isPublished());

        // Second edit before publish + immediate publish=true
        answer.setAwardedMarks(BigDecimal.valueOf(18.0));
        when(quizAttemptAnswerRepository.findByAttemptId(300)).thenReturn(List.of(answer));
        AnswerFeedbackResponse published = gradingService.addOrUpdateAnswerFeedback(
                instructor, 500, new AnswerFeedbackRequest("Final published feedback", true));

        assertEquals("Final published feedback", published.getFeedback());
        assertTrue(published.isPublished());
        assertEquals("PUBLISHED", published.getEvaluationStatus());
        assertEquals(originalEvalTime, answer.getEvaluatedAt());
        assertEquals(BigDecimal.valueOf(18.0), attempt.getObtainedMarks());
    }

    @Test
    @DisplayName("addOrUpdateSubmissionFeedback edits before publish, publishes when requested, and validates inputs/auth")
    void testAddOrUpdateSubmissionFeedback_ComprehensiveEdgeCases() {
        assertThrows(IllegalArgumentException.class,
                () -> gradingService.addOrUpdateSubmissionFeedback(instructor, null, new SubmissionFeedbackRequest("Text")));
        assertThrows(IllegalArgumentException.class,
                () -> gradingService.addOrUpdateSubmissionFeedback(instructor, 300, null));

        when(quizAttemptRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> gradingService.addOrUpdateSubmissionFeedback(instructor, 999, new SubmissionFeedbackRequest("Text")));

        when(quizAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));
        assertThrows(AccessDeniedException.class,
                () -> gradingService.addOrUpdateSubmissionFeedback(otherInstructor, 300, new SubmissionFeedbackRequest("Text")));

        // Update before publish then publish on second edit
        attempt.setIsPublished(false);
        SubmissionFeedbackResponse draft = gradingService.addOrUpdateSubmissionFeedback(
                instructor, 300, new SubmissionFeedbackRequest("Draft overall", false));
        assertFalse(draft.isPublished());

        SubmissionFeedbackResponse finalRes = gradingService.addOrUpdateSubmissionFeedback(
                instructor, 300, new SubmissionFeedbackRequest("Final overall", true));
        assertEquals("Final overall", finalRes.getOverallFeedback());
        assertTrue(finalRes.isPublished());
        assertTrue(attempt.isPublished());
    }

    @Test
    @DisplayName("getSubmissionFeedback validates null/missing, blocks other student/unassociated instructor, allows instructor & null feedbackReleased")
    void testGetSubmissionFeedback_VisibilityAndAuthEdgeCases() {
        assertThrows(InvalidCredentialsException.class,
                () -> gradingService.getSubmissionFeedback(null, 300));
        assertThrows(IllegalArgumentException.class,
                () -> gradingService.getSubmissionFeedback(instructor, null));

        when(quizAttemptRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> gradingService.getSubmissionFeedback(instructor, 999));

        attempt.setOverallFeedback("Confidential draft");
        attempt.setIsPublished(false);
        quiz.setFeedbackReleased(false);
        when(quizAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));

        // Another student denied (not owner, not instructor)
        User otherStudent = new User("Bob", "bob@olp.com", "hash", "student", "active");
        otherStudent.setId(101);
        assertThrows(AccessDeniedException.class,
                () -> gradingService.getSubmissionFeedback(otherStudent, 300));

        // Unassociated instructor denied
        assertThrows(AccessDeniedException.class,
                () -> gradingService.getSubmissionFeedback(otherInstructor, 300));

        // Course instructor and admin can inspect feedback even when unpublished and feedbackReleased=false
        SubmissionFeedbackResponse instView = gradingService.getSubmissionFeedback(instructor, 300);
        assertEquals("Confidential draft", instView.getOverallFeedback());
        assertFalse(instView.isPublished());

        SubmissionFeedbackResponse adminView = gradingService.getSubmissionFeedback(admin, 300);
        assertEquals("Confidential draft", adminView.getOverallFeedback());

        // Student can view when published and feedbackReleased is null (default enabled)
        quiz.setFeedbackReleased(null);
        attempt.setIsPublished(true);
        SubmissionFeedbackResponse studentView = gradingService.getSubmissionFeedback(student, 300);
        assertEquals("Confidential draft", studentView.getOverallFeedback());
        assertTrue(studentView.isPublished());
    }
}
