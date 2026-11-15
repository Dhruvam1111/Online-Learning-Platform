package com.olp.service;

import com.olp.dto.GradeOverrideRequest;
import com.olp.dto.GradeOverrideResponse;
import com.olp.dto.GradingResultVersionResponse;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Reusable Result-Versioning Service implementing Grade Overrides with Justification
 * (FR-I32, DR-01, DR-ED04).
 *
 * Ensures:
 * - Immutable version history is created for every grade override.
 * - Mandatory, non-empty justification recording.
 * - Score range bounds validation (awarded in [0, maxMarks]).
 * - Course association access control (DR-05).
 * - Extensibility for future question dispute resolution and result corrections.
 */
@Service
public class GradingResultVersioningService {

    private final GradingResultVersionRepository gradingResultVersionRepository;
    private final QuizAttemptAnswerRepository quizAttemptAnswerRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;

    @Autowired
    public GradingResultVersioningService(
            GradingResultVersionRepository gradingResultVersionRepository,
            QuizAttemptAnswerRepository quizAttemptAnswerRepository,
            QuizAttemptRepository quizAttemptRepository,
            CourseRepository courseRepository,
            CourseInstructorRepository courseInstructorRepository,
            UserRepository userRepository) {
        this.gradingResultVersionRepository = gradingResultVersionRepository;
        this.quizAttemptAnswerRepository = quizAttemptAnswerRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.userRepository = userRepository;
    }

    // =========================================================================
    // 1. ANSWER-LEVEL GRADE OVERRIDE (FR-I32)
    // =========================================================================

    /**
     * Overrides an automatically (or manually) graded answer mark with mandatory justification.
     * Creates an immutable result version preserving complete audit history (DR-ED04, DR-01).
     */
    @Transactional
    public GradeOverrideResponse overrideAnswerGrade(User currentUser, Integer answerId, GradeOverrideRequest request) {
        validateInstructor(currentUser);

        if (answerId == null) {
            throw new IllegalArgumentException("Answer ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Grade override request cannot be null");
        }

        // 1. Validate mandatory justification
        String justification = request.getJustification();
        if (justification == null || justification.trim().isEmpty()) {
            throw new IllegalArgumentException("Justification is mandatory and cannot be empty for grade overrides");
        }

        // 2. Fetch answer with details
        QuizAttemptAnswer answer = quizAttemptAnswerRepository.findByIdWithDetails(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission answer not found with id: " + answerId));

        QuizAttempt attempt = answer.getAttempt();
        if (attempt == null || attempt.getQuiz() == null || attempt.getQuiz().getCourse() == null) {
            throw new IllegalStateException("Answer record is corrupted: missing quiz or course associations");
        }

        // 3. Verify instructor course association (DR-05)
        verifyCourseAssociation(attempt.getQuiz().getCourse(), currentUser);

        // 4. Validate updated marks range
        BigDecimal newMarks = request.getUpdatedMarks();
        if (newMarks == null) {
            throw new IllegalArgumentException("Updated marks cannot be null");
        }
        if (newMarks.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Updated marks cannot be negative");
        }

        BigDecimal maxMarks = answer.getMaxMarks() != null ? answer.getMaxMarks() : BigDecimal.ZERO;
        if (newMarks.compareTo(maxMarks) > 0) {
            throw new IllegalArgumentException(
                    "Updated marks (" + newMarks + ") cannot exceed maximum marks (" + maxMarks + ")");
        }

        // 5. Prevent duplicate override operations
        BigDecimal previousMarks = answer.getAwardedMarks() != null ? answer.getAwardedMarks() : BigDecimal.ZERO;
        if (newMarks.compareTo(previousMarks) == 0) {
            throw new IllegalArgumentException(
                    "New marks (" + newMarks + ") are identical to existing awarded marks (" + previousMarks + ")");
        }

        EvaluationStatus previousStatus = answer.getEvaluationStatus();
        String statusBefore = previousStatus != null ? previousStatus.name() : null;

        // 6. Determine target status
        EvaluationStatus targetStatus;
        boolean publish = Boolean.TRUE.equals(request.getPublish()) || previousStatus == EvaluationStatus.PUBLISHED;
        if (publish) {
            targetStatus = EvaluationStatus.PUBLISHED;
        } else {
            if (newMarks.compareTo(maxMarks) >= 0) {
                targetStatus = EvaluationStatus.CORRECT;
            } else if (newMarks.compareTo(BigDecimal.ZERO) == 0) {
                targetStatus = EvaluationStatus.INCORRECT;
            } else {
                targetStatus = EvaluationStatus.PARTIALLY_CORRECT;
            }
        }
        String statusAfter = targetStatus.name();

        // 7. Determine version number
        int nextVersion = gradingResultVersionRepository.countByAnswerId(answerId) + 1;

        // 8. Create and persist immutable result version record
        LocalDateTime now = LocalDateTime.now();
        GradingResultVersion version = new GradingResultVersion(
                attempt,
                answer,
                nextVersion,
                previousMarks,
                newMarks,
                maxMarks,
                justification.trim(),
                currentUser,
                statusBefore,
                statusAfter,
                now
        );
        GradingResultVersion savedVersion = gradingResultVersionRepository.save(version);

        // 9. Update the answer with new mark, grader attribution, and overridden flag
        answer.setAwardedMarks(newMarks);
        answer.setEvaluationStatus(targetStatus);
        answer.setGrader(currentUser);
        answer.setEvaluatedAt(now);
        answer.setIsOverridden(true);
        quizAttemptAnswerRepository.save(answer);

        // 10. Recalculate attempt score
        recalculateAttemptScore(attempt);

        return GradeOverrideResponse.fromEntity(
                savedVersion,
                attempt,
                answer,
                "Grade successfully overridden (version " + nextVersion + ")"
        );
    }

    // =========================================================================
    // 2. ATTEMPT-LEVEL GRADE OVERRIDE (FR-I32)
    // =========================================================================

    /**
     * Overrides overall attempt marks with mandatory justification.
     * Creates an immutable result version preserving complete audit history (DR-ED04).
     */
    @Transactional
    public GradeOverrideResponse overrideAttemptGrade(User currentUser, Integer attemptId, GradeOverrideRequest request) {
        validateInstructor(currentUser);

        if (attemptId == null) {
            throw new IllegalArgumentException("Attempt ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Grade override request cannot be null");
        }

        String justification = request.getJustification();
        if (justification == null || justification.trim().isEmpty()) {
            throw new IllegalArgumentException("Justification is mandatory and cannot be empty for grade overrides");
        }

        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found with id: " + attemptId));

        if (attempt.getQuiz() == null || attempt.getQuiz().getCourse() == null) {
            throw new IllegalStateException("Attempt record is corrupted: missing quiz or course associations");
        }

        verifyCourseAssociation(attempt.getQuiz().getCourse(), currentUser);

        BigDecimal newMarks = request.getUpdatedMarks();
        if (newMarks == null) {
            throw new IllegalArgumentException("Updated marks cannot be null");
        }
        if (newMarks.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Updated marks cannot be negative");
        }

        BigDecimal maxMarks = attempt.getQuiz().getTotalMarks() != null
                ? attempt.getQuiz().getTotalMarks()
                : BigDecimal.ZERO;
        if (newMarks.compareTo(maxMarks) > 0) {
            throw new IllegalArgumentException(
                    "Updated marks (" + newMarks + ") cannot exceed quiz total marks (" + maxMarks + ")");
        }

        BigDecimal previousMarks = attempt.getObtainedMarks() != null ? attempt.getObtainedMarks() : BigDecimal.ZERO;
        if (newMarks.compareTo(previousMarks) == 0) {
            throw new IllegalArgumentException(
                    "New marks (" + newMarks + ") are identical to existing attempt marks (" + previousMarks + ")");
        }

        int nextVersion = gradingResultVersionRepository.countByAttemptIdAndAnswerIsNull(attemptId) + 1;
        LocalDateTime now = LocalDateTime.now();

        GradingResultVersion version = new GradingResultVersion(
                attempt,
                null,
                nextVersion,
                previousMarks,
                newMarks,
                maxMarks,
                justification.trim(),
                currentUser,
                attempt.getStatus() != null ? attempt.getStatus().name() : null,
                attempt.getStatus() != null ? attempt.getStatus().name() : null,
                now
        );
        GradingResultVersion savedVersion = gradingResultVersionRepository.save(version);

        attempt.setObtainedMarks(newMarks);
        attempt.setIsOverridden(true);
        attempt.setGrader(currentUser);
        if (Boolean.TRUE.equals(request.getPublish())) {
            attempt.setIsPublished(true);
        }
        quizAttemptRepository.save(attempt);

        return GradeOverrideResponse.fromEntity(
                savedVersion,
                attempt,
                null,
                "Attempt grade successfully overridden (version " + nextVersion + ")"
        );
    }

    // =========================================================================
    // 3. REUSABLE VERSIONING EXTENSION (DR-ED04)
    // =========================================================================

    /**
     * Generic reusable method to record a result version for future result corrections and dispute resolutions.
     */
    @Transactional
    public GradingResultVersion recordVersion(
            QuizAttempt attempt,
            QuizAttemptAnswer answer,
            BigDecimal previousMarks,
            BigDecimal newMarks,
            BigDecimal maxMarks,
            String justification,
            User actor,
            String statusBefore,
            String statusAfter) {

        Objects.requireNonNull(attempt, "Attempt cannot be null");
        Objects.requireNonNull(actor, "Actor cannot be null");
        if (justification == null || justification.trim().isEmpty()) {
            throw new IllegalArgumentException("Justification is mandatory when recording result versions");
        }

        int nextVersion;
        if (answer != null && answer.getId() != null) {
            nextVersion = gradingResultVersionRepository.countByAnswerId(answer.getId()) + 1;
        } else {
            nextVersion = gradingResultVersionRepository.countByAttemptIdAndAnswerIsNull(attempt.getId()) + 1;
        }

        GradingResultVersion version = new GradingResultVersion(
                attempt,
                answer,
                nextVersion,
                previousMarks != null ? previousMarks : BigDecimal.ZERO,
                newMarks != null ? newMarks : BigDecimal.ZERO,
                maxMarks != null ? maxMarks : BigDecimal.ZERO,
                justification.trim(),
                actor,
                statusBefore,
                statusAfter,
                LocalDateTime.now()
        );

        return gradingResultVersionRepository.save(version);
    }

    // =========================================================================
    // 4. HISTORY RETRIEVAL (DR-01, DR-ED04)
    // =========================================================================

    /**
     * Retrieves the complete version history for a specific answer.
     * Restricts access to authorized course instructors (DR-01).
     */
    @Transactional(readOnly = true)
    public List<GradingResultVersionResponse> getAnswerVersionHistory(User currentUser, Integer answerId) {
        validateInstructor(currentUser);
        if (answerId == null) {
            throw new IllegalArgumentException("Answer ID cannot be null");
        }

        QuizAttemptAnswer answer = quizAttemptAnswerRepository.findByIdWithDetails(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission answer not found with id: " + answerId));

        verifyCourseAssociation(answer.getAttempt().getQuiz().getCourse(), currentUser);

        List<GradingResultVersion> versions = gradingResultVersionRepository
                .findByAnswerIdOrderByVersionNumberAsc(answerId);

        return versions.stream()
                .map(GradingResultVersionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the complete version history for an attempt.
     * Restricts access to authorized course instructors (DR-01).
     */
    @Transactional(readOnly = true)
    public List<GradingResultVersionResponse> getAttemptVersionHistory(User currentUser, Integer attemptId) {
        validateInstructor(currentUser);
        if (attemptId == null) {
            throw new IllegalArgumentException("Attempt ID cannot be null");
        }

        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found with id: " + attemptId));

        verifyCourseAssociation(attempt.getQuiz().getCourse(), currentUser);

        List<GradingResultVersion> versions = gradingResultVersionRepository
                .findByAttemptIdOrderByVersionNumberAsc(attemptId);

        return versions.stream()
                .map(GradingResultVersionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // HELPER & AUTHORIZATION METHODS
    // =========================================================================

    public void recalculateAttemptScore(QuizAttempt attempt) {
        if (attempt == null || attempt.getId() == null) return;
        List<QuizAttemptAnswer> answers = quizAttemptAnswerRepository.findByAttemptId(attempt.getId());
        BigDecimal total = BigDecimal.ZERO;
        boolean hasPending = false;

        if (answers != null) {
            for (QuizAttemptAnswer ans : answers) {
                if (ans.getEvaluationStatus() == EvaluationStatus.MANUAL_GRADING_REQUIRED ||
                    ans.getEvaluationStatus() == EvaluationStatus.EVALUATED_UNPUBLISHED) {
                    hasPending = true;
                }
                if (ans.getEvaluationStatus() == EvaluationStatus.PUBLISHED ||
                    ans.getEvaluationStatus() == EvaluationStatus.CORRECT ||
                    ans.getEvaluationStatus() == EvaluationStatus.PARTIALLY_CORRECT) {
                    if (ans.getAwardedMarks() != null) {
                        total = total.add(ans.getAwardedMarks());
                    }
                }
            }
            if (!hasPending && !answers.isEmpty()) {
                attempt.setIsPublished(true);
            }
        }

        attempt.setObtainedMarks(total);
        quizAttemptRepository.save(attempt);
    }

    public void verifyCourseAssociation(Course course, User currentUser) {
        if (course == null || currentUser == null) {
            throw new AccessDeniedException("Access denied: invalid course or user");
        }
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            return;
        }
        boolean isOwner = course.getInstructor() != null && currentUser.getId().equals(course.getInstructor().getId());
        boolean isAssociated = courseInstructorRepository != null &&
                courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), currentUser.getId());

        if (!isOwner && !isAssociated) {
            throw new AccessDeniedException(
                    "Access denied: You are not associated with course " + course.getId() + " (DR-05)");
        }
    }

    public void validateInstructor(User currentUser) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (!"instructor".equalsIgnoreCase(currentUser.getRole()) && !"admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied: only instructors or admins can override grades");
        }
        if (!"admin".equalsIgnoreCase(currentUser.getRole()) && !"active".equalsIgnoreCase(currentUser.getAccountStatus())) {
            throw new AccessDeniedException("Access denied: Instructor account is awaiting admin approval");
        }
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
                    .orElseThrow(() -> new InvalidCredentialsException("User not found with email: " + email));
        }
        throw new InvalidCredentialsException("Invalid authentication principal");
    }
}
