package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying Quiz Marking Rules (FR-I65, Step 25):
 * - Negative marking alone
 * - Partial marking alone
 * - Numerical tolerance alone
 * - Every combination of marking rules
 * - Determinism of auto-grader results
 * - Display of marking rules on quiz settings/start payload (DR-ED03)
 * - Marks-sum consistency validation
 */
class MarkingRulesAutoGradingTest {

    private AutoGradingService autoGradingService;
    private User instructor;
    private Course course;
    private Quiz quiz;

    @BeforeEach
    void setUp() {
        autoGradingService = new AutoGradingService();

        instructor = new User("Dr. Turing", "turing@olp.com", "hash", "instructor", "active");
        instructor.setId(10);

        course = new Course(instructor, "Computer Systems", "CS Course", CourseStatus.PUBLISHED);
        course.setId(20);

        quiz = new Quiz(course, null, null, instructor, "Marking Rules Quiz", "Desc", "Instructions", QuizStatus.PUBLISHED);
        quiz.setId(100);
        quiz.setTimeLimitMinutes(30);
        quiz.setMaxAttempts(2);
    }

    // =========================================================================
    // 1. NEGATIVE MARKING ALONE
    // =========================================================================

    @Test
    @DisplayName("Negative marking alone: Correct single-choice awards full marks")
    void testNegativeMarkingAlone_Correct() {
        Question q = new Question(course, instructor, "What is 2+2?", QuestionType.MULTIPLE_CHOICE,
                "[\"2\", \"3\", \"4\", \"5\"]", "4",
                new BigDecimal("4.00"), new BigDecimal("1.00"), BigDecimal.ZERO, null, false);
        q.setId(1);

        GradingPolicy policy = GradingPolicy.withNegativeMarking(true);
        QuestionEvaluationResult result = autoGradingService.evaluateQuestion(q, "4", policy);

        assertEquals(EvaluationStatus.CORRECT, result.getStatus());
        assertEquals(new BigDecimal("4.00"), result.getAwardedMarks());
    }

    @Test
    @DisplayName("Negative marking alone: Incorrect single-choice applies negative deduction")
    void testNegativeMarkingAlone_IncorrectDeduction() {
        Question q = new Question(course, instructor, "What is 2+2?", QuestionType.MULTIPLE_CHOICE,
                "[\"2\", \"3\", \"4\", \"5\"]", "4",
                new BigDecimal("4.00"), new BigDecimal("1.00"), BigDecimal.ZERO, null, false);
        q.setId(1);

        GradingPolicy policy = GradingPolicy.withNegativeMarking(true);
        QuestionEvaluationResult result = autoGradingService.evaluateQuestion(q, "3", policy);

        assertEquals(EvaluationStatus.INCORRECT, result.getStatus());
        assertEquals(new BigDecimal("-1.00"), result.getAwardedMarks());
    }

    @Test
    @DisplayName("Negative marking alone: Unanswered question awards zero marks without negative penalty")
    void testNegativeMarkingAlone_UnansweredZeroMarks() {
        Question q = new Question(course, instructor, "What is 2+2?", QuestionType.MULTIPLE_CHOICE,
                "[\"2\", \"3\", \"4\", \"5\"]", "4",
                new BigDecimal("4.00"), new BigDecimal("1.00"), BigDecimal.ZERO, null, false);
        q.setId(1);

        GradingPolicy policy = GradingPolicy.withNegativeMarking(true);
        QuestionEvaluationResult result = autoGradingService.evaluateQuestion(q, null, policy);

        assertEquals(EvaluationStatus.UNANSWERED, result.getStatus());
        assertEquals(BigDecimal.ZERO, result.getAwardedMarks());
    }

    // =========================================================================
    // 2. PARTIAL MARKING ALONE (MULTIPLE SELECT)
    // =========================================================================

    @Test
    @DisplayName("Partial marking alone: Full match awards full marks")
    void testPartialMarkingAlone_FullMatch() {
        Question q = new Question(course, instructor, "Select JVM languages", QuestionType.MULTIPLE_SELECT,
                "[\"Java\", \"Kotlin\", \"Scala\", \"C++\"]", "[\"Java\", \"Kotlin\", \"Scala\"]",
                new BigDecimal("6.00"), new BigDecimal("2.00"), BigDecimal.ZERO, null, false);
        q.setId(2);

        GradingPolicy policy = GradingPolicy.withPartialMarking(true);
        QuestionEvaluationResult result = autoGradingService.evaluateQuestion(q, "[\"Java\", \"Kotlin\", \"Scala\"]", policy);

        assertEquals(EvaluationStatus.CORRECT, result.getStatus());
        assertEquals(new BigDecimal("6.00"), result.getAwardedMarks());
    }

    @Test
    @DisplayName("Partial marking alone: Partial correct selection awards proportional fractional marks")
    void testPartialMarkingAlone_PartialSelection() {
        Question q = new Question(course, instructor, "Select JVM languages", QuestionType.MULTIPLE_SELECT,
                "[\"Java\", \"Kotlin\", \"Scala\", \"C++\"]", "[\"Java\", \"Kotlin\", \"Scala\"]",
                new BigDecimal("6.00"), new BigDecimal("2.00"), BigDecimal.ZERO, null, false);
        q.setId(2);

        // Student selected 2 out of 3 correct options (Java, Kotlin), 0 wrong options
        // Proportional: (2/3) * 6.00 = 4.00
        GradingPolicy policy = GradingPolicy.withPartialMarking(true);
        QuestionEvaluationResult result = autoGradingService.evaluateQuestion(q, "[\"Java\", \"Kotlin\"]", policy);

        assertEquals(EvaluationStatus.PARTIALLY_CORRECT, result.getStatus());
        assertEquals(new BigDecimal("4.00"), result.getAwardedMarks());
    }

    @Test
    @DisplayName("Partial marking alone: Wrong options penalize net correct count")
    void testPartialMarkingAlone_WrongOptionsPenalize() {
        Question q = new Question(course, instructor, "Select JVM languages", QuestionType.MULTIPLE_SELECT,
                "[\"Java\", \"Kotlin\", \"Scala\", \"C++\"]", "[\"Java\", \"Kotlin\", \"Scala\"]",
                new BigDecimal("6.00"), new BigDecimal("2.00"), BigDecimal.ZERO, null, false);
        q.setId(2);

        // Student selected 2 correct (Java, Kotlin) + 1 incorrect (C++)
        // Net correct = max(0, 2 - 1) = 1. Proportional: (1/3) * 6.00 = 2.00
        GradingPolicy policy = GradingPolicy.withPartialMarking(true);
        QuestionEvaluationResult result = autoGradingService.evaluateQuestion(q, "[\"Java\", \"Kotlin\", \"C++\"]", policy);

        assertEquals(EvaluationStatus.PARTIALLY_CORRECT, result.getStatus());
        assertEquals(new BigDecimal("2.00"), result.getAwardedMarks());
    }

    @Test
    @DisplayName("Partial marking alone: Zero net correct awards zero marks when negative marking disabled")
    void testPartialMarkingAlone_ZeroNetCorrectAwardsZero() {
        Question q = new Question(course, instructor, "Select JVM languages", QuestionType.MULTIPLE_SELECT,
                "[\"Java\", \"Kotlin\", \"Scala\", \"C++\"]", "[\"Java\", \"Kotlin\", \"Scala\"]",
                new BigDecimal("6.00"), new BigDecimal("2.00"), BigDecimal.ZERO, null, false);
        q.setId(2);

        // Student selected 1 correct (Java) + 2 incorrect options -> net correct = max(0, 1 - 2) = 0
        GradingPolicy policy = GradingPolicy.withPartialMarking(true);
        QuestionEvaluationResult result = autoGradingService.evaluateQuestion(q, "[\"Java\", \"C++\", \"Ruby\"]", policy);

        assertEquals(EvaluationStatus.INCORRECT, result.getStatus());
        assertEquals(new BigDecimal("0.00"), result.getAwardedMarks());
    }

    // =========================================================================
    // 3. NUMERICAL TOLERANCE ALONE
    // =========================================================================

    @Test
    @DisplayName("Numerical tolerance alone: Match within tolerance awards full marks")
    void testNumericalToleranceAlone_WithinTolerance() {
        Question q = new Question(course, instructor, "Compute value of e", QuestionType.NUMERICAL,
                null, "2.718",
                new BigDecimal("5.00"), BigDecimal.ZERO, new BigDecimal("0.01"), null, false);
        q.setId(3);

        QuestionEvaluationResult result = autoGradingService.evaluateQuestion(q, "2.720");

        assertEquals(EvaluationStatus.CORRECT, result.getStatus());
        assertEquals(new BigDecimal("5.00"), result.getAwardedMarks());
    }

    @Test
    @DisplayName("Numerical tolerance alone: Value outside tolerance awards zero marks")
    void testNumericalToleranceAlone_OutsideTolerance() {
        Question q = new Question(course, instructor, "Compute value of e", QuestionType.NUMERICAL,
                null, "2.718",
                new BigDecimal("5.00"), BigDecimal.ZERO, new BigDecimal("0.01"), null, false);
        q.setId(3);

        QuestionEvaluationResult result = autoGradingService.evaluateQuestion(q, "2.750");

        assertEquals(EvaluationStatus.INCORRECT, result.getStatus());
        assertEquals(new BigDecimal("0.00"), result.getAwardedMarks());
    }

    // =========================================================================
    // 4. COMBINATION: NEGATIVE MARKING + PARTIAL MARKING
    // =========================================================================

    @Test
    @DisplayName("Combination (Negative + Partial): Zero net correct applies negative deduction")
    void testCombination_NegativeAndPartial_ZeroNetCorrectDeducts() {
        Question q = new Question(course, instructor, "Select JVM languages", QuestionType.MULTIPLE_SELECT,
                "[\"Java\", \"Kotlin\", \"Scala\", \"C++\"]", "[\"Java\", \"Kotlin\", \"Scala\"]",
                new BigDecimal("6.00"), new BigDecimal("1.50"), BigDecimal.ZERO, null, false);
        q.setId(4);

        GradingPolicy policy = new GradingPolicy(true, true, null);
        // Student selected 1 correct + 2 wrong -> net correct = 0 -> falls back to negative failure marks
        QuestionEvaluationResult result = autoGradingService.evaluateQuestion(q, "[\"Java\", \"C++\", \"Go\"]", policy);

        assertEquals(EvaluationStatus.INCORRECT, result.getStatus());
        assertEquals(new BigDecimal("-1.50"), result.getAwardedMarks());
    }

    @Test
    @DisplayName("Combination (Negative + Partial): Single-choice incorrect applies negative deduction")
    void testCombination_NegativeAndPartial_SingleChoiceIncorrect() {
        Question q = new Question(course, instructor, "Is functional programming declarative?", QuestionType.TRUE_FALSE,
                "[\"TRUE\", \"FALSE\"]", "TRUE",
                new BigDecimal("3.00"), new BigDecimal("1.00"), BigDecimal.ZERO, null, false);
        q.setId(5);

        GradingPolicy policy = new GradingPolicy(true, true, null);
        QuestionEvaluationResult result = autoGradingService.evaluateQuestion(q, "FALSE", policy);

        assertEquals(EvaluationStatus.INCORRECT, result.getStatus());
        assertEquals(new BigDecimal("-1.00"), result.getAwardedMarks());
    }

    // =========================================================================
    // 5. COMBINATION: NEGATIVE MARKING + NUMERICAL TOLERANCE
    // =========================================================================

    @Test
    @DisplayName("Combination (Negative + Tolerance): Outside tolerance applies negative penalty")
    void testCombination_NegativeAndTolerance_OutsideToleranceDeducts() {
        Question q = new Question(course, instructor, "Gravitational acceleration g (m/s^2)", QuestionType.NUMERICAL,
                null, "9.81",
                new BigDecimal("4.00"), new BigDecimal("1.00"), new BigDecimal("0.05"), null, false);
        q.setId(6);

        GradingPolicy policy = new GradingPolicy(true, false, null);
        // Submitted 9.95 is outside 9.81 +/- 0.05 (tolerance 9.76 - 9.86)
        QuestionEvaluationResult result = autoGradingService.evaluateQuestion(q, "9.95", policy);

        assertEquals(EvaluationStatus.INCORRECT, result.getStatus());
        assertEquals(new BigDecimal("-1.00"), result.getAwardedMarks());
    }

    @Test
    @DisplayName("Combination (Negative + Tolerance): Within tolerance awards full marks without penalty")
    void testCombination_NegativeAndTolerance_WithinToleranceFullMarks() {
        Question q = new Question(course, instructor, "Gravitational acceleration g (m/s^2)", QuestionType.NUMERICAL,
                null, "9.81",
                new BigDecimal("4.00"), new BigDecimal("1.00"), new BigDecimal("0.05"), null, false);
        q.setId(6);

        GradingPolicy policy = new GradingPolicy(true, false, null);
        QuestionEvaluationResult result = autoGradingService.evaluateQuestion(q, "9.83", policy);

        assertEquals(EvaluationStatus.CORRECT, result.getStatus());
        assertEquals(new BigDecimal("4.00"), result.getAwardedMarks());
    }

    // =========================================================================
    // 6. COMBINATION: PARTIAL MARKING + NUMERICAL TOLERANCE
    // =========================================================================

    @Test
    @DisplayName("Combination (Partial + Tolerance): Both rules operate harmoniously in the same quiz")
    void testCombination_PartialAndTolerance_InSameQuiz() {
        Question q1 = new Question(course, instructor, "Select JVM languages", QuestionType.MULTIPLE_SELECT,
                "[\"Java\", \"Kotlin\", \"Scala\", \"C++\"]", "[\"Java\", \"Kotlin\", \"Scala\"]",
                new BigDecimal("6.00"), BigDecimal.ZERO, BigDecimal.ZERO, null, false);
        q1.setId(11);

        Question q2 = new Question(course, instructor, "Gravitational acceleration g (m/s^2)", QuestionType.NUMERICAL,
                null, "9.81",
                new BigDecimal("4.00"), BigDecimal.ZERO, new BigDecimal("0.05"), null, false);
        q2.setId(12);

        QuizQuestion qq1 = new QuizQuestion(quiz, q1, 1, new BigDecimal("6.00"), BigDecimal.ZERO, true);
        QuizQuestion qq2 = new QuizQuestion(quiz, q2, 2, new BigDecimal("4.00"), BigDecimal.ZERO, true);

        GradingPolicy policy = new GradingPolicy(false, true, new BigDecimal("0.05"));

        List<QuestionAnswerSubmission> submissions = List.of(
                new QuestionAnswerSubmission(11, "[\"Java\", \"Kotlin\"]"), // 2/3 of 6 = 4.00
                new QuestionAnswerSubmission(12, "9.83")                     // within tolerance = 4.00
        );

        QuizEvaluationSummary summary = autoGradingService.evaluateQuizSubmission(
                quiz, List.of(qq1, qq2), submissions, policy);

        assertEquals(new BigDecimal("8.00"), summary.getTotalAwardedMarks());
        assertEquals(new BigDecimal("10.00"), summary.getTotalMaxMarks());
        assertEquals(1, summary.getCorrectCount());
        assertEquals(1, summary.getPartiallyCorrectCount());
    }

    // =========================================================================
    // 7. COMBINATION: ALL THREE COMBINED (NEGATIVE + PARTIAL + TOLERANCE)
    // =========================================================================

    @Test
    @DisplayName("Combination (All Three): Negative + Partial + Tolerance combined full evaluation")
    void testCombination_AllThreeCombined() {
        Question q1 = new Question(course, instructor, "Capital of France?", QuestionType.MULTIPLE_CHOICE,
                "[\"Paris\", \"Berlin\"]", "Paris",
                new BigDecimal("2.00"), new BigDecimal("0.50"), BigDecimal.ZERO, null, false);
        q1.setId(21);

        Question q2 = new Question(course, instructor, "Select prime numbers", QuestionType.MULTIPLE_SELECT,
                "[\"2\", \"3\", \"4\", \"5\"]", "[\"2\", \"3\", \"5\"]",
                new BigDecimal("6.00"), new BigDecimal("1.00"), BigDecimal.ZERO, null, false);
        q2.setId(22);

        Question q3 = new Question(course, instructor, "Pi approximation", QuestionType.NUMERICAL,
                null, "3.1415",
                new BigDecimal("4.00"), new BigDecimal("1.00"), BigDecimal.ZERO, null, false);
        q3.setId(23);

        QuizQuestion qq1 = new QuizQuestion(quiz, q1, 1, new BigDecimal("2.00"), new BigDecimal("0.50"), true);
        QuizQuestion qq2 = new QuizQuestion(quiz, q2, 2, new BigDecimal("6.00"), new BigDecimal("1.00"), true);
        QuizQuestion qq3 = new QuizQuestion(quiz, q3, 3, new BigDecimal("4.00"), new BigDecimal("1.00"), true);

        // Policy with all 3 rules enabled and quiz-level tolerance 0.01
        GradingPolicy policy = new GradingPolicy(true, true, new BigDecimal("0.01"));

        List<QuestionAnswerSubmission> submissions = List.of(
                new QuestionAnswerSubmission(21, "Berlin"),             // Incorrect -> -0.50
                new QuestionAnswerSubmission(22, "[\"2\", \"3\"]"),     // 2/3 of 6 = +4.00
                new QuestionAnswerSubmission(23, "3.1450")             // Within 0.01 tolerance (diff 0.0035) -> +4.00
        );

        QuizEvaluationSummary summary = autoGradingService.evaluateQuizSubmission(
                quiz, List.of(qq1, qq2, qq3), submissions, policy);

        // Expected: -0.50 + 4.00 + 4.00 = 7.50
        assertEquals(new BigDecimal("7.50"), summary.getTotalAwardedMarks());
        assertEquals(new BigDecimal("12.00"), summary.getTotalMaxMarks());
        assertEquals(1, summary.getCorrectCount());
        assertEquals(1, summary.getIncorrectCount());
        assertEquals(1, summary.getPartiallyCorrectCount());
    }

    // =========================================================================
    // 8. DEFAULT POLICY (ALL RULES DISABLED)
    // =========================================================================

    @Test
    @DisplayName("Default policy: Disables negative marking and partial marking")
    void testDefaultPolicy_DisabledRules() {
        Question q1 = new Question(course, instructor, "Capital of France?", QuestionType.MULTIPLE_CHOICE,
                "[\"Paris\", \"Berlin\"]", "Paris",
                new BigDecimal("2.00"), new BigDecimal("0.50"), BigDecimal.ZERO, null, false);
        q1.setId(31);

        Question q2 = new Question(course, instructor, "Select prime numbers", QuestionType.MULTIPLE_SELECT,
                "[\"2\", \"3\", \"4\", \"5\"]", "[\"2\", \"3\", \"5\"]",
                new BigDecimal("6.00"), new BigDecimal("1.00"), BigDecimal.ZERO, null, false);
        q2.setId(32);

        GradingPolicy defaultPolicy = GradingPolicy.defaultPolicy();

        // q1 incorrect: awards 0.00 (not -0.50)
        QuestionEvaluationResult r1 = autoGradingService.evaluateQuestion(q1, "Berlin", defaultPolicy);
        assertEquals(EvaluationStatus.INCORRECT, r1.getStatus());
        assertEquals(new BigDecimal("0.00"), r1.getAwardedMarks());

        // q2 partial: awards 0.00 (all-or-nothing when partial marking disabled)
        QuestionEvaluationResult r2 = autoGradingService.evaluateQuestion(q2, "[\"2\", \"3\"]", defaultPolicy);
        assertEquals(EvaluationStatus.INCORRECT, r2.getStatus());
        assertEquals(new BigDecimal("0.00"), r2.getAwardedMarks());
    }

    // =========================================================================
    // 9. DETERMINISM: IDENTICAL INPUTS ALWAYS PRODUCE IDENTICAL RESULTS
    // =========================================================================

    @Test
    @DisplayName("Determinism: Multiple evaluation runs with identical inputs produce identical results")
    void testGraderDeterminism() {
        Question q = new Question(course, instructor, "What is 10/2?", QuestionType.NUMERICAL,
                null, "5.0",
                new BigDecimal("5.00"), new BigDecimal("1.00"), new BigDecimal("0.1"), null, false);
        q.setId(41);
        QuizQuestion qq = new QuizQuestion(quiz, q, 1, new BigDecimal("5.00"), new BigDecimal("1.00"), true);

        GradingPolicy policy = new GradingPolicy(true, true, new BigDecimal("0.1"));
        List<QuestionAnswerSubmission> submissions = List.of(new QuestionAnswerSubmission(41, "5.05"));

        QuizEvaluationSummary run1 = autoGradingService.evaluateQuizSubmission(quiz, List.of(qq), submissions, policy);
        QuizEvaluationSummary run2 = autoGradingService.evaluateQuizSubmission(quiz, List.of(qq), submissions, policy);
        QuizEvaluationSummary run3 = autoGradingService.evaluateQuizSubmission(quiz, List.of(qq), submissions, policy);

        assertEquals(run1.getTotalAwardedMarks(), run2.getTotalAwardedMarks());
        assertEquals(run2.getTotalAwardedMarks(), run3.getTotalAwardedMarks());
        assertEquals(run1.getCorrectCount(), run2.getCorrectCount());
        assertEquals(run2.getCorrectCount(), run3.getCorrectCount());
    }

    // =========================================================================
    // 10. DR-ED03: MARKING RULES DISPLAY & MARKS CONSISTENCY
    // =========================================================================

    @Test
    @DisplayName("DR-ED03: Marking rules and summary are correctly computed and visible on settings response")
    void testMarkingRulesDisplayOnQuizSettings() {
        quiz.setNegativeMarking(true);
        quiz.setPartialMarking(true);
        quiz.setNumericTolerance(new BigDecimal("0.05"));
        quiz.setTotalMarks(new BigDecimal("10.00"));

        Question q = new Question(course, instructor, "Question 1", QuestionType.MULTIPLE_CHOICE,
                "[\"A\", \"B\"]", "A",
                new BigDecimal("10.00"), new BigDecimal("2.00"), BigDecimal.ZERO, null, false);
        QuizQuestion qq = new QuizQuestion(quiz, q, 1, new BigDecimal("10.00"), new BigDecimal("2.00"), true);

        QuizService quizService = new QuizService(null, null, null, null, null, null, null, null);
        QuizSettingsResponse settingsResponse = quizService.buildQuizSettingsResponse(
                quiz, List.of(qq), LocalDateTime.now(), 0L);

        assertTrue(Boolean.TRUE.equals(settingsResponse.getNegativeMarking()));
        assertTrue(Boolean.TRUE.equals(settingsResponse.getPartialMarking()));
        assertEquals(new BigDecimal("0.05"), settingsResponse.getNumericTolerance());
        assertNotNull(settingsResponse.getMarkingRulesSummary());
        assertTrue(settingsResponse.getMarkingRulesSummary().contains("Negative marking is ENABLED"));
        assertTrue(settingsResponse.getMarkingRulesSummary().contains("Partial marking is ENABLED"));
        assertTrue(settingsResponse.getMarkingRulesSummary().contains("Numerical tolerance: \u00b10.05"));
        assertTrue(settingsResponse.isMarksConsistent());
    }

    @Test
    @DisplayName("DR-ED03: Marking rules summary handles default (all disabled)")
    void testMarkingRulesSummary_Default() {
        String summary = QuizSettingsResponse.computeMarkingRulesSummary(false, false, BigDecimal.ZERO);
        assertEquals(
                "Negative marking is DISABLED (no marks deducted for incorrect answers). Partial marking is DISABLED (all-or-nothing for multiple-select questions).",
                summary
        );
    }

    @Test
    @DisplayName("DR-ED03: Quiz entity toGradingPolicy creates policy matching configured rules")
    void testQuizToGradingPolicy() {
        quiz.setNegativeMarking(true);
        quiz.setPartialMarking(true);
        quiz.setNumericTolerance(new BigDecimal("0.10"));

        GradingPolicy policy = quiz.toGradingPolicy();

        assertTrue(policy.isEnableNegativeMarking());
        assertTrue(policy.isEnablePartialMarking());
        assertEquals(new BigDecimal("0.10"), policy.getCustomTolerance());
    }
}
