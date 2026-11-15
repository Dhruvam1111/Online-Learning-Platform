package com.olp.service;

import com.olp.dto.ExamAttemptMonitoringSummaryResponse;
import com.olp.dto.ExamMonitoringTimelineResponse;
import com.olp.dto.MonitoringEventResponse;
import com.olp.dto.RecordMonitoringEventRequest;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for reviewing exam monitoring logs, event timelines, and proctoring activity (FR-I29, Step 55).
 */
@Service
public class ExamMonitoringLogService {

    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamMonitoringEventRepository monitoringEventRepository;
    private final ExamMonitoringConfigRepository monitoringConfigRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;

    @Autowired
    public ExamMonitoringLogService(ExamRepository examRepository,
                                   ExamAttemptRepository examAttemptRepository,
                                   ExamMonitoringEventRepository monitoringEventRepository,
                                   ExamMonitoringConfigRepository monitoringConfigRepository,
                                   CourseInstructorRepository courseInstructorRepository,
                                   UserRepository userRepository) {
        this.examRepository = examRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.monitoringEventRepository = monitoringEventRepository;
        this.monitoringConfigRepository = monitoringConfigRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.userRepository = userRepository;
    }

    /**
     * Resolves authenticated User from Security Authentication object.
     */
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
        if (authentication.getName() != null) {
            return userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new InvalidCredentialsException("User not found: " + authentication.getName()));
        }
        throw new InvalidCredentialsException("Unable to resolve authenticated user");
    }

    /**
     * Validates whether a user is an instructor of the given course or an admin (NFR-18, DR-05).
     */
    public boolean isInstructorOfCourse(User user, Course course) {
        if (user == null || course == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        if (course.getInstructor() != null && Objects.equals(course.getInstructor().getId(), user.getId())) {
            return true;
        }
        return courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), user.getId());
    }

    /**
     * List exam attempt monitoring summaries for an exam, optionally filtered by flagged activity (FR-I29, Step 55).
     */
    @Transactional(readOnly = true)
    public List<ExamAttemptMonitoringSummaryResponse> getExamAttemptMonitoringList(User currentUser,
                                                                                  Integer examId,
                                                                                  Boolean flaggedOnly) {
        if (examId == null) {
            throw new IllegalArgumentException("Exam ID cannot be null");
        }
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));

        if (!isInstructorOfCourse(currentUser, exam.getCourse())) {
            throw new AccessDeniedException("User is not authorized to review monitoring logs for course: " + exam.getCourse().getId());
        }

        List<ExamAttempt> attempts = examAttemptRepository.findByExamId(examId);
        List<ExamAttemptMonitoringSummaryResponse> summaries = new ArrayList<>();

        for (ExamAttempt attempt : attempts) {
            List<ExamMonitoringEvent> events = monitoringEventRepository.findByAttemptIdOrderByEventTimestampAsc(attempt.getId());
            boolean isFlagged = calculateIsFlagged(attempt, events);

            if (Boolean.TRUE.equals(flaggedOnly) && !isFlagged) {
                continue;
            }

            String highestSeverity = calculateHighestSeverity(events);

            summaries.add(new ExamAttemptMonitoringSummaryResponse(
                    attempt.getId(),
                    exam.getId(),
                    exam.getTitle(),
                    attempt.getStudent() != null ? attempt.getStudent().getId() : null,
                    attempt.getStudent() != null ? attempt.getStudent().getName() : null,
                    attempt.getStudent() != null ? attempt.getStudent().getEmail() : null,
                    attempt.getAttemptNumber(),
                    attempt.getStatus(),
                    attempt.getReviewStatus() != null ? attempt.getReviewStatus().name() : "NONE",
                    isFlagged,
                    attempt.getTabSwitchCount() != null ? attempt.getTabSwitchCount() : 0,
                    attempt.getFullscreenExitCount() != null ? attempt.getFullscreenExitCount() : 0,
                    (long) events.size(),
                    highestSeverity,
                    attempt.getStartedAt(),
                    attempt.getSubmittedAt()
            ));
        }

        return summaries;
    }

    /**
     * Retrieves the chronological event timeline and severity counters for an exam attempt (FR-I29, Step 55).
     */
    @Transactional(readOnly = true)
    public ExamMonitoringTimelineResponse getAttemptTimeline(User currentUser, Integer attemptId) {
        if (attemptId == null) {
            throw new IllegalArgumentException("Attempt ID cannot be null");
        }
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam attempt not found: " + attemptId));

        if (!isInstructorOfCourse(currentUser, attempt.getExam().getCourse())) {
            throw new AccessDeniedException("User is not authorized to review monitoring timeline for this attempt");
        }

        List<ExamMonitoringEvent> events = monitoringEventRepository.findByAttemptIdOrderByEventTimestampAsc(attemptId);
        boolean isFlagged = calculateIsFlagged(attempt, events);

        long lowCount = events.stream().filter(e -> e.getSeverity() == MonitoringSeverity.LOW).count();
        long medCount = events.stream().filter(e -> e.getSeverity() == MonitoringSeverity.MEDIUM).count();
        long highCount = events.stream().filter(e -> e.getSeverity() == MonitoringSeverity.HIGH).count();
        long critCount = events.stream().filter(e -> e.getSeverity() == MonitoringSeverity.CRITICAL).count();

        List<MonitoringEventResponse> eventResponses = events.stream()
                .map(MonitoringEventResponse::new)
                .collect(Collectors.toList());

        return new ExamMonitoringTimelineResponse(
                attempt.getId(),
                attempt.getExam().getId(),
                attempt.getExam().getTitle(),
                attempt.getStudent() != null ? attempt.getStudent().getId() : null,
                attempt.getStudent() != null ? attempt.getStudent().getName() : null,
                attempt.getStudent() != null ? attempt.getStudent().getEmail() : null,
                attempt.getAttemptNumber(),
                attempt.getStatus(),
                attempt.getReviewStatus() != null ? attempt.getReviewStatus().name() : "NONE",
                isFlagged,
                attempt.getTabSwitchCount() != null ? attempt.getTabSwitchCount() : 0,
                attempt.getFullscreenExitCount() != null ? attempt.getFullscreenExitCount() : 0,
                (long) events.size(),
                lowCount,
                medCount,
                highCount,
                critCount,
                eventResponses
        );
    }

    /**
     * Records a proctoring event, increments violation counters, and flags attempt if thresholds breached (FR-I29, Step 55).
     */
    @Transactional
    public MonitoringEventResponse recordMonitoringEvent(User currentUser,
                                                         Integer attemptId,
                                                         RecordMonitoringEventRequest request) {
        if (attemptId == null) {
            throw new IllegalArgumentException("Attempt ID cannot be null");
        }
        if (request == null || request.getEventType() == null) {
            throw new IllegalArgumentException("Valid monitoring event request with eventType is required");
        }

        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam attempt not found: " + attemptId));

        boolean isAdmin = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());
        boolean isOwner = currentUser != null && attempt.getStudent() != null && Objects.equals(attempt.getStudent().getId(), currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Cannot record monitoring event for another student's attempt");
        }

        MonitoringSeverity effectiveSeverity = request.getSeverity() != null
                ? request.getSeverity()
                : defaultSeverityFor(request.getEventType());

        if (request.getEventType() == MonitoringEventType.TAB_SWITCH) {
            attempt.setTabSwitchCount((attempt.getTabSwitchCount() != null ? attempt.getTabSwitchCount() : 0) + 1);
        } else if (request.getEventType() == MonitoringEventType.FULLSCREEN_EXIT) {
            attempt.setFullscreenExitCount((attempt.getFullscreenExitCount() != null ? attempt.getFullscreenExitCount() : 0) + 1);
        }

        boolean breached = false;
        Optional<ExamMonitoringConfig> configOpt = monitoringConfigRepository.findByExamId(attempt.getExam().getId());
        if (configOpt.isPresent()) {
            ExamMonitoringConfig config = configOpt.get();
            if (Boolean.TRUE.equals(config.getTabSwitchDetection())
                    && config.getTabSwitchLimit() != null
                    && attempt.getTabSwitchCount() >= config.getTabSwitchLimit()) {
                breached = true;
            }
            if (Boolean.TRUE.equals(config.getRequireFullscreen())
                    && config.getFullscreenExitLimit() != null
                    && attempt.getFullscreenExitCount() >= config.getFullscreenExitLimit()) {
                breached = true;
            }
        }

        if (breached || effectiveSeverity == MonitoringSeverity.HIGH || effectiveSeverity == MonitoringSeverity.CRITICAL) {
            if (attempt.getReviewStatus() == null || attempt.getReviewStatus() == ExamAttemptReviewStatus.NONE) {
                attempt.setReviewStatus(ExamAttemptReviewStatus.PENDING);
            }
            String breachNotice = request.getEventType().name() + " (" + effectiveSeverity.name() + ")";
            String existingFlags = attempt.getMonitoringFlags();
            if (existingFlags == null || existingFlags.isBlank()) {
                attempt.setMonitoringFlags(breachNotice);
            } else if (!existingFlags.contains(breachNotice)) {
                attempt.setMonitoringFlags(existingFlags + "; " + breachNotice);
            }
        }

        examAttemptRepository.save(attempt);

        ExamMonitoringEvent event = new ExamMonitoringEvent(
                attempt,
                request.getEventType(),
                effectiveSeverity,
                request.getEventTimestamp() != null ? request.getEventTimestamp() : LocalDateTime.now(),
                request.getDurationSeconds(),
                request.getMetadataJson(),
                request.getNotes()
        );

        ExamMonitoringEvent saved = monitoringEventRepository.save(event);
        return new MonitoringEventResponse(saved);
    }

    public boolean calculateIsFlagged(ExamAttempt attempt, List<ExamMonitoringEvent> events) {
        if (attempt == null) {
            return false;
        }
        if (attempt.getReviewStatus() == ExamAttemptReviewStatus.CLEARED) {
            return false;
        }
        if (attempt.getReviewStatus() == ExamAttemptReviewStatus.PENDING
                || attempt.getReviewStatus() == ExamAttemptReviewStatus.ESCALATED) {
            return true;
        }
        if (attempt.getTabSwitchCount() != null && attempt.getTabSwitchCount() > 0) {
            return true;
        }
        if (attempt.getFullscreenExitCount() != null && attempt.getFullscreenExitCount() > 0) {
            return true;
        }
        if (attempt.getMonitoringFlags() != null && !attempt.getMonitoringFlags().isBlank()) {
            return true;
        }
        if (events != null) {
            return events.stream().anyMatch(e -> e.getSeverity() == MonitoringSeverity.HIGH || e.getSeverity() == MonitoringSeverity.CRITICAL);
        }
        return false;
    }

    public String calculateHighestSeverity(List<ExamMonitoringEvent> events) {
        if (events == null || events.isEmpty()) {
            return null;
        }
        if (events.stream().anyMatch(e -> e.getSeverity() == MonitoringSeverity.CRITICAL)) {
            return "CRITICAL";
        }
        if (events.stream().anyMatch(e -> e.getSeverity() == MonitoringSeverity.HIGH)) {
            return "HIGH";
        }
        if (events.stream().anyMatch(e -> e.getSeverity() == MonitoringSeverity.MEDIUM)) {
            return "MEDIUM";
        }
        if (events.stream().anyMatch(e -> e.getSeverity() == MonitoringSeverity.LOW)) {
            return "LOW";
        }
        return null;
    }

    public MonitoringSeverity defaultSeverityFor(MonitoringEventType eventType) {
        if (eventType == null) {
            return MonitoringSeverity.LOW;
        }
        return switch (eventType) {
            case SCREEN_SHARE_STOPPED, DEVICE_DISCONNECTED -> MonitoringSeverity.HIGH;
            case TAB_SWITCH, FULLSCREEN_EXIT, BROWSER_WARNING -> MonitoringSeverity.MEDIUM;
            default -> MonitoringSeverity.LOW;
        };
    }
}
