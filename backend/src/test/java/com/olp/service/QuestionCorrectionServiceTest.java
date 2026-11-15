package com.olp.service;

import com.olp.dto.ApplyQuestionCorrectionRequest;
import com.olp.dto.ApplyQuestionCorrectionResponse;
import com.olp.dto.CompletionEvaluationResult;
import com.olp.entity.*;
import com.olp.event.QuestionCorrectionAppliedEvent;
import com.olp.event.StudentScoreCorrectedEvent;
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
 * Unit and branch coverage tests for QuestionCorrectionService (FR-I37, Step 54).
 */
@ExtendWith(MockitoExtension.class)
class QuestionCorrectionServiceTest {

    @Mock
    private ReportedQuestionRepository reportedQuestionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private ExamQuestionRepository examQuestionRepository;

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private QuizAttemptAnswerRepository quizAttemptAnswerRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResultVersionRepository resultVersionRepository;

    @Mock
    private CourseCompletionService courseCompletionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private QuestionCorrectionService service;

    private final Instant fixedInstant = Instant.parse("2026-11-15T14:00:00Z");
    private final ZoneId zoneId = ZoneId.of("UTC");
    private final Clock fixedClock = Clock.fixed(fixedInstant, zoneId);

    private User primaryInstructor;
    private User student1;
    private User student2;
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
        primaryInstructor.setName("Prof Alan");
        primaryInstructor.setEmail("alan@example.com");
        primaryInstructor.setRole("INSTRUCTOR");

        student1 = new User();
        student1.setId(101);
        student1.setName("Student Alice");
        student1.setEmail("alice@example.com");
        student1.setRole("STUDENT");

        student2 = new User();
        student2.setId(102);
        student2.setName("Student Bob");
        student2.setEmail("bob@example.com");
        student2.setRole("STUDENT");

        course = new Course();
        course.setId(10);
        course.setTitle("Operating Systems");
        course.setInstructor(primaryInstructor);

        question = new Question();
        question.setId(501);
        question.setQuestionText("Explain Page Replacement algorithms.");
        question.setDefaultMarks(BigDecimal.valueOf(5));
        question.setCourse(course);

        exam = new Exam();
        exam.setId(301);
        exam.setTitle("OS Midterm Exam");
        exam.setCourse(course);
        exam.setMaxMarks(BigDecimal.valueOf(100));
        exam.setQuestionCorrectionPolicy(QuestionCorrectionPolicy.AWARD_FULL_MARKS);

        quiz = new Quiz();
        quiz.setId(401);
        quiz.setTitle("OS Module Quiz");
        quiz.setCourse(course);
        quiz.setTotalMarks(BigDecimal.valueOf(50));

        reportedQuestionExam = new ReportedQuestion(AssessmentType.EXAM, 301, 501, course);
        reportedQuestionExam.setId(10);
        reportedQuestionExam.setStatus(ReportedQuestionStatus.PENDING);
        reportedQuestionExam.setCorrectionApplied(false);

        reportedQuestionQuiz = new ReportedQuestion(AssessmentType.QUIZ, 401, 501, course);
        reportedQuestionQuiz.setId(20);
        reportedQuestionQuiz.setStatus(ReportedQuestionStatus.PENDING);
        reportedQuestionQuiz.setCorrectionApplied(false);
    }

    @Test
    @DisplayName("applyCorrection on exam attempts with AWARD_FULL_MARKS recomputes scores, logs ResultVersion, and notifies")
    void applyCorrection_Exam_AwardFullMarks_Success() {
        when(reportedQuestionRepository.findById(10)).thenReturn(Optional.of(reportedQuestionExam));
        when(questionRepository.findById(501)).thenReturn(Optional.of(question));
        when(examRepository.findById(301)).thenReturn(Optional.of(exam));
        when(examQuestionRepository.findByExamIdAndQuestionId(301, 501)).thenReturn(Optional.empty());

        ExamAttempt attempt1 = new ExamAttempt();
        attempt1.setId(1001);
        attempt1.setExam(exam);
        attempt1.setStudent(student1);
        attempt1.setStatus("GRADED");
        attempt1.setTotalMarks(BigDecimal.valueOf(100));
        attempt1.setObtainedMarks(BigDecimal.valueOf(70));

        ExamAttempt attempt2 = new ExamAttempt();
        attempt2.setId(1002);
        attempt2.setExam(exam);
        attempt2.setStudent(student2);
        attempt2.setStatus("SUBMITTED");
        attempt2.setTotalMarks(BigDecimal.valueOf(100));
        attempt2.setObtainedMarks(BigDecimal.valueOf(98));

        when(examAttemptRepository.findByExamId(301)).thenReturn(List.of(attempt1, attempt2));

        CompletionEvaluationResult evalResult = new CompletionEvaluationResult();
        evalResult.setEligible(true);
        when(courseCompletionService.evaluate(any(User.class), any(Course.class))).thenReturn(evalResult);

        when(reportedQuestionRepository.save(any(ReportedQuestion.class))).thenAnswer(i -> i.getArgument(0));

        ApplyQuestionCorrectionRequest req = new ApplyQuestionCorrectionRequest(
                QuestionCorrectionPolicy.AWARD_FULL_MARKS, "Flawed question options; awarded full 5 marks");

        ApplyQuestionCorrectionResponse response = service.applyCorrection(primaryInstructor, 10, req);

        assertNotNull(response);
        assertFalse(response.isAlreadyApplied());
        assertEquals(2, response.getAffectedAttemptsCount());
        assertEquals(QuestionCorrectionPolicy.AWARD_FULL_MARKS, response.getPolicy());

        // Attempt 1: 70 + 5 = 75
        assertEquals(BigDecimal.valueOf(75), attempt1.getObtainedMarks());
        // Attempt 2: 98 + 5 = capped at 100
        assertEquals(BigDecimal.valueOf(100), attempt2.getObtainedMarks());

        // Verifies ResultVersions saved
        verify(resultVersionRepository, times(2)).save(any(ResultVersion.class));
        verify(examAttemptRepository, times(2)).save(any(ExamAttempt.class));

        // Verifies events published
        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher, atLeast(3)).publishEvent(captor.capture());

        long studentEvents = captor.getAllValues().stream()
                .filter(e -> e instanceof StudentScoreCorrectedEvent)
                .count();
        long batchEvents = captor.getAllValues().stream()
                .filter(e -> e instanceof QuestionCorrectionAppliedEvent)
                .count();

        assertEquals(2, studentEvents);
        assertEquals(1, batchEvents);

        // Verifies ReportedQuestion marked as applied
        assertTrue(reportedQuestionExam.getCorrectionApplied());
        assertNotNull(reportedQuestionExam.getCorrectionAppliedAt());
        assertEquals("AWARD_FULL_MARKS", reportedQuestionExam.getCorrectionPolicy());
    }

    @Test
    @DisplayName("applyCorrection on exam attempts with DROP_AND_RESCALE adjusts totals and rescales scores")
    void applyCorrection_Exam_DropAndRescale_Success() {
        when(reportedQuestionRepository.findById(10)).thenReturn(Optional.of(reportedQuestionExam));
        when(questionRepository.findById(501)).thenReturn(Optional.of(question));
        when(examRepository.findById(301)).thenReturn(Optional.of(exam));
        when(examQuestionRepository.findByExamIdAndQuestionId(301, 501)).thenReturn(Optional.empty());

        ExamAttempt attempt = new ExamAttempt();
        attempt.setId(1001);
        attempt.setExam(exam);
        attempt.setStudent(student1);
        attempt.setStatus("GRADED");
        attempt.setTotalMarks(BigDecimal.valueOf(100));
        attempt.setObtainedMarks(BigDecimal.valueOf(76));

        when(examAttemptRepository.findByExamId(301)).thenReturn(List.of(attempt));
        when(reportedQuestionRepository.save(any(ReportedQuestion.class))).thenAnswer(i -> i.getArgument(0));

        ApplyQuestionCorrectionRequest req = new ApplyQuestionCorrectionRequest(
                QuestionCorrectionPolicy.DROP_AND_RESCALE, "Question excluded from exam total");

        ApplyQuestionCorrectionResponse response = service.applyCorrection(primaryInstructor, 10, req);

        assertEquals(1, response.getAffectedAttemptsCount());
        assertEquals(QuestionCorrectionPolicy.DROP_AND_RESCALE, response.getPolicy());
        // New total = 100 - 5 = 95
        assertEquals(BigDecimal.valueOf(95), attempt.getTotalMarks());
        // Rescaled score = 76 * 100 / 95 = 80.00
        assertEquals(new BigDecimal("80.00"), attempt.getObtainedMarks());
    }

    @Test
    @DisplayName("applyCorrection on quiz attempts updates attempt and answer records")
    void applyCorrection_Quiz_Success() {
        when(reportedQuestionRepository.findById(20)).thenReturn(Optional.of(reportedQuestionQuiz));
        when(questionRepository.findById(501)).thenReturn(Optional.of(question));
        when(quizRepository.findById(401)).thenReturn(Optional.of(quiz));
        when(quizQuestionRepository.findByQuizIdAndQuestionId(401, 501)).thenReturn(Optional.empty());

        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(2001);
        attempt.setQuiz(quiz);
        attempt.setStudent(student1);
        attempt.setStatus(QuizAttemptStatus.SUBMITTED);
        attempt.setObtainedMarks(BigDecimal.valueOf(30));

        when(quizAttemptRepository.findByQuizId(401)).thenReturn(List.of(attempt));

        QuizAttemptAnswer answer = new QuizAttemptAnswer();
        answer.setId(3001);
        answer.setAttempt(attempt);
        answer.setQuestion(question);
        answer.setAwardedMarks(BigDecimal.ZERO);
        answer.setEvaluationStatus(EvaluationStatus.INCORRECT);

        when(quizAttemptAnswerRepository.findByAttemptIdAndQuestionId(2001, 501))
                .thenReturn(Optional.of(answer));
        when(reportedQuestionRepository.save(any(ReportedQuestion.class))).thenAnswer(i -> i.getArgument(0));

        ApplyQuestionCorrectionResponse response = service.applyCorrection(primaryInstructor, 20, null);

        assertEquals(1, response.getAffectedAttemptsCount());
        assertEquals(BigDecimal.valueOf(35), attempt.getObtainedMarks());
        assertEquals(BigDecimal.valueOf(5), answer.getAwardedMarks());
        assertEquals(EvaluationStatus.CORRECT, answer.getEvaluationStatus());

        verify(quizAttemptAnswerRepository, times(1)).save(answer);
        verify(resultVersionRepository, times(1)).save(any(ResultVersion.class));
    }

    @Test
    @DisplayName("applyCorrection idempotency: already applied question returns immediately with zero modifications")
    void applyCorrection_Idempotency_AlreadyApplied_NoChanges() {
        reportedQuestionExam.setCorrectionApplied(true);
        reportedQuestionExam.setCorrectionAppliedAt(LocalDateTime.ofInstant(fixedInstant, zoneId));
        reportedQuestionExam.setCorrectionAppliedBy(primaryInstructor);
        reportedQuestionExam.setCorrectionPolicy("AWARD_FULL_MARKS");
        reportedQuestionExam.setAffectedAttemptsCount(5);

        when(reportedQuestionRepository.findById(10)).thenReturn(Optional.of(reportedQuestionExam));

        ApplyQuestionCorrectionResponse response = service.applyCorrection(primaryInstructor, 10, null);

        assertNotNull(response);
        assertTrue(response.isAlreadyApplied());
        assertEquals(5, response.getAffectedAttemptsCount());
        assertTrue(response.getMessage().contains("already been applied"));

        // Verifies NO modifications to attempts or result versions
        verifyNoInteractions(examAttemptRepository);
        verifyNoInteractions(quizAttemptRepository);
        verifyNoInteractions(resultVersionRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("applyCorrection throws AccessDeniedException for unauthorized instructor")
    void applyCorrection_UnauthorizedInstructor_Throws() {
        User stranger = new User();
        stranger.setId(999);
        stranger.setRole("INSTRUCTOR");

        when(reportedQuestionRepository.findById(10)).thenReturn(Optional.of(reportedQuestionExam));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), 999)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                service.applyCorrection(stranger, 10, null));
    }

    @Test
    @DisplayName("applyCorrection throws ResourceNotFoundException on non-existent reported question")
    void applyCorrection_NonExistent_Throws() {
        when(reportedQuestionRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.applyCorrection(primaryInstructor, 999, null));
    }

    @Test
    @DisplayName("resolveUser resolves User, email, and handles unauthenticated")
    void resolveUser_SuccessAndFailure() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(primaryInstructor);
        assertSame(primaryInstructor, service.resolveUser(auth));

        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(null));
    }
}
