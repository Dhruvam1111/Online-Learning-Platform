package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service implementing FR-I19 (Create Quiz & Quiz-Question ordered mapping).
 */
@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final LessonRepository lessonRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;
    private Clock clock = Clock.systemDefaultZone();

    public QuizService(QuizRepository quizRepository,
                       QuestionRepository questionRepository,
                       QuizQuestionRepository quizQuestionRepository,
                       CourseRepository courseRepository,
                       CourseModuleRepository courseModuleRepository,
                       LessonRepository lessonRepository,
                       CourseInstructorRepository courseInstructorRepository,
                       UserRepository userRepository) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.courseRepository = courseRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.lessonRepository = lessonRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.userRepository = userRepository;
    }

    void setClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
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

    // =========================================================================
    // QUIZ CRUD
    // =========================================================================

    @Transactional
    public QuizResponse createQuiz(User currentUser, QuizRequest request) {
        validateInstructor(currentUser);
        if (request == null) {
            throw new IllegalArgumentException("Quiz request cannot be null");
        }

        HierarchyTarget target = resolveHierarchyTarget(request.getCourseId(), request.getModuleId(), request.getLessonId());
        Course course = verifyCourseAssociation(target.courseId(), currentUser);
        validateHierarchyConsistency(course, target.module(), target.lesson());

        String title = validateQuizTitle(request.getTitle());
        QuizStatus status = request.getStatus() != null && !request.getStatus().trim().isEmpty()
                ? QuizStatus.fromString(request.getStatus())
                : QuizStatus.DRAFT;

        validateQuizNumericFields(request);
        validateAvailabilityWindowBounds(request.getAvailableFrom(), request.getAvailableUntil());

        Quiz quiz = new Quiz(
                course,
                target.module(),
                target.lesson(),
                currentUser,
                title,
                trimToNull(request.getDescription()),
                trimToNull(request.getInstructions()),
                status
        );
        quiz.setTimeLimitMinutes(request.getTimeLimitMinutes());
        quiz.setNumberOfQuestions(request.getNumberOfQuestions());
        quiz.setMaxAttempts(request.getMaxAttempts() != null ? request.getMaxAttempts() : 1);
        quiz.setAvailableFrom(request.getAvailableFrom());
        quiz.setAvailableUntil(request.getAvailableUntil());
        quiz.setPassingMarks(request.getPassingMarks());
        quiz.setTotalMarks(request.getTotalMarks() != null ? request.getTotalMarks() : BigDecimal.ZERO);

        Quiz saved = quizRepository.save(quiz);

        List<QuizQuestion> attachedQuestions = new ArrayList<>();
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            attachedQuestions = attachQuestionsInternal(saved, currentUser, request.getQuestions());
            BigDecimal sumOfMarks = calculateTotalMarks(attachedQuestions);
            if (request.getTotalMarks() == null) {
                saved.setTotalMarks(sumOfMarks);
                saved = quizRepository.save(saved);
            } else if (request.getTotalMarks().compareTo(sumOfMarks) != 0) {
                throw new IllegalArgumentException(
                        "Total marks (" + request.getTotalMarks() + ") must equal the sum of linked question marks ("
                                + sumOfMarks + ")");
            }
        } else if (status == QuizStatus.PUBLISHED && request.getTotalMarks() != null
                && request.getTotalMarks().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException(
                    "Total marks (" + request.getTotalMarks() + ") must equal the sum of linked question marks (0)");
        }

        validatePassingVsTotalMarks(saved.getPassingMarks(), saved.getTotalMarks());
        return mapToQuizResponse(saved, attachedQuestions);
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> getMyQuizzes(User currentUser, String statusFilter) {
        validateInstructor(currentUser);
        List<Quiz> quizzes;
        if (statusFilter != null && !statusFilter.trim().isEmpty()) {
            QuizStatus status = QuizStatus.fromString(statusFilter);
            quizzes = quizRepository.findByAssociatedInstructorIdAndStatus(currentUser.getId(), status);
        } else {
            quizzes = quizRepository.findByAssociatedInstructorId(currentUser.getId());
        }
        return mapQuizListResponses(quizzes);
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> getQuizzesByCourse(User currentUser, Integer courseId, String statusFilter) {
        verifyCourseAssociation(courseId, currentUser);
        List<Quiz> quizzes;
        if (statusFilter != null && !statusFilter.trim().isEmpty()) {
            QuizStatus status = QuizStatus.fromString(statusFilter);
            quizzes = quizRepository.findByCourseIdAndStatusOrderByCreatedAtDesc(courseId, status);
        } else {
            quizzes = quizRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
        }
        return mapQuizListResponses(quizzes);
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> getQuizzesByModule(User currentUser, Integer courseId, Integer moduleId) {
        if (moduleId == null) {
            throw new IllegalArgumentException("Module ID cannot be null");
        }
        CourseModule module = courseModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));
        Integer resolvedCourseId = module.getCourse().getId();
        if (courseId != null && !courseId.equals(resolvedCourseId)) {
            throw new IllegalArgumentException("Module does not belong to the specified course");
        }
        verifyCourseAssociation(resolvedCourseId, currentUser);
        List<Quiz> quizzes = quizRepository.findByModuleIdOrderByCreatedAtDesc(moduleId);
        return mapQuizListResponses(quizzes);
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuizById(User currentUser, Integer quizId) {
        Quiz quiz = findAndVerifyQuiz(quizId, currentUser);
        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        return mapToQuizResponse(quiz, questions);
    }

    @Transactional
    public QuizResponse updateQuiz(User currentUser, Integer quizId, QuizRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Quiz update request cannot be null");
        }
        Quiz quiz = findAndVerifyQuiz(quizId, currentUser);

        if (request.getCourseId() != null && !request.getCourseId().equals(quiz.getCourse().getId())) {
            throw new IllegalArgumentException("Cannot reassign quiz to a different course");
        }

        String title = validateQuizTitle(request.getTitle());
        validateQuizNumericFields(request);

        if (request.getModuleId() != null) {
            CourseModule module = courseModuleRepository.findByIdAndCourseId(request.getModuleId(), quiz.getCourse().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Module not found with id: " + request.getModuleId() + " in course: " + quiz.getCourse().getId()));
            quiz.setModule(module);
        }

        if (request.getLessonId() != null) {
            Lesson lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + request.getLessonId()));
            validateQuizLessonType(lesson);
            validateHierarchyConsistency(quiz.getCourse(), quiz.getModule(), lesson);
            quiz.setLesson(lesson);
        } else if (request.getModuleId() != null && quiz.getLesson() != null) {
            validateHierarchyConsistency(quiz.getCourse(), quiz.getModule(), quiz.getLesson());
        }

        quiz.setTitle(title);
        if (request.getDescription() != null) {
            quiz.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getInstructions() != null) {
            quiz.setInstructions(trimToNull(request.getInstructions()));
        }
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            quiz.setStatus(QuizStatus.fromString(request.getStatus()));
        }
        if (request.getTotalMarks() != null) {
            quiz.setTotalMarks(request.getTotalMarks());
        }
        if (request.getPassingMarks() != null) {
            quiz.setPassingMarks(request.getPassingMarks());
        }
        if (request.getTimeLimitMinutes() != null) {
            quiz.setTimeLimitMinutes(request.getTimeLimitMinutes());
        }
        if (request.getNumberOfQuestions() != null) {
            quiz.setNumberOfQuestions(request.getNumberOfQuestions());
        }
        if (request.getMaxAttempts() != null) {
            quiz.setMaxAttempts(request.getMaxAttempts());
        }
        if (request.getAvailableFrom() != null) {
            quiz.setAvailableFrom(request.getAvailableFrom());
        }
        if (request.getAvailableUntil() != null) {
            quiz.setAvailableUntil(request.getAvailableUntil());
        }
        if (quiz.getMaxAttempts() == null) {
            quiz.setMaxAttempts(1);
        }

        validateAvailabilityWindowBounds(quiz.getAvailableFrom(), quiz.getAvailableUntil());
        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        BigDecimal sumOfMarks = calculateTotalMarks(questions);
        if (quiz.getStatus() == QuizStatus.PUBLISHED || !questions.isEmpty()) {
            if (quiz.getTotalMarks() == null || quiz.getTotalMarks().compareTo(sumOfMarks) != 0) {
                throw new IllegalArgumentException(
                        "Total marks (" + quiz.getTotalMarks() + ") must equal the sum of linked question marks ("
                                + sumOfMarks + ")");
            }
        }
        if (quiz.getStatus() == QuizStatus.PUBLISHED && quiz.getNumberOfQuestions() != null
                && !questions.isEmpty() && quiz.getNumberOfQuestions() > questions.size()) {
            throw new IllegalArgumentException(
                    "Configured number of questions cannot exceed linked questions count (" + questions.size() + ")");
        }

        validatePassingVsTotalMarks(quiz.getPassingMarks(), quiz.getTotalMarks());

        Quiz saved = quizRepository.save(quiz);
        return mapToQuizResponse(saved, questions);
    }

    @Transactional
    public ApiResponse deleteQuiz(User currentUser, Integer quizId) {
        Quiz quiz = findAndVerifyQuiz(quizId, currentUser);
        quizRepository.delete(quiz);
        return new ApiResponse(true, "Quiz deleted successfully");
    }

    // =========================================================================
    // QUIZ SETTINGS (FR-I20, DR-ED03)
    // =========================================================================

    @Transactional(readOnly = true)
    public QuizSettingsResponse getQuizSettings(User currentUser, Integer quizId) {
        Quiz quiz = findAndVerifyQuiz(quizId, currentUser);
        List<QuizQuestion> linkedQuestions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        return buildQuizSettingsResponse(quiz, linkedQuestions, LocalDateTime.now(clock), null);
    }

    @Transactional
    public QuizSettingsResponse configureQuizSettings(User currentUser, Integer quizId, QuizSettingsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Quiz settings request cannot be null");
        }
        Quiz quiz = findAndVerifyQuiz(quizId, currentUser);
        validateQuizSettingsFields(request);

        if (request.getTimeLimitMinutes() != null) {
            quiz.setTimeLimitMinutes(request.getTimeLimitMinutes());
        }
        if (request.getNumberOfQuestions() != null) {
            quiz.setNumberOfQuestions(request.getNumberOfQuestions());
        }
        if (request.getMaxAttempts() != null) {
            quiz.setMaxAttempts(request.getMaxAttempts());
        } else if (quiz.getMaxAttempts() == null) {
            quiz.setMaxAttempts(1);
        }
        if (request.getAvailableFrom() != null) {
            quiz.setAvailableFrom(request.getAvailableFrom());
        }
        if (request.getAvailableUntil() != null) {
            quiz.setAvailableUntil(request.getAvailableUntil());
        }
        if (request.getPassingMarks() != null) {
            quiz.setPassingMarks(request.getPassingMarks());
        }

        validateAvailabilityWindowBounds(quiz.getAvailableFrom(), quiz.getAvailableUntil());

        List<QuizQuestion> linkedQuestions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        BigDecimal questionMarksSum = calculateTotalMarks(linkedQuestions);

        if (request.getTotalMarks() != null) {
            if (request.getTotalMarks().compareTo(questionMarksSum) != 0) {
                throw new IllegalArgumentException(
                        "Total marks (" + request.getTotalMarks() + ") must equal the sum of linked question marks ("
                                + questionMarksSum + ") per DR-ED03");
            }
            quiz.setTotalMarks(request.getTotalMarks());
        } else {
            quiz.setTotalMarks(questionMarksSum);
        }

        if (quiz.getStatus() == QuizStatus.PUBLISHED && quiz.getNumberOfQuestions() != null
                && !linkedQuestions.isEmpty() && quiz.getNumberOfQuestions() > linkedQuestions.size()) {
            throw new IllegalArgumentException(
                    "Configured number of questions cannot exceed linked questions count (" + linkedQuestions.size() + ")");
        }

        validatePassingVsTotalMarks(quiz.getPassingMarks(), quiz.getTotalMarks());

        Quiz saved = quizRepository.save(quiz);
        return buildQuizSettingsResponse(saved, linkedQuestions, LocalDateTime.now(clock), null);
    }

    // =========================================================================
    // QUIZ-QUESTION MAPPING & ORDERING
    // =========================================================================

    @Transactional(readOnly = true)
    public List<QuizQuestionResponse> getQuizQuestions(User currentUser, Integer quizId) {
        Quiz quiz = findAndVerifyQuiz(quizId, currentUser);
        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        return questions.stream().map(this::mapToQuizQuestionResponse).toList();
    }

    @Transactional
    public QuizQuestionResponse addQuestionToQuiz(User currentUser, Integer quizId, QuizQuestionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Quiz question request cannot be null");
        }
        Quiz quiz = findAndVerifyQuiz(quizId, currentUser);

        QuizQuestion savedLink = attachSingleQuestion(quiz, currentUser, request);

        List<QuizQuestion> allQuestions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        quiz.setTotalMarks(calculateTotalMarks(allQuestions));
        quizRepository.save(quiz);

        return mapToQuizQuestionResponse(savedLink);
    }

    @Transactional
    public QuizQuestionResponse updateQuizQuestion(User currentUser,
                                                   Integer quizId,
                                                   Integer quizQuestionId,
                                                   QuizQuestionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Quiz question update request cannot be null");
        }
        Quiz quiz = findAndVerifyQuiz(quizId, currentUser);
        QuizQuestion quizQuestion = quizQuestionRepository.findByIdAndQuizId(quizQuestionId, quiz.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Quiz question mapping not found with id: " + quizQuestionId + " in quiz: " + quizId));

        validateNonNegative(request.getMarks(), "Marks");
        validateNonNegative(request.getNegativeMarks(), "Negative marks");

        if (request.getMarks() != null) {
            quizQuestion.setMarks(request.getMarks());
        }
        if (request.getNegativeMarks() != null) {
            quizQuestion.setNegativeMarks(request.getNegativeMarks());
        }
        if (request.getIsRequired() != null) {
            quizQuestion.setIsRequired(request.getIsRequired());
        }
        if (request.getOrderIndex() != null) {
            if (request.getOrderIndex() < 1) {
                throw new IllegalArgumentException("Order index must be at least 1");
            }
            quizQuestion.setOrderIndex(request.getOrderIndex());
        }

        QuizQuestion savedLink = quizQuestionRepository.save(quizQuestion);

        List<QuizQuestion> allQuestions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        quiz.setTotalMarks(calculateTotalMarks(allQuestions));
        quizRepository.save(quiz);

        return mapToQuizQuestionResponse(savedLink);
    }

    @Transactional
    public ApiResponse removeQuestionFromQuiz(User currentUser, Integer quizId, Integer quizQuestionId) {
        Quiz quiz = findAndVerifyQuiz(quizId, currentUser);
        QuizQuestion target = quizQuestionRepository.findByIdAndQuizId(quizQuestionId, quiz.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Quiz question mapping not found with id: " + quizQuestionId + " in quiz: " + quizId));

        quizQuestionRepository.delete(target);

        List<QuizQuestion> remaining = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        List<QuizQuestion> reordered = new ArrayList<>();
        int index = 1;
        for (QuizQuestion qq : remaining) {
            if (!qq.getId().equals(quizQuestionId)) {
                qq.setOrderIndex(index++);
                reordered.add(qq);
            }
        }
        if (!reordered.isEmpty()) {
            quizQuestionRepository.saveAll(reordered);
        }
        quiz.setTotalMarks(calculateTotalMarks(reordered));
        quizRepository.save(quiz);

        return new ApiResponse(true, "Question removed from quiz successfully");
    }

    @Transactional
    public List<QuizQuestionResponse> reorderQuizQuestions(User currentUser,
                                                             Integer quizId,
                                                             ReorderStructureRequest request) {
        Quiz quiz = findAndVerifyQuiz(quizId, currentUser);
        if (request == null || request.getOrderedIds() == null || request.getOrderedIds().isEmpty()) {
            throw new IllegalArgumentException("Ordered quiz-question IDs list cannot be empty");
        }

        List<QuizQuestion> existing = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Quiz has no questions to reorder");
        }

        List<Integer> orderedIds = request.getOrderedIds();
        validateNoDuplicates(orderedIds);

        Map<Integer, QuizQuestion> byId = new LinkedHashMap<>();
        for (QuizQuestion qq : existing) {
            byId.put(qq.getId(), qq);
        }

        if (orderedIds.size() != byId.size() || !byId.keySet().containsAll(orderedIds)) {
            throw new IllegalArgumentException(
                    "Ordered IDs must contain the exact set of quiz-question IDs belonging to this quiz");
        }

        List<QuizQuestion> updated = new ArrayList<>(orderedIds.size());
        for (int i = 0; i < orderedIds.size(); i++) {
            QuizQuestion qq = byId.get(orderedIds.get(i));
            qq.setOrderIndex(i + 1);
            updated.add(qq);
        }

        quizQuestionRepository.saveAll(updated);
        return updated.stream().map(this::mapToQuizQuestionResponse).toList();
    }

    // =========================================================================
    // AUTHORIZATION & VALIDATION HELPERS
    // =========================================================================

    public Course verifyCourseAssociation(Integer courseId, User currentUser) {
        validateInstructor(currentUser);
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        boolean isOwner = course.getInstructor() != null && currentUser.getId().equals(course.getInstructor().getId());
        boolean isCoInstructor = courseInstructorRepository != null &&
                courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, currentUser.getId());
        boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getRole());

        if (!isOwner && !isCoInstructor && !isAdmin) {
            throw new AccessDeniedException(
                    "Cannot manage quizzes: you are not an authorized instructor for this course");
        }

        return course;
    }

    private Quiz findAndVerifyQuiz(Integer quizId, User currentUser) {
        validateInstructor(currentUser);
        if (quizId == null) {
            throw new IllegalArgumentException("Quiz ID cannot be null");
        }
        Quiz quiz = quizRepository.findByIdWithDetails(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));
        verifyCourseAssociation(quiz.getCourse().getId(), currentUser);
        return quiz;
    }

    private void validateInstructor(User currentUser) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (!"instructor".equalsIgnoreCase(currentUser.getRole()) && !"admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied: only instructors or admins can manage quizzes");
        }
        if ("instructor".equalsIgnoreCase(currentUser.getRole())
                && !"active".equalsIgnoreCase(currentUser.getAccountStatus())) {
            throw new AccessDeniedException("Access denied: Instructor account is awaiting admin approval");
        }
    }

    private record HierarchyTarget(Integer courseId, CourseModule module, Lesson lesson) {}

    private HierarchyTarget resolveHierarchyTarget(Integer courseId, Integer moduleId, Integer lessonId) {
        if (courseId == null && moduleId == null && lessonId == null) {
            throw new IllegalArgumentException("Either courseId, moduleId, or lessonId must be provided");
        }

        CourseModule module = null;
        if (moduleId != null) {
            module = courseModuleRepository.findById(moduleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));
        }

        Lesson lesson = null;
        if (lessonId != null) {
            lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));
            validateQuizLessonType(lesson);
        }

        Integer resolvedCourseId = courseId;
        if (resolvedCourseId == null && module != null) {
            resolvedCourseId = module.getCourse().getId();
        }
        if (resolvedCourseId == null && lesson != null) {
            resolvedCourseId = lesson.getSection().getModule().getCourse().getId();
        }

        return new HierarchyTarget(resolvedCourseId, module, lesson);
    }

    private void validateQuizLessonType(Lesson lesson) {
        if (lesson.getLessonType() != LessonType.QUIZ) {
            throw new IllegalArgumentException(
                    "Selected lesson must have type QUIZ, found: " + lesson.getLessonType());
        }
    }

    private void validateHierarchyConsistency(Course course, CourseModule module, Lesson lesson) {
        if (module != null && !module.getCourse().getId().equals(course.getId())) {
            throw new IllegalArgumentException("Module does not belong to the specified course");
        }
        if (lesson != null) {
            CourseModule lessonModule = lesson.getSection().getModule();
            if (!lessonModule.getCourse().getId().equals(course.getId())) {
                throw new IllegalArgumentException("Lesson does not belong to the specified course");
            }
            if (module != null && !lessonModule.getId().equals(module.getId())) {
                throw new IllegalArgumentException("Lesson does not belong to the specified module");
            }
        }
    }

    private List<QuizQuestion> attachQuestionsInternal(Quiz quiz, User currentUser, List<QuizQuestionRequest> requests) {
        List<QuizQuestion> attached = new ArrayList<>();
        for (QuizQuestionRequest req : requests) {
            if (req == null) {
                throw new IllegalArgumentException("Question item cannot be null");
            }
            attached.add(attachSingleQuestion(quiz, currentUser, req));
        }
        return attached;
    }

    private QuizQuestion attachSingleQuestion(Quiz quiz, User currentUser, QuizQuestionRequest request) {
        validateNonNegative(request.getMarks(), "Marks");
        validateNonNegative(request.getNegativeMarks(), "Negative marks");
        validateNonNegative(request.getDefaultMarks(), "Default marks");

        Question question;
        if (request.getQuestionId() != null) {
            question = questionRepository.findById(request.getQuestionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + request.getQuestionId()));
            if (question.getCourse() != null && !question.getCourse().getId().equals(quiz.getCourse().getId())) {
                throw new AccessDeniedException("Cannot link a question from another course");
            }
            if (question.getCourse() == null && !Boolean.TRUE.equals(question.getIsBankQuestion())
                    && (question.getCreatedBy() == null || !question.getCreatedBy().getId().equals(currentUser.getId()))) {
                throw new AccessDeniedException("Cannot link an unshared question from another instructor");
            }
        } else {
            if (request.getQuestionText() == null || request.getQuestionText().trim().isEmpty()) {
                throw new IllegalArgumentException("Question text is required when creating a new question");
            }
            QuestionType questionType = request.getQuestionType() != null && !request.getQuestionType().trim().isEmpty()
                    ? QuestionType.fromString(request.getQuestionType())
                    : QuestionType.MULTIPLE_CHOICE;

            BigDecimal defaultMarks = request.getDefaultMarks() != null
                    ? request.getDefaultMarks()
                    : (request.getMarks() != null ? request.getMarks() : BigDecimal.ONE);
            BigDecimal negativeMarks = request.getNegativeMarks() != null
                    ? request.getNegativeMarks()
                    : BigDecimal.ZERO;
            BigDecimal numericalTolerance = request.getNumericalTolerance() != null
                    ? request.getNumericalTolerance()
                    : BigDecimal.ZERO;
            validateNonNegative(numericalTolerance, "Numerical tolerance");
            Boolean requiresManualGrading = request.getRequiresManualGrading() != null
                    ? request.getRequiresManualGrading()
                    : (questionType == QuestionType.DESCRIPTIVE);

            question = new Question(
                    quiz.getCourse(),
                    currentUser,
                    request.getQuestionText().trim(),
                    questionType,
                    trimToNull(request.getOptionsJson()),
                    trimToNull(request.getCorrectAnswer()),
                    defaultMarks,
                    negativeMarks,
                    numericalTolerance,
                    trimToNull(request.getGradingRubric()),
                    requiresManualGrading
            );
            question.setTopic(trimToNull(request.getTopic()));
            question.setDifficulty(trimToNull(request.getDifficulty()));
            question.setExplanation(trimToNull(request.getExplanation()));
            question.setIsBankQuestion(Boolean.TRUE.equals(request.getIsBankQuestion()));
            question = questionRepository.save(question);
        }

        if (quizQuestionRepository.existsByQuizIdAndQuestionId(quiz.getId(), question.getId())) {
            throw new IllegalStateException("Question " + question.getId() + " is already attached to this quiz");
        }

        int orderIndex = request.getOrderIndex() != null
                ? request.getOrderIndex()
                : quizQuestionRepository.findMaxOrderIndexByQuizId(quiz.getId()) + 1;
        if (orderIndex < 1) {
            throw new IllegalArgumentException("Order index must be at least 1");
        }

        QuizQuestion quizQuestion = new QuizQuestion(
                quiz,
                question,
                orderIndex,
                request.getMarks(),
                request.getNegativeMarks(),
                request.getIsRequired() != null ? request.getIsRequired() : Boolean.TRUE
        );
        return quizQuestionRepository.save(quizQuestion);
    }

    private String validateQuizTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Quiz title is required");
        }
        String trimmed = title.trim();
        if (trimmed.length() > 200) {
            throw new IllegalArgumentException("Quiz title must not exceed 200 characters");
        }
        return trimmed;
    }

    private void validateQuizNumericFields(QuizRequest request) {
        validateNonNegative(request.getTotalMarks(), "Total marks");
        validateNonNegative(request.getPassingMarks(), "Passing marks");
        if (request.getTimeLimitMinutes() != null && request.getTimeLimitMinutes() < 1) {
            throw new IllegalArgumentException("Time limit must be at least 1 minute");
        }
        if (request.getNumberOfQuestions() != null && request.getNumberOfQuestions() < 1) {
            throw new IllegalArgumentException("Number of questions must be at least 1");
        }
        if (request.getMaxAttempts() != null && request.getMaxAttempts() < 1) {
            throw new IllegalArgumentException("Max attempts must be at least 1");
        }
    }

    private void validateQuizSettingsFields(QuizSettingsRequest request) {
        validateNonNegative(request.getTotalMarks(), "Total marks");
        validateNonNegative(request.getPassingMarks(), "Passing marks");
        if (request.getTimeLimitMinutes() != null && request.getTimeLimitMinutes() < 1) {
            throw new IllegalArgumentException("Duration (timeLimitMinutes) must be at least 1 minute");
        }
        if (request.getNumberOfQuestions() != null && request.getNumberOfQuestions() < 1) {
            throw new IllegalArgumentException("Number of questions must be at least 1");
        }
        if (request.getMaxAttempts() != null && request.getMaxAttempts() < 1) {
            throw new IllegalArgumentException("Allowed attempts (maxAttempts) must be at least 1");
        }
    }

    public void validateAvailabilityWindowBounds(LocalDateTime availableFrom, LocalDateTime availableUntil) {
        if (availableFrom != null && availableUntil != null && !availableFrom.isBefore(availableUntil)) {
            throw new IllegalArgumentException("Availability start time must be strictly before end time");
        }
    }

    private void validatePassingVsTotalMarks(BigDecimal passingMarks, BigDecimal totalMarks) {
        if (passingMarks != null && totalMarks != null
                && totalMarks.compareTo(BigDecimal.ZERO) > 0
                && passingMarks.compareTo(totalMarks) > 0) {
            throw new IllegalArgumentException("Passing marks cannot exceed total marks");
        }
    }

    private void validateNonNegative(BigDecimal value, String fieldName) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
    }

    private void validateNoDuplicates(List<Integer> ids) {
        Set<Integer> seen = new HashSet<>();
        for (Integer id : ids) {
            if (id == null) {
                throw new IllegalArgumentException("Quiz-question ID in reorder list cannot be null");
            }
            if (!seen.add(id)) {
                throw new IllegalArgumentException("Duplicate quiz-question ID in reorder list: " + id);
            }
        }
    }

    public BigDecimal calculateTotalMarks(List<QuizQuestion> quizQuestions) {
        BigDecimal total = BigDecimal.ZERO;
        if (quizQuestions == null) {
            return total;
        }
        for (QuizQuestion qq : quizQuestions) {
            BigDecimal effective = qq.getMarks() != null ? qq.getMarks() : qq.getQuestion().getDefaultMarks();
            if (effective != null) {
                total = total.add(effective);
            }
        }
        return total;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // =========================================================================
    // RESPONSE MAPPERS
    // =========================================================================

    private List<QuizResponse> mapQuizListResponses(List<Quiz> quizzes) {
        List<QuizResponse> responses = new ArrayList<>(quizzes.size());
        for (Quiz quiz : quizzes) {
            List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
            responses.add(mapToQuizResponse(quiz, questions));
        }
        return responses;
    }

    private QuizResponse mapToQuizResponse(Quiz quiz, List<QuizQuestion> questions) {
        QuizResponse response = new QuizResponse();
        response.setId(quiz.getId());
        response.setCourseId(quiz.getCourse().getId());
        response.setCourseTitle(quiz.getCourse().getTitle());
        if (quiz.getModule() != null) {
            response.setModuleId(quiz.getModule().getId());
            response.setModuleTitle(quiz.getModule().getTitle());
        }
        if (quiz.getLesson() != null) {
            response.setLessonId(quiz.getLesson().getId());
            response.setLessonTitle(quiz.getLesson().getTitle());
        }
        if (quiz.getCreatedBy() != null) {
            response.setCreatedById(quiz.getCreatedBy().getId());
            response.setCreatedByName(quiz.getCreatedBy().getName());
        }
        response.setTitle(quiz.getTitle());
        response.setDescription(quiz.getDescription());
        response.setInstructions(quiz.getInstructions());
        response.setStatus(quiz.getStatus() != null ? quiz.getStatus().name() : QuizStatus.DRAFT.name());
        response.setTotalMarks(quiz.getTotalMarks());
        response.setPassingMarks(quiz.getPassingMarks());
        response.setTimeLimitMinutes(quiz.getTimeLimitMinutes());
        response.setNumberOfQuestions(quiz.getNumberOfQuestions());
        response.setMaxAttempts(quiz.getMaxAttempts());
        response.setAvailableFrom(quiz.getAvailableFrom());
        response.setAvailableUntil(quiz.getAvailableUntil());
        List<QuizQuestionResponse> questionResponses = questions.stream()
                .map(this::mapToQuizQuestionResponse)
                .toList();
        response.setQuestionCount(questionResponses.size());
        response.setQuestions(questionResponses);
        response.setCreatedAt(quiz.getCreatedAt());
        response.setUpdatedAt(quiz.getUpdatedAt());
        return response;
    }

    public QuizSettingsResponse buildQuizSettingsResponse(Quiz quiz,
                                                          List<QuizQuestion> linkedQuestions,
                                                          LocalDateTime serverNow,
                                                          Long attemptsUsed) {
        List<QuizQuestion> safeQuestions = linkedQuestions != null ? linkedQuestions : List.of();
        BigDecimal calculatedSum = calculateTotalMarks(safeQuestions);
        int maxAttempts = quiz.getMaxAttempts() != null && quiz.getMaxAttempts() >= 1 ? quiz.getMaxAttempts() : 1;

        QuizSettingsResponse response = new QuizSettingsResponse();
        response.setQuizId(quiz.getId());
        response.setCourseId(quiz.getCourse().getId());
        response.setTitle(quiz.getTitle());
        response.setStatus(quiz.getStatus() != null ? quiz.getStatus().name() : QuizStatus.DRAFT.name());
        response.setTimeLimitMinutes(quiz.getTimeLimitMinutes());
        response.setNumberOfQuestions(quiz.getNumberOfQuestions());
        response.setLinkedQuestionCount(safeQuestions.size());
        response.setTotalMarks(quiz.getTotalMarks());
        response.setCalculatedQuestionMarksSum(calculatedSum);
        response.setMarksConsistent(quiz.getTotalMarks() != null && quiz.getTotalMarks().compareTo(calculatedSum) == 0);
        response.setPassingMarks(quiz.getPassingMarks());
        response.setMaxAttempts(maxAttempts);
        response.setAvailableFrom(quiz.getAvailableFrom());
        response.setAvailableUntil(quiz.getAvailableUntil());
        response.setServerTime(serverNow);

        boolean currentlyAvailable = quiz.getStatus() == QuizStatus.PUBLISHED;
        String availabilityMessage = "Available";
        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            currentlyAvailable = false;
            availabilityMessage = "Quiz is not published yet";
        } else if (quiz.getAvailableFrom() != null && serverNow.isBefore(quiz.getAvailableFrom())) {
            currentlyAvailable = false;
            availabilityMessage = "Quiz opens at " + quiz.getAvailableFrom();
        } else if (quiz.getAvailableUntil() != null && serverNow.isAfter(quiz.getAvailableUntil())) {
            currentlyAvailable = false;
            availabilityMessage = "Quiz closed at " + quiz.getAvailableUntil();
        }
        response.setCurrentlyAvailable(currentlyAvailable);
        response.setAvailabilityMessage(availabilityMessage);

        if (attemptsUsed != null) {
            response.setAttemptsUsed(attemptsUsed);
            response.setRemainingAttempts((int) Math.max(0L, maxAttempts - attemptsUsed));
        }
        return response;
    }

    private QuizQuestionResponse mapToQuizQuestionResponse(QuizQuestion qq) {
        Question q = qq.getQuestion();
        QuizQuestionResponse response = new QuizQuestionResponse();
        response.setId(qq.getId());
        response.setQuizId(qq.getQuiz().getId());
        response.setQuestionId(q.getId());
        response.setOrderIndex(qq.getOrderIndex());
        response.setMarks(qq.getMarks());
        response.setEffectiveMarks(qq.getMarks() != null ? qq.getMarks() : q.getDefaultMarks());
        response.setNegativeMarks(qq.getNegativeMarks());
        response.setEffectiveNegativeMarks(
                qq.getNegativeMarks() != null ? qq.getNegativeMarks() : q.getNegativeMarks());
        response.setIsRequired(qq.getIsRequired());
        response.setQuestionText(q.getQuestionText());
        response.setQuestionType(q.getQuestionType() != null ? q.getQuestionType().name() : null);
        response.setOptionsJson(q.getOptionsJson());
        response.setCorrectAnswer(q.getCorrectAnswer());
        response.setDefaultMarks(q.getDefaultMarks());
        response.setNumericalTolerance(q.getNumericalTolerance());
        response.setGradingRubric(q.getGradingRubric());
        response.setRequiresManualGrading(
                Boolean.TRUE.equals(q.getRequiresManualGrading()) || q.getQuestionType() == QuestionType.DESCRIPTIVE);
        response.setTopic(q.getTopic());
        response.setDifficulty(q.getDifficulty());
        response.setExplanation(q.getExplanation());
        response.setIsBankQuestion(q.getIsBankQuestion());
        response.setCreatedAt(qq.getCreatedAt());
        return response;
    }
}
