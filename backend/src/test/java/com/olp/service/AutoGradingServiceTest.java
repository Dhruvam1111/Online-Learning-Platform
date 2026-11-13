package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Unit Tests for AutoGradingService (FR-I22).
 * Verifies auto-grading for all 5 question types, marking schemes, determinism,
 * quiz-question overrides, and extensibility hooks (negative marking, partial marking, tolerance).
 */
class AutoGradingServiceTest {

    private AutoGradingService gradingService;
    private User instructor;
    private Course course;

    @BeforeEach
    void setUp() {
        gradingService = new AutoGradingService();
        instructor = new User("Dr. Hopper", "hopper@olp.com", "hash", "instructor", "active");
        instructor.setId(1);
        course = new Course(instructor, "Computer Science", "Desc", CourseStatus.PUBLISHED);
        course.setId(10);
    }

    // =========================================================================
    // 1. MULTIPLE_CHOICE EVALUATION TESTS
    // =========================================================================

    @Test
    @DisplayName("MULTIPLE_CHOICE: Correct single answer awards full marks")
    void testMultipleChoice_Correct() {
        Question q = new Question(
                course, instructor, "What is 2+2?", QuestionType.MULTIPLE_CHOICE,
                "[\"2\", \"3\", \"4\", \"5\"]", "4",
                new BigDecimal("2.00"), new BigDecimal("0.50"), BigDecimal.ZERO, null, false);
        q.setId(101);

        QuestionEvaluationResult result = gradingService.evaluateQuestion(q, "4");

        assertEquals(EvaluationStatus.CORRECT, result.getStatus());
        assertEquals(new BigDecimal("2.00"), result.getAwardedMarks());
        assertEquals(new BigDecimal("2.00"), result.getMaxMarks());
        assertFalse(result.isRequiresManualGrading());
    }

    @Test
    @DisplayName("MULTIPLE_CHOICE: Case-insensitive and trimmed answer matches correctly")
    void testMultipleChoice_CaseInsensitiveAndTrimmed() {
        Question q = new Question(
                course, instructor, "Primary language of Java platform?", QuestionType.MULTIPLE_CHOICE,
                "[\"Java\", \"Python\"]", "Java",
                new BigDecimal("3.00"), BigDecimal.ZERO, BigDecimal.ZERO, null, false);
        q.setId(102);

        QuestionEvaluationResult result = gradingService.evaluateQuestion(q, "  java  ");

        assertEquals(EvaluationStatus.CORRECT, result.getStatus());
        assertEquals(new BigDecimal("3.00"), result.getAwardedMarks());
    }

    @Test
    @DisplayName("MULTIPLE_CHOICE: Incorrect answer awards zero marks without negative marking")
    void testMultipleChoice_Incorrect_DefaultZero() {
        Question q = new Question(
                course, instructor, "What is 2+2?", QuestionType.MULTIPLE_CHOICE,
                "[\"2\", \"3\", \"4\", \"5\"]", "4",
                new BigDecimal("2.00"), new BigDecimal("0.50"), BigDecimal.ZERO, null, false);
        q.setId(103);

        QuestionEvaluationResult result = gradingService.evaluateQuestion(q, "3");

        assertEquals(EvaluationStatus.INCORRECT, result.getStatus());
        assertEquals(new BigDecimal("0.00"), result.getAwardedMarks());
        assertEquals(new BigDecimal("2.00"), result.getMaxMarks());
    }

    @Test
    @DisplayName("MULTIPLE_CHOICE: Unanswered submission awards zero and UNANSWERED status")
    void testMultipleChoice_Unanswered() {
        Question q = new Question(
                course, instructor, "What is 2+2?", QuestionType.MULTIPLE_CHOICE,
                "[\"2\", \"3\", \"4\", \"5\"]", "4",
                new BigDecimal("2.00"), BigDecimal.ZERO, BigDecimal.ZERO, null, false);
        q.setId(104);

        QuestionEvaluationResult resultNull = gradingService.evaluateQuestion(q, null);
        assertEquals(EvaluationStatus.UNANSWERED, resultNull.getStatus());
        assertEquals(BigDecimal.ZERO, resultNull.getAwardedMarks());

        QuestionEvaluationResult resultEmpty = gradingService.evaluateQuestion(q, "   ");
        assertEquals(EvaluationStatus.UNANSWERED, resultEmpty.getStatus());
    }

    // =========================================================================
    // 2. MULTIPLE_SELECT EVALUATION TESTS
    // =========================================================================

    @Test
    @DisplayName("MULTIPLE_SELECT: Full exact set match awards full marks (comma separated or JSON array)")
    void testMultipleSelect_Correct_ExactMatch() {
        Question q = new Question(
                course, instructor, "Select JVM languages", QuestionType.MULTIPLE_SELECT,
                "[\"Java\", \"Kotlin\", \"Scala\", \"C#\"]", "[\"Java\", \"Kotlin\", \"Scala\"]",
                new BigDecimal("4.00"), BigDecimal.ZERO, BigDecimal.ZERO, null, false);
        q.setId(201);

        // JSON array format submission
        QuestionEvaluationResult resJson = gradingService.evaluateQuestion(q, "[\"Kotlin\", \"java\", \"SCALA\"]");
        assertEquals(EvaluationStatus.CORRECT, resJson.getStatus());
        assertEquals(new BigDecimal("4.00"), resJson.getAwardedMarks());

        // Comma separated format submission
        QuestionEvaluationResult resComma = gradingService.evaluateQuestion(q, "Scala, Kotlin, Java");
        assertEquals(EvaluationStatus.CORRECT, resComma.getStatus());
        assertEquals(new BigDecimal("4.00"), resComma.getAwardedMarks());
    }

    @Test
    @DisplayName("MULTIPLE_SELECT: Incomplete or incorrect set awards zero without partial marking")
    void testMultipleSelect_Incomplete_DefaultZero() {
        Question q = new Question(
                course, instructor, "Select JVM languages", QuestionType.MULTIPLE_SELECT,
                "[\"Java\", \"Kotlin\", \"Scala\", \"C#\"]", "[\"Java\", \"Kotlin\", \"Scala\"]",
                new BigDecimal("4.00"), BigDecimal.ZERO, BigDecimal.ZERO, null, false);
        q.setId(202);

        // Subset only
        QuestionEvaluationResult subset = gradingService.evaluateQuestion(q, "Java, Kotlin");
        assertEquals(EvaluationStatus.INCORRECT, subset.getStatus());
        assertEquals(new BigDecimal("0.00"), subset.getAwardedMarks());

        // Extra wrong option
        QuestionEvaluationResult wrong = gradingService.evaluateQuestion(q, "Java, Kotlin, Scala, C#");
        assertEquals(EvaluationStatus.INCORRECT, wrong.getStatus());
        assertEquals(new BigDecimal("0.00"), wrong.getAwardedMarks());
    }

    @Test
    @DisplayName("MULTIPLE_SELECT: Partial marking hook awards proportional score (FR-I65 extension)")
    void testMultipleSelect_PartialMarkingHook() {
        Question q = new Question(
                course, instructor, "Select 4 prime numbers", QuestionType.MULTIPLE_SELECT,
                "[\"2\", \"3\", \"5\", \"7\", \"9\"]", "[\"2\", \"3\", \"5\", \"7\"]",
                new BigDecimal("4.00"), BigDecimal.ZERO, BigDecimal.ZERO, null, false);
        q.setId(203);

        GradingPolicy partialPolicy = GradingPolicy.withPartialMarking(true);

        // 3 of 4 correct selected (0 incorrect) -> 3/4 * 4.00 = 3.00
        QuestionEvaluationResult res3 = gradingService.evaluateQuestion(q, "2, 3, 5", partialPolicy);
        assertEquals(EvaluationStatus.PARTIALLY_CORRECT, res3.getStatus());
        assertEquals(new BigDecimal("3.00"), res3.getAwardedMarks());

        // 2 of 4 correct selected, 1 incorrect selected -> net (2-1) = 1/4 * 4.00 = 1.00
        QuestionEvaluationResult resNet = gradingService.evaluateQuestion(q, "2, 3, 9", partialPolicy);
        assertEquals(EvaluationStatus.PARTIALLY_CORRECT, resNet.getStatus());
        assertEquals(new BigDecimal("1.00"), resNet.getAwardedMarks());
    }

    // =========================================================================
    // 3. TRUE_FALSE EVALUATION TESTS
    // =========================================================================

    @Test
    @DisplayName("TRUE_FALSE: Correct boolean evaluates to CORRECT")
    void testTrueFalse_Correct() {
        Question qTrue = new Question(
                course, instructor, "Is Java object-oriented?", QuestionType.TRUE_FALSE,
                "[\"TRUE\", \"FALSE\"]", "TRUE",
                new BigDecimal("1.50"), BigDecimal.ZERO, BigDecimal.ZERO, null, false);
        qTrue.setId(301);

        assertEquals(EvaluationStatus.CORRECT, gradingService.evaluateQuestion(qTrue, "true").getStatus());
        assertEquals(EvaluationStatus.CORRECT, gradingService.evaluateQuestion(qTrue, "TRUE").getStatus());
        assertEquals(EvaluationStatus.CORRECT, gradingService.evaluateQuestion(qTrue, "1").getStatus());
        assertEquals(EvaluationStatus.CORRECT, gradingService.evaluateQuestion(qTrue, "yes").getStatus());

        Question qFalse = new Question(
                course, instructor, "Is HTML a programming language?", QuestionType.TRUE_FALSE,
                "[\"TRUE\", \"FALSE\"]", "FALSE",
                new BigDecimal("1.50"), BigDecimal.ZERO, BigDecimal.ZERO, null, false);
        qFalse.setId(302);

        assertEquals(EvaluationStatus.CORRECT, gradingService.evaluateQuestion(qFalse, "false").getStatus());
        assertEquals(EvaluationStatus.CORRECT, gradingService.evaluateQuestion(qFalse, "0").getStatus());
        assertEquals(EvaluationStatus.CORRECT, gradingService.evaluateQuestion(qFalse, "no").getStatus());
    }

    @Test
    @DisplayName("TRUE_FALSE: Incorrect boolean evaluates to INCORRECT")
    void testTrueFalse_Incorrect() {
        Question q = new Question(
                course, instructor, "Is Java statically typed?", QuestionType.TRUE_FALSE,
                "[\"TRUE\", \"FALSE\"]", "TRUE",
                new BigDecimal("2.00"), BigDecimal.ZERO, BigDecimal.ZERO, null, false);
        q.setId(303);

        QuestionEvaluationResult res = gradingService.evaluateQuestion(q, "false");
        assertEquals(EvaluationStatus.INCORRECT, res.getStatus());
        assertEquals(new BigDecimal("0.00"), res.getAwardedMarks());
    }

    @Test
    @DisplayName("TRUE_FALSE: Non-boolean text returns INVALID_SUBMISSION")
    void testTrueFalse_InvalidText() {
        Question q = new Question(
                course, instructor, "Is Java statically typed?", QuestionType.TRUE_FALSE,
                "[\"TRUE\", \"FALSE\"]", "TRUE",
                new BigDecimal("2.00"), BigDecimal.ZERO, BigDecimal.ZERO, null, false);
        q.setId(304);

        QuestionEvaluationResult res = gradingService.evaluateQuestion(q, "maybe");
        assertEquals(EvaluationStatus.INVALID_SUBMISSION, res.getStatus());
        assertEquals(BigDecimal.ZERO, res.getAwardedMarks());
    }

    // =========================================================================
    // 4. NUMERICAL EVALUATION TESTS
    // =========================================================================

    @Test
    @DisplayName("NUMERICAL: Exact and within tolerance awards full marks")
    void testNumerical_WithinTolerance() {
        Question q = new Question(
                course, instructor, "What is Pi to 2 decimals?", QuestionType.NUMERICAL,
                null, "3.14159",
                new BigDecimal("2.50"), BigDecimal.ZERO, new BigDecimal("0.01"), null, false);
        q.setId(401);

        // Exact
        QuestionEvaluationResult resExact = gradingService.evaluateQuestion(q, "3.14159");
        assertEquals(EvaluationStatus.CORRECT, resExact.getStatus());
        assertEquals(new BigDecimal("2.50"), resExact.getAwardedMarks());

        // Within tolerance (+0.005)
        QuestionEvaluationResult resWithin = gradingService.evaluateQuestion(q, "3.14659");
        assertEquals(EvaluationStatus.CORRECT, resWithin.getStatus());
        assertEquals(new BigDecimal("2.50"), resWithin.getAwardedMarks());

        // Within tolerance (-0.009)
        QuestionEvaluationResult resMinus = gradingService.evaluateQuestion(q, "3.13259");
        assertEquals(EvaluationStatus.CORRECT, resMinus.getStatus());
    }

    @Test
    @DisplayName("NUMERICAL: Outside tolerance awards zero marks")
    void testNumerical_OutsideTolerance() {
        Question q = new Question(
                course, instructor, "Calculate speed of light (x10^8 m/s)", QuestionType.NUMERICAL,
                null, "3.00",
                new BigDecimal("5.00"), BigDecimal.ZERO, new BigDecimal("0.05"), null, false);
        q.setId(402);

        QuestionEvaluationResult res = gradingService.evaluateQuestion(q, "3.10");
        assertEquals(EvaluationStatus.INCORRECT, res.getStatus());
        assertEquals(new BigDecimal("0.00"), res.getAwardedMarks());
    }

    @Test
    @DisplayName("NUMERICAL: Unparseable numeric text returns INVALID_SUBMISSION")
    void testNumerical_UnparseableText() {
        Question q = new Question(
                course, instructor, "Enter a number", QuestionType.NUMERICAL,
                null, "100.0",
                new BigDecimal("2.00"), BigDecimal.ZERO, new BigDecimal("0.5"), null, false);
        q.setId(403);

        QuestionEvaluationResult res = gradingService.evaluateQuestion(q, "one hundred");
        assertEquals(EvaluationStatus.INVALID_SUBMISSION, res.getStatus());
        assertEquals(BigDecimal.ZERO, res.getAwardedMarks());
    }

    @Test
    @DisplayName("NUMERICAL: Custom tolerance policy hook overrides question tolerance (FR-I65)")
    void testNumerical_CustomToleranceHook() {
        Question q = new Question(
                course, instructor, "Approximate e", QuestionType.NUMERICAL,
                null, "2.718",
                new BigDecimal("3.00"), BigDecimal.ZERO, new BigDecimal("0.001"), null, false);
        q.setId(404);

        // Under question tolerance 0.001, 2.75 is incorrect
        assertEquals(EvaluationStatus.INCORRECT, gradingService.evaluateQuestion(q, "2.75").getStatus());

        // Under relaxed policy tolerance 0.05, 2.75 is within 0.032 diff -> CORRECT
        GradingPolicy policy = new GradingPolicy(false, false, new BigDecimal("0.05"));
        QuestionEvaluationResult res = gradingService.evaluateQuestion(q, "2.75", policy);
        assertEquals(EvaluationStatus.CORRECT, res.getStatus());
        assertEquals(new BigDecimal("3.00"), res.getAwardedMarks());
    }

    // =========================================================================
    // 5. DESCRIPTIVE QUESTION TESTS (MANUAL GRADING ROUTING)
    // =========================================================================

    @Test
    @DisplayName("DESCRIPTIVE: Never auto-evaluated; routed to MANUAL_GRADING_REQUIRED with 0 marks")
    void testDescriptive_RoutedToManualGrading() {
        Question q = new Question(
                course, instructor, "Explain polymorphism in OOP with examples", QuestionType.DESCRIPTIVE,
                null, null,
                new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                "Grade based on clarity (4 pts), correct syntax (3 pts), realism (3 pts)", true);
        q.setId(501);

        QuestionEvaluationResult res = gradingService.evaluateQuestion(q, "Polymorphism means many forms...");

        assertEquals(EvaluationStatus.MANUAL_GRADING_REQUIRED, res.getStatus());
        assertEquals(BigDecimal.ZERO, res.getAwardedMarks());
        assertEquals(new BigDecimal("10.00"), res.getMaxMarks());
        assertTrue(res.isRequiresManualGrading());
        assertTrue(res.getFeedback().contains("Rubric"));
    }

    // =========================================================================
    // 6. EXTENSIBILITY: NEGATIVE MARKING HOOK TESTS
    // =========================================================================

    @Test
    @DisplayName("Negative Marking Hook: Applies negative penalty when enabled in policy")
    void testNegativeMarkingHook_EnabledInPolicy() {
        Question q = new Question(
                course, instructor, "What is capital of France?", QuestionType.MULTIPLE_CHOICE,
                "[\"Paris\", \"London\", \"Berlin\", \"Rome\"]", "Paris",
                new BigDecimal("4.00"), new BigDecimal("1.00"), BigDecimal.ZERO, null, false);
        q.setId(601);

        GradingPolicy negativeEnabled = GradingPolicy.withNegativeMarking(true);
        QuestionEvaluationResult res = gradingService.evaluateQuestion(q, "London", negativeEnabled);

        assertEquals(EvaluationStatus.INCORRECT, res.getStatus());
        assertEquals(new BigDecimal("-1.00"), res.getAwardedMarks());
    }

    @Test
    @DisplayName("Negative Marking Hook: Does not apply negative penalty when disabled in policy")
    void testNegativeMarkingHook_DisabledByDefault() {
        Question q = new Question(
                course, instructor, "What is capital of France?", QuestionType.MULTIPLE_CHOICE,
                "[\"Paris\", \"London\", \"Berlin\", \"Rome\"]", "Paris",
                new BigDecimal("4.00"), new BigDecimal("1.00"), BigDecimal.ZERO, null, false);
        q.setId(602);

        QuestionEvaluationResult res = gradingService.evaluateQuestion(q, "London", GradingPolicy.defaultPolicy());

        assertEquals(EvaluationStatus.INCORRECT, res.getStatus());
        assertEquals(new BigDecimal("0.00"), res.getAwardedMarks());
    }

    // =========================================================================
    // 7. QUIZ-QUESTION OVERRIDES TEST
    // =========================================================================

    @Test
    @DisplayName("QuizQuestion: Overrides question default marks and negative marks")
    void testQuizQuestion_MarksOverride() {
        Question baseQuestion = new Question(
                course, instructor, "Basic Q", QuestionType.MULTIPLE_CHOICE,
                "[\"A\", \"B\"]", "A",
                new BigDecimal("1.00"), new BigDecimal("0.25"), BigDecimal.ZERO, null, false);
        baseQuestion.setId(701);

        Quiz quiz = new Quiz(course, null, null, instructor, "Midterm", "Desc", null, QuizStatus.PUBLISHED);
        quiz.setId(10);

        QuizQuestion qq = new QuizQuestion(
                quiz, baseQuestion, 1,
                new BigDecimal("5.00"), // Custom marks override
                new BigDecimal("1.50"), // Custom negative marks override
                true
        );

        // Correct answer gets overridden 5.00 marks
        QuestionEvaluationResult resCorrect = gradingService.evaluateQuizQuestion(qq, "A");
        assertEquals(EvaluationStatus.CORRECT, resCorrect.getStatus());
        assertEquals(new BigDecimal("5.00"), resCorrect.getAwardedMarks());
        assertEquals(new BigDecimal("5.00"), resCorrect.getMaxMarks());

        // Incorrect with negative marking policy gets overridden -1.50
        QuestionEvaluationResult resNeg = gradingService.evaluateQuizQuestion(qq, "B", GradingPolicy.withNegativeMarking(true));
        assertEquals(EvaluationStatus.INCORRECT, resNeg.getStatus());
        assertEquals(new BigDecimal("-1.50"), resNeg.getAwardedMarks());
    }

    // =========================================================================
    // 8. DETERMINISM TEST
    // =========================================================================

    @Test
    @DisplayName("Determinism: Identical inputs evaluated 50 times produce identical results")
    void testDeterminism_IdenticalInputsProduceIdenticalOutputs() {
        Question q = new Question(
                course, instructor, "Solve: 10 + 20", QuestionType.NUMERICAL,
                null, "30.0",
                new BigDecimal("5.00"), new BigDecimal("1.00"), new BigDecimal("0.1"), null, false);
        q.setId(801);

        QuestionEvaluationResult baseline = gradingService.evaluateQuestion(q, "30.05");

        for (int i = 0; i < 50; i++) {
            QuestionEvaluationResult run = gradingService.evaluateQuestion(q, "30.05");
            assertEquals(baseline.getStatus(), run.getStatus(), "Status must be deterministic on run " + i);
            assertEquals(baseline.getAwardedMarks(), run.getAwardedMarks(), "Awarded marks must be deterministic on run " + i);
            assertEquals(baseline.getMaxMarks(), run.getMaxMarks());
            assertEquals(baseline.getFeedback(), run.getFeedback());
        }
    }

    // =========================================================================
    // 9. FULL QUIZ SUBMISSION EVALUATION TEST
    // =========================================================================

    @Test
    @DisplayName("Quiz Submission Evaluation: Sums marks correctly across mixed question types and handles pass/fail")
    void testEvaluateQuizSubmission_MixedQuestions() {
        Quiz quiz = new Quiz(course, null, null, instructor, "Final Exam", "Desc", null, QuizStatus.PUBLISHED);
        quiz.setId(901);
        quiz.setPassingMarks(new BigDecimal("7.00"));

        Question q1 = new Question(course, instructor, "Q1 MC", QuestionType.MULTIPLE_CHOICE, "[\"A\", \"B\"]", "A", new BigDecimal("3.00"), BigDecimal.ZERO, BigDecimal.ZERO, null, false);
        q1.setId(1);
        Question q2 = new Question(course, instructor, "Q2 TF", QuestionType.TRUE_FALSE, "[\"TRUE\", \"FALSE\"]", "TRUE", new BigDecimal("2.00"), BigDecimal.ZERO, BigDecimal.ZERO, null, false);
        q2.setId(2);
        Question q3 = new Question(course, instructor, "Q3 NUM", QuestionType.NUMERICAL, null, "10.0", new BigDecimal("5.00"), BigDecimal.ZERO, new BigDecimal("0.5"), null, false);
        q3.setId(3);
        Question q4 = new Question(course, instructor, "Q4 DESC", QuestionType.DESCRIPTIVE, null, null, new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO, "Rubric", true);
        q4.setId(4);

        QuizQuestion qq1 = new QuizQuestion(quiz, q1, 1, new BigDecimal("3.00"), BigDecimal.ZERO, true);
        QuizQuestion qq2 = new QuizQuestion(quiz, q2, 2, new BigDecimal("2.00"), BigDecimal.ZERO, true);
        QuizQuestion qq3 = new QuizQuestion(quiz, q3, 3, new BigDecimal("5.00"), BigDecimal.ZERO, true);
        QuizQuestion qq4 = new QuizQuestion(quiz, q4, 4, new BigDecimal("10.00"), BigDecimal.ZERO, true);

        List<QuizQuestion> quizQuestions = List.of(qq1, qq2, qq3, qq4);

        // Student answers Q1 correct, Q2 incorrect, Q3 correct within tolerance, Q4 descriptive
        List<QuestionAnswerSubmission> answers = List.of(
                new QuestionAnswerSubmission(1, "A"),
                new QuestionAnswerSubmission(2, "FALSE"),
                new QuestionAnswerSubmission(3, "10.2"),
                new QuestionAnswerSubmission(4, "Detailed student essay")
        );

        QuizEvaluationSummary summary = gradingService.evaluateQuizSubmission(
                quiz, quizQuestions, answers, GradingPolicy.defaultPolicy());

        assertNotNull(summary);
        assertEquals(901, summary.getQuizId());
        // Awarded: 3.00 (Q1) + 0.00 (Q2) + 5.00 (Q3) + 0.00 (Q4 descriptive) = 8.00
        assertEquals(new BigDecimal("8.00"), summary.getTotalAwardedMarks());
        // Max total: 3.00 + 2.00 + 5.00 + 10.00 = 20.00
        assertEquals(new BigDecimal("20.00"), summary.getTotalMaxMarks());
        assertEquals(1, summary.getCorrectCount()); // Q1 (or Q1 + Q3 = 2 correct)
        // Let's verify: Q1 is correct, Q3 is correct -> 2 correct!
        assertEquals(2, summary.getCorrectCount());
        assertEquals(1, summary.getIncorrectCount()); // Q2
        assertEquals(1, summary.getManualGradingCount()); // Q4
        assertEquals(0, summary.getUnansweredCount());
        // 8.00 >= passing marks 7.00 -> passed == true
        assertTrue(summary.getPassed());
        assertEquals(4, summary.getResults().size());
    }
}

