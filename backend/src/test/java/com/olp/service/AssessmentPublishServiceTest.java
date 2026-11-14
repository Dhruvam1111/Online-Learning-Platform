package com.olp.service;

import com.olp.dto.AssessmentPublishStatusResponse;
import com.olp.dto.PublishAssessmentResultsRequest;
import com.olp.dto.WithholdAssessmentResultsRequest;
import com.olp.entity.*;
import com.olp.event.AssessmentResultsPublishedEvent;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit and branch coverage tests for AssessmentPublishService (FR-I34, Step 51).
 */
@ExtendWith(MockitoExtension.class)
class AssessmentPublishServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private QuizAttemptAnswerRepository quizAttemptAnswerRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AssessmentPublishService service;

    private User instructor;
    private User collaborator;
    private User admin;
    private User student;
    private Course course;
    private Exam exam;
    private Assignment assignment;
    private Quiz quiz;
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        service.setClock(fixedClock);

        instructor = new User();
        instructor.setId(10);
        instructor.setEmail("instructor@example.com");
        instructor.setName("Instructor Jane");
        instructor.setRole("INSTRUCTOR");

        collaborator = new User();
        collaborator.setId(11);
        collaborator.setEmail("collab@example.com");
        collaborator.setName("Instructor Bob");
        collaborator.setRole("INSTRUCTOR");

        admin = new User();
        admin.setId(1);
        admin.setEmail("admin@example.com");
        admin.setName("Admin User");
        admin.setRole("ADMIN");

        student = new User();
        student.setId(20);
        student.setEmail("student@example.com");
        student.setName("Student Alice");
        student.setRole("STUDENT");

        course = new Course();
        course.setId(101);
        course.setTitle("Software Engineering");
        course.setInstructor(instructor);

        exam = new Exam();
        exam.setId(100);
        exam.setTitle("Midterm Exam");
        exam.setCourse(course);
        exam.setResultsPublished(false);
        exam.setResultsWithheld(false);

        assignment = new Assignment();
        assignment.setId(200);
        assignment.setTitle("Project Milestone");
        assignment.setCourse(course);
        assignment.setResultsPublished(false);
        assignment.setResultsWithheld(false);

        quiz = new Quiz();
        quiz.setId(300);
        quiz.setTitle("Sprint Quiz");
        quiz.setCourse(course);
        quiz.setResultsPublished(false);
        quiz.setResultsWithheld(false);
    }

    // =========================================================================
    // 1. USER RESOLUTION & PARSING TESTS
    // =========================================================================

    @Test
    @DisplayName("resolveUser succeeds with User principal")
    void testResolveUser_WithUserPrincipal() {
        when(authentication.getPrincipal()).thenReturn(instructor);
        User result = service.resolveUser(authentication);
        assertEquals(instructor, result);
    }

    @Test
    @DisplayName("resolveUser succeeds with email string principal")
    void testResolveUser_WithEmailPrincipal() {
        when(authentication.getPrincipal()).thenReturn("instructor@example.com");
        when(userRepository.findByEmail("instructor@example.com")).thenReturn(Optional.of(instructor));

        User result = service.resolveUser(authentication);
        assertEquals(instructor, result);
    }

    @Test
    @DisplayName("resolveUser throws when user email not found")
    void testResolveUser_WithUnknownEmailThrows() {
        when(authentication.getPrincipal()).thenReturn("unknown@example.com");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(authentication));
    }

    @Test
    @DisplayName("resolveUser throws when authentication is null")
    void testResolveUser_NullAuthThrows() {
        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(null));
    }

    @Test
    @DisplayName("resolveUser throws when principal is unexpected type")
    void testResolveUser_UnexpectedPrincipalThrows() {
        when(authentication.getPrincipal()).thenReturn(12345);
        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(authentication));
    }

    @Test
    @DisplayName("parseAssessmentType parses valid strings case-insensitively")
    void testParseAssessmentType_Valid() {
        assertEquals(AssessmentType.EXAM, service.parseAssessmentType("exam"));
        assertEquals(AssessmentType.EXAM, service.parseAssessmentType("EXAM"));
        assertEquals(AssessmentType.ASSIGNMENT, service.parseAssessmentType("assignment"));
        assertEquals(AssessmentType.QUIZ, service.parseAssessmentType("quiz"));
    }

    @Test
    @DisplayName("parseAssessmentType throws on invalid or blank type")
    void testParseAssessmentType_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> service.parseAssessmentType(null));
        assertThrows(IllegalArgumentException.class, () -> service.parseAssessmentType(""));
        assertThrows(IllegalArgumentException.class, () -> service.parseAssessmentType("invalid_type"));
    }

    // =========================================================================
    // 2. AUTHORIZATION & GET PUBLISH STATUS TESTS
    // =========================================================================

    @Test
    @DisplayName("getPublishStatus succeeds for Exam with owner instructor")
    void testGetPublishStatus_Exam_Owner() {
        when(examRepository.findById(100)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.countByExamId(100)).thenReturn(10L);
        when(examAttemptRepository.countUngradedByExamId(100)).thenReturn(2L);
        when(examAttemptRepository.countGradedByExamId(100)).thenReturn(8L);

        AssessmentPublishStatusResponse status = service.getPublishStatus(instructor, AssessmentType.EXAM, 100);

        assertNotNull(status);
        assertEquals(AssessmentType.EXAM, status.getAssessmentType());
        assertEquals(100, status.getAssessmentId());
        assertEquals("Midterm Exam", status.getAssessmentTitle());
        assertEquals(10, status.getTotalSubmissions());
        assertEquals(8, status.getGradedCount());
        assertEquals(2, status.getUngradedCount());
        assertFalse(status.getResultsPublished());
        assertFalse(status.getResultsWithheld());
        assertFalse(status.getIsLocked());
    }

    @Test
    @DisplayName("getPublishStatus succeeds for Assignment with collaborator instructor")
    void testGetPublishStatus_Assignment_Collaborator() {
        when(assignmentRepository.findById(200)).thenReturn(Optional.of(assignment));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(101, 11)).thenReturn(true);
        when(assignmentSubmissionRepository.countByAssignmentId(200)).thenReturn(15L);
        when(assignmentSubmissionRepository.countUngradedByAssignmentId(200)).thenReturn(0L);
        when(assignmentSubmissionRepository.countGradedByAssignmentId(200)).thenReturn(15L);

        AssessmentPublishStatusResponse status = service.getPublishStatus(collaborator, AssessmentType.ASSIGNMENT, 200);

        assertNotNull(status);
        assertEquals(AssessmentType.ASSIGNMENT, status.getAssessmentType());
        assertEquals(15, status.getTotalSubmissions());
        assertEquals(0, status.getUngradedCount());
    }

    @Test
    @DisplayName("getPublishStatus succeeds for Quiz with Admin user")
    void testGetPublishStatus_Quiz_Admin() {
        when(quizRepository.findById(300)).thenReturn(Optional.of(quiz));
        when(quizAttemptRepository.countByQuizId(300)).thenReturn(5L);
        when(quizAttemptAnswerRepository.countPendingDescriptiveByQuizId(300)).thenReturn(1L);

        AssessmentPublishStatusResponse status = service.getPublishStatus(admin, AssessmentType.QUIZ, 300);

        assertNotNull(status);
        assertEquals(AssessmentType.QUIZ, status.getAssessmentType());
        assertEquals(5, status.getTotalSubmissions());
        assertEquals(1, status.getUngradedCount());
        assertEquals(4, status.getGradedCount());
    }

    @Test
    @DisplayName("getPublishStatus throws AccessDeniedException for unauthorized user")
    void testGetPublishStatus_UnauthorizedThrows() {
        when(examRepository.findById(100)).thenReturn(Optional.of(exam));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(101, 20)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.getPublishStatus(student, AssessmentType.EXAM, 100));
    }

    @Test
    @DisplayName("getPublishStatus throws ResourceNotFoundException when assessment not found")
    void testGetPublishStatus_NotFoundThrows() {
        when(examRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getPublishStatus(instructor, AssessmentType.EXAM, 999));
    }

    @Test
    @DisplayName("getPublishStatus throws on null arguments")
    void testGetPublishStatus_NullArgsThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.getPublishStatus(instructor, null, 100));
        assertThrows(IllegalArgumentException.class, () -> service.getPublishStatus(instructor, AssessmentType.EXAM, null));
    }

    // =========================================================================
    // 3. PUBLISH RESULTS GATE & LOCK TESTS (DR-01)
    // =========================================================================

    @Test
    @DisplayName("publishResults throws 409 IllegalStateException when ungraded items remain without confirmation")
    void testPublishResults_UngradedUnconfirmedThrows() {
        when(examRepository.findById(100)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.countByExamId(100)).thenReturn(10L);
        when(examAttemptRepository.countUngradedByExamId(100)).thenReturn(3L);

        PublishAssessmentResultsRequest req = new PublishAssessmentResultsRequest(false, true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.publishResults(instructor, AssessmentType.EXAM, 100, req));
        assertTrue(ex.getMessage().contains("3 ungraded submissions remain"));
        verify(examRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("publishResults succeeds when confirmed despite ungraded items, locks attempts and dispatches event")
    void testPublishResults_ConfirmedUngraded_SucceedsAndLocks() {
        when(examRepository.findById(100)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.countByExamId(100)).thenReturn(2L);
        when(examAttemptRepository.countUngradedByExamId(100)).thenReturn(1L);
        when(examAttemptRepository.countGradedByExamId(100)).thenReturn(1L);

        ExamAttempt att1 = new ExamAttempt(exam, student, 1, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        att1.setId(501);
        ExamAttempt att2 = new ExamAttempt(exam, instructor, 1, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        att2.setId(502);
        when(examAttemptRepository.findByExamId(100)).thenReturn(List.of(att1, att2));

        PublishAssessmentResultsRequest req = new PublishAssessmentResultsRequest(true, true);
        AssessmentPublishStatusResponse response = service.publishResults(instructor, AssessmentType.EXAM, 100, req);

        assertTrue(response.getResultsPublished());
        assertFalse(response.getResultsWithheld());
        assertTrue(response.getIsLocked());
        assertEquals(2, response.getStudentsNotifiedCount());

        // Verify entities locked and published
        assertTrue(exam.getResultsPublished());
        assertFalse(exam.getResultsWithheld());
        assertTrue(att1.getIsPublished());
        assertTrue(att1.getIsLocked());
        assertTrue(att2.getIsPublished());
        assertTrue(att2.getIsLocked());

        verify(examRepository).save(exam);
        verify(examAttemptRepository).saveAll(any());
        verify(eventPublisher).publishEvent(any(AssessmentResultsPublishedEvent.class));
    }

    @Test
    @DisplayName("publishResults for Assignment locks submissions and respects notifyStudents=false")
    void testPublishResults_Assignment_NoNotification() {
        when(assignmentRepository.findById(200)).thenReturn(Optional.of(assignment));
        when(assignmentSubmissionRepository.countByAssignmentId(200)).thenReturn(1L);
        when(assignmentSubmissionRepository.countUngradedByAssignmentId(200)).thenReturn(0L);
        when(assignmentSubmissionRepository.countGradedByAssignmentId(200)).thenReturn(1L);

        AssignmentSubmission sub = new AssignmentSubmission();
        sub.setId(601);
        sub.setAssignment(assignment);
        sub.setStudent(student);
        when(assignmentSubmissionRepository.findByAssignmentId(200)).thenReturn(List.of(sub));

        PublishAssessmentResultsRequest req = new PublishAssessmentResultsRequest(false, false);
        AssessmentPublishStatusResponse response = service.publishResults(instructor, AssessmentType.ASSIGNMENT, 200, req);

        assertTrue(response.getResultsPublished());
        assertTrue(response.getIsLocked());
        assertTrue(sub.getIsPublished());
        assertTrue(sub.getIsLocked());

        verify(assignmentRepository).save(assignment);
        verify(assignmentSubmissionRepository).saveAll(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("publishResults for Quiz publishes unpublished descriptive answers and locks attempts")
    void testPublishResults_Quiz_PublishesDescriptiveAnswers() {
        when(quizRepository.findById(300)).thenReturn(Optional.of(quiz));
        when(quizAttemptRepository.countByQuizId(300)).thenReturn(1L);
        when(quizAttemptAnswerRepository.countPendingDescriptiveByQuizId(300)).thenReturn(0L);

        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(701);
        attempt.setQuiz(quiz);
        attempt.setStudent(student);
        when(quizAttemptRepository.findByQuizId(300)).thenReturn(List.of(attempt));

        QuizAttemptAnswer answer = new QuizAttemptAnswer();
        answer.setId(801);
        answer.setEvaluationStatus(EvaluationStatus.EVALUATED_UNPUBLISHED);
        when(quizAttemptAnswerRepository.findEvaluatedUnpublishedByQuizId(300)).thenReturn(List.of(answer));

        PublishAssessmentResultsRequest req = new PublishAssessmentResultsRequest(false, true);
        AssessmentPublishStatusResponse response = service.publishResults(instructor, AssessmentType.QUIZ, 300, req);

        assertTrue(response.getResultsPublished());
        assertTrue(attempt.getIsPublished());
        assertTrue(attempt.getIsLocked());
        assertEquals(EvaluationStatus.PUBLISHED, answer.getEvaluationStatus());

        verify(quizRepository).save(quiz);
        verify(quizAttemptRepository).saveAll(any());
        verify(quizAttemptAnswerRepository).saveAll(any());
        verify(eventPublisher).publishEvent(any(AssessmentResultsPublishedEvent.class));
    }

    // =========================================================================
    // 4. WITHHOLD RESULTS TESTS
    // =========================================================================

    @Test
    @DisplayName("withholdResults for Exam hides marks by setting isPublished=false on attempts")
    void testWithholdResults_Exam() {
        exam.setResultsPublished(true);
        when(examRepository.findById(100)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.countByExamId(100)).thenReturn(1L);

        ExamAttempt attempt = new ExamAttempt(exam, student, 1, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        attempt.setIsPublished(true);
        when(examAttemptRepository.findByExamId(100)).thenReturn(List.of(attempt));

        AssessmentPublishStatusResponse response = service.withholdResults(instructor, AssessmentType.EXAM, 100, null);

        assertTrue(response.getResultsWithheld());
        assertFalse(response.getResultsPublished());
        assertTrue(exam.getResultsWithheld());
        assertFalse(exam.getResultsPublished());
        assertFalse(attempt.getIsPublished());

        verify(examRepository).save(exam);
        verify(examAttemptRepository).saveAll(any());
    }

    @Test
    @DisplayName("withholdResults for Assignment hides marks on submissions")
    void testWithholdResults_Assignment() {
        when(assignmentRepository.findById(200)).thenReturn(Optional.of(assignment));
        when(assignmentSubmissionRepository.countByAssignmentId(200)).thenReturn(1L);

        AssignmentSubmission sub = new AssignmentSubmission();
        sub.setIsPublished(true);
        when(assignmentSubmissionRepository.findByAssignmentId(200)).thenReturn(List.of(sub));

        AssessmentPublishStatusResponse response = service.withholdResults(
                instructor, AssessmentType.ASSIGNMENT, 200, new WithholdAssessmentResultsRequest("Moderation"));

        assertTrue(response.getResultsWithheld());
        assertFalse(response.getResultsPublished());
        assertFalse(sub.getIsPublished());
        verify(assignmentRepository).save(assignment);
        verify(assignmentSubmissionRepository).saveAll(any());
    }

    @Test
    @DisplayName("withholdResults for Quiz hides marks on quiz attempts")
    void testWithholdResults_Quiz() {
        when(quizRepository.findById(300)).thenReturn(Optional.of(quiz));
        when(quizAttemptRepository.countByQuizId(300)).thenReturn(1L);

        QuizAttempt attempt = new QuizAttempt();
        attempt.setIsPublished(true);
        when(quizAttemptRepository.findByQuizId(300)).thenReturn(List.of(attempt));

        AssessmentPublishStatusResponse response = service.withholdResults(instructor, AssessmentType.QUIZ, 300, null);

        assertTrue(response.getResultsWithheld());
        assertFalse(response.getResultsPublished());
        assertFalse(attempt.getIsPublished());
        verify(quizRepository).save(quiz);
        verify(quizAttemptRepository).saveAll(any());
    }

    // =========================================================================
    // 5. ENFORCE RESULT LOCK TESTS (DR-01)
    // =========================================================================

    @Test
    @DisplayName("enforceResultLock throws 409 IllegalStateException if exam attempt is locked")
    void testEnforceResultLock_ExamLockedThrows() {
        ExamAttempt attempt = new ExamAttempt(exam, student, 1, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        attempt.setIsLocked(true);
        when(examAttemptRepository.findById(501)).thenReturn(Optional.of(attempt));

        assertThrows(IllegalStateException.class, () -> service.enforceResultLock(AssessmentType.EXAM, 501));
    }

    @Test
    @DisplayName("enforceResultLock throws 409 IllegalStateException if assignment submission is locked")
    void testEnforceResultLock_AssignmentLockedThrows() {
        AssignmentSubmission sub = new AssignmentSubmission();
        sub.setAssignment(assignment);
        sub.setIsLocked(true);
        when(assignmentSubmissionRepository.findById(601)).thenReturn(Optional.of(sub));

        assertThrows(IllegalStateException.class, () -> service.enforceResultLock(AssessmentType.ASSIGNMENT, 601));
    }

    @Test
    @DisplayName("enforceResultLock throws 409 IllegalStateException if quiz attempt is locked")
    void testEnforceResultLock_QuizLockedThrows() {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setIsLocked(true);
        when(quizAttemptRepository.findById(701)).thenReturn(Optional.of(attempt));

        assertThrows(IllegalStateException.class, () -> service.enforceResultLock(AssessmentType.QUIZ, 701));
    }

    @Test
    @DisplayName("enforceResultLock does not throw when attempt is unlocked or null")
    void testEnforceResultLock_UnlockedDoesNotThrow() {
        ExamAttempt attempt = new ExamAttempt(exam, student, 1, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        attempt.setIsLocked(false);
        when(examAttemptRepository.findById(501)).thenReturn(Optional.of(attempt));

        assertDoesNotThrow(() -> service.enforceResultLock(AssessmentType.EXAM, 501));
        assertDoesNotThrow(() -> service.enforceResultLock(null, 501));
        assertDoesNotThrow(() -> service.enforceResultLock(AssessmentType.EXAM, null));
    }
}
