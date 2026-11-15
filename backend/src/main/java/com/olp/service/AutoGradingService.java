package com.olp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.GradingPolicy;
import com.olp.dto.QuestionAnswerSubmission;
import com.olp.dto.QuestionEvaluationResult;
import com.olp.dto.QuizEvaluationSummary;
import com.olp.entity.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Reusable Auto-Grading and Marking Service (FR-I22).
 * Evaluates submitted answers against configured question correct answers and marking schemes.
 * Deterministic: identical inputs always produce identical outputs.
 * Provides clear extensibility hooks for negative marking, partial marking, and numerical tolerance.
 */
@Service
public class AutoGradingService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================================================================
    // SINGLE QUESTION EVALUATION
    // =========================================================================

    public QuestionEvaluationResult evaluateQuestion(Question question, String submittedAnswer) {
        return evaluateQuestion(question, submittedAnswer, GradingPolicy.defaultPolicy());
    }

    public QuestionEvaluationResult evaluateQuestion(Question question, String submittedAnswer, GradingPolicy policy) {
        if (question == null) {
            return new QuestionEvaluationResult(
                    null, null, EvaluationStatus.INVALID_SUBMISSION,
                    BigDecimal.ZERO, BigDecimal.ZERO, submittedAnswer, null, null,
                    "Question cannot be null", false);
        }

        BigDecimal maxMarks = question.getDefaultMarks() != null ? question.getDefaultMarks() : BigDecimal.ONE;
        BigDecimal negativeMarks = question.getNegativeMarks() != null ? question.getNegativeMarks() : BigDecimal.ZERO;
        BigDecimal tolerance = resolveTolerance(question.getNumericalTolerance(), policy);

        return evaluateInternal(
                question.getId(),
                question.getQuestionType(),
                question.getCorrectAnswer(),
                maxMarks,
                negativeMarks,
                tolerance,
                question.getGradingRubric(),
                submittedAnswer,
                policy
        );
    }

    public QuestionEvaluationResult evaluateQuizQuestion(QuizQuestion quizQuestion, String submittedAnswer) {
        return evaluateQuizQuestion(quizQuestion, submittedAnswer, GradingPolicy.defaultPolicy());
    }

    public QuestionEvaluationResult evaluateQuizQuestion(QuizQuestion quizQuestion, String submittedAnswer, GradingPolicy policy) {
        if (quizQuestion == null || quizQuestion.getQuestion() == null) {
            return new QuestionEvaluationResult(
                    null, null, EvaluationStatus.INVALID_SUBMISSION,
                    BigDecimal.ZERO, BigDecimal.ZERO, submittedAnswer, null, null,
                    "Quiz question cannot be null", false);
        }

        Question question = quizQuestion.getQuestion();
        BigDecimal maxMarks = quizQuestion.getMarks() != null ? quizQuestion.getMarks() : question.getDefaultMarks();
        if (maxMarks == null) {
            maxMarks = BigDecimal.ONE;
        }

        BigDecimal negativeMarks = quizQuestion.getNegativeMarks() != null
                ? quizQuestion.getNegativeMarks()
                : question.getNegativeMarks();
        if (negativeMarks == null) {
            negativeMarks = BigDecimal.ZERO;
        }

        BigDecimal tolerance = resolveTolerance(question.getNumericalTolerance(), policy);

        return evaluateInternal(
                question.getId(),
                question.getQuestionType(),
                question.getCorrectAnswer(),
                maxMarks,
                negativeMarks,
                tolerance,
                question.getGradingRubric(),
                submittedAnswer,
                policy
        );
    }

    // =========================================================================
    // QUIZ SUBMISSION EVALUATION
    // =========================================================================

    public QuizEvaluationSummary evaluateQuizSubmission(Quiz quiz,
                                                        List<QuizQuestion> quizQuestions,
                                                        List<QuestionAnswerSubmission> submissions,
                                                        GradingPolicy customPolicy) {
        GradingPolicy policy = customPolicy != null ? customPolicy : GradingPolicy.defaultPolicy();
        Map<Integer, String> answersByQuestionId = new LinkedHashMap<>();

        if (submissions != null) {
            for (QuestionAnswerSubmission sub : submissions) {
                if (sub != null && sub.getQuestionId() != null) {
                    String ans = sub.getAnswer();
                    if ((ans == null || ans.trim().isEmpty()) && sub.getSelectedOptions() != null && !sub.getSelectedOptions().isEmpty()) {
                        ans = toJson(sub.getSelectedOptions());
                    }
                    answersByQuestionId.put(sub.getQuestionId(), ans);
                }
            }
        }

        QuizEvaluationSummary summary = new QuizEvaluationSummary();
        if (quiz != null) {
            summary.setQuizId(quiz.getId());
            summary.setPassingMarks(quiz.getPassingMarks());
        }

        List<QuizQuestion> safeQuestions = quizQuestions != null ? quizQuestions : List.of();
        BigDecimal totalAwarded = BigDecimal.ZERO;
        BigDecimal totalMax = BigDecimal.ZERO;
        int correctCount = 0;
        int incorrectCount = 0;
        int partiallyCorrectCount = 0;
        int manualGradingCount = 0;
        int unansweredCount = 0;
        List<QuestionEvaluationResult> results = new ArrayList<>(safeQuestions.size());

        for (QuizQuestion qq : safeQuestions) {
            String answer = answersByQuestionId.get(qq.getQuestion().getId());
            QuestionEvaluationResult res = evaluateQuizQuestion(qq, answer, policy);
            results.add(res);

            totalAwarded = totalAwarded.add(res.getAwardedMarks());
            totalMax = totalMax.add(res.getMaxMarks());

            switch (res.getStatus()) {
                case CORRECT -> correctCount++;
                case INCORRECT -> incorrectCount++;
                case PARTIALLY_CORRECT -> partiallyCorrectCount++;
                case MANUAL_GRADING_REQUIRED -> manualGradingCount++;
                case UNANSWERED -> unansweredCount++;
                case INVALID_SUBMISSION -> incorrectCount++;
            }
        }

        summary.setTotalAwardedMarks(totalAwarded.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalMaxMarks(totalMax.setScale(2, RoundingMode.HALF_UP));
        summary.setCorrectCount(correctCount);
        summary.setIncorrectCount(incorrectCount);
        summary.setPartiallyCorrectCount(partiallyCorrectCount);
        summary.setManualGradingCount(manualGradingCount);
        summary.setUnansweredCount(unansweredCount);
        summary.setResults(results);

        if (summary.getPassingMarks() != null) {
            summary.setPassed(totalAwarded.compareTo(summary.getPassingMarks()) >= 0);
        }

        return summary;
    }

    // =========================================================================
    // CORE DETERMINISTIC EVALUATOR PER QUESTION TYPE
    // =========================================================================

    private QuestionEvaluationResult evaluateInternal(Integer questionId,
                                                      QuestionType questionType,
                                                      String correctAnswer,
                                                      BigDecimal maxMarks,
                                                      BigDecimal negativeMarks,
                                                      BigDecimal tolerance,
                                                      String rubric,
                                                      String submittedAnswer,
                                                      GradingPolicy policy) {
        if (questionType == null) {
            return new QuestionEvaluationResult(
                    questionId, null, EvaluationStatus.INVALID_SUBMISSION,
                    BigDecimal.ZERO, maxMarks, submittedAnswer, correctAnswer, tolerance,
                    "Question type is null", false);
        }

        String trimmedSubmission = trimToNull(submittedAnswer);

        // 1. DESCRIPTIVE Questions are never auto-evaluated; routed to manual grading
        if (questionType == QuestionType.DESCRIPTIVE) {
            String feedback = rubric != null && !rubric.trim().isEmpty()
                    ? "Submission marked for manual grading (Rubric: " + rubric.trim() + ")"
                    : "Submission marked for manual grading by instructor";
            return new QuestionEvaluationResult(
                    questionId, questionType, EvaluationStatus.MANUAL_GRADING_REQUIRED,
                    BigDecimal.ZERO, maxMarks, trimmedSubmission, null, null,
                    feedback, true);
        }

        // 2. Unanswered check for objective questions
        if (trimmedSubmission == null) {
            return new QuestionEvaluationResult(
                    questionId, questionType, EvaluationStatus.UNANSWERED,
                    BigDecimal.ZERO, maxMarks, null, correctAnswer, tolerance,
                    "No answer provided", false);
        }

        return switch (questionType) {
            case MULTIPLE_CHOICE -> evaluateMultipleChoice(questionId, correctAnswer, maxMarks, negativeMarks, trimmedSubmission, policy);
            case MULTIPLE_SELECT -> evaluateMultipleSelect(questionId, correctAnswer, maxMarks, negativeMarks, trimmedSubmission, policy);
            case TRUE_FALSE -> evaluateTrueFalse(questionId, correctAnswer, maxMarks, negativeMarks, trimmedSubmission, policy);
            case NUMERICAL -> evaluateNumerical(questionId, correctAnswer, maxMarks, negativeMarks, tolerance, trimmedSubmission, policy);
            case SHORT_ANSWER -> evaluateShortAnswer(questionId, correctAnswer, maxMarks, negativeMarks, trimmedSubmission, policy);
            case DESCRIPTIVE -> throw new IllegalStateException("Descriptive already handled");
        };
    }

    private QuestionEvaluationResult evaluateMultipleChoice(Integer questionId,
                                                            String correctAnswer,
                                                            BigDecimal maxMarks,
                                                            BigDecimal negativeMarks,
                                                            String submission,
                                                            GradingPolicy policy) {
        String expected = trimToNull(correctAnswer);
        if (expected == null) {
            return new QuestionEvaluationResult(
                    questionId, QuestionType.MULTIPLE_CHOICE, EvaluationStatus.INVALID_SUBMISSION,
                    BigDecimal.ZERO, maxMarks, submission, null, null,
                    "Question has no configured correct answer", false);
        }

        if (submission.equalsIgnoreCase(expected)) {
            return new QuestionEvaluationResult(
                    questionId, QuestionType.MULTIPLE_CHOICE, EvaluationStatus.CORRECT,
                    maxMarks, maxMarks, submission, expected, null,
                    "Correct", false);
        }

        BigDecimal awarded = resolveFailureMarks(negativeMarks, policy);
        return new QuestionEvaluationResult(
                questionId, QuestionType.MULTIPLE_CHOICE, EvaluationStatus.INCORRECT,
                awarded, maxMarks, submission, expected, null,
                "Incorrect", false);
    }

    private QuestionEvaluationResult evaluateMultipleSelect(Integer questionId,
                                                            String correctAnswer,
                                                            BigDecimal maxMarks,
                                                            BigDecimal negativeMarks,
                                                            String submission,
                                                            GradingPolicy policy) {
        Set<String> expectedSet = parseNormalizedStringSet(correctAnswer);
        if (expectedSet.isEmpty()) {
            return new QuestionEvaluationResult(
                    questionId, QuestionType.MULTIPLE_SELECT, EvaluationStatus.INVALID_SUBMISSION,
                    BigDecimal.ZERO, maxMarks, submission, null, null,
                    "Question has no configured correct answers", false);
        }

        Set<String> studentSet = parseNormalizedStringSet(submission);
        if (studentSet.isEmpty()) {
            return new QuestionEvaluationResult(
                    questionId, QuestionType.MULTIPLE_SELECT, EvaluationStatus.UNANSWERED,
                    BigDecimal.ZERO, maxMarks, submission, correctAnswer, null,
                    "No options selected", false);
        }

        // Full match
        if (expectedSet.equals(studentSet)) {
            return new QuestionEvaluationResult(
                    questionId, QuestionType.MULTIPLE_SELECT, EvaluationStatus.CORRECT,
                    maxMarks, maxMarks, submission, correctAnswer, null,
                    "All correct options selected", false);
        }

        // Extensible hook for partial marking (FR-I65)
        if (policy != null && policy.isEnablePartialMarking()) {
            int totalCorrect = expectedSet.size();
            int selectedCorrect = 0;
            int selectedIncorrect = 0;

            for (String opt : studentSet) {
                if (expectedSet.contains(opt)) {
                    selectedCorrect++;
                } else {
                    selectedIncorrect++;
                }
            }

            int netCorrect = Math.max(0, selectedCorrect - selectedIncorrect);
            if (netCorrect > 0) {
                BigDecimal fraction = BigDecimal.valueOf(netCorrect)
                        .divide(BigDecimal.valueOf(totalCorrect), 4, RoundingMode.HALF_UP);
                BigDecimal partialAward = maxMarks.multiply(fraction).setScale(2, RoundingMode.HALF_UP);

                if (partialAward.compareTo(maxMarks) < 0) {
                    return new QuestionEvaluationResult(
                            questionId, QuestionType.MULTIPLE_SELECT, EvaluationStatus.PARTIALLY_CORRECT,
                            partialAward, maxMarks, submission, correctAnswer, null,
                            "Partially correct (" + selectedCorrect + "/" + totalCorrect + " correct, "
                                    + selectedIncorrect + " incorrect)", false);
                }
            }
        }

        BigDecimal awarded = resolveFailureMarks(negativeMarks, policy);
        return new QuestionEvaluationResult(
                questionId, QuestionType.MULTIPLE_SELECT, EvaluationStatus.INCORRECT,
                awarded, maxMarks, submission, correctAnswer, null,
                "Incorrect option selection", false);
    }

    private QuestionEvaluationResult evaluateTrueFalse(Integer questionId,
                                                       String correctAnswer,
                                                       BigDecimal maxMarks,
                                                       BigDecimal negativeMarks,
                                                       String submission,
                                                       GradingPolicy policy) {
        String normalizedExpected = normalizeBooleanString(correctAnswer);
        if (normalizedExpected == null) {
            return new QuestionEvaluationResult(
                    questionId, QuestionType.TRUE_FALSE, EvaluationStatus.INVALID_SUBMISSION,
                    BigDecimal.ZERO, maxMarks, submission, correctAnswer, null,
                    "Configured answer is not a valid boolean", false);
        }

        String normalizedStudent = normalizeBooleanString(submission);
        if (normalizedStudent == null) {
            return new QuestionEvaluationResult(
                    questionId, QuestionType.TRUE_FALSE, EvaluationStatus.INVALID_SUBMISSION,
                    BigDecimal.ZERO, maxMarks, submission, normalizedExpected, null,
                    "Submitted answer '" + submission + "' is not a valid TRUE or FALSE value", false);
        }

        if (normalizedStudent.equals(normalizedExpected)) {
            return new QuestionEvaluationResult(
                    questionId, QuestionType.TRUE_FALSE, EvaluationStatus.CORRECT,
                    maxMarks, maxMarks, normalizedStudent, normalizedExpected, null,
                    "Correct", false);
        }

        BigDecimal awarded = resolveFailureMarks(negativeMarks, policy);
        return new QuestionEvaluationResult(
                questionId, QuestionType.TRUE_FALSE, EvaluationStatus.INCORRECT,
                awarded, maxMarks, normalizedStudent, normalizedExpected, null,
                "Incorrect", false);
    }

    private QuestionEvaluationResult evaluateNumerical(Integer questionId,
                                                       String correctAnswer,
                                                       BigDecimal maxMarks,
                                                       BigDecimal negativeMarks,
                                                       BigDecimal tolerance,
                                                       String submission,
                                                       GradingPolicy policy) {
        BigDecimal expectedNum;
        try {
            expectedNum = new BigDecimal(correctAnswer.trim());
        } catch (Exception ex) {
            return new QuestionEvaluationResult(
                    questionId, QuestionType.NUMERICAL, EvaluationStatus.INVALID_SUBMISSION,
                    BigDecimal.ZERO, maxMarks, submission, correctAnswer, tolerance,
                    "Configured answer is not a valid number", false);
        }

        BigDecimal studentNum;
        try {
            studentNum = new BigDecimal(submission.trim());
        } catch (Exception ex) {
            return new QuestionEvaluationResult(
                    questionId, QuestionType.NUMERICAL, EvaluationStatus.INVALID_SUBMISSION,
                    BigDecimal.ZERO, maxMarks, submission, correctAnswer, tolerance,
                    "Submitted answer '" + submission + "' is not a valid number", false);
        }

        BigDecimal effectiveTolerance = tolerance != null && tolerance.compareTo(BigDecimal.ZERO) >= 0
                ? tolerance
                : BigDecimal.ZERO;
        BigDecimal diff = studentNum.subtract(expectedNum).abs();

        if (diff.compareTo(effectiveTolerance) <= 0) {
            return new QuestionEvaluationResult(
                    questionId, QuestionType.NUMERICAL, EvaluationStatus.CORRECT,
                    maxMarks, maxMarks, submission, correctAnswer, effectiveTolerance,
                    "Correct (diff: " + diff.toPlainString() + " <= tolerance: " + effectiveTolerance.toPlainString() + ")",
                    false);
        }

        BigDecimal awarded = resolveFailureMarks(negativeMarks, policy);
        return new QuestionEvaluationResult(
                questionId, QuestionType.NUMERICAL, EvaluationStatus.INCORRECT,
                awarded, maxMarks, submission, correctAnswer, effectiveTolerance,
                "Incorrect: " + submission + " (expected " + expectedNum.toPlainString() + " \u00b1 " + effectiveTolerance.toPlainString() + ")",
                false);
    }

    private QuestionEvaluationResult evaluateShortAnswer(Integer questionId,
                                                         String correctAnswer,
                                                         BigDecimal maxMarks,
                                                         BigDecimal negativeMarks,
                                                         String submission,
                                                         GradingPolicy policy) {
        String expected = trimToNull(correctAnswer);
        if (expected == null) {
            return new QuestionEvaluationResult(
                    questionId, QuestionType.SHORT_ANSWER, EvaluationStatus.INVALID_SUBMISSION,
                    BigDecimal.ZERO, maxMarks, submission, null, null,
                    "Question has no configured correct answer", false);
        }

        if (submission.equalsIgnoreCase(expected)) {
            return new QuestionEvaluationResult(
                    questionId, QuestionType.SHORT_ANSWER, EvaluationStatus.CORRECT,
                    maxMarks, maxMarks, submission, expected, null,
                    "Correct", false);
        }

        BigDecimal awarded = resolveFailureMarks(negativeMarks, policy);
        return new QuestionEvaluationResult(
                questionId, QuestionType.SHORT_ANSWER, EvaluationStatus.INCORRECT,
                awarded, maxMarks, submission, expected, null,
                "Incorrect", false);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private BigDecimal resolveTolerance(BigDecimal questionTolerance, GradingPolicy policy) {
        if (policy != null && policy.getCustomTolerance() != null && policy.getCustomTolerance().compareTo(BigDecimal.ZERO) > 0) {
            return policy.getCustomTolerance();
        }
        return questionTolerance != null && questionTolerance.compareTo(BigDecimal.ZERO) >= 0
                ? questionTolerance
                : BigDecimal.ZERO;
    }

    private BigDecimal resolveFailureMarks(BigDecimal negativeMarks, GradingPolicy policy) {
        if (policy != null && policy.isEnableNegativeMarking() && negativeMarks != null && negativeMarks.compareTo(BigDecimal.ZERO) > 0) {
            return negativeMarks.negate().setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private Set<String> parseNormalizedStringSet(String input) {
        Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        String trimmed = trimToNull(input);
        if (trimmed == null) {
            return set;
        }

        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                List<String> list = objectMapper.readValue(trimmed, new TypeReference<>() {});
                for (String s : list) {
                    String clean = trimToNull(s);
                    if (clean != null) {
                        set.add(clean);
                    }
                }
                return set;
            } catch (Exception ignored) {
                // fallback to comma parsing
            }
        }

        if (trimmed.contains(",")) {
            for (String part : trimmed.split(",")) {
                String clean = trimToNull(part);
                if (clean != null) {
                    set.add(clean);
                }
            }
            return set;
        }

        set.add(trimmed);
        return set;
    }

    private String normalizeBooleanString(String value) {
        String clean = trimToNull(value);
        if (clean == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(clean) || "t".equalsIgnoreCase(clean) || "1".equalsIgnoreCase(clean) || "yes".equalsIgnoreCase(clean)) {
            return "TRUE";
        }
        if ("false".equalsIgnoreCase(clean) || "f".equalsIgnoreCase(clean) || "0".equalsIgnoreCase(clean) || "no".equalsIgnoreCase(clean)) {
            return "FALSE";
        }
        return null;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ex) {
            return String.valueOf(obj);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

