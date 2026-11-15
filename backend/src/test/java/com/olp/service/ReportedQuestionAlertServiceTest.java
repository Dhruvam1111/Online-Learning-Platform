package com.olp.service;

import com.olp.dto.QuestionReportRequest;
import com.olp.dto.ReportedQuestionInboxResponse;
import com.olp.entity.*;
import com.olp.event.QuestionReportNotificationEvent;
import com.olp.event.QuestionReportedEvent;
import com.olp.event.ReportedQuestionEscalatedEvent;
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
 * Unit and branch coverage tests for ReportedQuestionAlertService (FR-I35, Step 52).
 */
@ExtendWith(MockitoExtension.class)
class ReportedQuestionAlertServiceTest {

    @Mock
    private ReportedQuestionRepository reportedQuestionRepository;

    @Mock
    private StudentQuestionReportRepository studentQuestionReportRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReportedQuestionAlertService service;

    private final Instant fixedInstant = Instant.parse("2026-11-15T10:00:00Z");
    private final ZoneId zoneId = ZoneId.of("UTC");
    private final Clock fixedClock = Clock.fixed(fixedInstant, zoneId);

    private User student;
    private User primaryInstructor;
    private User coInstructor;
    private User admin;
    private Course course;
    private Question question;
    private Exam exam;
    private Quiz quiz;

    @BeforeEach
    void setUp() {
        service.setClock(fixedClock);

        student = new User();
        student.setId(101);
        student.setName("Student Alice");
        student.setEmail("alice@example.com");
        student.setRole("STUDENT");

        primaryInstructor = new User();
        primaryInstructor.setId(201);
        primaryInstructor.setName("Prof Bob");
        primaryInstructor.setEmail("bob@example.com");
        primaryInstructor.setRole("INSTRUCTOR");

        coInstructor = new User();
        coInstructor.setId(202);
        coInstructor.setName("TA Charlie");
        coInstructor.setEmail("charlie@example.com");
        coInstructor.setRole("INSTRUCTOR");

        admin = new User();
        admin.setId(1);
        admin.setName("Admin User");
        admin.setEmail("admin@example.com");
        admin.setRole("ADMIN");

        course = new Course();
        course.setId(10);
        course.setTitle("Distributed Systems");
        course.setInstructor(primaryInstructor);

        question = new Question();
        question.setId(501);
        question.setQuestionText("What is Paxos?");
        question.setCourse(course);

        exam = new Exam();
        exam.setId(301);
        exam.setTitle("Midterm Exam");
        exam.setCourse(course);

        quiz = new Quiz();
        quiz.setId(401);
        quiz.setTitle("Weekly Quiz");
        quiz.setCourse(course);
    }

    @Test
    @DisplayName("reportQuestion creates new inbox item and publishes notification events")
    void reportQuestion_NewReport_Success() {
        QuestionReportRequest request = new QuestionReportRequest("Typo in question", "Option B is duplicated", 1001);

        when(questionRepository.findById(501)).thenReturn(Optional.of(question));
        when(examRepository.findById(301)).thenReturn(Optional.of(exam));
        when(reportedQuestionRepository.findByAssessmentTypeAndAssessmentIdAndQuestionId(
                AssessmentType.EXAM, 301, 501)).thenReturn(Optional.empty());

        when(reportedQuestionRepository.save(any(ReportedQuestion.class))).thenAnswer(invocation -> {
            ReportedQuestion rq = invocation.getArgument(0);
            rq.setId(77);
            return rq;
        });

        CourseInstructor coInstRecord = new CourseInstructor();
        coInstRecord.setCourse(course);
        coInstRecord.setInstructor(coInstructor);
        when(courseInstructorRepository.findByCourseId(course.getId())).thenReturn(List.of(coInstRecord));

        ReportedQuestionInboxResponse response = service.reportQuestion(
                student, AssessmentType.EXAM, 301, 501, request);

        assertNotNull(response);
        assertEquals(77, response.getId());
        assertEquals(1, response.getReportCount());
        assertEquals(ReportedQuestionStatus.PENDING, response.getStatus());
        assertEquals("Typo in question", response.getLatestReason());
        assertEquals("Option B is duplicated", response.getLatestExplanation());
        assertEquals(LocalDateTime.ofInstant(fixedInstant, zoneId).plusHours(48), response.getEscalateAt());
        assertFalse(response.getEscalatedToAdmin());

        verify(studentQuestionReportRepository, times(1)).save(any(StudentQuestionReport.class));

        ArgumentCaptor<org.springframework.context.ApplicationEvent> eventCaptor =
                ArgumentCaptor.forClass(org.springframework.context.ApplicationEvent.class);
        verify(eventPublisher, atLeast(2)).publishEvent(eventCaptor.capture());

        boolean hasReportedEvent = eventCaptor.getAllValues().stream()
                .anyMatch(e -> e instanceof QuestionReportedEvent);
        long notificationCount = eventCaptor.getAllValues().stream()
                .filter(e -> e instanceof QuestionReportNotificationEvent)
                .count();

        assertTrue(hasReportedEvent);
        assertEquals(2, notificationCount); // Primary + Co-instructor
    }

    @Test
    @DisplayName("reportQuestion for quiz increments report count for another student")
    void reportQuestion_ExistingReport_DifferentStudent_IncrementsCount() {
        QuestionReportRequest request = new QuestionReportRequest("Ambiguous wording", "Cannot decide between A and C", 2002);

        when(questionRepository.findById(501)).thenReturn(Optional.of(question));
        when(quizRepository.findById(401)).thenReturn(Optional.of(quiz));

        ReportedQuestion existing = new ReportedQuestion(AssessmentType.QUIZ, 401, 501, course);
        existing.setId(88);
        existing.setReportCount(1);
        existing.setStatus(ReportedQuestionStatus.PENDING);
        existing.setLatestReason("Initial reason");

        when(reportedQuestionRepository.findByAssessmentTypeAndAssessmentIdAndQuestionId(
                AssessmentType.QUIZ, 401, 501)).thenReturn(Optional.of(existing));
        when(studentQuestionReportRepository.findByReportedQuestionIdAndStudentId(88, student.getId()))
                .thenReturn(Optional.empty());
        when(reportedQuestionRepository.save(any(ReportedQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportedQuestionInboxResponse response = service.reportQuestion(
                student, AssessmentType.QUIZ, 401, 501, request);

        assertNotNull(response);
        assertEquals(88, response.getId());
        assertEquals(2, response.getReportCount());
        assertEquals("Ambiguous wording", response.getLatestReason());
        verify(studentQuestionReportRepository, times(1)).save(any(StudentQuestionReport.class));
    }

    @Test
    @DisplayName("reportQuestion duplicate from same student updates existing report without incrementing count")
    void reportQuestion_DuplicateStudent_UpdatesExistingReport() {
        QuestionReportRequest request = new QuestionReportRequest("Updated reason", "More detailed explanation", 1001);

        when(questionRepository.findById(501)).thenReturn(Optional.of(question));
        when(examRepository.findById(301)).thenReturn(Optional.of(exam));

        ReportedQuestion existing = new ReportedQuestion(AssessmentType.EXAM, 301, 501, course);
        existing.setId(99);
        existing.setReportCount(3);
        existing.setStatus(ReportedQuestionStatus.PENDING);

        StudentQuestionReport studentReport = new StudentQuestionReport(existing, student, 1001, "Old reason", "Old exp");

        when(reportedQuestionRepository.findByAssessmentTypeAndAssessmentIdAndQuestionId(
                AssessmentType.EXAM, 301, 501)).thenReturn(Optional.of(existing));
        when(studentQuestionReportRepository.findByReportedQuestionIdAndStudentId(99, student.getId()))
                .thenReturn(Optional.of(studentReport));
        when(reportedQuestionRepository.save(any(ReportedQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportedQuestionInboxResponse response = service.reportQuestion(
                student, AssessmentType.EXAM, 301, 501, request);

        assertEquals(3, response.getReportCount());
        assertEquals("Updated reason", response.getLatestReason());
        assertEquals("Updated reason", studentReport.getReason());
        assertEquals("More detailed explanation", studentReport.getExplanation());
    }

    @Test
    @DisplayName("reportQuestion reopens DISMISSED question to PENDING")
    void reportQuestion_ReopensDismissedQuestion() {
        QuestionReportRequest request = new QuestionReportRequest("New flaw discovered", "Key incorrect", null);

        when(questionRepository.findById(501)).thenReturn(Optional.of(question));
        when(examRepository.findById(301)).thenReturn(Optional.of(exam));

        ReportedQuestion existing = new ReportedQuestion(AssessmentType.EXAM, 301, 501, course);
        existing.setId(55);
        existing.setStatus(ReportedQuestionStatus.DISMISSED);

        when(reportedQuestionRepository.findByAssessmentTypeAndAssessmentIdAndQuestionId(
                AssessmentType.EXAM, 301, 501)).thenReturn(Optional.of(existing));
        when(studentQuestionReportRepository.findByReportedQuestionIdAndStudentId(55, student.getId()))
                .thenReturn(Optional.empty());
        when(reportedQuestionRepository.save(any(ReportedQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportedQuestionInboxResponse response = service.reportQuestion(
                student, AssessmentType.EXAM, 301, 501, request);

        assertEquals(ReportedQuestionStatus.PENDING, response.getStatus());
    }

    @Test
    @DisplayName("reportQuestion throws on missing reason or unauthenticated student")
    void reportQuestion_ValidationErrors() {
        assertThrows(InvalidCredentialsException.class, () ->
                service.reportQuestion(null, AssessmentType.EXAM, 301, 501, new QuestionReportRequest("Reason", "Exp", null)));

        assertThrows(IllegalArgumentException.class, () ->
                service.reportQuestion(student, AssessmentType.EXAM, 301, 501, new QuestionReportRequest("", "Exp", null)));

        when(questionRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                service.reportQuestion(student, AssessmentType.EXAM, 301, 999, new QuestionReportRequest("Reason", "Exp", null)));

        when(questionRepository.findById(501)).thenReturn(Optional.of(question));
        when(examRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                service.reportQuestion(student, AssessmentType.EXAM, 999, 501, new QuestionReportRequest("Reason", "Exp", null)));

        assertThrows(IllegalArgumentException.class, () ->
                service.reportQuestion(student, AssessmentType.ASSIGNMENT, 301, 501, new QuestionReportRequest("Reason", "Exp", null)));
    }

    @Test
    @DisplayName("getInstructorInbox returns all reports for ADMIN")
    void getInstructorInbox_Admin_ReturnsAll() {
        ReportedQuestion rq = new ReportedQuestion(AssessmentType.EXAM, 301, 501, course);
        rq.setId(10);
        rq.setStatus(ReportedQuestionStatus.PENDING);

        when(reportedQuestionRepository.findAllOrderByLastReportedAtDesc()).thenReturn(List.of(rq));

        List<ReportedQuestionInboxResponse> inbox = service.getInstructorInbox(admin, null, null);
        assertEquals(1, inbox.size());
        assertEquals(10, inbox.get(0).getId());

        when(reportedQuestionRepository.findAllByStatusOrderByLastReportedAtDesc(ReportedQuestionStatus.PENDING))
                .thenReturn(List.of(rq));
        List<ReportedQuestionInboxResponse> filtered = service.getInstructorInbox(admin, null, ReportedQuestionStatus.PENDING);
        assertEquals(1, filtered.size());
    }

    @Test
    @DisplayName("getInstructorInbox returns accessible course reports for INSTRUCTOR")
    void getInstructorInbox_Instructor_ReturnsAssignedCourses() {
        when(courseRepository.findByInstructorId(primaryInstructor.getId())).thenReturn(List.of(course));
        when(courseInstructorRepository.findByInstructorId(primaryInstructor.getId())).thenReturn(Collections.emptyList());

        ReportedQuestion rq = new ReportedQuestion(AssessmentType.EXAM, 301, 501, course);
        rq.setId(20);

        when(reportedQuestionRepository.findByCourseIdIn(any())).thenReturn(List.of(rq));

        List<ReportedQuestionInboxResponse> inbox = service.getInstructorInbox(primaryInstructor, null, null);
        assertEquals(1, inbox.size());
        assertEquals(20, inbox.get(0).getId());
    }

    @Test
    @DisplayName("getInstructorInbox with courseId validates authorization")
    void getInstructorInbox_SpecificCourse_AuthorizationCheck() {
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(reportedQuestionRepository.findByCourseId(course.getId())).thenReturn(Collections.emptyList());

        List<ReportedQuestionInboxResponse> inbox = service.getInstructorInbox(primaryInstructor, course.getId(), null);
        assertNotNull(inbox);

        User stranger = new User();
        stranger.setId(999);
        stranger.setRole("INSTRUCTOR");

        when(courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), stranger.getId()))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                service.getInstructorInbox(stranger, course.getId(), null));
    }

    @Test
    @DisplayName("escalateUnresolvedReports updates overdue items to ESCALATED and publishes event")
    void escalateUnresolvedReports_Success() {
        ReportedQuestion rq = new ReportedQuestion(AssessmentType.EXAM, 301, 501, course);
        rq.setId(66);
        rq.setStatus(ReportedQuestionStatus.PENDING);
        rq.setEscalatedToAdmin(false);
        rq.setFirstReportedAt(LocalDateTime.ofInstant(fixedInstant, zoneId).minusHours(50));
        rq.setEscalateAt(LocalDateTime.ofInstant(fixedInstant, zoneId).minusHours(2));

        when(reportedQuestionRepository.findUnresolvedForEscalation(any(), any())).thenReturn(List.of(rq));
        when(reportedQuestionRepository.save(any(ReportedQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int escalated = service.escalateUnresolvedReports();

        assertEquals(1, escalated);
        assertTrue(rq.getEscalatedToAdmin());
        assertEquals(ReportedQuestionStatus.ESCALATED, rq.getStatus());
        assertNotNull(rq.getEscalatedAt());

        verify(eventPublisher, times(1)).publishEvent(any(ReportedQuestionEscalatedEvent.class));
    }

    @Test
    @DisplayName("resolveUser handles User principal, email String, and auth name")
    void resolveUser_HandlesAllPrincipalTypes() {
        Authentication authUser = mock(Authentication.class);
        when(authUser.getPrincipal()).thenReturn(student);
        assertSame(student, service.resolveUser(authUser));

        Authentication authEmail = mock(Authentication.class);
        when(authEmail.getPrincipal()).thenReturn("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(student));
        assertSame(student, service.resolveUser(authEmail));

        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(null));
    }
}
