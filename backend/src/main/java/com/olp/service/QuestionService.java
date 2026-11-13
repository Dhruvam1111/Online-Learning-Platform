package com.olp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.ApiResponse;
import com.olp.dto.QuestionRequest;
import com.olp.dto.QuestionResponse;
import com.olp.entity.Course;
import com.olp.entity.Question;
import com.olp.entity.QuestionType;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.QuestionRepository;
import com.olp.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service implementing FR-I21 (Quiz Question Types CRUD, validation, manual grading routing,
 * and quiz-independent question management).
 */
@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuestionService(QuestionRepository questionRepository,
                           CourseRepository courseRepository,
                           CourseInstructorRepository courseInstructorRepository,
                           UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.userRepository = userRepository;
    }

    public User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        if (principal instanceof String email) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new InvalidCredentialsException("User not found"));
        }
        throw new InvalidCredentialsException("Invalid authentication principal");
    }

    @Transactional
    public QuestionResponse createQuestion(User currentUser, QuestionRequest request) {
        validateInstructor(currentUser);
        if (request == null) {
            throw new IllegalArgumentException("Question request cannot be null");
        }

        Course course = request.getCourseId() != null
                ? verifyCourseAssociation(request.getCourseId(), currentUser)
                : null;

        QuestionType questionType = QuestionType.fromString(request.getQuestionType());
        ValidatedQuestionData validated = validateAndNormalizeQuestion(questionType, request);

        Question question = new Question(
                course,
                currentUser,
                validated.questionText(),
                questionType,
                validated.optionsJson(),
                validated.correctAnswer(),
                validated.defaultMarks(),
                validated.negativeMarks(),
                validated.numericalTolerance(),
                validated.gradingRubric(),
                validated.requiresManualGrading()
        );
        question.setTopic(trimToNull(request.getTopic()));
        question.setDifficulty(trimToNull(request.getDifficulty()));
        question.setExplanation(trimToNull(request.getExplanation()));
        question.setIsBankQuestion(Boolean.TRUE.equals(request.getIsBankQuestion()));

        Question saved = questionRepository.save(question);
        return mapToQuestionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestions(User currentUser,
                                               Integer courseId,
                                               String questionTypeFilter,
                                               Boolean manualGradingOnly) {
        validateInstructor(currentUser);
        List<Question> questions;
        if (courseId != null) {
            verifyCourseAssociation(courseId, currentUser);
            questions = questionRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
        } else {
            questions = questionRepository.findAccessibleByInstructorId(currentUser.getId());
        }

        QuestionType parsedType = (questionTypeFilter != null && !questionTypeFilter.trim().isEmpty())
                ? QuestionType.fromString(questionTypeFilter)
                : null;

        return questions.stream()
                .filter(q -> parsedType == null || q.getQuestionType() == parsedType)
                .filter(q -> !Boolean.TRUE.equals(manualGradingOnly) || requiresManualGrading(q))
                .map(this::mapToQuestionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestionById(User currentUser, Integer questionId) {
        Question question = findAndVerifyQuestionAccess(questionId, currentUser);
        return mapToQuestionResponse(question);
    }

    @Transactional
    public QuestionResponse updateQuestion(User currentUser, Integer questionId, QuestionRequest request) {
        validateInstructor(currentUser);
        if (request == null) {
            throw new IllegalArgumentException("Question update request cannot be null");
        }
        Question question = findAndVerifyQuestionAccess(questionId, currentUser);

        if (request.getCourseId() != null) {
            Course course = verifyCourseAssociation(request.getCourseId(), currentUser);
            question.setCourse(course);
        }

        QuestionType questionType = QuestionType.fromString(request.getQuestionType());
        ValidatedQuestionData validated = validateAndNormalizeQuestion(questionType, request);

        question.setQuestionText(validated.questionText());
        question.setQuestionType(questionType);
        question.setOptionsJson(validated.optionsJson());
        question.setCorrectAnswer(validated.correctAnswer());
        question.setDefaultMarks(validated.defaultMarks());
        question.setNegativeMarks(validated.negativeMarks());
        question.setNumericalTolerance(validated.numericalTolerance());
        question.setGradingRubric(validated.gradingRubric());
        question.setRequiresManualGrading(validated.requiresManualGrading());

        if (request.getTopic() != null) {
            question.setTopic(trimToNull(request.getTopic()));
        }
        if (request.getDifficulty() != null) {
            question.setDifficulty(trimToNull(request.getDifficulty()));
        }
        if (request.getExplanation() != null) {
            question.setExplanation(trimToNull(request.getExplanation()));
        }
        if (request.getIsBankQuestion() != null) {
            question.setIsBankQuestion(request.getIsBankQuestion());
        }

        Question saved = questionRepository.save(question);
        return mapToQuestionResponse(saved);
    }

    @Transactional
    public ApiResponse deleteQuestion(User currentUser, Integer questionId) {
        Question question = findAndVerifyQuestionAccess(questionId, currentUser);
        questionRepository.delete(question);
        return new ApiResponse(true, "Question deleted successfully");
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getManualGradingQuestions(User currentUser, Integer courseId) {
        validateInstructor(currentUser);
        List<Question> questions;
        if (courseId != null) {
            verifyCourseAssociation(courseId, currentUser);
            questions = questionRepository.findManualGradingQuestionsByCourseId(courseId);
        } else {
            questions = questionRepository.findManualGradingQuestionsByInstructorId(currentUser.getId());
        }
        return questions.stream().map(this::mapToQuestionResponse).toList();
    }

    @Transactional
    public QuestionResponse sendForManualGrading(User currentUser, Integer questionId, String rubricOverride) {
        Question question = findAndVerifyQuestionAccess(questionId, currentUser);
        if (question.getQuestionType() != QuestionType.DESCRIPTIVE) {
            throw new IllegalArgumentException(
                    "Only DESCRIPTIVE questions can be routed for manual grading, found: " + question.getQuestionType());
        }
        String effectiveRubric = trimToNull(rubricOverride) != null
                ? trimToNull(rubricOverride)
                : trimToNull(question.getGradingRubric());
        if (effectiveRubric == null) {
            throw new IllegalArgumentException("A grading rubric is required before routing for manual grading");
        }

        question.setGradingRubric(effectiveRubric);
        question.setRequiresManualGrading(Boolean.TRUE);
        Question saved = questionRepository.save(question);
        return mapToQuestionResponse(saved);
    }

    public boolean requiresManualGrading(Question question) {
        return question != null
                && (Boolean.TRUE.equals(question.getRequiresManualGrading())
                || question.getQuestionType() == QuestionType.DESCRIPTIVE);
    }

    // =========================================================================
    // TYPE-SPECIFIC VALIDATION & NORMALIZATION
    // =========================================================================

    private record ValidatedQuestionData(
            String questionText,
            String optionsJson,
            String correctAnswer,
            BigDecimal defaultMarks,
            BigDecimal negativeMarks,
            BigDecimal numericalTolerance,
            String gradingRubric,
            Boolean requiresManualGrading) {}

    private ValidatedQuestionData validateAndNormalizeQuestion(QuestionType type, QuestionRequest request) {
        String questionText = trimToNull(request.getQuestionText());
        if (questionText == null) {
            throw new IllegalArgumentException("Question text is required");
        }

        BigDecimal defaultMarks = request.getDefaultMarks() != null ? request.getDefaultMarks() : BigDecimal.ONE;
        if (defaultMarks.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Marks cannot be negative");
        }

        BigDecimal negativeMarks = request.getNegativeMarks() != null ? request.getNegativeMarks() : BigDecimal.ZERO;
        if (negativeMarks.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Negative marks cannot be negative");
        }

        BigDecimal tolerance = request.getNumericalTolerance() != null ? request.getNumericalTolerance() : BigDecimal.ZERO;
        if (tolerance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Numerical tolerance cannot be negative");
        }

        return switch (type) {
            case MULTIPLE_CHOICE -> validateMultipleChoice(questionText, request, defaultMarks, negativeMarks);
            case MULTIPLE_SELECT -> validateMultipleSelect(questionText, request, defaultMarks, negativeMarks);
            case TRUE_FALSE -> validateTrueFalse(questionText, request, defaultMarks, negativeMarks);
            case NUMERICAL -> validateNumerical(questionText, request, defaultMarks, negativeMarks, tolerance);
            case DESCRIPTIVE -> validateDescriptive(questionText, request, negativeMarks);
            case SHORT_ANSWER -> new ValidatedQuestionData(
                    questionText,
                    trimToNull(request.getOptionsJson()),
                    trimToNull(request.getCorrectAnswer()),
                    defaultMarks,
                    negativeMarks,
                    BigDecimal.ZERO,
                    trimToNull(request.getGradingRubric()),
                    Boolean.TRUE.equals(request.getRequiresManualGrading()));
        };
    }

    private ValidatedQuestionData validateMultipleChoice(String questionText,
                                                         QuestionRequest request,
                                                         BigDecimal defaultMarks,
                                                         BigDecimal negativeMarks) {
        List<String> options = resolveOptions(request.getOptions(), request.getOptionsJson());
        if (options.size() < 2) {
            throw new IllegalArgumentException("MULTIPLE_CHOICE questions require at least 2 distinct options");
        }

        List<String> answers = resolveAnswersList(request.getCorrectAnswers(), request.getCorrectAnswer(), false);
        if (answers.size() != 1) {
            throw new IllegalArgumentException("MULTIPLE_CHOICE questions must have exactly one correct answer");
        }

        String matchedOption = findMatchingOption(answers.get(0), options);
        if (matchedOption == null) {
            throw new IllegalArgumentException(
                    "Correct answer '" + answers.get(0) + "' must match one of the provided options");
        }

        return new ValidatedQuestionData(
                questionText,
                toJson(options),
                matchedOption,
                defaultMarks,
                negativeMarks,
                BigDecimal.ZERO,
                null,
                Boolean.FALSE
        );
    }

    private ValidatedQuestionData validateMultipleSelect(String questionText,
                                                         QuestionRequest request,
                                                         BigDecimal defaultMarks,
                                                         BigDecimal negativeMarks) {
        List<String> options = resolveOptions(request.getOptions(), request.getOptionsJson());
        if (options.size() < 2) {
            throw new IllegalArgumentException("MULTIPLE_SELECT questions require at least 2 distinct options");
        }

        List<String> rawAnswers = resolveAnswersList(request.getCorrectAnswers(), request.getCorrectAnswer(), true);
        if (rawAnswers.isEmpty()) {
            throw new IllegalArgumentException("MULTIPLE_SELECT questions require at least one correct answer");
        }

        List<String> normalizedAnswers = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String ans : rawAnswers) {
            String matched = findMatchingOption(ans, options);
            if (matched == null) {
                throw new IllegalArgumentException(
                        "Correct answer '" + ans + "' must match one of the provided options");
            }
            if (seen.add(matched.toLowerCase())) {
                normalizedAnswers.add(matched);
            }
        }

        return new ValidatedQuestionData(
                questionText,
                toJson(options),
                toJson(normalizedAnswers),
                defaultMarks,
                negativeMarks,
                BigDecimal.ZERO,
                null,
                Boolean.FALSE
        );
    }

    private ValidatedQuestionData validateTrueFalse(String questionText,
                                                    QuestionRequest request,
                                                    BigDecimal defaultMarks,
                                                    BigDecimal negativeMarks) {
        List<String> providedOptions = resolveOptions(request.getOptions(), request.getOptionsJson());
        if (!providedOptions.isEmpty()) {
            if (providedOptions.size() != 2) {
                throw new IllegalArgumentException("TRUE_FALSE questions may only have TRUE and FALSE options");
            }
            for (String opt : providedOptions) {
                if (!"TRUE".equalsIgnoreCase(opt) && !"FALSE".equalsIgnoreCase(opt)) {
                    throw new IllegalArgumentException("TRUE_FALSE options must be TRUE and FALSE");
                }
            }
        }

        List<String> answers = resolveAnswersList(request.getCorrectAnswers(), request.getCorrectAnswer(), false);
        if (answers.size() != 1) {
            throw new IllegalArgumentException("TRUE_FALSE questions must have exactly one correct answer (TRUE or FALSE)");
        }
        String raw = answers.get(0);
        if (!"TRUE".equalsIgnoreCase(raw) && !"FALSE".equalsIgnoreCase(raw)) {
            throw new IllegalArgumentException("TRUE_FALSE correct answer must be TRUE or FALSE");
        }

        return new ValidatedQuestionData(
                questionText,
                toJson(List.of("TRUE", "FALSE")),
                raw.toUpperCase(Locale.ROOT),
                defaultMarks,
                negativeMarks,
                BigDecimal.ZERO,
                null,
                Boolean.FALSE
        );
    }

    private ValidatedQuestionData validateNumerical(String questionText,
                                                    QuestionRequest request,
                                                    BigDecimal defaultMarks,
                                                    BigDecimal negativeMarks,
                                                    BigDecimal tolerance) {
        String rawAnswer = trimToNull(request.getCorrectAnswer());
        if (rawAnswer == null) {
            throw new IllegalArgumentException("NUMERICAL questions require a numeric correct answer");
        }
        try {
            new BigDecimal(rawAnswer);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("NUMERICAL correct answer must be a valid number: " + rawAnswer);
        }

        return new ValidatedQuestionData(
                questionText,
                null,
                rawAnswer,
                defaultMarks,
                negativeMarks,
                tolerance,
                null,
                Boolean.FALSE
        );
    }

    private ValidatedQuestionData validateDescriptive(String questionText,
                                                      QuestionRequest request,
                                                      BigDecimal negativeMarks) {
        BigDecimal maxMarks = request.getDefaultMarks();
        if (maxMarks == null || maxMarks.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("DESCRIPTIVE questions require positive maximum marks (defaultMarks > 0)");
        }
        String rubric = trimToNull(request.getGradingRubric());
        if (rubric == null) {
            throw new IllegalArgumentException("DESCRIPTIVE questions require a grading rubric");
        }

        return new ValidatedQuestionData(
                questionText,
                null,
                null,
                maxMarks,
                negativeMarks,
                BigDecimal.ZERO,
                rubric,
                Boolean.TRUE
        );
    }

    // =========================================================================
    // PARSING & AUTHORIZATION HELPERS
    // =========================================================================

    private List<String> resolveOptions(List<String> optionsList, String optionsJson) {
        List<String> raw = new ArrayList<>();
        if (optionsList != null && !optionsList.isEmpty()) {
            raw.addAll(optionsList);
        } else if (trimToNull(optionsJson) != null) {
            raw.addAll(parseJsonStringList(optionsJson, "optionsJson"));
        }

        List<String> cleaned = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String item : raw) {
            String trimmed = trimToNull(item);
            if (trimmed == null) {
                throw new IllegalArgumentException("Question options cannot contain blank values");
            }
            if (!seen.add(trimmed.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Duplicate option found: " + trimmed);
            }
            cleaned.add(trimmed);
        }
        return cleaned;
    }

    private List<String> resolveAnswersList(List<String> answersList, String singleOrJsonAnswer, boolean allowCommaSeparated) {
        List<String> raw = new ArrayList<>();
        if (answersList != null && !answersList.isEmpty()) {
            raw.addAll(answersList);
        } else {
            String trimmed = trimToNull(singleOrJsonAnswer);
            if (trimmed != null) {
                if (trimmed.startsWith("[")) {
                    raw.addAll(parseJsonStringList(trimmed, "correctAnswer"));
                } else if (allowCommaSeparated && trimmed.contains(",")) {
                    for (String part : trimmed.split(",")) {
                        raw.add(part);
                    }
                } else {
                    raw.add(trimmed);
                }
            }
        }

        List<String> cleaned = new ArrayList<>();
        for (String item : raw) {
            String val = trimToNull(item);
            if (val != null) {
                cleaned.add(val);
            }
        }
        return cleaned;
    }

    private List<String> parseJsonStringList(String json, String fieldName) {
        try {
            return objectMapper.readValue(json.trim(), new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JSON array for " + fieldName);
        }
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize list to JSON", ex);
        }
    }

    private String findMatchingOption(String candidate, List<String> options) {
        for (String opt : options) {
            if (opt.equalsIgnoreCase(candidate)) {
                return opt;
            }
        }
        return null;
    }

    private Question findAndVerifyQuestionAccess(Integer questionId, User currentUser) {
        validateInstructor(currentUser);
        if (questionId == null) {
            throw new IllegalArgumentException("Question ID cannot be null");
        }
        Question question = questionRepository.findByIdWithDetails(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getRole());
        if (isAdmin) {
            return question;
        }
        if (question.getCourse() != null) {
            verifyCourseAssociation(question.getCourse().getId(), currentUser);
            return question;
        }
        if (question.getCreatedBy() == null || !question.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Cannot manage question owned by another instructor");
        }
        return question;
    }

    public Course verifyCourseAssociation(Integer courseId, User currentUser) {
        validateInstructor(currentUser);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        boolean isOwner = course.getInstructor() != null && currentUser.getId().equals(course.getInstructor().getId());
        boolean isCoInstructor = courseInstructorRepository != null &&
                courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, currentUser.getId());
        boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getRole());

        if (!isOwner && !isCoInstructor && !isAdmin) {
            throw new AccessDeniedException("Cannot manage questions: you are not an authorized instructor for this course");
        }
        return course;
    }

    private void validateInstructor(User currentUser) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (!"instructor".equalsIgnoreCase(currentUser.getRole()) && !"admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied: only instructors or admins can manage questions");
        }
        if ("instructor".equalsIgnoreCase(currentUser.getRole())
                && !"active".equalsIgnoreCase(currentUser.getAccountStatus())) {
            throw new AccessDeniedException("Access denied: Instructor account is awaiting admin approval");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public QuestionResponse mapToQuestionResponse(Question q) {
        QuestionResponse response = new QuestionResponse();
        response.setId(q.getId());
        if (q.getCourse() != null) {
            response.setCourseId(q.getCourse().getId());
            response.setCourseTitle(q.getCourse().getTitle());
        }
        if (q.getCreatedBy() != null) {
            response.setCreatedById(q.getCreatedBy().getId());
            response.setCreatedByName(q.getCreatedBy().getName());
        }
        response.setQuestionText(q.getQuestionText());
        response.setQuestionType(q.getQuestionType() != null ? q.getQuestionType().name() : null);
        response.setOptionsJson(q.getOptionsJson());
        if (q.getOptionsJson() != null && q.getOptionsJson().trim().startsWith("[")) {
            response.setOptions(parseJsonStringList(q.getOptionsJson(), "optionsJson"));
        }
        response.setCorrectAnswer(q.getCorrectAnswer());
        if (q.getQuestionType() == QuestionType.MULTIPLE_SELECT
                && q.getCorrectAnswer() != null && q.getCorrectAnswer().trim().startsWith("[")) {
            response.setCorrectAnswers(parseJsonStringList(q.getCorrectAnswer(), "correctAnswer"));
        }
        response.setDefaultMarks(q.getDefaultMarks());
        response.setNegativeMarks(q.getNegativeMarks());
        response.setNumericalTolerance(q.getNumericalTolerance());
        response.setGradingRubric(q.getGradingRubric());
        response.setRequiresManualGrading(requiresManualGrading(q));
        response.setTopic(q.getTopic());
        response.setDifficulty(q.getDifficulty());
        response.setExplanation(q.getExplanation());
        response.setIsBankQuestion(q.getIsBankQuestion());
        response.setCreatedAt(q.getCreatedAt());
        response.setUpdatedAt(q.getUpdatedAt());
        return response;
    }
}

