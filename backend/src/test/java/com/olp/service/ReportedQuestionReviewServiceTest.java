package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.event.ReportedQuestionReviewedEvent;
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
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit and branch coverage tests for ReportedQuestionReviewService (FR-I36, Step 53).
 */
@ExtendWith(MockitoExtension.class)
class ReportedQuestionReviewServiceTest {

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
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReportedQuestionReviewService service;

    private final Instant fixedInstant = Instant.parse("2026-11-15T12:00:00Z");
    private final ZoneId zoneId = ZoneId.of("UTC");
    private final Clock fixedClock = Clock.fixed(fixedInstant, zoneId);

    private User primaryInstructor;
    private User admin;
    private Course course;
    private Question question;
    private Exam exam;
    private Quiz quiz;
    private ReportedQuestion reportedQuestionExam;
    private ReportedQuestion reportedQuestionQuiz;

    @BeforeEach
    void setUp() {
        service.setClock(fixedClock);

        primaryInstructor = new User();
        primaryInstructor.setId(201);
        primaryInstructor.setName("Prof Bob");
        primaryInstructor.setEmail("bob@example.com");
        primaryInstructor.setRole("INSTRUCTOR");

        admin = new User();
        admin.setId(1);
        admin.setName("Admin User");
        admin.setEmail("admin@example.com");
        admin.setRole("ADMIN");

        course = new Course();
        course.setId(10);
        course.setTitle("Operating Systems");
        course.setInstructor(primaryInstructor);

        question = new Question();
        question.setId(501);
        question.setQuestionText("Explain deadlock conditions.");
        question.setOptionsJson("[\"Mutual Exclusion\",\"Hold and Wait\"]");
        question.setCorrectAnswer("All four");
        question.setCourse(course);

        exam = new Exam();
        exam.setId(301);
        exam.setTitle("OS Midterm");
        exam.setCourse(course);

        quiz = new Quiz();
        quiz.setId(401);
        quiz.setTitle("OS Quiz 1");
        quiz.setCourse(course);

        reportedQuestionExam = new ReportedQuestion(AssessmentType.EXAM, 301, 501, course);
        reportedQuestionExam.setId(100);
        reportedQuestionExam.setReportCount(2);
        reportedQuestionExam.setStatus(ReportedQuestionStatus.PENDING);
        reportedQuestionExam.setLatestReason("Ambiguous wording");
        reportedQuestionExam.setLatestExplanation("Could mean 3 or 4 conditions");

        reportedQuestionQuiz = new ReportedQuestion(AssessmentType.QUIZ, 401, 501, course);
        reportedQuestionQuiz.setId(200);
        reportedQuestionQuiz.setReportCount(1);
        reportedQuestionQuiz.setStatus(ReportedQuestionStatus.PENDING);
    }

    @Test
    @DisplayName("getReportedQuestions for ADMIN returns filtered list from findWithFiltersAdmin")
    void getReportedQuestions_Admin_Success() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(reportedQuestionRepository.findWithFiltersAdmin(10, AssessmentType.EXAM, 301, ReportedQuestionStatus.PENDING))
                .thenReturn(List.of(reportedQuestionExam));

        List<ReportedQuestionInboxResponse> result = service.getReportedQuestions(
                admin, 10, AssessmentType.EXAM, 301, ReportedQuestionStatus.PENDING);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100, result.get(0).getId());
    }

    @Test
    @DisplayName("getReportedQuestions for INSTRUCTOR returns list from accessible courses")
    void getReportedQuestions_Instructor_Success() {
        when(courseRepository.findByInstructorId(primaryInstructor.getId())).thenReturn(List.of(course));
        when(courseInstructorRepository.findByInstructorId(primaryInstructor.getId())).thenReturn(Collections.emptyList());

        when(reportedQuestionRepository.findWithFilters(Set.of(10), 10, null, null, null))
                .thenReturn(List.of(reportedQuestionExam));

        when(courseRepository.findById(10)).thenReturn(Optional.of(course));

        List<ReportedQuestionInboxResponse> result = service.getReportedQuestions(
                primaryInstructor, 10, null, null, null);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getReportedQuestions unauthorized course throws AccessDeniedException")
    void getReportedQuestions_UnauthorizedCourse_Throws() {
        Course otherCourse = new Course();
        otherCourse.setId(99);
        User otherInstructor = new User();
        otherInstructor.setId(888);
        otherCourse.setInstructor(otherInstructor);

        when(courseRepository.findById(99)).thenReturn(Optional.of(otherCourse));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(99, primaryInstructor.getId())).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                service.getReportedQuestions(primaryInstructor, 99, null, null, null));
    }

    @Test
    @DisplayName("getReportDetails returns question text, answers, student reports, and affected count")
    void getReportDetails_Success() {
        when(reportedQuestionRepository.findById(100)).thenReturn(Optional.of(reportedQuestionExam));
        when(questionRepository.findById(501)).thenReturn(Optional.of(question));
        when(examRepository.findById(301)).thenReturn(Optional.of(exam));
        when(examAttemptRepository.countByExamId(301)).thenReturn(42L);

        User student = new User();
        student.setId(55);
        student.setName("Student Dan");
        student.setEmail("dan@example.com");

        StudentQuestionReport sqr = new StudentQuestionReport(reportedQuestionExam, student, 101, "Typo", "Detailed");
        sqr.setId(1);
        when(studentQuestionReportRepository.findByReportedQuestionId(100)).thenReturn(List.of(sqr));

        ReportedQuestionDetailResponse details = service.getReportDetails(primaryInstructor, 100);

        assertNotNull(details);
        assertEquals(100, details.getId());
        assertEquals("Explain deadlock conditions.", details.getQuestionText());
        assertEquals("All four", details.getCorrectAnswer());
        assertEquals(42L, details.getAffectedAttemptsCount());
        assertEquals(1, details.getReports().size());
        assertEquals("Student Dan", details.getReports().get(0).getStudentName());
    }

    @Test
    @DisplayName("getAffectedAttemptsCount returns count for Exam and Quiz")
    void getAffectedAttemptsCount_ExamAndQuiz() {
        when(reportedQuestionRepository.findById(100)).thenReturn(Optional.of(reportedQuestionExam));
        when(examAttemptRepository.countByExamId(301)).thenReturn(35L);

        AffectedAttemptsCountResponse respExam = service.getAffectedAttemptsCount(primaryInstructor, 100);
        assertEquals(35L, respExam.getAffectedAttemptsCount());

        when(reportedQuestionRepository.findById(200)).thenReturn(Optional.of(reportedQuestionQuiz));
        when(quizAttemptRepository.countByQuizId(401)).thenReturn(18L);

        AffectedAttemptsCountResponse respQuiz = service.getAffectedAttemptsCount(primaryInstructor, 200);
        assertEquals(18L, respQuiz.getAffectedAttemptsCount());
    }

    @Test
    @DisplayName("reviewReportedQuestion executes CORRECT action, sets status and dispatches event")
    void reviewReportedQuestion_CorrectAction_Success() {
        when(reportedQuestionRepository.findById(100)).thenReturn(Optional.of(reportedQuestionExam));
        when(reportedQuestionRepository.save(any(ReportedQuestion.class))).thenAnswer(i -> i.getArgument(0));
        when(examAttemptRepository.countByExamId(301)).thenReturn(50L);

        ReportedQuestionReviewRequest request = new ReportedQuestionReviewRequest(
                ReportedQuestionReviewAction.CORRECT, "Updated answer key to accept both options");

        ReportedQuestionDetailResponse response = service.reviewReportedQuestion(primaryInstructor, 100, request);

        assertNotNull(response);
        assertEquals(ReportedQuestionStatus.RESOLVED_CORRECTED, response.getStatus());
        assertEquals("CORRECT", response.getResolutionAction());
        assertEquals("Updated answer key to accept both options", response.getResolutionComment());
        assertEquals(primaryInstructor.getId(), response.getResolvedById());
        assertNotNull(response.getResolvedAt());
        assertEquals(50L, response.getAffectedAttemptsCount());

        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertTrue(captor.getValue() instanceof ReportedQuestionReviewedEvent);
        ReportedQuestionReviewedEvent event = (ReportedQuestionReviewedEvent) captor.getValue();
        assertEquals("CORRECT", event.getAction());
        assertEquals(50L, event.getAffectedAttemptsCount());
    }

    @Test
    @DisplayName("reviewReportedQuestion executes INVALIDATE action")
    void reviewReportedQuestion_InvalidateAction_Success() {
        when(reportedQuestionRepository.findById(100)).thenReturn(Optional.of(reportedQuestionExam));
        when(reportedQuestionRepository.save(any(ReportedQuestion.class))).thenAnswer(i -> i.getArgument(0));
        when(examAttemptRepository.countByExamId(301)).thenReturn(30L);

        ReportedQuestionReviewRequest request = new ReportedQuestionReviewRequest(
                ReportedQuestionReviewAction.INVALIDATE, "Flawed question, bonus marks awarded");

        ReportedQuestionDetailResponse response = service.reviewReportedQuestion(primaryInstructor, 100, request);

        assertEquals(ReportedQuestionStatus.RESOLVED_INVALIDATED, response.getStatus());
        assertEquals("INVALIDATE", response.getResolutionAction());
    }

    @Test
    @DisplayName("reviewReportedQuestion executes DISMISS action")
    void reviewReportedQuestion_DismissAction_Success() {
        when(reportedQuestionRepository.findById(100)).thenReturn(Optional.of(reportedQuestionExam));
        when(reportedQuestionRepository.save(any(ReportedQuestion.class))).thenAnswer(i -> i.getArgument(0));
        when(examAttemptRepository.countByExamId(301)).thenReturn(30L);

        ReportedQuestionReviewRequest request = new ReportedQuestionReviewRequest(
                ReportedQuestionReviewAction.DISMISS, "Question text is correct as per textbook");

        ReportedQuestionDetailResponse response = service.reviewReportedQuestion(primaryInstructor, 100, request);

        assertEquals(ReportedQuestionStatus.DISMISSED, response.getStatus());
        assertEquals("DISMISS", response.getResolutionAction());
    }

    @Test
    @DisplayName("reviewReportedQuestion validation errors throw IllegalArgumentException")
    void reviewReportedQuestion_ValidationErrors() {
        assertThrows(InvalidCredentialsException.class, () ->
                service.reviewReportedQuestion(null, 100, new ReportedQuestionReviewRequest(ReportedQuestionReviewAction.CORRECT, "Notes")));

        assertThrows(IllegalArgumentException.class, () ->
                service.reviewReportedQuestion(primaryInstructor, 100, new ReportedQuestionReviewRequest(null, "Notes")));

        assertThrows(IllegalArgumentException.class, () ->
                service.reviewReportedQuestion(primaryInstructor, 100, new ReportedQuestionReviewRequest(ReportedQuestionReviewAction.CORRECT, "  ")));

        when(reportedQuestionRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                service.reviewReportedQuestion(primaryInstructor, 999, new ReportedQuestionReviewRequest(ReportedQuestionReviewAction.CORRECT, "Notes")));
    }
}
