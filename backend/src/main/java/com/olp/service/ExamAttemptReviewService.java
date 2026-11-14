package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.event.ExamAttemptEscalatedEvent;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for reviewing, flagging, clearing, and escalating exam attempts with append-only audit history (FR-I69, Step 56).
 * Enforces DR-07: flagging/escalating records an administrative recommendation and never modifies student marks.
 */
@Service
public class ExamAttemptReviewService {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAttemptReviewHistoryRepository reviewHistoryRepository;
    private final ExamRepository examRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private Clock clock = Clock.systemDefaultZone();

    @Autowired
    public ExamAttemptReviewService(ExamAttemptRepository examAttemptRepository,
                                   ExamAttemptReviewHistoryRepository reviewHistoryRepository,
                                   ExamRepository examRepository,
                                   CourseRepository courseRepository,
                                   CourseInstructorRepository courseInstructorRepository,
                                   UserRepository userRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.examAttemptRepository = examAttemptRepository;
        this.reviewHistoryRepository = reviewHistoryRepository;
        this.examRepository = examRepository;
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    public void setClock(Clock clock) {
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
                    .orElseThrow(() -> new InvalidCredentialsException("User not found: " + email));
        }
        throw new InvalidCredentialsException("Invalid authentication principal");
    }

    public void checkCourseAccess(User currentUser, Course course) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if ("ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            return;
        }
        boolean isCreator = course.getInstructor() != null && course.getInstructor().getId().equals(currentUser.getId());
        boolean isAssociated = courseInstructorRepository != null
                && courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), currentUser.getId());
        if (!isCreator && !isAssociated) {
            throw new AccessDeniedException("User is not authorized to review attempts for course: " + course.getId());
        }
    }

    /**
     * Retrieves flagged and reviewed attempts for an instructor's course or exam.
     */
    @Transactional(readOnly = true)
    public List<FlaggedAttemptSummaryResponse> getFlaggedAttempts(User currentUser, Integer courseId, Integer examId,
                                                                   ExamAttemptReviewStatus status) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }

        List<ExamAttempt> attempts;
        if (examId != null) {
            Exam exam = examRepository.findById(examId)
                    .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
            checkCourseAccess(currentUser, exam.getCourse());
            attempts = examAttemptRepository.findFlaggedByExamId(examId, status);
        } else if (courseId != null) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
            checkCourseAccess(currentUser, course);
            attempts = examAttemptRepository.findFlaggedByCourseIds(List.of(courseId), status);
        } else {
            // All courses instructor is associated with
            List<Integer> courseIds = getInstructorCourseIds(currentUser);
            if (courseIds.isEmpty()) {
                return Collections.emptyList();
            }
            attempts = examAttemptRepository.findFlaggedByCourseIds(courseIds, status);
        }

        return attempts.stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves detailed review status, flags, marks, and audit history for an attempt.
     */
    @Transactional(readOnly = true)
    public ExamAttemptReviewResponse getAttemptReviewDetails(User currentUser, Integer attemptId) {
        if (attemptId == null) {
            throw new IllegalArgumentException("Attempt ID cannot be null");
        }
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam attempt not found: " + attemptId));
        checkCourseAccess(currentUser, attempt.getExam().getCourse());

        List<ExamAttemptReviewHistory> history = reviewHistoryRepository.findByAttemptIdOrderByCreatedAtDesc(attemptId);
        List<ExamAttemptReviewHistoryResponse> historyDtos = history.stream()
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());

        return mapToReviewResponse(attempt, historyDtos, "Attempt review details retrieved successfully");
    }

    /**
     * Performs a review action on an exam attempt (FLAG, CLEAR, ESCALATE, UPDATE_NOTES).
     * Enforces DR-07: Never alters or penalises student marks.
     */
    @Transactional
    public ExamAttemptReviewResponse reviewAttempt(User currentUser, Integer attemptId, ExamAttemptReviewRequest request) {
        if (attemptId == null) {
            throw new IllegalArgumentException("Attempt ID cannot be null");
        }
        if (request == null || request.getAction() == null || request.getAction().isBlank()) {
            throw new IllegalArgumentException("Review action is required (FLAG, CLEAR, ESCALATE, UPDATE_NOTES)");
        }

        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam attempt not found: " + attemptId));
        checkCourseAccess(currentUser, attempt.getExam().getCourse());

        String previousStatus = attempt.getReviewStatus() != null ? attempt.getReviewStatus().name() : ExamAttemptReviewStatus.NONE.name();
        String actionUpper = request.getAction().trim().toUpperCase(Locale.ROOT);
        ExamAttemptReviewStatus newStatus;

        LocalDateTime now = LocalDateTime.now(clock);

        switch (actionUpper) {
            case "FLAG" -> newStatus = ExamAttemptReviewStatus.PENDING;
            case "CLEAR" -> newStatus = ExamAttemptReviewStatus.CLEARED;
            case "ESCALATE" -> newStatus = ExamAttemptReviewStatus.ESCALATED;
            case "UPDATE_NOTES" -> newStatus = attempt.getReviewStatus() != null ? attempt.getReviewStatus() : ExamAttemptReviewStatus.PENDING;
            default -> throw new IllegalArgumentException("Invalid review action: " + request.getAction() +
                    ". Must be FLAG, CLEAR, ESCALATE, or UPDATE_NOTES.");
        }

        // Apply review update to attempt
        attempt.setReviewStatus(newStatus);
        if (request.getNotes() != null) {
            attempt.setReviewNotes(request.getNotes().trim());
        }
        attempt.setReviewedBy(currentUser);
        attempt.setReviewedAt(now);

        // DR-07: Escalation is recommendation-only; student marks are strictly preserved
        if ("ESCALATE".equals(actionUpper)) {
            attempt.setEscalatedAt(now);
            String reason = request.getReason() != null && !request.getReason().isBlank()
                    ? request.getReason().trim()
                    : (request.getNotes() != null ? request.getNotes().trim() : "Flagged integrity review escalation");
            String recommendation = request.getRecommendation() != null && !request.getRecommendation().isBlank()
                    ? request.getRecommendation().trim()
                    : "MANUAL_AUDIT";

            attempt.setEscalationReason(reason);
            attempt.setEscalationRecommendation(recommendation);

            if (eventPublisher != null) {
                eventPublisher.publishEvent(new ExamAttemptEscalatedEvent(
                        this,
                        attempt.getId(),
                        attempt.getExam().getId(),
                        attempt.getExam().getTitle(),
                        attempt.getExam().getCourse().getId(),
                        attempt.getStudent().getId(),
                        reason,
                        recommendation,
                        currentUser.getId(),
                        now
                ));
            }
        }

        ExamAttempt savedAttempt = examAttemptRepository.save(attempt);

        // Append to immutable review history
        ExamAttemptReviewHistory historyEntry = new ExamAttemptReviewHistory(
                savedAttempt,
                previousStatus,
                newStatus.name(),
                actionUpper,
                request.getNotes(),
                request.getRecommendation(),
                currentUser,
                now
        );
        reviewHistoryRepository.save(historyEntry);

        List<ExamAttemptReviewHistory> allHistory = reviewHistoryRepository.findByAttemptIdOrderByCreatedAtDesc(attemptId);
        List<ExamAttemptReviewHistoryResponse> historyDtos = allHistory.stream()
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());

        String message = switch (actionUpper) {
            case "FLAG" -> "Attempt flagged for proctoring review";
            case "CLEAR" -> "Attempt cleared from proctoring flags";
            case "ESCALATE" -> "Attempt escalated to administration with recommendation (DR-07 marks preserved)";
            case "UPDATE_NOTES" -> "Review notes updated successfully";
            default -> "Review action recorded successfully";
        };

        return mapToReviewResponse(savedAttempt, historyDtos, message);
    }

    /**
     * Retrieves append-only history of review decisions for an attempt.
     */
    @Transactional(readOnly = true)
    public List<ExamAttemptReviewHistoryResponse> getReviewHistory(User currentUser, Integer attemptId) {
        if (attemptId == null) {
            throw new IllegalArgumentException("Attempt ID cannot be null");
        }
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam attempt not found: " + attemptId));
        checkCourseAccess(currentUser, attempt.getExam().getCourse());

        return reviewHistoryRepository.findByAttemptIdOrderByCreatedAtDesc(attemptId).stream()
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());
    }

    private List<Integer> getInstructorCourseIds(User instructor) {
        if ("ADMIN".equalsIgnoreCase(instructor.getRole())) {
            return courseRepository.findAll().stream().map(Course::getId).collect(Collectors.toList());
        }
        Set<Integer> courseIds = new HashSet<>();
        courseIds.addAll(courseRepository.findByInstructorId(instructor.getId()).stream().map(Course::getId).toList());
        if (courseInstructorRepository != null) {
            courseIds.addAll(courseInstructorRepository.findByInstructorId(instructor.getId()).stream()
                    .map(ci -> ci.getCourse().getId()).toList());
        }
        return new ArrayList<>(courseIds);
    }

    private FlaggedAttemptSummaryResponse mapToSummary(ExamAttempt attempt) {
        return new FlaggedAttemptSummaryResponse(
                attempt.getId(),
                attempt.getExam().getId(),
                attempt.getExam().getTitle(),
                attempt.getExam().getCourse().getId(),
                attempt.getStudent().getId(),
                attempt.getStudent().getName(),
                attempt.getAttemptNumber(),
                attempt.getTabSwitchCount(),
                attempt.getFullscreenExitCount(),
                attempt.getMonitoringFlags(),
                attempt.getReviewStatus(),
                attempt.getReviewNotes(),
                attempt.getReviewedBy() != null ? attempt.getReviewedBy().getId() : null,
                attempt.getReviewedBy() != null ? attempt.getReviewedBy().getName() : null,
                attempt.getReviewedAt(),
                attempt.getEscalatedAt(),
                attempt.getEscalationRecommendation()
        );
    }

    private ExamAttemptReviewResponse mapToReviewResponse(ExamAttempt attempt,
                                                          List<ExamAttemptReviewHistoryResponse> history,
                                                          String message) {
        ExamAttemptReviewResponse res = new ExamAttemptReviewResponse();
        res.setAttemptId(attempt.getId());
        res.setExamId(attempt.getExam().getId());
        res.setExamTitle(attempt.getExam().getTitle());
        res.setCourseId(attempt.getExam().getCourse().getId());
        res.setStudentId(attempt.getStudent().getId());
        res.setStudentName(attempt.getStudent().getName());
        res.setAttemptNumber(attempt.getAttemptNumber());
        res.setReviewStatus(attempt.getReviewStatus());
        res.setReviewNotes(attempt.getReviewNotes());
        res.setReviewedById(attempt.getReviewedBy() != null ? attempt.getReviewedBy().getId() : null);
        res.setReviewedByName(attempt.getReviewedBy() != null ? attempt.getReviewedBy().getName() : null);
        res.setReviewedAt(attempt.getReviewedAt());
        res.setEscalatedAt(attempt.getEscalatedAt());
        res.setEscalationReason(attempt.getEscalationReason());
        res.setEscalationRecommendation(attempt.getEscalationRecommendation());
        res.setTabSwitchCount(attempt.getTabSwitchCount());
        res.setFullscreenExitCount(attempt.getFullscreenExitCount());
        res.setMonitoringFlags(attempt.getMonitoringFlags());
        res.setObtainedMarks(attempt.getObtainedMarks());
        res.setTotalMarks(attempt.getTotalMarks());
        res.setMessage(message);
        res.setHistory(history);
        return res;
    }

    private ExamAttemptReviewHistoryResponse mapToHistoryDto(ExamAttemptReviewHistory h) {
        return new ExamAttemptReviewHistoryResponse(
                h.getId(),
                h.getAttempt().getId(),
                h.getPreviousStatus(),
                h.getNewStatus(),
                h.getAction(),
                h.getNotes(),
                h.getRecommendation(),
                h.getReviewedBy() != null ? h.getReviewedBy().getId() : null,
                h.getReviewedBy() != null ? h.getReviewedBy().getName() : null,
                h.getCreatedAt()
        );
    }
}
