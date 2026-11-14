package com.olp.service;

import com.olp.dto.QuestionAnswerSubmission;
import com.olp.dto.QuestionEvaluationResult;
import com.olp.dto.QuizAttemptResponse;
import com.olp.dto.QuizEvaluationSummary;
import com.olp.dto.QuizSettingsResponse;
import com.olp.dto.QuizSubmissionRequest;
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

        @Mock
        private QuizAttemptAnswerRepository quizAttemptAnswerRepository;

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
                                enrollmentRepository, userRepository, quizService,
                                new AutoGradingService(), quizAttemptAnswerRepository);

                setServerTime(baseTime);

                instructor = new User("Dr. Owner", "owner@olp.com", "hash", "instructor", "active");
                instructor.setId(1);

                enrolledStudent = new User("Alice", "alice@olp.com", "hash", "student", "active");
                enrolledStudent.setId(3);

                unenrolledStudent = new User("Bob", "bob@olp.com", "hash", "student", "active");
                unenrolledStudent.setId(4);

                course = new Course(instructor, "Web Dev", "Desc", CourseStatus.PUBLISHED);
                course.setId(10);

                publishedQuiz = new Quiz(course, null, null, instructor, "Midterm Quiz", "Desc", null,
                                QuizStatus.PUBLISHED);
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
                assertThrows(AccessDeniedException.class,
                                () -> attemptService.getStudentQuizSettings(unenrolledStudent, 100));

                // Draft quiz blocked
                when(quizRepository.findByIdWithDetails(101)).thenReturn(Optional.of(draftQuiz));
                assertThrows(IllegalStateException.class,
                                () -> attemptService.getStudentQuizSettings(enrolledStudent, 101));

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
                QuizAttempt persisted = new QuizAttempt(publishedQuiz, enrolledStudent, 1, baseTime,
                                baseTime.plusMinutes(30));
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

                // Second attempt started at 11:45 when window ends at 12:00 -> capped at 12:00
                // (15 min)
                setServerTime(LocalDateTime.of(2026, 11, 12, 11, 45, 0));
                when(quizAttemptRepository.countByQuizIdAndStudentId(100, 3)).thenReturn(1L);
                QuizAttemptResponse capped = attemptService.startQuizAttempt(enrolledStudent, 100);
                assertEquals(LocalDateTime.of(2026, 11, 12, 12, 0, 0), capped.getExpiresAt());
                assertEquals(900L, capped.getRemainingSeconds());

                // Submit attempt #2 after expiry (12:02) -> marks EXPIRED and throws
                // QuizAttemptExpiredException
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

        @Test
        void testSubmitQuizAttempt_WithAnswers_AutoGradesAndPersistsAnswers() {
                QuizAttempt activeAttempt = new QuizAttempt(
                                publishedQuiz, enrolledStudent, 1, baseTime, baseTime.plusMinutes(30));
                activeAttempt.setId(700);

                when(quizAttemptRepository.findByIdAndQuizId(700, 100)).thenReturn(Optional.of(activeAttempt));
                when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
                when(quizAttemptRepository.countByQuizIdAndStudentId(100, 3)).thenReturn(1L);

                Question q1 = new Question(
                                course, instructor, "Capital of France?", QuestionType.MULTIPLE_CHOICE,
                                "[\"Paris\", \"Rome\"]", "Paris",
                                new BigDecimal("5.00"), BigDecimal.ZERO, BigDecimal.ZERO, null, false);
                q1.setId(10);
                QuizQuestion qq1 = new QuizQuestion(publishedQuiz, q1, 1, new BigDecimal("5.00"), BigDecimal.ZERO,
                                true);

                Question q2 = new Question(
                                course, instructor, "Explain OOP", QuestionType.DESCRIPTIVE,
                                null, null,
                                new BigDecimal("5.00"), BigDecimal.ZERO, BigDecimal.ZERO, "Rubric", true);
                q2.setId(20);
                QuizQuestion qq2 = new QuizQuestion(publishedQuiz, q2, 2, new BigDecimal("5.00"), BigDecimal.ZERO,
                                true);

                when(quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(100)).thenReturn(List.of(qq1, qq2));

                QuizSubmissionRequest submissionRequest = new QuizSubmissionRequest(List.of(
                                new QuestionAnswerSubmission(10, "Paris"),
                                new QuestionAnswerSubmission(20, "OOP has classes")));

                QuizAttemptResponse response = attemptService.submitQuizAttempt(enrolledStudent, 100, 700,
                                submissionRequest);

                assertNotNull(response);
                assertEquals("SUBMITTED", response.getStatus());
                assertEquals(new BigDecimal("5.00"), response.getObtainedMarks());
                assertNotNull(response.getEvaluationSummary());
                assertEquals(1, response.getEvaluationSummary().getCorrectCount());
                assertEquals(1, response.getEvaluationSummary().getManualGradingCount());
                verify(quizAttemptAnswerRepository, times(2)).save(any(QuizAttemptAnswer.class));
        }

        @Test
        void testGetAttemptEvaluation_SuccessForStudentOwner() {
                QuizAttempt submitted = new QuizAttempt(
                                publishedQuiz, enrolledStudent, 1, baseTime, baseTime.plusMinutes(30));
                submitted.setId(800);
                submitted.setStatus(QuizAttemptStatus.SUBMITTED);
                submitted.setObtainedMarks(new BigDecimal("5.00"));

                when(quizAttemptRepository.findByIdAndQuizId(800, 100)).thenReturn(Optional.of(submitted));

                Question q1 = new Question(
                                course, instructor, "What is 2+2?", QuestionType.MULTIPLE_CHOICE,
                                "[\"4\", \"5\"]", "4",
                                new BigDecimal("5.00"), BigDecimal.ZERO, BigDecimal.ZERO, null, false);
                q1.setId(50);

                QuizAttemptAnswer ans = new QuizAttemptAnswer(
                                submitted, q1, "4", new BigDecimal("5.00"), new BigDecimal("5.00"),
                                EvaluationStatus.CORRECT, "Correct", baseTime);
                ans.setId(1);

                when(quizAttemptAnswerRepository.findByAttemptId(800)).thenReturn(List.of(ans));

                QuizEvaluationSummary summary = attemptService.getAttemptEvaluation(enrolledStudent, 100, 800);

                assertNotNull(summary);
                assertEquals(100, summary.getQuizId());
                assertEquals(new BigDecimal("5.00"), summary.getTotalAwardedMarks());
                assertEquals(1, summary.getCorrectCount());
                assertEquals(1, summary.getResults().size());
                assertEquals(EvaluationStatus.CORRECT, summary.getResults().get(0).getStatus());
        }

        @Test
        void testGetAttemptEvaluation_AccessDeniedForOtherStudent() {
                QuizAttempt submitted = new QuizAttempt(
                                publishedQuiz, enrolledStudent, 1, baseTime, baseTime.plusMinutes(30));
                submitted.setId(800);

                when(quizAttemptRepository.findByIdAndQuizId(800, 100)).thenReturn(Optional.of(submitted));

                assertThrows(AccessDeniedException.class,
                                () -> attemptService.getAttemptEvaluation(unenrolledStudent, 100, 800));
        }

        @Test
        void testGetAttemptEvaluation_StudentCannotSeeUnpublishedMarks() {
                QuizAttempt submitted = new QuizAttempt(
                                publishedQuiz, enrolledStudent, 1, baseTime, baseTime.plusMinutes(30));
                submitted.setId(801);
                submitted.setStatus(QuizAttemptStatus.SUBMITTED);
                submitted.setObtainedMarks(new BigDecimal("0.00"));

                when(quizAttemptRepository.findByIdAndQuizId(801, 100)).thenReturn(Optional.of(submitted));

                Question descQ = new Question(
                                course, instructor, "Explain polymorphism", QuestionType.DESCRIPTIVE,
                                null, null,
                                new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO, "Rubric", true);
                descQ.setId(60);

                QuizAttemptAnswer unpubAns = new QuizAttemptAnswer(
                                submitted, descQ, "Student explanation",
                                new BigDecimal("8.50"), new BigDecimal("10.00"),
                                EvaluationStatus.EVALUATED_UNPUBLISHED, "Instructor draft note", baseTime);
                unpubAns.setId(10);

                when(quizAttemptAnswerRepository.findByAttemptId(801)).thenReturn(List.of(unpubAns));

                QuizEvaluationSummary studentSummary = attemptService.getAttemptEvaluation(enrolledStudent, 100, 801);

                assertNotNull(studentSummary);
                assertEquals(1, studentSummary.getResults().size());
                QuestionEvaluationResult result = studentSummary.getResults().get(0);

                // Student must NOT see unpublished marks or feedback (FR-I30 Requirement 3)
                assertEquals(EvaluationStatus.MANUAL_GRADING_REQUIRED, result.getStatus());
                assertEquals(BigDecimal.ZERO, result.getAwardedMarks());
                assertNull(result.getFeedback());
                assertTrue(result.isRequiresManualGrading());
                assertEquals(1, studentSummary.getManualGradingCount());
        }

        @Test
        void testGetAttemptEvaluation_InstructorCanSeeUnpublishedMarks() {
                QuizAttempt submitted = new QuizAttempt(
                                publishedQuiz, enrolledStudent, 1, baseTime, baseTime.plusMinutes(30));
                submitted.setId(802);
                submitted.setStatus(QuizAttemptStatus.SUBMITTED);
                submitted.setObtainedMarks(new BigDecimal("0.00"));

                when(quizAttemptRepository.findByIdAndQuizId(802, 100)).thenReturn(Optional.of(submitted));

                Question descQ = new Question(
                                course, instructor, "Explain polymorphism", QuestionType.DESCRIPTIVE,
                                null, null,
                                new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO, "Rubric", true);
                descQ.setId(60);

                QuizAttemptAnswer unpubAns = new QuizAttemptAnswer(
                                submitted, descQ, "Student explanation",
                                new BigDecimal("8.50"), new BigDecimal("10.00"),
                                EvaluationStatus.EVALUATED_UNPUBLISHED, "Instructor draft note", baseTime);
                unpubAns.setId(11);

                when(quizAttemptAnswerRepository.findByAttemptId(802)).thenReturn(List.of(unpubAns));
                when(courseRepository.findById(10)).thenReturn(Optional.of(course));

                QuizEvaluationSummary instructorSummary = attemptService.getAttemptEvaluation(instructor, 100, 802);

                assertNotNull(instructorSummary);
                assertEquals(1, instructorSummary.getResults().size());
                QuestionEvaluationResult result = instructorSummary.getResults().get(0);

                // Instructor CAN see unpublished marks and feedback
                assertEquals(EvaluationStatus.EVALUATED_UNPUBLISHED, result.getStatus());
                assertEquals(new BigDecimal("8.50"), result.getAwardedMarks());
                assertEquals("Instructor draft note", result.getFeedback());
        }

        @Test
        void testGetAttemptEvaluation_StudentCanSeePublishedMarks() {
                QuizAttempt submitted = new QuizAttempt(
                                publishedQuiz, enrolledStudent, 1, baseTime, baseTime.plusMinutes(30));
                submitted.setId(803);
                submitted.setStatus(QuizAttemptStatus.SUBMITTED);
                submitted.setObtainedMarks(new BigDecimal("8.50"));

                when(quizAttemptRepository.findByIdAndQuizId(803, 100)).thenReturn(Optional.of(submitted));

                Question descQ = new Question(
                                course, instructor, "Explain polymorphism", QuestionType.DESCRIPTIVE,
                                null, null,
                                new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO, "Rubric", true);
                descQ.setId(60);

                QuizAttemptAnswer pubAns = new QuizAttemptAnswer(
                                submitted, descQ, "Student explanation",
                                new BigDecimal("8.50"), new BigDecimal("10.00"),
                                EvaluationStatus.PUBLISHED, "Great explanation!", baseTime);
                pubAns.setId(12);

                when(quizAttemptAnswerRepository.findByAttemptId(803)).thenReturn(List.of(pubAns));

                QuizEvaluationSummary studentSummary = attemptService.getAttemptEvaluation(enrolledStudent, 100, 803);

                assertNotNull(studentSummary);
                assertEquals(1, studentSummary.getResults().size());
                QuestionEvaluationResult result = studentSummary.getResults().get(0);

                // Published marks and feedback ARE visible to students
                assertEquals(EvaluationStatus.PUBLISHED, result.getStatus());
                assertEquals(new BigDecimal("8.50"), result.getAwardedMarks());
                assertEquals("Great explanation!", result.getFeedback());
                assertFalse(result.isRequiresManualGrading());
        }

        @Test
        void testGetAttemptEvaluation_StudentCannotSeeFeedbackWhenFeedbackReleasedIsFalse() {
                publishedQuiz.setFeedbackReleased(false);
                QuizAttempt submitted = new QuizAttempt(
                                publishedQuiz, enrolledStudent, 1, baseTime, baseTime.plusMinutes(30));
                submitted.setId(804);
                submitted.setStatus(QuizAttemptStatus.SUBMITTED);
                submitted.setIsPublished(true);
                submitted.setOverallFeedback("Overall great job!");

                when(quizAttemptRepository.findByIdAndQuizId(804, 100)).thenReturn(Optional.of(submitted));

                Question descQ = new Question(
                                course, instructor, "Explain polymorphism", QuestionType.DESCRIPTIVE,
                                null, null,
                                new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO, "Rubric", true);
                descQ.setId(61);

                QuizAttemptAnswer pubAns = new QuizAttemptAnswer(
                                submitted, descQ, "Student explanation",
                                new BigDecimal("8.50"), new BigDecimal("10.00"),
                                EvaluationStatus.PUBLISHED, "Great explanation!", baseTime);
                pubAns.setId(13);

                when(quizAttemptAnswerRepository.findByAttemptId(804)).thenReturn(List.of(pubAns));

                QuizEvaluationSummary studentSummary = attemptService.getAttemptEvaluation(enrolledStudent, 100, 804);

                assertNotNull(studentSummary);
                assertFalse(studentSummary.getFeedbackReleased());
                // When feedbackReleased is false, student should not see answer feedback or overall feedback
                assertNull(studentSummary.getOverallFeedback());
                assertEquals(1, studentSummary.getResults().size());
                assertNull(studentSummary.getResults().get(0).getFeedback());
        }

        @Test
        void testGetAttemptEvaluation_StudentCannotSeeOverallFeedbackWhenUnpublished() {
                publishedQuiz.setFeedbackReleased(true);
                QuizAttempt submitted = new QuizAttempt(
                                publishedQuiz, enrolledStudent, 1, baseTime, baseTime.plusMinutes(30));
                submitted.setId(805);
                submitted.setStatus(QuizAttemptStatus.SUBMITTED);
                submitted.setIsPublished(false);
                submitted.setOverallFeedback("Draft instructor comment");

                when(quizAttemptRepository.findByIdAndQuizId(805, 100)).thenReturn(Optional.of(submitted));
                when(quizAttemptAnswerRepository.findByAttemptId(805)).thenReturn(List.of());

                QuizEvaluationSummary studentSummary = attemptService.getAttemptEvaluation(enrolledStudent, 100, 805);

                assertNotNull(studentSummary);
                assertFalse(studentSummary.getResultsPublished());
                // Unpublished overall feedback must NOT be exposed to student
                assertNull(studentSummary.getOverallFeedback());
        }

        @Test
        void testGetAttemptEvaluation_StudentCanSeeOverallFeedbackWhenPublishedAndFeedbackReleased() {
                publishedQuiz.setFeedbackReleased(true);
                QuizAttempt submitted = new QuizAttempt(
                                publishedQuiz, enrolledStudent, 1, baseTime, baseTime.plusMinutes(30));
                submitted.setId(806);
                submitted.setStatus(QuizAttemptStatus.SUBMITTED);
                submitted.setIsPublished(true);
                submitted.setOverallFeedback("Excellent performance throughout!");

                when(quizAttemptRepository.findByIdAndQuizId(806, 100)).thenReturn(Optional.of(submitted));
                when(quizAttemptAnswerRepository.findByAttemptId(806)).thenReturn(List.of());

                QuizEvaluationSummary studentSummary = attemptService.getAttemptEvaluation(enrolledStudent, 100, 806);

                assertNotNull(studentSummary);
                assertTrue(studentSummary.getResultsPublished());
                assertTrue(studentSummary.getFeedbackReleased());
                assertEquals("Excellent performance throughout!", studentSummary.getOverallFeedback());
        }
}
