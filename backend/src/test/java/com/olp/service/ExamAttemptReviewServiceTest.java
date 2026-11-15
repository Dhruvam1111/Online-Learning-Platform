package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.event.ExamAttemptEscalatedEvent;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit and branch coverage tests for ExamAttemptReviewService (FR-I69, Step 56).
 * Verifies append-only history, course instructor RBAC, and DR-07 mark preservation.
 */
@ExtendWith(MockitoExtension.class)
class ExamAttemptReviewServiceTest {

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private ExamAttemptReviewHistoryRepository reviewHistoryRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ExamAttemptReviewService service;

    private User ownerInstructor;
    private User collaboratorInstructor;
    private User admin;
    private User student;
    private Course course;
    private Exam exam;
    private ExamAttempt attempt;
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-14T14:00:00Z"), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        service.setClock(fixedClock);

        ownerInstructor = new User();
        ownerInstructor.setId(10);
        ownerInstructor.setEmail("owner@example.com");
        ownerInstructor.setName("Dr. Owner");
        ownerInstructor.setRole("INSTRUCTOR");

        collaboratorInstructor = new User();
        collaboratorInstructor.setId(11);
        collaboratorInstructor.setEmail("collab@example.com");
        collaboratorInstructor.setName("Dr. Collab");
        collaboratorInstructor.setRole("INSTRUCTOR");

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
        course.setId(100);
        course.setTitle("Distributed Systems");
        course.setInstructor(ownerInstructor);

        exam = new Exam();
        exam.setId(200);
        exam.setTitle("Midterm Examination");
        exam.setCourse(course);

        attempt = new ExamAttempt(exam, student, 1, LocalDateTime.now(fixedClock), LocalDateTime.now(fixedClock).plusHours(2));
        attempt.setId(300);
        attempt.setTabSwitchCount(4);
        attempt.setFullscreenExitCount(2);
        attempt.setMonitoringFlags("EXCEEDED_TAB_LIMIT");
        attempt.setObtainedMarks(new BigDecimal("78.50"));
        attempt.setTotalMarks(new BigDecimal("100.00"));
        attempt.setReviewStatus(ExamAttemptReviewStatus.NONE);
    }

    // =========================================================================
    // 1. USER RESOLUTION & AUTHORIZATION TESTS
    // =========================================================================

    @Test
    @DisplayName("resolveUser resolves User principal, email string, and throws on invalid")
    void testResolveUser() {
        when(authentication.getPrincipal()).thenReturn(ownerInstructor);
        assertEquals(ownerInstructor, service.resolveUser(authentication));

        when(authentication.getPrincipal()).thenReturn("owner@example.com");
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerInstructor));
        assertEquals(ownerInstructor, service.resolveUser(authentication));

        when(authentication.getPrincipal()).thenReturn("missing@example.com");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(authentication));

        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(null));

        when(authentication.getPrincipal()).thenReturn(9999);
        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(authentication));
    }

    @Test
    @DisplayName("checkCourseAccess allows owner, collaborator, and admin, rejects unauthorized")
    void testCheckCourseAccess() {
        assertDoesNotThrow(() -> service.checkCourseAccess(ownerInstructor, course));
        assertDoesNotThrow(() -> service.checkCourseAccess(admin, course));

        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 11)).thenReturn(true);
        assertDoesNotThrow(() -> service.checkCourseAccess(collaboratorInstructor, course));

        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 20)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> service.checkCourseAccess(student, course));
        assertThrows(InvalidCredentialsException.class, () -> service.checkCourseAccess(null, course));
    }

    // =========================================================================
    // 2. GET FLAGGED ATTEMPTS & DETAILS TESTS
    // =========================================================================

    @Test
    @DisplayName("getFlaggedAttempts by examId returns mapped summaries")
    void testGetFlaggedAttempts_ByExamId() {
        when(examRepository.findById(200)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.findFlaggedByExamId(200, ExamAttemptReviewStatus.PENDING))
                .thenReturn(List.of(attempt));

        List<FlaggedAttemptSummaryResponse> summaries =
                service.getFlaggedAttempts(ownerInstructor, null, 200, ExamAttemptReviewStatus.PENDING);

        assertNotNull(summaries);
        assertEquals(1, summaries.size());
        assertEquals(300, summaries.get(0).getAttemptId());
        assertEquals(4, summaries.get(0).getTabSwitchCount());
        verify(examAttemptRepository).findFlaggedByExamId(200, ExamAttemptReviewStatus.PENDING);
    }

    @Test
    @DisplayName("getFlaggedAttempts by courseId returns mapped summaries")
    void testGetFlaggedAttempts_ByCourseId() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(examAttemptRepository.findFlaggedByCourseIds(List.of(100), null))
                .thenReturn(List.of(attempt));

        List<FlaggedAttemptSummaryResponse> summaries =
                service.getFlaggedAttempts(ownerInstructor, 100, null, null);

        assertNotNull(summaries);
        assertEquals(1, summaries.size());
        verify(examAttemptRepository).findFlaggedByCourseIds(List.of(100), null);
    }

    @Test
    @DisplayName("getFlaggedAttempts across all instructor courses returns empty when instructor has no courses")
    void testGetFlaggedAttempts_NoCoursesReturnsEmpty() {
        when(courseRepository.findByInstructorId(10)).thenReturn(Collections.emptyList());
        when(courseInstructorRepository.findByInstructorId(10)).thenReturn(Collections.emptyList());

        List<FlaggedAttemptSummaryResponse> summaries =
                service.getFlaggedAttempts(ownerInstructor, null, null, null);

        assertNotNull(summaries);
        assertTrue(summaries.isEmpty());
        verify(examAttemptRepository, never()).findFlaggedByCourseIds(any(), any());
    }

    @Test
    @DisplayName("getAttemptReviewDetails retrieves details and audit history")
    void testGetAttemptReviewDetails() {
        when(examAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));
        ExamAttemptReviewHistory historyEntry = new ExamAttemptReviewHistory(
                attempt, "NONE", "PENDING", "FLAG", "Flagged note", null, ownerInstructor, LocalDateTime.now(fixedClock)
        );
        when(reviewHistoryRepository.findByAttemptIdOrderByCreatedAtDesc(300)).thenReturn(List.of(historyEntry));

        ExamAttemptReviewResponse response = service.getAttemptReviewDetails(ownerInstructor, 300);

        assertNotNull(response);
        assertEquals(300, response.getAttemptId());
        assertEquals("Midterm Examination", response.getExamTitle());
        assertEquals(1, response.getHistory().size());
        assertEquals("FLAG", response.getHistory().get(0).getAction());
        assertEquals(new BigDecimal("78.50"), response.getObtainedMarks());
    }

    // =========================================================================
    // 3. REVIEW ATTEMPT ACTIONS & DR-07 ENFORCEMENT
    // =========================================================================

    @Test
    @DisplayName("reviewAttempt with FLAG action transitions to PENDING and strictly preserves marks (DR-07)")
    void testReviewAttempt_Flag_PreservesMarks() {
        when(examAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        ExamAttemptReviewRequest req = new ExamAttemptReviewRequest("FLAG", "Reviewing high tab switches", null, null);
        ExamAttemptReviewResponse response = service.reviewAttempt(ownerInstructor, 300, req);

        assertNotNull(response);
        assertEquals(ExamAttemptReviewStatus.PENDING, response.getReviewStatus());
        assertEquals("Reviewing high tab switches", response.getReviewNotes());

        // DR-07 verification: marks must remain untouched
        assertEquals(new BigDecimal("78.50"), attempt.getObtainedMarks(), "Obtained marks must NOT be modified (DR-07)");
        assertEquals(new BigDecimal("100.00"), attempt.getTotalMarks(), "Total marks must NOT be modified (DR-07)");
        assertEquals(new BigDecimal("78.50"), response.getObtainedMarks());

        // Append-only history verification
        ArgumentCaptor<ExamAttemptReviewHistory> historyCaptor = ArgumentCaptor.forClass(ExamAttemptReviewHistory.class);
        verify(reviewHistoryRepository).save(historyCaptor.capture());
        ExamAttemptReviewHistory history = historyCaptor.getValue();
        assertEquals("NONE", history.getPreviousStatus());
        assertEquals("PENDING", history.getNewStatus());
        assertEquals("FLAG", history.getAction());
        assertEquals(ownerInstructor, history.getReviewedBy());
    }

    @Test
    @DisplayName("reviewAttempt with CLEAR action transitions to CLEARED and strictly preserves marks (DR-07)")
    void testReviewAttempt_Clear_PreservesMarks() {
        attempt.setReviewStatus(ExamAttemptReviewStatus.PENDING);
        when(examAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        ExamAttemptReviewRequest req = new ExamAttemptReviewRequest("CLEAR", "False alarm - tab switch was permitted", null, null);
        ExamAttemptReviewResponse response = service.reviewAttempt(ownerInstructor, 300, req);

        assertEquals(ExamAttemptReviewStatus.CLEARED, response.getReviewStatus());
        assertEquals(new BigDecimal("78.50"), attempt.getObtainedMarks(), "Marks untouched on clear (DR-07)");

        ArgumentCaptor<ExamAttemptReviewHistory> historyCaptor = ArgumentCaptor.forClass(ExamAttemptReviewHistory.class);
        verify(reviewHistoryRepository).save(historyCaptor.capture());
        assertEquals("PENDING", historyCaptor.getValue().getPreviousStatus());
        assertEquals("CLEARED", historyCaptor.getValue().getNewStatus());
    }

    @Test
    @DisplayName("reviewAttempt with ESCALATE action dispatches event, records recommendation, and strictly preserves marks (DR-07)")
    void testReviewAttempt_Escalate_DispatchesEventAndPreservesMarks() {
        attempt.setReviewStatus(ExamAttemptReviewStatus.PENDING);
        when(examAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        ExamAttemptReviewRequest req = new ExamAttemptReviewRequest(
                "ESCALATE",
                "Severe tab switches and screen exits",
                "Suspected outside collaboration",
                "DISQUALIFY_ATTEMPT"
        );
        ExamAttemptReviewResponse response = service.reviewAttempt(ownerInstructor, 300, req);

        assertEquals(ExamAttemptReviewStatus.ESCALATED, response.getReviewStatus());
        assertEquals("Suspected outside collaboration", attempt.getEscalationReason());
        assertEquals("DISQUALIFY_ATTEMPT", attempt.getEscalationRecommendation());
        assertNotNull(attempt.getEscalatedAt());

        // DR-07: Escalation is recommendation-only; marks are never altered
        assertEquals(new BigDecimal("78.50"), attempt.getObtainedMarks(), "Marks untouched on escalation (DR-07)");

        // Event dispatched for administrative review (FR-A19)
        ArgumentCaptor<ExamAttemptEscalatedEvent> eventCaptor = ArgumentCaptor.forClass(ExamAttemptEscalatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        ExamAttemptEscalatedEvent event = eventCaptor.getValue();
        assertEquals(300, event.getAttemptId());
        assertEquals("Suspected outside collaboration", event.getReason());
        assertEquals("DISQUALIFY_ATTEMPT", event.getRecommendation());
        assertEquals(10, event.getEscalatedById());

        // Append-only audit record
        ArgumentCaptor<ExamAttemptReviewHistory> historyCaptor = ArgumentCaptor.forClass(ExamAttemptReviewHistory.class);
        verify(reviewHistoryRepository).save(historyCaptor.capture());
        assertEquals("ESCALATE", historyCaptor.getValue().getAction());
        assertEquals("ESCALATED", historyCaptor.getValue().getNewStatus());
    }

    @Test
    @DisplayName("reviewAttempt with UPDATE_NOTES keeps review status and updates notes")
    void testReviewAttempt_UpdateNotes() {
        attempt.setReviewStatus(ExamAttemptReviewStatus.PENDING);
        when(examAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        ExamAttemptReviewRequest req = new ExamAttemptReviewRequest("UPDATE_NOTES", "Student submitted doctor note", null, null);
        ExamAttemptReviewResponse response = service.reviewAttempt(ownerInstructor, 300, req);

        assertEquals(ExamAttemptReviewStatus.PENDING, response.getReviewStatus());
        assertEquals("Student submitted doctor note", attempt.getReviewNotes());
    }

    @Test
    @DisplayName("reviewAttempt throws IllegalArgumentException on invalid action")
    void testReviewAttempt_InvalidActionThrows() {
        when(examAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));

        ExamAttemptReviewRequest req = new ExamAttemptReviewRequest("UNKNOWN_ACTION", "Notes", null, null);
        assertThrows(IllegalArgumentException.class, () -> service.reviewAttempt(ownerInstructor, 300, req));
    }

    @Test
    @DisplayName("reviewAttempt throws on null attemptId or null request")
    void testReviewAttempt_NullArgsThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.reviewAttempt(ownerInstructor, null, new ExamAttemptReviewRequest()));
        assertThrows(IllegalArgumentException.class, () -> service.reviewAttempt(ownerInstructor, 300, null));
    }

    @Test
    @DisplayName("reviewAttempt throws ResourceNotFoundException when attempt does not exist")
    void testReviewAttempt_NotFoundThrows() {
        when(examAttemptRepository.findById(999)).thenReturn(Optional.empty());
        ExamAttemptReviewRequest req = new ExamAttemptReviewRequest("FLAG", "Notes", null, null);
        assertThrows(ResourceNotFoundException.class, () -> service.reviewAttempt(ownerInstructor, 999, req));
    }

    @Test
    @DisplayName("getReviewHistory returns history list")
    void testGetReviewHistory() {
        when(examAttemptRepository.findById(300)).thenReturn(Optional.of(attempt));
        ExamAttemptReviewHistory historyEntry = new ExamAttemptReviewHistory(
                attempt, "NONE", "PENDING", "FLAG", "Note", null, ownerInstructor, LocalDateTime.now(fixedClock)
        );
        when(reviewHistoryRepository.findByAttemptIdOrderByCreatedAtDesc(300)).thenReturn(List.of(historyEntry));

        List<ExamAttemptReviewHistoryResponse> history = service.getReviewHistory(ownerInstructor, 300);
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("FLAG", history.get(0).getAction());
    }
}
