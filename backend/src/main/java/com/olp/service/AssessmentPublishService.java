package com.olp.service;

import com.olp.dto.AssessmentPublishStatusResponse;
import com.olp.dto.PublishAssessmentResultsRequest;
import com.olp.dto.WithholdAssessmentResultsRequest;
import com.olp.entity.*;
import com.olp.event.AssessmentResultsPublishedEvent;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Service for managing publication, withholding, and locking of assessment results (FR-I34, Step 51).
 * Enforces ungraded gates, result locking (DR-01), withholding from students, and notification dispatch.
 */
@Service
public class AssessmentPublishService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAttemptAnswerRepository quizAttemptAnswerRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private Clock clock = Clock.systemDefaultZone();

    @Autowired
    public AssessmentPublishService(QuizRepository quizRepository,
                                   QuizAttemptRepository quizAttemptRepository,
                                   QuizAttemptAnswerRepository quizAttemptAnswerRepository,
                                   AssignmentRepository assignmentRepository,
                                   AssignmentSubmissionRepository assignmentSubmissionRepository,
                                   ExamRepository examRepository,
                                   ExamAttemptRepository examAttemptRepository,
                                   CourseInstructorRepository courseInstructorRepository,
                                   UserRepository userRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizAttemptAnswerRepository = quizAttemptAnswerRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
        this.examRepository = examRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public User resolveUser(Authentication authentication) {
        return com.olp.security.SecurityUtils.resolveUser(authentication, userRepository);
    }

    public AssessmentType parseAssessmentType(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) {
            throw new IllegalArgumentException("Assessment type cannot be blank");
        }
        try {
            return AssessmentType.valueOf(typeStr.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid assessment type: " + typeStr + ". Must be QUIZ, ASSIGNMENT, or EXAM.");
        }
    }

    private void checkInstructorAccess(User currentUser, Course course) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (!com.olp.security.SecurityUtils.isCourseOwnerOrCoInstructorOrAdmin(
                course, currentUser, courseInstructorRepository)) {
            throw new AccessDeniedException("User is not authorized to manage results for course: " + course.getId());
        }
    }

    /**
     * Get publication and grading status for an assessment.
     */
    @Transactional(readOnly = true)
    public AssessmentPublishStatusResponse getPublishStatus(User currentUser, AssessmentType type, Integer assessmentId) {
        if (type == null) {
            throw new IllegalArgumentException("Assessment type cannot be null");
        }
        if (assessmentId == null) {
            throw new IllegalArgumentException("Assessment ID cannot be null");
        }

        return switch (type) {
            case EXAM -> {
                Exam exam = examRepository.findById(assessmentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + assessmentId));
                checkInstructorAccess(currentUser, exam.getCourse());

                long total = examAttemptRepository.countByExamId(assessmentId);
                long ungraded = examAttemptRepository.countUngradedByExamId(assessmentId);
                long graded = examAttemptRepository.countGradedByExamId(assessmentId);
                boolean isLocked = Boolean.TRUE.equals(exam.getResultsPublished());

                yield new AssessmentPublishStatusResponse(
                        AssessmentType.EXAM,
                        exam.getId(),
                        exam.getTitle(),
                        exam.getCourse().getId(),
                        exam.getResultsPublished(),
                        exam.getResultsWithheld(),
                        exam.getResultsPublishedAt(),
                        total,
                        graded,
                        ungraded,
                        isLocked,
                        0,
                        "Exam result publish status retrieved successfully"
                );
            }
            case ASSIGNMENT -> {
                Assignment assignment = assignmentRepository.findById(assessmentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assessmentId));
                checkInstructorAccess(currentUser, assignment.getCourse());

                long total = assignmentSubmissionRepository.countByAssignmentId(assessmentId);
                long ungraded = assignmentSubmissionRepository.countUngradedByAssignmentId(assessmentId);
                long graded = assignmentSubmissionRepository.countGradedByAssignmentId(assessmentId);
                boolean isLocked = Boolean.TRUE.equals(assignment.getResultsPublished());

                yield new AssessmentPublishStatusResponse(
                        AssessmentType.ASSIGNMENT,
                        assignment.getId(),
                        assignment.getTitle(),
                        assignment.getCourse().getId(),
                        assignment.getResultsPublished(),
                        assignment.getResultsWithheld(),
                        assignment.getResultsPublishedAt(),
                        total,
                        graded,
                        ungraded,
                        isLocked,
                        0,
                        "Assignment result publish status retrieved successfully"
                );
            }
            case QUIZ -> {
                Quiz quiz = quizRepository.findById(assessmentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + assessmentId));
                checkInstructorAccess(currentUser, quiz.getCourse());

                long total = quizAttemptRepository.countByQuizId(assessmentId);
                long ungraded = quizAttemptAnswerRepository.countPendingDescriptiveByQuizId(assessmentId);
                long graded = Math.max(0, total - (ungraded > 0 ? 1 : 0));
                boolean isLocked = Boolean.TRUE.equals(quiz.getResultsPublished());

                yield new AssessmentPublishStatusResponse(
                        AssessmentType.QUIZ,
                        quiz.getId(),
                        quiz.getTitle(),
                        quiz.getCourse().getId(),
                        quiz.getResultsPublished(),
                        quiz.getResultsWithheld(),
                        quiz.getResultsPublishedAt(),
                        total,
                        graded,
                        ungraded,
                        isLocked,
                        0,
                        "Quiz result publish status retrieved successfully"
                );
            }
        };
    }

    /**
     * Publish results to students, locking evaluated records against future edits (DR-01).
     * Requires explicit confirmation if ungraded items remain.
     */
    @Transactional
    public AssessmentPublishStatusResponse publishResults(User currentUser, AssessmentType type, Integer assessmentId,
                                                         PublishAssessmentResultsRequest request) {
        if (type == null) {
            throw new IllegalArgumentException("Assessment type cannot be null");
        }
        if (assessmentId == null) {
            throw new IllegalArgumentException("Assessment ID cannot be null");
        }
        if (request == null) {
            request = new PublishAssessmentResultsRequest(false, true);
        }

        LocalDateTime now = LocalDateTime.now(clock);

        return switch (type) {
            case EXAM -> {
                Exam exam = examRepository.findById(assessmentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + assessmentId));
                checkInstructorAccess(currentUser, exam.getCourse());

                long total = examAttemptRepository.countByExamId(assessmentId);
                long ungraded = examAttemptRepository.countUngradedByExamId(assessmentId);
                long graded = examAttemptRepository.countGradedByExamId(assessmentId);

                if (ungraded > 0 && !Boolean.TRUE.equals(request.getConfirmUngraded())) {
                    throw new IllegalStateException("Cannot publish results: " + ungraded +
                            " ungraded submissions remain. Provide explicit confirmation (confirmUngraded=true) to publish partial results.");
                }

                exam.setResultsPublished(true);
                exam.setResultsPublishedAt(now);
                exam.setResultsWithheld(false);
                examRepository.save(exam);

                List<ExamAttempt> attempts = examAttemptRepository.findByExamId(assessmentId);
                for (ExamAttempt a : attempts) {
                    a.setIsPublished(true);
                    a.setPublishedAt(now);
                    a.setIsLocked(true);
                }
                examAttemptRepository.saveAll(attempts);

                int notifiedCount = attempts.stream()
                        .map(a -> a.getStudent().getId())
                        .collect(Collectors.toSet()).size();

                if (Boolean.TRUE.equals(request.getNotifyStudents()) && eventPublisher != null) {
                    eventPublisher.publishEvent(new AssessmentResultsPublishedEvent(
                            this, AssessmentType.EXAM, exam.getId(), exam.getTitle(), exam.getCourse().getId(), notifiedCount, now));
                }

                yield new AssessmentPublishStatusResponse(
                        AssessmentType.EXAM,
                        exam.getId(),
                        exam.getTitle(),
                        exam.getCourse().getId(),
                        true,
                        false,
                        now,
                        total,
                        graded,
                        ungraded,
                        true,
                        notifiedCount,
                        "Exam results published and locked successfully"
                );
            }
            case ASSIGNMENT -> {
                Assignment assignment = assignmentRepository.findById(assessmentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assessmentId));
                checkInstructorAccess(currentUser, assignment.getCourse());

                long total = assignmentSubmissionRepository.countByAssignmentId(assessmentId);
                long ungraded = assignmentSubmissionRepository.countUngradedByAssignmentId(assessmentId);
                long graded = assignmentSubmissionRepository.countGradedByAssignmentId(assessmentId);

                if (ungraded > 0 && !Boolean.TRUE.equals(request.getConfirmUngraded())) {
                    throw new IllegalStateException("Cannot publish results: " + ungraded +
                            " ungraded submissions remain. Provide explicit confirmation (confirmUngraded=true) to publish partial results.");
                }

                assignment.setResultsPublished(true);
                assignment.setResultsPublishedAt(now);
                assignment.setResultsWithheld(false);
                assignmentRepository.save(assignment);

                List<AssignmentSubmission> submissions = assignmentSubmissionRepository.findByAssignmentId(assessmentId);
                for (AssignmentSubmission s : submissions) {
                    s.setIsPublished(true);
                    s.setPublishedAt(now);
                    s.setIsLocked(true);
                }
                assignmentSubmissionRepository.saveAll(submissions);

                int notifiedCount = submissions.stream()
                        .map(s -> s.getStudent().getId())
                        .collect(Collectors.toSet()).size();

                if (Boolean.TRUE.equals(request.getNotifyStudents()) && eventPublisher != null) {
                    eventPublisher.publishEvent(new AssessmentResultsPublishedEvent(
                            this, AssessmentType.ASSIGNMENT, assignment.getId(), assignment.getTitle(), assignment.getCourse().getId(), notifiedCount, now));
                }

                yield new AssessmentPublishStatusResponse(
                        AssessmentType.ASSIGNMENT,
                        assignment.getId(),
                        assignment.getTitle(),
                        assignment.getCourse().getId(),
                        true,
                        false,
                        now,
                        total,
                        graded,
                        ungraded,
                        true,
                        notifiedCount,
                        "Assignment results published and locked successfully"
                );
            }
            case QUIZ -> {
                Quiz quiz = quizRepository.findById(assessmentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + assessmentId));
                checkInstructorAccess(currentUser, quiz.getCourse());

                long total = quizAttemptRepository.countByQuizId(assessmentId);
                long ungraded = quizAttemptAnswerRepository.countPendingDescriptiveByQuizId(assessmentId);
                long graded = Math.max(0, total - (ungraded > 0 ? 1 : 0));

                if (ungraded > 0 && !Boolean.TRUE.equals(request.getConfirmUngraded())) {
                    throw new IllegalStateException("Cannot publish results: " + ungraded +
                            " ungraded submissions remain. Provide explicit confirmation (confirmUngraded=true) to publish partial results.");
                }

                quiz.setResultsPublished(true);
                quiz.setResultsPublishedAt(now);
                quiz.setResultsWithheld(false);
                quizRepository.save(quiz);

                List<QuizAttempt> attempts = quizAttemptRepository.findByQuizId(assessmentId);
                for (QuizAttempt a : attempts) {
                    a.setIsPublished(true);
                    a.setPublishedAt(now);
                    a.setIsLocked(true);
                }
                quizAttemptRepository.saveAll(attempts);

                List<QuizAttemptAnswer> unpubAnswers = quizAttemptAnswerRepository.findEvaluatedUnpublishedByQuizId(assessmentId);
                for (QuizAttemptAnswer ans : unpubAnswers) {
                    ans.setEvaluationStatus(EvaluationStatus.PUBLISHED);
                }
                quizAttemptAnswerRepository.saveAll(unpubAnswers);

                int notifiedCount = attempts.stream()
                        .map(a -> a.getStudent().getId())
                        .collect(Collectors.toSet()).size();

                if (Boolean.TRUE.equals(request.getNotifyStudents()) && eventPublisher != null) {
                    eventPublisher.publishEvent(new AssessmentResultsPublishedEvent(
                            this, AssessmentType.QUIZ, quiz.getId(), quiz.getTitle(), quiz.getCourse().getId(), notifiedCount, now));
                }

                yield new AssessmentPublishStatusResponse(
                        AssessmentType.QUIZ,
                        quiz.getId(),
                        quiz.getTitle(),
                        quiz.getCourse().getId(),
                        true,
                        false,
                        now,
                        total,
                        graded,
                        ungraded,
                        true,
                        notifiedCount,
                        "Quiz results published and locked successfully"
                );
            }
        };
    }

    /**
     * Withhold assessment results from students.
     */
    @Transactional
    public AssessmentPublishStatusResponse withholdResults(User currentUser, AssessmentType type, Integer assessmentId,
                                                          WithholdAssessmentResultsRequest request) {
        if (type == null) {
            throw new IllegalArgumentException("Assessment type cannot be null");
        }
        if (assessmentId == null) {
            throw new IllegalArgumentException("Assessment ID cannot be null");
        }
        if (request == null) {
            request = new WithholdAssessmentResultsRequest();
        }

        return switch (type) {
            case EXAM -> {
                Exam exam = examRepository.findById(assessmentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + assessmentId));
                checkInstructorAccess(currentUser, exam.getCourse());

                long total = examAttemptRepository.countByExamId(assessmentId);
                long ungraded = examAttemptRepository.countUngradedByExamId(assessmentId);
                long graded = examAttemptRepository.countGradedByExamId(assessmentId);

                exam.setResultsWithheld(true);
                exam.setResultsPublished(false);
                examRepository.save(exam);

                List<ExamAttempt> attempts = examAttemptRepository.findByExamId(assessmentId);
                for (ExamAttempt a : attempts) {
                    a.setIsPublished(false);
                }
                examAttemptRepository.saveAll(attempts);

                yield new AssessmentPublishStatusResponse(
                        AssessmentType.EXAM,
                        exam.getId(),
                        exam.getTitle(),
                        exam.getCourse().getId(),
                        false,
                        true,
                        exam.getResultsPublishedAt(),
                        total,
                        graded,
                        ungraded,
                        Boolean.TRUE.equals(exam.getResultsPublished()),
                        0,
                        "Exam results withheld successfully"
                );
            }
            case ASSIGNMENT -> {
                Assignment assignment = assignmentRepository.findById(assessmentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assessmentId));
                checkInstructorAccess(currentUser, assignment.getCourse());

                long total = assignmentSubmissionRepository.countByAssignmentId(assessmentId);
                long ungraded = assignmentSubmissionRepository.countUngradedByAssignmentId(assessmentId);
                long graded = assignmentSubmissionRepository.countGradedByAssignmentId(assessmentId);

                assignment.setResultsWithheld(true);
                assignment.setResultsPublished(false);
                assignmentRepository.save(assignment);

                List<AssignmentSubmission> submissions = assignmentSubmissionRepository.findByAssignmentId(assessmentId);
                for (AssignmentSubmission s : submissions) {
                    s.setIsPublished(false);
                }
                assignmentSubmissionRepository.saveAll(submissions);

                yield new AssessmentPublishStatusResponse(
                        AssessmentType.ASSIGNMENT,
                        assignment.getId(),
                        assignment.getTitle(),
                        assignment.getCourse().getId(),
                        false,
                        true,
                        assignment.getResultsPublishedAt(),
                        total,
                        graded,
                        ungraded,
                        Boolean.TRUE.equals(assignment.getResultsPublished()),
                        0,
                        "Assignment results withheld successfully"
                );
            }
            case QUIZ -> {
                Quiz quiz = quizRepository.findById(assessmentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + assessmentId));
                checkInstructorAccess(currentUser, quiz.getCourse());

                long total = quizAttemptRepository.countByQuizId(assessmentId);
                long ungraded = quizAttemptAnswerRepository.countPendingDescriptiveByQuizId(assessmentId);
                long graded = Math.max(0, total - (ungraded > 0 ? 1 : 0));

                quiz.setResultsWithheld(true);
                quiz.setResultsPublished(false);
                quizRepository.save(quiz);

                List<QuizAttempt> attempts = quizAttemptRepository.findByQuizId(assessmentId);
                for (QuizAttempt a : attempts) {
                    a.setIsPublished(false);
                }
                quizAttemptRepository.saveAll(attempts);

                yield new AssessmentPublishStatusResponse(
                        AssessmentType.QUIZ,
                        quiz.getId(),
                        quiz.getTitle(),
                        quiz.getCourse().getId(),
                        false,
                        true,
                        quiz.getResultsPublishedAt(),
                        total,
                        graded,
                        ungraded,
                        Boolean.TRUE.equals(quiz.getResultsPublished()),
                        0,
                        "Quiz results withheld successfully"
                );
            }
        };
    }

    /**
     * Enforce result lock check: throws IllegalStateException (409) if the assessment attempt/submission is locked (DR-01).
     */
    @Transactional(readOnly = true)
    public void enforceResultLock(AssessmentType type, Integer attemptOrSubmissionId) {
        if (type == null || attemptOrSubmissionId == null) {
            return;
        }
        switch (type) {
            case EXAM -> {
                ExamAttempt attempt = examAttemptRepository.findById(attemptOrSubmissionId).orElse(null);
                if (attempt != null && (Boolean.TRUE.equals(attempt.getIsLocked()) || Boolean.TRUE.equals(attempt.getExam().getResultsPublished()))) {
                    throw new IllegalStateException("Cannot modify graded results: exam results have been published and locked (DR-01)");
                }
            }
            case ASSIGNMENT -> {
                AssignmentSubmission sub = assignmentSubmissionRepository.findById(attemptOrSubmissionId).orElse(null);
                if (sub != null && (Boolean.TRUE.equals(sub.getIsLocked()) || Boolean.TRUE.equals(sub.getAssignment().getResultsPublished()))) {
                    throw new IllegalStateException("Cannot modify graded results: assignment results have been published and locked (DR-01)");
                }
            }
            case QUIZ -> {
                QuizAttempt attempt = quizAttemptRepository.findById(attemptOrSubmissionId).orElse(null);
                if (attempt != null && (Boolean.TRUE.equals(attempt.getIsLocked()) || Boolean.TRUE.equals(attempt.getQuiz().getResultsPublished()))) {
                    throw new IllegalStateException("Cannot modify graded results: quiz results have been published and locked (DR-01)");
                }
            }
        }
    }
}
