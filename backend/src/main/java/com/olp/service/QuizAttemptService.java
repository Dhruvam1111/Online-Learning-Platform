package com.olp.service;

import com.olp.dto.QuestionEvaluationResult;
import com.olp.dto.QuizAttemptResponse;
import com.olp.dto.QuizEvaluationSummary;
import com.olp.dto.QuizSettingsResponse;
import com.olp.dto.QuizSubmissionRequest;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.QuizAttemptExpiredException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import java.util.Optional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service implementing server-side Quiz availability window checks, persistent attempt-limit enforcement,
 * authoritative server-clock timer enforcement, and auto-evaluation answer grading (FR-I20, FR-I22, DR-ED06).
 */
@Service
public class QuizAttemptService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final QuizService quizService;
    private final AutoGradingService autoGradingService;
    private final QuizAttemptAnswerRepository quizAttemptAnswerRepository;
    private Clock clock = Clock.systemDefaultZone();

    @Autowired
    public QuizAttemptService(QuizRepository quizRepository,
                              QuizQuestionRepository quizQuestionRepository,
                              QuizAttemptRepository quizAttemptRepository,
                              EnrollmentRepository enrollmentRepository,
                              UserRepository userRepository,
                              QuizService quizService,
                              AutoGradingService autoGradingService,
                              QuizAttemptAnswerRepository quizAttemptAnswerRepository) {
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.quizService = quizService;
        this.autoGradingService = autoGradingService != null ? autoGradingService : new AutoGradingService();
        this.quizAttemptAnswerRepository = quizAttemptAnswerRepository;
    }

    public QuizAttemptService(QuizRepository quizRepository,
                              QuizQuestionRepository quizQuestionRepository,
                              QuizAttemptRepository quizAttemptRepository,
                              EnrollmentRepository enrollmentRepository,
                              UserRepository userRepository,
                              QuizService quizService) {
        this(quizRepository, quizQuestionRepository, quizAttemptRepository,
             enrollmentRepository, userRepository, quizService,
             new AutoGradingService(), null);
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

    @Transactional(readOnly = true)
    public QuizSettingsResponse getStudentQuizSettings(User currentUser, Integer quizId) {
        validateActiveStudent(currentUser);
        Quiz quiz = findPublishedQuizAndVerifyEnrollment(quizId, currentUser);
        List<QuizQuestion> linkedQuestions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        long attemptsUsed = quizAttemptRepository.countByQuizIdAndStudentId(quiz.getId(), currentUser.getId());
        return quizService.buildQuizSettingsResponse(quiz, linkedQuestions, LocalDateTime.now(clock), attemptsUsed);
    }

    @Transactional
    public QuizAttemptResponse startQuizAttempt(User currentUser, Integer quizId) {
        validateActiveStudent(currentUser);
        Quiz quiz = findPublishedQuizAndVerifyEnrollment(quizId, currentUser);

        LocalDateTime serverNow = LocalDateTime.now(clock);
        validateAvailabilityWindow(quiz, serverNow);

        expireStaleInProgressAttempts(quiz, currentUser.getId(), serverNow);

        List<QuizAttempt> activeAttempts = quizAttemptRepository
                .findByQuizIdAndStudentIdAndStatusOrderByAttemptNumberDesc(
                        quiz.getId(), currentUser.getId(), QuizAttemptStatus.IN_PROGRESS);
        if (!activeAttempts.isEmpty()) {
            throw new IllegalStateException("An active attempt is already in progress for this quiz");
        }

        long usedAttempts = quizAttemptRepository.countByQuizIdAndStudentId(quiz.getId(), currentUser.getId());
        int allowedAttempts = resolveAllowedAttempts(quiz);
        validateAttemptLimit(allowedAttempts, usedAttempts);

        LocalDateTime expiresAt = computeExpiresAt(quiz, serverNow);
        QuizAttempt newAttempt = new QuizAttempt(
                quiz,
                currentUser,
                (int) usedAttempts + 1,
                serverNow,
                expiresAt
        );

        QuizAttempt saved = quizAttemptRepository.save(newAttempt);
        return mapToAttemptResponse(saved, quiz, serverNow, usedAttempts + 1);
    }

    @Transactional
    public QuizAttemptResponse getQuizAttemptStatus(User currentUser, Integer quizId, Integer attemptId) {
        validateActiveStudent(currentUser);
        QuizAttempt attempt = findAndVerifyStudentAttempt(quizId, attemptId, currentUser);
        Quiz quiz = attempt.getQuiz();
        LocalDateTime serverNow = LocalDateTime.now(clock);

        if (attempt.getStatus() == QuizAttemptStatus.IN_PROGRESS && isAttemptExpired(quiz, attempt, serverNow)) {
            attempt.setStatus(QuizAttemptStatus.EXPIRED);
            attempt.setSubmittedAt(serverNow);
            attempt = quizAttemptRepository.save(attempt);
        }

        long usedAttempts = quizAttemptRepository.countByQuizIdAndStudentId(quiz.getId(), currentUser.getId());
        return mapToAttemptResponse(attempt, quiz, serverNow, usedAttempts);
    }

    @Transactional
    public List<QuizAttemptResponse> getStudentQuizAttempts(User currentUser, Integer quizId) {
        validateActiveStudent(currentUser);
        Quiz quiz = findPublishedQuizAndVerifyEnrollment(quizId, currentUser);
        LocalDateTime serverNow = LocalDateTime.now(clock);
        expireStaleInProgressAttempts(quiz, currentUser.getId(), serverNow);

        List<QuizAttempt> attempts = quizAttemptRepository
                .findByQuizIdAndStudentIdOrderByAttemptNumberDesc(quiz.getId(), currentUser.getId());
        long usedAttempts = attempts.size();
        return attempts.stream()
                .map(a -> mapToAttemptResponse(a, quiz, serverNow, usedAttempts))
                .toList();
    }

    @Transactional(noRollbackFor = QuizAttemptExpiredException.class)
    public QuizAttemptResponse submitQuizAttempt(User currentUser, Integer quizId, Integer attemptId) {
        return submitQuizAttempt(currentUser, quizId, attemptId, null);
    }

    @Transactional(noRollbackFor = QuizAttemptExpiredException.class)
    public QuizAttemptResponse submitQuizAttempt(User currentUser,
                                                 Integer quizId,
                                                 Integer attemptId,
                                                 QuizSubmissionRequest submissionRequest) {
        validateActiveStudent(currentUser);
        QuizAttempt attempt = findAndVerifyStudentAttempt(quizId, attemptId, currentUser);
        Quiz quiz = attempt.getQuiz();
        LocalDateTime serverNow = LocalDateTime.now(clock);

        if (attempt.getStatus() != QuizAttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException("Quiz attempt is already " + attempt.getStatus().name());
        }

        if (isAttemptExpired(quiz, attempt, serverNow)) {
            attempt.setStatus(QuizAttemptStatus.EXPIRED);
            attempt.setSubmittedAt(serverNow);
            quizAttemptRepository.save(attempt);
            throw new QuizAttemptExpiredException(
                    "Quiz time limit has expired according to server time (expired at " + attempt.getExpiresAt() + ")");
        }

        attempt.setStatus(QuizAttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(serverNow);

        QuizEvaluationSummary summary = null;
        if (submissionRequest != null && submissionRequest.getAnswers() != null && !submissionRequest.getAnswers().isEmpty()) {
            List<QuizQuestion> linkedQuestions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
            summary = autoGradingService.evaluateQuizSubmission(
                    quiz, linkedQuestions, submissionRequest.getAnswers(), submissionRequest.getPolicy());

            attempt.setObtainedMarks(summary.getTotalAwardedMarks());

            if (quizAttemptAnswerRepository != null) {
                Map<Integer, Question> questionMap = new HashMap<>();
                for (QuizQuestion qq : linkedQuestions) {
                    if (qq.getQuestion() != null) {
                        questionMap.put(qq.getQuestion().getId(), qq.getQuestion());
                    }
                }

                for (QuestionEvaluationResult result : summary.getResults()) {
                    Question question = questionMap.get(result.getQuestionId());
                    if (question != null) {
                        QuizAttemptAnswer attemptAnswer = new QuizAttemptAnswer(
                                attempt,
                                question,
                                result.getSubmittedAnswer(),
                                result.getAwardedMarks(),
                                result.getMaxMarks(),
                                result.getStatus(),
                                result.getFeedback(),
                                serverNow
                        );
                        quizAttemptAnswerRepository.save(attemptAnswer);
                    }
                }
            }
        }

        QuizAttempt saved = quizAttemptRepository.save(attempt);
        long usedAttempts = quizAttemptRepository.countByQuizIdAndStudentId(quiz.getId(), currentUser.getId());
        QuizAttemptResponse response = mapToAttemptResponse(saved, quiz, serverNow, usedAttempts);
        if (summary != null) {
            response.setEvaluationSummary(summary);
        }
        return response;
    }

    @Transactional(readOnly = true)
    public QuizEvaluationSummary getAttemptEvaluation(User currentUser, Integer quizId, Integer attemptId) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (quizId == null || attemptId == null) {
            throw new IllegalArgumentException("Quiz ID and Attempt ID cannot be null");
        }

        QuizAttempt attempt = quizAttemptRepository.findByIdAndQuizId(attemptId, quizId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Quiz attempt not found with id: " + attemptId + " for quiz: " + quizId));

        Quiz quiz = attempt.getQuiz();

        boolean isStudent = "student".equalsIgnoreCase(currentUser.getRole());
        boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getRole());
        if (isStudent) {
            if (!attempt.getStudent().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Access denied: you do not own this quiz attempt");
            }
        } else if (!isAdmin) {
            quizService.verifyCourseAssociation(quiz.getCourse().getId(), currentUser);
        }

        List<QuizAttemptAnswer> storedAnswers = quizAttemptAnswerRepository != null
                ? quizAttemptAnswerRepository.findByAttemptId(attemptId)
                : List.of();

        boolean feedbackEnabled = quiz.getFeedbackReleased() == null || quiz.getFeedbackReleased();
        boolean attemptPublished = Boolean.TRUE.equals(attempt.getIsPublished());

        QuizEvaluationSummary summary = new QuizEvaluationSummary();
        summary.setQuizId(quiz.getId());
        summary.setAttemptId(attempt.getId());
        summary.setPassingMarks(quiz.getPassingMarks());
        summary.setFeedbackReleased(feedbackEnabled);
        summary.setResultsPublished(attemptPublished);
        summary.setIsOverridden(attempt.isOverridden());

        if (isStudent) {
            // Student visibility: overall feedback only if feedback released and results published
            if (feedbackEnabled && attemptPublished) {
                summary.setOverallFeedback(attempt.getOverallFeedback());
                if (attempt.getGrader() != null) {
                    summary.setGraderId(attempt.getGrader().getId());
                    summary.setGraderName(attempt.getGrader().getName());
                }
                summary.setFeedbackAt(attempt.getFeedbackAt());
            }
        } else {
            // Instructors and admins always see overall feedback
            summary.setOverallFeedback(attempt.getOverallFeedback());
            if (attempt.getGrader() != null) {
                summary.setGraderId(attempt.getGrader().getId());
                summary.setGraderName(attempt.getGrader().getName());
            }
            summary.setFeedbackAt(attempt.getFeedbackAt());
        }

        BigDecimal totalAwarded = attempt.getObtainedMarks() != null ? attempt.getObtainedMarks() : BigDecimal.ZERO;
        BigDecimal totalMax = quiz.getTotalMarks() != null ? quiz.getTotalMarks() : BigDecimal.ZERO;
        int correct = 0;
        int incorrect = 0;
        int partial = 0;
        int manual = 0;
        int unanswered = 0;

        List<QuestionEvaluationResult> results = new ArrayList<>();
        for (QuizAttemptAnswer ans : storedAnswers) {
            Question q = ans.getQuestion();
            EvaluationStatus status = ans.getEvaluationStatus();
            BigDecimal marks = ans.getAwardedMarks();
            String feedback = ans.getFeedback();
            boolean requiresManual = status == EvaluationStatus.MANUAL_GRADING_REQUIRED;

            // Result & Feedback Visibility (FR-I30 Requirement 3, FR-I31 Requirement 3, FR-I23):
            // Graded marks and feedback must remain unpublished and invisible to students until explicitly published.
            if (isStudent) {
                if (status == EvaluationStatus.EVALUATED_UNPUBLISHED) {
                    status = EvaluationStatus.MANUAL_GRADING_REQUIRED;
                    marks = BigDecimal.ZERO;
                    feedback = null;
                    requiresManual = true;
                } else if (!feedbackEnabled) {
                    feedback = null;
                } else if (requiresManual && status != EvaluationStatus.PUBLISHED) {
                    feedback = null;
                }
            }

            QuestionEvaluationResult res = new QuestionEvaluationResult(
                    q.getId(),
                    q.getQuestionType(),
                    status,
                    marks,
                    ans.getMaxMarks(),
                    ans.getSubmittedAnswer(),
                    q.getCorrectAnswer(),
                    q.getNumericalTolerance(),
                    feedback,
                    requiresManual
            );
            results.add(res);

            if (status != null) {
                switch (status) {
                    case CORRECT -> correct++;
                    case INCORRECT -> incorrect++;
                    case PARTIALLY_CORRECT -> partial++;
                    case MANUAL_GRADING_REQUIRED -> manual++;
                    case EVALUATED_UNPUBLISHED -> manual++;
                    case PUBLISHED -> {
                        if (marks != null && ans.getMaxMarks() != null && marks.compareTo(ans.getMaxMarks()) >= 0) {
                            correct++;
                        } else if (marks != null && marks.compareTo(BigDecimal.ZERO) > 0) {
                            partial++;
                        } else {
                            incorrect++;
                        }
                    }
                    case UNANSWERED -> unanswered++;
                    case INVALID_SUBMISSION -> incorrect++;
                }
            }
        }

        summary.setTotalAwardedMarks(totalAwarded);
        summary.setTotalMaxMarks(totalMax);
        summary.setCorrectCount(correct);
        summary.setIncorrectCount(incorrect);
        summary.setPartiallyCorrectCount(partial);
        summary.setManualGradingCount(manual);
        summary.setUnansweredCount(unanswered);
        summary.setResults(results);

        if (summary.getPassingMarks() != null) {
            summary.setPassed(totalAwarded.compareTo(summary.getPassingMarks()) >= 0);
        }

        return summary;
    }

    // =========================================================================
    // REUSABLE SERVER-SIDE ENFORCEMENT HELPERS (S19, S22, S24)
    // =========================================================================

    public void validateAvailabilityWindow(Quiz quiz, LocalDateTime serverNow) {
        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new IllegalStateException("Quiz is not published and cannot be attempted");
        }
        if (quiz.getAvailableFrom() != null && serverNow.isBefore(quiz.getAvailableFrom())) {
            throw new IllegalStateException(
                    "Quiz is not available yet. Available from: " + quiz.getAvailableFrom());
        }
        if (quiz.getAvailableUntil() != null && serverNow.isAfter(quiz.getAvailableUntil())) {
            throw new IllegalStateException(
                    "Quiz availability window has closed at: " + quiz.getAvailableUntil());
        }
    }

    public void validateAttemptLimit(int allowedAttempts, long usedAttempts) {
        if (usedAttempts >= allowedAttempts) {
            throw new IllegalStateException(
                    "Maximum allowed attempts (" + allowedAttempts + ") reached for this quiz");
        }
    }

    public LocalDateTime computeExpiresAt(Quiz quiz, LocalDateTime startedAt) {
        LocalDateTime timerDeadline = quiz.getTimeLimitMinutes() != null
                ? startedAt.plusMinutes(quiz.getTimeLimitMinutes())
                : null;
        LocalDateTime windowEnd = quiz.getAvailableUntil();

        if (timerDeadline != null && windowEnd != null) {
            return timerDeadline.isBefore(windowEnd) ? timerDeadline : windowEnd;
        }
        return timerDeadline != null ? timerDeadline : windowEnd;
    }

    public boolean isAttemptExpired(Quiz quiz, QuizAttempt attempt, LocalDateTime serverNow) {
        if (attempt.getExpiresAt() != null && serverNow.isAfter(attempt.getExpiresAt())) {
            return true;
        }
        if (quiz.getTimeLimitMinutes() != null && attempt.getStartedAt() != null
                && serverNow.isAfter(attempt.getStartedAt().plusMinutes(quiz.getTimeLimitMinutes()))) {
            return true;
        }
        return quiz.getAvailableUntil() != null && serverNow.isAfter(quiz.getAvailableUntil());
    }

    public Long calculateRemainingSeconds(Quiz quiz, QuizAttempt attempt, LocalDateTime serverNow) {
        if (attempt.getStatus() != QuizAttemptStatus.IN_PROGRESS) {
            return 0L;
        }
        LocalDateTime effectiveExpiry = attempt.getExpiresAt();
        if (effectiveExpiry == null) {
            effectiveExpiry = computeExpiresAt(quiz, attempt.getStartedAt());
        }
        if (effectiveExpiry == null) {
            return null;
        }
        long seconds = ChronoUnit.SECONDS.between(serverNow, effectiveExpiry);
        return Math.max(0L, seconds);
    }



        // =========================================================================
    // STUDENT COURSE QUIZZES & IN-PROGRESS ATTEMPT HELPERS (FR-S18, FR-S21)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<QuizSettingsResponse> getStudentCourseQuizzes(User currentUser, Integer courseId) {
        validateActiveStudent(currentUser);
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        if (!enrollmentRepository.existsByStudentIdAndCourseId(currentUser.getId(), courseId)) {
            throw new AccessDeniedException("Student is not enrolled in this course");
        }
        List<Quiz> quizzes = quizRepository.findByCourseIdAndStatusOrderByCreatedAtDesc(courseId, QuizStatus.PUBLISHED);
        LocalDateTime serverNow = LocalDateTime.now(clock);
        return quizzes.stream().map(q -> {
            List<QuizQuestion> linked = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(q.getId());
            long used = quizAttemptRepository.countByQuizIdAndStudentId(q.getId(), currentUser.getId());
            return quizService.buildQuizSettingsResponse(q, linked, serverNow, used);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<com.olp.dto.StudentQuizQuestionDto> getAttemptQuestions(User currentUser, Integer quizId, Integer attemptId) {
        validateActiveStudent(currentUser);
        QuizAttempt attempt = findAndVerifyStudentAttempt(quizId, attemptId, currentUser);
        Quiz quiz = attempt.getQuiz();
        LocalDateTime serverNow = LocalDateTime.now(clock);

        if (attempt.getStatus() != QuizAttemptStatus.IN_PROGRESS || isAttemptExpired(quiz, attempt, serverNow)) {
            throw new IllegalStateException("Quiz attempt is not active (status: " + attempt.getStatus() + ")");
        }

        List<QuizQuestion> linkedQuestions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        List<QuizAttemptAnswer> savedAnswers = quizAttemptAnswerRepository != null
                ? quizAttemptAnswerRepository.findByAttemptId(attemptId)
                : List.of();

        Map<Integer, String> savedMap = new HashMap<>();
        for (QuizAttemptAnswer ans : savedAnswers) {
            if (ans.getQuestion() != null) {
                savedMap.put(ans.getQuestion().getId(), ans.getSubmittedAnswer());
            }
        }

        List<com.olp.dto.StudentQuizQuestionDto> dtos = new ArrayList<>();
        for (QuizQuestion qq : linkedQuestions) {
            Question q = qq.getQuestion();
            if (q != null) {
                com.olp.dto.StudentQuizQuestionDto dto = new com.olp.dto.StudentQuizQuestionDto();
                dto.setQuestionId(q.getId());
                dto.setQuestionText(q.getQuestionText());
                dto.setQuestionType(q.getQuestionType() != null ? q.getQuestionType().name() : null);
                dto.setOptionsJson(q.getOptionsJson());
                dto.setOrderIndex(qq.getOrderIndex());
                dto.setMarks(qq.getMarks() != null ? new BigDecimal(qq.getMarks().toString()) : BigDecimal.ONE);
                dto.setSavedAnswer(savedMap.get(q.getId()));
                dtos.add(dto);
            }
        }
        return dtos;
    }

    @Transactional
    public void saveAttemptAnswer(User currentUser, Integer quizId, Integer attemptId, com.olp.dto.SaveAnswerRequest request) {
        validateActiveStudent(currentUser);
        if (request == null || request.getQuestionId() == null) {
            throw new IllegalArgumentException("Question ID is required");
        }

        QuizAttempt attempt = findAndVerifyStudentAttempt(quizId, attemptId, currentUser);
        Quiz quiz = attempt.getQuiz();
        LocalDateTime serverNow = LocalDateTime.now(clock);

        if (attempt.getStatus() != QuizAttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException("Quiz attempt is already " + attempt.getStatus().name());
        }

        if (isAttemptExpired(quiz, attempt, serverNow)) {
            attempt.setStatus(QuizAttemptStatus.EXPIRED);
            attempt.setSubmittedAt(serverNow);
            quizAttemptRepository.save(attempt);
            throw new QuizAttemptExpiredException(
                    "Quiz time limit has expired according to server time (expired at " + attempt.getExpiresAt() + ")");
        }

        List<QuizQuestion> linkedQuestions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        QuizQuestion targetQQ = linkedQuestions.stream()
                .filter(qq -> qq.getQuestion() != null && qq.getQuestion().getId().equals(request.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Question not found in quiz: " + request.getQuestionId()));

        Question question = targetQQ.getQuestion();

        if (quizAttemptAnswerRepository != null) {
            Optional<QuizAttemptAnswer> existingOpt = quizAttemptAnswerRepository.findByAttemptIdAndQuestionId(attemptId, question.getId());
            if (existingOpt.isPresent()) {
                QuizAttemptAnswer existing = existingOpt.get();
                existing.setSubmittedAnswer(request.getSelectedAnswer());
                quizAttemptAnswerRepository.save(existing);
            } else {
                QuizAttemptAnswer newAnswer = new QuizAttemptAnswer(
                        attempt,
                        question,
                        request.getSelectedAnswer(),
                        null,
                        null,
                        null,
                        null,
                        serverNow
                );
                quizAttemptAnswerRepository.save(newAnswer);
            }
        }
    }

    private void expireStaleInProgressAttempts(Quiz quiz, Integer studentId, LocalDateTime serverNow) {
        List<QuizAttempt> inProgress = quizAttemptRepository
                .findByQuizIdAndStudentIdAndStatusOrderByAttemptNumberDesc(
                        quiz.getId(), studentId, QuizAttemptStatus.IN_PROGRESS);
        for (QuizAttempt attempt : inProgress) {
            if (isAttemptExpired(quiz, attempt, serverNow)) {
                attempt.setStatus(QuizAttemptStatus.EXPIRED);
                attempt.setSubmittedAt(serverNow);
                quizAttemptRepository.save(attempt);
            }
        }
    }

    private Quiz findPublishedQuizAndVerifyEnrollment(Integer quizId, User student) {
        if (quizId == null) {
            throw new IllegalArgumentException("Quiz ID cannot be null");
        }
        Quiz quiz = quizRepository.findByIdWithDetails(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));
        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new IllegalStateException("Quiz is not published");
        }
        if (!enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), quiz.getCourse().getId())) {
            throw new AccessDeniedException("Student is not enrolled in this course");
        }
        return quiz;
    }

    private QuizAttempt findAndVerifyStudentAttempt(Integer quizId, Integer attemptId, User student) {
        if (quizId == null || attemptId == null) {
            throw new IllegalArgumentException("Quiz ID and Attempt ID cannot be null");
        }
        QuizAttempt attempt = quizAttemptRepository.findByIdAndQuizId(attemptId, quizId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Quiz attempt not found with id: " + attemptId + " for quiz: " + quizId));
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new AccessDeniedException("Access denied: you do not own this quiz attempt");
        }
        return attempt;
    }

    private void validateActiveStudent(User currentUser) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (!"student".equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied: only students can attempt quizzes");
        }
        if (!"active".equalsIgnoreCase(currentUser.getAccountStatus())) {
            throw new AccessDeniedException("Access denied: student account is not active");
        }
    }

    private int resolveAllowedAttempts(Quiz quiz) {
        return quiz.getMaxAttempts() != null && quiz.getMaxAttempts() >= 1 ? quiz.getMaxAttempts() : 1;
    }

    private QuizAttemptResponse mapToAttemptResponse(QuizAttempt attempt,
                                                     Quiz quiz,
                                                     LocalDateTime serverNow,
                                                     long usedAttempts) {
        int maxAttempts = resolveAllowedAttempts(quiz);
        QuizAttemptResponse response = new QuizAttemptResponse();
        response.setId(attempt.getId());
        response.setQuizId(quiz.getId());
        response.setQuizTitle(quiz.getTitle());
        response.setStudentId(attempt.getStudent().getId());
        response.setAttemptNumber(attempt.getAttemptNumber());
        response.setMaxAttempts(maxAttempts);
        response.setRemainingAttempts((int) Math.max(0L, maxAttempts - usedAttempts));
        response.setStatus(attempt.getStatus().name());
        response.setStartedAt(attempt.getStartedAt());
        response.setExpiresAt(attempt.getExpiresAt());
        response.setSubmittedAt(attempt.getSubmittedAt());
        response.setServerTime(serverNow);
        response.setRemainingSeconds(calculateRemainingSeconds(quiz, attempt, serverNow));
        response.setObtainedMarks(attempt.getObtainedMarks());
        response.setTotalMarks(quiz.getTotalMarks());
        return response;
    }
}

