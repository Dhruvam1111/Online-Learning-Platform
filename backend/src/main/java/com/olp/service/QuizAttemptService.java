package com.olp.service;

import com.olp.dto.QuizAttemptResponse;
import com.olp.dto.QuizSettingsResponse;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.QuizAttemptExpiredException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service implementing server-side Quiz availability window checks, persistent attempt-limit enforcement,
 * and authoritative server-clock timer enforcement (FR-I20, DR-ED06) reusable by S19, S22, and S24.
 */
@Service
public class QuizAttemptService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final QuizService quizService;
    private Clock clock = Clock.systemDefaultZone();

    public QuizAttemptService(QuizRepository quizRepository,
                              QuizQuestionRepository quizQuestionRepository,
                              QuizAttemptRepository quizAttemptRepository,
                              EnrollmentRepository enrollmentRepository,
                              UserRepository userRepository,
                              QuizService quizService) {
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.quizService = quizService;
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
        QuizAttempt saved = quizAttemptRepository.save(attempt);
        long usedAttempts = quizAttemptRepository.countByQuizIdAndStudentId(quiz.getId(), currentUser.getId());
        return mapToAttemptResponse(saved, quiz, serverNow, usedAttempts);
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

