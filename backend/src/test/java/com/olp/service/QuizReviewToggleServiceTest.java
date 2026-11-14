package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FR-I23: Answer Review and Feedback Toggle (Step 27).
 * Verifies server-side redaction, UTC deadline boundary checks, and exam defaults.
 */
@ExtendWith(MockitoExtension.class)
class QuizReviewToggleServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private QuizAttemptAnswerRepository quizAttemptAnswerRepository;

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
    private User student;
    private Course course;

    private final LocalDateTime deadlineUtc = LocalDateTime.of(2026, 11, 20, 15, 0, 0);

    @BeforeEach
    void setUp() {
        quizService = new QuizService(
                quizRepository, questionRepository, quizQuestionRepository,
                courseRepository, courseModuleRepository, lessonRepository,
                courseInstructorRepository, userRepository);

        attemptService = new QuizAttemptService(
                quizRepository, quizQuestionRepository, quizAttemptRepository,
                enrollmentRepository, userRepository, quizService,
                new AutoGradingService(), quizAttemptAnswerRepository);

        instructor = new User("Dr. Alpha", "alpha@olp.com", "hash", "instructor", "active");
        instructor.setId(1);

        student = new User("Sam Student", "student@olp.com", "hash", "student", "active");
        student.setId(2);

        course = new Course(instructor, "Computer Architecture", "CPU Design", CourseStatus.PUBLISHED);
        course.setId(10);
    }

    private void setServerTimeUtc(LocalDateTime dateTime) {
        Clock fixed = Clock.fixed(dateTime.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        quizService.setClock(fixed);
        attemptService.setClock(fixed);
    }

    private Question buildTestQuestion(Integer id, String text, String correctAnswer) {
        Question q = new Question();
        q.setId(id);
        q.setCourse(course);
        q.setCreatedBy(instructor);
        q.setQuestionText(text);
        q.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        q.setOptionsJson("[\"A\",\"B\",\"C\",\"D\"]");
        q.setCorrectAnswer(correctAnswer);
        q.setDefaultMarks(new BigDecimal("5.00"));
        q.setTopic("Hardware");
        q.setDifficulty("MEDIUM");
        return q;
    }

    @Test
    void testCreateExam_DefaultsToOff() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> {
            Quiz q = inv.getArgument(0);
            q.setId(101);
            return q;
        });

        QuizRequest request = new QuizRequest();
        request.setCourseId(10);
        request.setTitle("Final Examination");
        request.setIsExam(true);
        request.setTotalMarks(BigDecimal.ZERO);

        QuizResponse response = quizService.createQuiz(instructor, request);

        assertNotNull(response);
        assertTrue(response.getIsExam());
        assertFalse(response.getShowCorrectAnswers(), "Exams must default showCorrectAnswers to false");
        assertFalse(response.getShowFeedback(), "Exams must default showFeedback to false");
        assertEquals("AFTER_DEADLINE", response.getAnswerReleaseTiming(), "Exams must default answerReleaseTiming to AFTER_DEADLINE");
    }

    @Test
    void testCreateRegularQuiz_DefaultsToOn() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> {
            Quiz q = inv.getArgument(0);
            q.setId(102);
            return q;
        });

        QuizRequest request = new QuizRequest();
        request.setCourseId(10);
        request.setTitle("Weekly Practice Quiz");
        request.setIsExam(false);
        request.setTotalMarks(BigDecimal.ZERO);

        QuizResponse response = quizService.createQuiz(instructor, request);

        assertNotNull(response);
        assertFalse(response.getIsExam());
        assertTrue(response.getShowCorrectAnswers(), "Regular quizzes default showCorrectAnswers to true");
        assertTrue(response.getShowFeedback(), "Regular quizzes default showFeedback to true");
        assertEquals("IMMEDIATELY", response.getAnswerReleaseTiming(), "Regular quizzes default timing to IMMEDIATELY");
    }

    @Test
    void testValidateAnswerReleaseTiming_InvalidValue_ThrowsIllegalArgument() {
        when(quizRepository.findByIdWithDetails(200)).thenReturn(Optional.of(
                new Quiz(course, null, null, instructor, "Quiz", "Desc", null, QuizStatus.DRAFT)));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));

        QuizSettingsRequest req = new QuizSettingsRequest();
        req.setAnswerReleaseTiming("INVALID_TIMING_MODE");

        assertThrows(IllegalArgumentException.class, () ->
                quizService.configureQuizSettings(instructor, 200, req)
        );
    }

    @Test
    void testSubmitAttempt_WhenCorrectAnswersHidden_RedactsFromPayload() {
        setServerTimeUtc(LocalDateTime.of(2026, 11, 20, 10, 0, 0));

        Quiz quiz = new Quiz(course, null, null, instructor, "Quiz", "Desc", null, QuizStatus.PUBLISHED);
        quiz.setId(201);
        quiz.setShowCorrectAnswers(false);
        quiz.setShowFeedback(false);
        quiz.setAnswerReleaseTiming("NEVER");
        quiz.setTotalMarks(new BigDecimal("5.00"));

        Question q = buildTestQuestion(50, "What is the ALU?", "Arithmetic Logic Unit");

        QuizAttempt attempt = new QuizAttempt(quiz, student, 1,
                LocalDateTime.of(2026, 11, 20, 9, 30, 0),
                LocalDateTime.of(2026, 11, 20, 10, 30, 0));
        attempt.setId(301);

        QuizQuestion qq = new QuizQuestion(quiz, q, 1, new BigDecimal("5.00"), BigDecimal.ZERO, true);

        when(quizAttemptRepository.findByIdAndQuizId(301, 201)).thenReturn(Optional.of(attempt));
        when(quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(201)).thenReturn(List.of(qq));
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        QuizSubmissionRequest submission = new QuizSubmissionRequest();
        QuestionAnswerSubmission ansSub = new QuestionAnswerSubmission();
        ansSub.setQuestionId(50);
        ansSub.setAnswer("Arithmetic Logic Unit");
        submission.setAnswers(List.of(ansSub));

        QuizAttemptResponse response = attemptService.submitQuizAttempt(student, 201, 301, submission);

        assertNotNull(response);
        assertNotNull(response.getEvaluationSummary());
        assertFalse(response.getEvaluationSummary().getCorrectAnswersVisible(), "Flag indicates answers are hidden");

        List<QuestionEvaluationResult> results = response.getEvaluationSummary().getResults();
        assertEquals(1, results.size());
        assertNull(results.get(0).getCorrectAnswer(), "Correct answer must be completely stripped/redacted from payload");
        assertNull(results.get(0).getFeedback(), "Feedback must be completely stripped/redacted from payload");
    }

    @Test
    void testSubmitAttempt_WhenImmediate_ReturnsCorrectAnswers() {
        setServerTimeUtc(LocalDateTime.of(2026, 11, 20, 10, 0, 0));

        Quiz quiz = new Quiz(course, null, null, instructor, "Open Quiz", "Desc", null, QuizStatus.PUBLISHED);
        quiz.setId(202);
        quiz.setShowCorrectAnswers(true);
        quiz.setShowFeedback(true);
        quiz.setAnswerReleaseTiming("IMMEDIATELY");
        quiz.setTotalMarks(new BigDecimal("5.00"));

        Question q = buildTestQuestion(51, "What is a register?", "Fast storage");

        QuizAttempt attempt = new QuizAttempt(quiz, student, 1,
                LocalDateTime.of(2026, 11, 20, 9, 30, 0),
                LocalDateTime.of(2026, 11, 20, 10, 30, 0));
        attempt.setId(302);

        QuizQuestion qq = new QuizQuestion(quiz, q, 1, new BigDecimal("5.00"), BigDecimal.ZERO, true);

        when(quizAttemptRepository.findByIdAndQuizId(302, 202)).thenReturn(Optional.of(attempt));
        when(quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(202)).thenReturn(List.of(qq));
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        QuizSubmissionRequest submission = new QuizSubmissionRequest();
        QuestionAnswerSubmission ansSub = new QuestionAnswerSubmission();
        ansSub.setQuestionId(51);
        ansSub.setAnswer("Fast storage");
        submission.setAnswers(List.of(ansSub));

        QuizAttemptResponse response = attemptService.submitQuizAttempt(student, 202, 302, submission);

        assertNotNull(response);
        assertNotNull(response.getEvaluationSummary());
        assertTrue(response.getEvaluationSummary().getCorrectAnswersVisible());
        assertEquals("Fast storage", response.getEvaluationSummary().getResults().get(0).getCorrectAnswer());
    }

    @Test
    void testAfterDeadlineRelease_BeforeDeadline_HidesAnswersFromStudent() {
        // Server time: 14:59:59 UTC (1 second before deadline 15:00:00)
        setServerTimeUtc(deadlineUtc.minusSeconds(1));

        Quiz quiz = new Quiz(course, null, null, instructor, "Deadline Quiz", "Desc", null, QuizStatus.PUBLISHED);
        quiz.setId(203);
        quiz.setShowCorrectAnswers(true);
        quiz.setShowFeedback(true);
        quiz.setAnswerReleaseTiming("AFTER_DEADLINE");
        quiz.setAvailableUntil(deadlineUtc);
        quiz.setTotalMarks(new BigDecimal("5.00"));

        Question q = buildTestQuestion(52, "What is pipelining?", "Instruction overlap");

        QuizAttempt attempt = new QuizAttempt(quiz, student, 1,
                LocalDateTime.of(2026, 11, 20, 14, 0, 0),
                deadlineUtc);
        attempt.setId(303);
        attempt.setStatus(QuizAttemptStatus.SUBMITTED);
        attempt.setIsPublished(true);

        QuizAttemptAnswer answer = new QuizAttemptAnswer(attempt, q, "Instruction overlap",
                new BigDecimal("5.00"), new BigDecimal("5.00"), EvaluationStatus.CORRECT, "Good job", LocalDateTime.now());

        when(quizAttemptRepository.findByIdAndQuizId(303, 203)).thenReturn(Optional.of(attempt));
        when(quizAttemptAnswerRepository.findByAttemptId(303)).thenReturn(List.of(answer));

        QuizEvaluationSummary summary = attemptService.getAttemptEvaluation(student, 203, 303);

        assertNotNull(summary);
        assertFalse(summary.getCorrectAnswersVisible(), "Before deadline, correct answers must not be visible");
        assertEquals(1, summary.getResults().size());
        assertNull(summary.getResults().get(0).getCorrectAnswer(), "Answer must be null in response payload before deadline");
    }

    @Test
    void testAfterDeadlineRelease_AtExactDeadline_RevealsAnswersToStudent() {
        // Server time: exact deadline 15:00:00 UTC
        setServerTimeUtc(deadlineUtc);

        Quiz quiz = new Quiz(course, null, null, instructor, "Deadline Quiz", "Desc", null, QuizStatus.PUBLISHED);
        quiz.setId(204);
        quiz.setShowCorrectAnswers(true);
        quiz.setShowFeedback(true);
        quiz.setAnswerReleaseTiming("AFTER_DEADLINE");
        quiz.setAvailableUntil(deadlineUtc);
        quiz.setTotalMarks(new BigDecimal("5.00"));

        Question q = buildTestQuestion(53, "What is a cache?", "Fast SRAM");

        QuizAttempt attempt = new QuizAttempt(quiz, student, 1,
                LocalDateTime.of(2026, 11, 20, 14, 0, 0),
                deadlineUtc);
        attempt.setId(304);
        attempt.setStatus(QuizAttemptStatus.SUBMITTED);
        attempt.setIsPublished(true);

        QuizAttemptAnswer answer = new QuizAttemptAnswer(attempt, q, "Fast SRAM",
                new BigDecimal("5.00"), new BigDecimal("5.00"), EvaluationStatus.CORRECT, "Accurate", deadlineUtc);

        when(quizAttemptRepository.findByIdAndQuizId(304, 204)).thenReturn(Optional.of(attempt));
        when(quizAttemptAnswerRepository.findByAttemptId(304)).thenReturn(List.of(answer));

        QuizEvaluationSummary summary = attemptService.getAttemptEvaluation(student, 204, 304);

        assertNotNull(summary);
        assertTrue(summary.getCorrectAnswersVisible(), "At exact deadline, answers become visible");
        assertEquals("Fast SRAM", summary.getResults().get(0).getCorrectAnswer());
        assertEquals("Accurate", summary.getResults().get(0).getFeedback());
    }

    @Test
    void testAfterDeadlineRelease_AfterDeadline_RevealsAnswersToStudent() {
        // Server time: 10 minutes past deadline UTC
        setServerTimeUtc(deadlineUtc.plusMinutes(10));

        Quiz quiz = new Quiz(course, null, null, instructor, "Deadline Quiz", "Desc", null, QuizStatus.PUBLISHED);
        quiz.setId(205);
        quiz.setShowCorrectAnswers(true);
        quiz.setShowFeedback(true);
        quiz.setAnswerReleaseTiming("AFTER_DEADLINE");
        quiz.setAvailableUntil(deadlineUtc);
        quiz.setTotalMarks(new BigDecimal("5.00"));

        Question q = buildTestQuestion(54, "What is virtual memory?", "Paging mechanism");

        QuizAttempt attempt = new QuizAttempt(quiz, student, 1,
                LocalDateTime.of(2026, 11, 20, 14, 0, 0),
                deadlineUtc);
        attempt.setId(305);
        attempt.setStatus(QuizAttemptStatus.SUBMITTED);
        attempt.setIsPublished(true);

        QuizAttemptAnswer answer = new QuizAttemptAnswer(attempt, q, "Paging mechanism",
                new BigDecimal("5.00"), new BigDecimal("5.00"), EvaluationStatus.CORRECT, "Correct", deadlineUtc);

        when(quizAttemptRepository.findByIdAndQuizId(305, 205)).thenReturn(Optional.of(attempt));
        when(quizAttemptAnswerRepository.findByAttemptId(305)).thenReturn(List.of(answer));

        QuizEvaluationSummary summary = attemptService.getAttemptEvaluation(student, 205, 305);

        assertNotNull(summary);
        assertTrue(summary.getCorrectAnswersVisible(), "After deadline, answers must be visible");
        assertEquals("Paging mechanism", summary.getResults().get(0).getCorrectAnswer());
    }

    @Test
    void testNeverRelease_AfterDeadline_StillHidesAnswers() {
        // Server time: after deadline, but release timing is NEVER
        setServerTimeUtc(deadlineUtc.plusHours(1));

        Quiz quiz = new Quiz(course, null, null, instructor, "Strict Exam", "Desc", null, QuizStatus.PUBLISHED);
        quiz.setId(206);
        quiz.setShowCorrectAnswers(false);
        quiz.setShowFeedback(false);
        quiz.setAnswerReleaseTiming("NEVER");
        quiz.setAvailableUntil(deadlineUtc);
        quiz.setTotalMarks(new BigDecimal("5.00"));

        Question q = buildTestQuestion(55, "Classified Question", "Top Secret Answer");

        QuizAttempt attempt = new QuizAttempt(quiz, student, 1,
                LocalDateTime.of(2026, 11, 20, 14, 0, 0),
                deadlineUtc);
        attempt.setId(306);
        attempt.setStatus(QuizAttemptStatus.SUBMITTED);
        attempt.setIsPublished(true);

        QuizAttemptAnswer answer = new QuizAttemptAnswer(attempt, q, "Top Secret Answer",
                new BigDecimal("5.00"), new BigDecimal("5.00"), EvaluationStatus.CORRECT, "Feedback", deadlineUtc);

        when(quizAttemptRepository.findByIdAndQuizId(306, 206)).thenReturn(Optional.of(attempt));
        when(quizAttemptAnswerRepository.findByAttemptId(306)).thenReturn(List.of(answer));

        QuizEvaluationSummary summary = attemptService.getAttemptEvaluation(student, 206, 306);

        assertNotNull(summary);
        assertFalse(summary.getCorrectAnswersVisible());
        assertNull(summary.getResults().get(0).getCorrectAnswer(), "Answers remain hidden when set to NEVER");
    }

    @Test
    void testInstructor_AlwaysSeesAnswersAndFeedback() {
        // Before deadline, answers hidden from students
        setServerTimeUtc(deadlineUtc.minusHours(2));

        Quiz quiz = new Quiz(course, null, null, instructor, "Exam", "Desc", null, QuizStatus.PUBLISHED);
        quiz.setId(207);
        quiz.setShowCorrectAnswers(false);
        quiz.setShowFeedback(false);
        quiz.setAnswerReleaseTiming("NEVER");
        quiz.setTotalMarks(new BigDecimal("5.00"));

        Question q = buildTestQuestion(56, "Exam Question", "Correct Model Answer");

        QuizAttempt attempt = new QuizAttempt(quiz, student, 1,
                LocalDateTime.of(2026, 11, 20, 12, 0, 0),
                deadlineUtc);
        attempt.setId(307);
        attempt.setOverallFeedback("Instructor overall notes");

        QuizAttemptAnswer answer = new QuizAttemptAnswer(attempt, q, "Correct Model Answer",
                new BigDecimal("5.00"), new BigDecimal("5.00"), EvaluationStatus.CORRECT, "Detailed grader rubric", LocalDateTime.now());

        when(quizAttemptRepository.findByIdAndQuizId(307, 207)).thenReturn(Optional.of(attempt));
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(quizAttemptAnswerRepository.findByAttemptId(307)).thenReturn(List.of(answer));

        QuizEvaluationSummary summary = attemptService.getAttemptEvaluation(instructor, 207, 307);

        assertNotNull(summary);
        assertEquals("Instructor overall notes", summary.getOverallFeedback(), "Instructors always see feedback");
        assertEquals("Correct Model Answer", summary.getResults().get(0).getCorrectAnswer(), "Instructors always see correct answers");
        assertEquals("Detailed grader rubric", summary.getResults().get(0).getFeedback(), "Instructors always see question feedback");
    }
}

