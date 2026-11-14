package com.olp.service;

import com.olp.dto.AnswerFeedbackRequest;
import com.olp.dto.AnswerFeedbackResponse;
import com.olp.dto.DescriptiveSubmissionResponse;
import com.olp.dto.GradeSubmissionRequest;
import com.olp.dto.GradeSubmissionResponse;
import com.olp.dto.PageResponse;
import com.olp.dto.PublishResultResponse;
import com.olp.dto.SubmissionFeedbackRequest;
import com.olp.dto.SubmissionFeedbackResponse;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing the instructor grading queue, evaluating descriptive submissions,
 * maintaining unpublished status (EVALUATED_UNPUBLISHED), and publishing assessment results (FR-I30, FR-I31, FR-I34).
 */
@Service
public class DescriptiveGradingService {

    private final QuizAttemptAnswerRepository quizAttemptAnswerRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRepository quizRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;

    @Autowired
    public DescriptiveGradingService(
            QuizAttemptAnswerRepository quizAttemptAnswerRepository,
            QuizAttemptRepository quizAttemptRepository,
            QuizRepository quizRepository,
            CourseRepository courseRepository,
            @Autowired(required = false) CourseInstructorRepository courseInstructorRepository,
            UserRepository userRepository) {
        this.quizAttemptAnswerRepository = quizAttemptAnswerRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizRepository = quizRepository;
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.userRepository = userRepository;
    }

    // =========================================================================
    // 1. GRADING QUEUE (FR-I30)
    // =========================================================================

    /**
     * Lists descriptive submissions in the grading queue for courses associated with the instructor.
     * Displays ungraded submissions first, followed by evaluated submissions.
     */
    @Transactional(readOnly = true)
    public List<DescriptiveSubmissionResponse> getGradingQueue(
            User currentUser,
            Integer courseId,
            Integer quizId,
            String statusStr) {

        validateInstructor(currentUser);
        Set<Integer> courseIds = resolveAndVerifyCourseIds(currentUser, courseId);
        if (courseIds.isEmpty()) {
            return Collections.emptyList();
        }

        if (quizId != null) {
            verifyQuizInCourses(quizId, courseIds);
        }

        EvaluationStatus filterStatus = parseEvaluationStatus(statusStr);
        List<QuizAttemptAnswer> answers = quizAttemptAnswerRepository.findDescriptiveGradingQueue(
                courseIds, courseId, quizId, filterStatus);

        return answers.stream()
                .map(DescriptiveSubmissionResponse::fromEntity)
                .toList();
    }

    /**
     * Paginated grading queue for instructor courses.
     */
    @Transactional(readOnly = true)
    public PageResponse<DescriptiveSubmissionResponse> getGradingQueuePaged(
            User currentUser,
            Integer courseId,
            Integer quizId,
            String statusStr,
            int page,
            int size) {

        validateInstructor(currentUser);
        Set<Integer> courseIds = resolveAndVerifyCourseIds(currentUser, courseId);
        if (courseIds.isEmpty()) {
            return new PageResponse<>(Collections.emptyList(), page, size, 0, 0, true);
        }

        if (quizId != null) {
            verifyQuizInCourses(quizId, courseIds);
        }

        EvaluationStatus filterStatus = parseEvaluationStatus(statusStr);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<QuizAttemptAnswer> pageResult = quizAttemptAnswerRepository.findDescriptiveGradingQueuePaged(
                courseIds, courseId, quizId, filterStatus, pageable);

        List<DescriptiveSubmissionResponse> dtos = pageResult.getContent().stream()
                .map(DescriptiveSubmissionResponse::fromEntity)
                .toList();

        return new PageResponse<>(dtos, pageResult.getNumber(), pageResult.getSize(),
                pageResult.getTotalElements(), pageResult.getTotalPages(), pageResult.isLast());
    }

    /**
     * Retrieves a single submission item for evaluation.
     */
    @Transactional(readOnly = true)
    public DescriptiveSubmissionResponse getSubmissionDetails(User currentUser, Integer answerId) {
        validateInstructor(currentUser);
        if (answerId == null) {
            throw new IllegalArgumentException("Submission answer ID cannot be null");
        }

        QuizAttemptAnswer answer = quizAttemptAnswerRepository.findByIdWithDetails(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission answer not found with id: " + answerId));

        verifyAnswerAssociation(answer, currentUser);
        return DescriptiveSubmissionResponse.fromEntity(answer);
    }

    // =========================================================================
    // 2. GRADE SUBMISSIONS (FR-I30, FR-I31)
    // =========================================================================

    /**
     * Evaluates a descriptive submission:
     * - Validates non-negative marks and bounds (awarded <= maxMarks).
     * - Saves awarded marks, grader ID, grading timestamp, and feedback.
     * - Sets status to EVALUATED_UNPUBLISHED by default (or PUBLISHED if requested).
     */
    @Transactional
    public GradeSubmissionResponse gradeSubmission(User currentUser, Integer answerId, GradeSubmissionRequest request) {
        validateInstructor(currentUser);
        if (answerId == null) {
            throw new IllegalArgumentException("Submission answer ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Grading request cannot be null");
        }

        QuizAttemptAnswer answer = quizAttemptAnswerRepository.findByIdWithDetails(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission answer not found with id: " + answerId));

        verifyAnswerAssociation(answer, currentUser);

        if (answer.getEvaluationStatus() == EvaluationStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot modify evaluation or feedback: answer results have already been published");
        }

        // Marks bounds validation
        BigDecimal awarded = request.getAwardedMarks();
        if (awarded == null) {
            throw new IllegalArgumentException("Awarded marks cannot be null");
        }
        if (awarded.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Awarded marks cannot be negative");
        }

        BigDecimal maxMarks = answer.getMaxMarks() != null ? answer.getMaxMarks() : BigDecimal.ZERO;
        if (awarded.compareTo(maxMarks) > 0) {
            throw new IllegalArgumentException(
                    "Awarded marks (" + awarded + ") cannot exceed maximum marks (" + maxMarks + ")");
        }

        // Determine target evaluation status
        boolean publish = Boolean.TRUE.equals(request.getPublish()) || "PUBLISHED".equalsIgnoreCase(request.getStatus());
        EvaluationStatus targetStatus = publish ? EvaluationStatus.PUBLISHED : EvaluationStatus.EVALUATED_UNPUBLISHED;

        LocalDateTime now = LocalDateTime.now();
        answer.setAwardedMarks(awarded);
        answer.setFeedback(request.getFeedback());
        answer.setGrader(currentUser);
        answer.setEvaluatedAt(now);
        answer.setFeedbackAt(now);
        answer.setEvaluationStatus(targetStatus);

        QuizAttemptAnswer saved = quizAttemptAnswerRepository.save(answer);

        // If published immediately, update the attempt score and check attempt publish status
        if (publish) {
            recalculateAttemptScore(answer.getAttempt());
        }

        DescriptiveSubmissionResponse dto = DescriptiveSubmissionResponse.fromEntity(saved);
        String message = publish
                ? "Submission graded and published successfully"
                : "Submission graded successfully with status EVALUATED_UNPUBLISHED";

        return new GradeSubmissionResponse(message, dto);
    }

    // =========================================================================
    // 3. RESULT PUBLISHING WORKFLOW (FR-I28, FR-I34)
    // =========================================================================

    /**
     * Publishes a single evaluated submission, making it visible to the student.
     */
    @Transactional
    public PublishResultResponse publishAnswerResult(User currentUser, Integer answerId) {
        validateInstructor(currentUser);
        QuizAttemptAnswer answer = quizAttemptAnswerRepository.findByIdWithDetails(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission answer not found with id: " + answerId));

        verifyAnswerAssociation(answer, currentUser);

        if (answer.getEvaluationStatus() == EvaluationStatus.PUBLISHED) {
            return new PublishResultResponse("Result is already published", 0,
                    answer.getAttempt().getQuiz().getId(), answer.getAttempt().getId());
        }

        answer.setEvaluationStatus(EvaluationStatus.PUBLISHED);
        quizAttemptAnswerRepository.save(answer);

        recalculateAttemptScore(answer.getAttempt());

        return new PublishResultResponse("Graded submission published successfully", 1,
                answer.getAttempt().getQuiz().getId(), answer.getAttempt().getId());
    }

    /**
     * Publishes all evaluated unpublished submissions for a given quiz.
     */
    @Transactional
    public PublishResultResponse publishQuizResults(User currentUser, Integer quizId) {
        validateInstructor(currentUser);
        if (quizId == null) {
            throw new IllegalArgumentException("Quiz ID cannot be null");
        }

        Quiz quiz = quizRepository.findByIdWithDetails(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));

        verifyCourseAssociation(quiz.getCourse(), currentUser);

        List<QuizAttemptAnswer> unpublished = quizAttemptAnswerRepository.findEvaluatedUnpublishedByQuizId(quizId);
        int count = 0;
        Set<QuizAttempt> affectedAttempts = new HashSet<>();

        for (QuizAttemptAnswer ans : unpublished) {
            ans.setEvaluationStatus(EvaluationStatus.PUBLISHED);
            quizAttemptAnswerRepository.save(ans);
            affectedAttempts.add(ans.getAttempt());
            count++;
        }

        for (QuizAttempt att : affectedAttempts) {
            recalculateAttemptScore(att);
        }

        return new PublishResultResponse("Published " + count + " evaluated submission(s) for quiz: " + quiz.getTitle(),
                count, quizId, null);
    }

    /**
     * Publishes all evaluated unpublished submissions for a specific student attempt.
     */
    @Transactional
    public PublishResultResponse publishAttemptResults(User currentUser, Integer attemptId) {
        validateInstructor(currentUser);
        if (attemptId == null) {
            throw new IllegalArgumentException("Attempt ID cannot be null");
        }

        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found with id: " + attemptId));

        verifyCourseAssociation(attempt.getQuiz().getCourse(), currentUser);

        List<QuizAttemptAnswer> unpublished = quizAttemptAnswerRepository.findEvaluatedUnpublishedByAttemptId(attemptId);
        int count = 0;
        for (QuizAttemptAnswer ans : unpublished) {
            ans.setEvaluationStatus(EvaluationStatus.PUBLISHED);
            quizAttemptAnswerRepository.save(ans);
            count++;
        }

        attempt.setIsPublished(true);
        recalculateAttemptScore(attempt);

        return new PublishResultResponse("Published " + count + " evaluated submission(s) for attempt #" + attemptId,
                count, attempt.getQuiz().getId(), attemptId);
    }

    // =========================================================================
    // 4. FEEDBACK ON GRADED WORK (FR-I31)
    // =========================================================================

    /**
     * Adds or updates answer-level feedback for a descriptive answer (FR-I31).
     * Prevents feedback modification after results are published.
     */
    @Transactional
    public AnswerFeedbackResponse addOrUpdateAnswerFeedback(User currentUser, Integer answerId, AnswerFeedbackRequest request) {
        validateInstructor(currentUser);
        if (answerId == null) {
            throw new IllegalArgumentException("Answer ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Feedback request cannot be null");
        }

        QuizAttemptAnswer answer = quizAttemptAnswerRepository.findByIdWithDetails(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission answer not found with id: " + answerId));

        verifyAnswerAssociation(answer, currentUser);

        // Disallow modification if already published
        if (answer.getEvaluationStatus() == EvaluationStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot modify feedback: answer results have already been published");
        }

        LocalDateTime now = LocalDateTime.now();
        answer.setFeedback(request.getFeedback());
        answer.setGrader(currentUser);
        answer.setFeedbackAt(now);
        if (answer.getEvaluatedAt() == null) {
            answer.setEvaluatedAt(now);
        }

        boolean publish = Boolean.TRUE.equals(request.getPublish());
        if (publish) {
            answer.setEvaluationStatus(EvaluationStatus.PUBLISHED);
            recalculateAttemptScore(answer.getAttempt());
        }

        QuizAttemptAnswer saved = quizAttemptAnswerRepository.save(answer);
        return AnswerFeedbackResponse.fromEntity(saved, "Answer feedback saved successfully");
    }

    /**
     * Adds or updates submission-level overall feedback for an attempt (FR-I31).
     * Prevents feedback modification after results are published.
     */
    @Transactional
    public SubmissionFeedbackResponse addOrUpdateSubmissionFeedback(User currentUser, Integer attemptId, SubmissionFeedbackRequest request) {
        validateInstructor(currentUser);
        if (attemptId == null) {
            throw new IllegalArgumentException("Attempt ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Feedback request cannot be null");
        }

        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found with id: " + attemptId));

        verifyCourseAssociation(attempt.getQuiz().getCourse(), currentUser);

        // Disallow modification if already published
        if (Boolean.TRUE.equals(attempt.getIsPublished())) {
            throw new IllegalStateException("Cannot modify submission feedback: results have already been published");
        }

        LocalDateTime now = LocalDateTime.now();
        attempt.setOverallFeedback(request.getFeedback());
        attempt.setGrader(currentUser);
        attempt.setFeedbackAt(now);

        boolean publish = Boolean.TRUE.equals(request.getPublish());
        if (publish) {
            attempt.setIsPublished(true);
        }

        QuizAttempt saved = quizAttemptRepository.save(attempt);
        return SubmissionFeedbackResponse.fromEntity(saved, "Submission feedback saved successfully");
    }

    /**
     * Retrieves submission-level feedback with appropriate access control and student visibility masking (FR-I31, FR-I23).
     */
    @Transactional(readOnly = true)
    public SubmissionFeedbackResponse getSubmissionFeedback(User currentUser, Integer attemptId) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (attemptId == null) {
            throw new IllegalArgumentException("Attempt ID cannot be null");
        }

        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found with id: " + attemptId));

        boolean isStudent = currentUser.getId().equals(attempt.getStudent().getId());
        if (isStudent) {
            Quiz quiz = attempt.getQuiz();
            boolean feedbackReleased = quiz.getFeedbackReleased() == null || quiz.getFeedbackReleased();
            boolean isPublished = Boolean.TRUE.equals(attempt.getIsPublished());

            if (!feedbackReleased || !isPublished) {
                throw new AccessDeniedException("Feedback is not yet visible or results have not been published");
            }
        } else {
            // Instructor or Admin check
            verifyCourseAssociation(attempt.getQuiz().getCourse(), currentUser);
        }

        return SubmissionFeedbackResponse.fromEntity(attempt, "Submission feedback retrieved successfully");
    }

    /**
     * Updates an attempt's isPublished status if all evaluated answers are published.
     */
    public void checkAndUpdateAttemptPublishStatus(QuizAttempt attempt) {
        recalculateAttemptScore(attempt);
    }

    // =========================================================================
    // HELPER & AUTHORIZATION METHODS
    // =========================================================================

    public void recalculateAttemptScore(QuizAttempt attempt) {
        if (attempt == null) return;
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

    private Set<Integer> resolveAndVerifyCourseIds(User currentUser, Integer courseId) {
        if (courseId != null) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
            if (course.isDeleted()) {
                throw new ResourceNotFoundException("Course not found with id: " + courseId);
            }
            verifyCourseAssociation(course, currentUser);
            return Collections.singleton(courseId);
        }

        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            return courseRepository.findAll().stream()
                    .filter(c -> !c.isDeleted())
                    .map(Course::getId)
                    .collect(Collectors.toSet());
        }

        Set<Integer> courseIds = new LinkedHashSet<>();
        List<Course> primaryCourses = courseRepository.findByInstructorIdOrderByIdAsc(currentUser.getId());
        if (primaryCourses != null) {
            for (Course c : primaryCourses) {
                if (c != null && c.getId() != null && !c.isDeleted()) {
                    courseIds.add(c.getId());
                }
            }
        }
        if (courseInstructorRepository != null) {
            List<CourseInstructor> associations = courseInstructorRepository.findByInstructorId(currentUser.getId());
            if (associations != null) {
                for (CourseInstructor ci : associations) {
                    if (ci != null && ci.getCourse() != null && ci.getCourse().getId() != null && !ci.getCourse().isDeleted()) {
                        courseIds.add(ci.getCourse().getId());
                    }
                }
            }
        }
        return courseIds;
    }

    private void verifyAnswerAssociation(QuizAttemptAnswer answer, User currentUser) {
        if (answer.getAttempt() == null ||
            answer.getAttempt().getQuiz() == null ||
            answer.getAttempt().getQuiz().getCourse() == null) {
            throw new IllegalStateException("Answer record is corrupted: missing quiz/course relationships");
        }
        verifyCourseAssociation(answer.getAttempt().getQuiz().getCourse(), currentUser);
    }

    private void verifyCourseAssociation(Course course, User currentUser) {
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

    private void verifyQuizInCourses(Integer quizId, Set<Integer> allowedCourseIds) {
        Quiz quiz = quizRepository.findByIdWithDetails(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));
        if (quiz.getCourse() == null || !allowedCourseIds.contains(quiz.getCourse().getId())) {
            throw new AccessDeniedException("Access denied: Quiz does not belong to an associated course");
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

    private void validateInstructor(User currentUser) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (!"instructor".equalsIgnoreCase(currentUser.getRole()) && !"admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied: only instructors or admins can grade submissions");
        }
        if (!"admin".equalsIgnoreCase(currentUser.getRole()) && !"active".equalsIgnoreCase(currentUser.getAccountStatus())) {
            throw new AccessDeniedException("Access denied: Instructor account is awaiting admin approval");
        }
    }

    private EvaluationStatus parseEvaluationStatus(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty() || "ALL".equalsIgnoreCase(statusStr.trim())) {
            return null;
        }
        String clean = statusStr.trim().toUpperCase();
        if ("UNGRADED".equals(clean) || "PENDING".equals(clean)) {
            return EvaluationStatus.MANUAL_GRADING_REQUIRED;
        }
        if ("GRADED".equals(clean)) {
            return EvaluationStatus.EVALUATED_UNPUBLISHED;
        }
        try {
            return EvaluationStatus.valueOf(clean);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid grading status: " + statusStr +
                    ". Allowed: UNGRADED, EVALUATED_UNPUBLISHED, PUBLISHED, ALL");
        }
    }
}
