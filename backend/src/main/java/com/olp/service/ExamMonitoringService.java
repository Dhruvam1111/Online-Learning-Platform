package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service managing examination monitoring rules, proctoring configuration, student consent validation (DR-06),
 * and unsupported browser fallback handling (NFR-25) (FR-I26, Step 40).
 */
@Service
public class ExamMonitoringService {

    private final ExamRepository examRepository;
    private final ExamMonitoringConfigRepository monitoringConfigRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final EnrollmentRepository enrollmentRepository;

    private Clock clock = Clock.systemUTC();

    public ExamMonitoringService(ExamRepository examRepository,
                                 ExamMonitoringConfigRepository monitoringConfigRepository,
                                 ExamAttemptRepository examAttemptRepository,
                                 CourseRepository courseRepository,
                                 CourseInstructorRepository courseInstructorRepository,
                                 EnrollmentRepository enrollmentRepository) {
        this.examRepository = examRepository;
        this.monitoringConfigRepository = monitoringConfigRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public void setClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Transactional
    public ExamMonitoringConfigResponse saveMonitoringConfig(User currentUser, Integer examId, ExamMonitoringConfigRequest request) {
        validateInstructorOrAdmin(currentUser);
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
        verifyCourseAssociation(exam.getCourse().getId(), currentUser);

        if (request == null) {
            throw new IllegalArgumentException("Monitoring configuration cannot be null");
        }

        // Validate thresholds
        if (request.getTabSwitchLimit() != null && (request.getTabSwitchLimit() < 0 || request.getTabSwitchLimit() > 50)) {
            throw new IllegalArgumentException("Tab switch limit must be between 0 and 50");
        }
        if (request.getFullscreenExitLimit() != null && (request.getFullscreenExitLimit() < 0 || request.getFullscreenExitLimit() > 50)) {
            throw new IllegalArgumentException("Fullscreen exit limit must be between 0 and 50");
        }

        String policy = request.getUnsupportedBrowserPolicy() != null ? request.getUnsupportedBrowserPolicy().toUpperCase() : "WARN";
        if (!"WARN".equals(policy) && !"BLOCK".equals(policy)) {
            throw new IllegalArgumentException("Unsupported browser policy must be 'WARN' or 'BLOCK'");
        }

        ExamMonitoringConfig config = monitoringConfigRepository.findByExamId(examId)
                .orElseGet(() -> new ExamMonitoringConfig(
                        exam,
                        request.getTabSwitchDetection(),
                        request.getTabSwitchLimit(),
                        request.getRequireFullscreen(),
                        request.getFullscreenExitLimit(),
                        request.getRequireScreenShare(),
                        request.getRequireWebcam(),
                        request.getRequireMicrophone(),
                        request.getRequireStudentConsent(),
                        policy,
                        request.getConsentText()
                ));

        if (config.getId() != null) {
            if (request.getTabSwitchDetection() != null) config.setTabSwitchDetection(request.getTabSwitchDetection());
            if (request.getTabSwitchLimit() != null) config.setTabSwitchLimit(request.getTabSwitchLimit());
            if (request.getRequireFullscreen() != null) config.setRequireFullscreen(request.getRequireFullscreen());
            if (request.getFullscreenExitLimit() != null) config.setFullscreenExitLimit(request.getFullscreenExitLimit());
            if (request.getRequireScreenShare() != null) config.setRequireScreenShare(request.getRequireScreenShare());
            if (request.getRequireWebcam() != null) config.setRequireWebcam(request.getRequireWebcam());
            if (request.getRequireMicrophone() != null) config.setRequireMicrophone(request.getRequireMicrophone());
            if (request.getRequireStudentConsent() != null) config.setRequireStudentConsent(request.getRequireStudentConsent());
            config.setUnsupportedBrowserPolicy(policy);
            if (request.getConsentText() != null) config.setConsentText(request.getConsentText());
        }

        ExamMonitoringConfig saved = monitoringConfigRepository.save(config);
        return mapToResponse(saved, exam, null);
    }

    public ExamMonitoringConfigResponse getMonitoringConfig(User currentUser, Integer examId, Boolean browserSupportsFullscreen) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));

        if (currentUser != null && !"admin".equalsIgnoreCase(currentUser.getRole())) {
            boolean isInstructor = "instructor".equalsIgnoreCase(currentUser.getRole());
            if (isInstructor) {
                verifyCourseAssociation(exam.getCourse().getId(), currentUser);
            } else {
                // Students can only read rules for exams of courses they are enrolled in
                if (Boolean.TRUE.equals(exam.getRequireEnrollment())) {
                    boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(currentUser.getId(), exam.getCourse().getId());
                    if (!enrolled) {
                        throw new AccessDeniedException("Student is not enrolled in course: " + exam.getCourse().getId());
                    }
                }
            }
        }

        ExamMonitoringConfig config = monitoringConfigRepository.findByExamId(examId)
                .orElseGet(() -> new ExamMonitoringConfig(exam, true, 3, true, 2, false, false, false, true, "WARN", null));

        return mapToResponse(config, exam, browserSupportsFullscreen);
    }

    @Transactional
    public ExamConsentResponse recordStudentConsent(User currentUser, Integer examId, ExamConsentRequest request) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));

        if (Boolean.TRUE.equals(exam.getRequireEnrollment())) {
            boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(currentUser.getId(), exam.getCourse().getId());
            if (!enrolled) {
                throw new AccessDeniedException("Student is not enrolled in course: " + exam.getCourse().getId());
            }
        }

        ExamMonitoringConfig config = monitoringConfigRepository.findByExamId(examId)
                .orElseGet(() -> new ExamMonitoringConfig(exam, true, 3, true, 2, false, false, false, true, "WARN", null));

        LocalDateTime serverNow = LocalDateTime.now(clock);

        if (request == null || !Boolean.TRUE.equals(request.getConsentGiven())) {
            return new ExamConsentResponse(
                    examId, currentUser.getId(), false, null, true,
                    "Consent was declined. You cannot take this proctored examination without consenting to monitoring.",
                    null
            );
        }

        boolean lacksFullscreen = Boolean.FALSE.equals(request.getBrowserSupportsFullscreen());
        String warning = null;
        boolean blocked = false;

        if (lacksFullscreen && Boolean.TRUE.equals(config.getRequireFullscreen())) {
            if ("BLOCK".equalsIgnoreCase(config.getUnsupportedBrowserPolicy())) {
                blocked = true;
                return new ExamConsentResponse(
                        examId, currentUser.getId(), true, serverNow, true,
                        "Blocked: Your browser does not support Fullscreen API required for this exam.",
                        "Browser Fullscreen API is unsupported and policy is set to BLOCK."
                );
            } else {
                warning = "Warning: Fullscreen API is not supported by your browser. Academic integrity warnings may occur.";
            }
        }

        // If an active in-progress attempt exists, record consent on it
        List<ExamAttempt> active = examAttemptRepository.findActiveAttempts(examId, currentUser.getId());
        if (!active.isEmpty()) {
            ExamAttempt attempt = active.get(0);
            attempt.setConsentGiven(true);
            attempt.setConsentGivenAt(serverNow);
            examAttemptRepository.save(attempt);
        }

        return new ExamConsentResponse(
                examId, currentUser.getId(), true, serverNow, blocked,
                "Consent recorded successfully.", warning
        );
    }

    private ExamMonitoringConfigResponse mapToResponse(ExamMonitoringConfig config, Exam exam, Boolean browserSupportsFullscreen) {
        ExamMonitoringConfigResponse res = new ExamMonitoringConfigResponse();
        res.setId(config.getId());
        res.setExamId(exam.getId());
        res.setExamTitle(exam.getTitle());
        res.setTabSwitchDetection(config.getTabSwitchDetection());
        res.setTabSwitchLimit(config.getTabSwitchLimit());
        res.setRequireFullscreen(config.getRequireFullscreen());
        res.setFullscreenExitLimit(config.getFullscreenExitLimit());
        res.setRequireScreenShare(config.getRequireScreenShare());
        res.setRequireWebcam(config.getRequireWebcam());
        res.setRequireMicrophone(config.getRequireMicrophone());
        res.setRequireStudentConsent(config.getRequireStudentConsent());
        res.setUnsupportedBrowserPolicy(config.getUnsupportedBrowserPolicy());
        res.setConsentText(config.getConsentText() != null ? config.getConsentText() : defaultConsentText(exam));

        // NFR-25 browser fallback warning
        if (Boolean.FALSE.equals(browserSupportsFullscreen) && Boolean.TRUE.equals(config.getRequireFullscreen())) {
            if ("BLOCK".equalsIgnoreCase(config.getUnsupportedBrowserPolicy())) {
                res.setFullscreenSupportWarning("BLOCKED: Your browser does not support the Fullscreen API. You must use a supported browser.");
            } else {
                res.setFullscreenSupportWarning("WARNING: Your browser lacks Fullscreen API support. Examination integrity warnings may be triggered.");
            }
        }

        return res;
    }

    private String defaultConsentText(Exam exam) {
        return "I hereby consent to examination integrity monitoring for '" + exam.getTitle() +
                "', including tab-switch tracking, fullscreen enforcement, and proctoring audits.";
    }

    public void verifyCourseAssociation(Integer courseId, User user) {
        if ("admin".equalsIgnoreCase(user.getRole())) {
            return;
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
        boolean isOwner = course.getInstructor() != null && course.getInstructor().getId().equals(user.getId());
        boolean isCollaborator = courseInstructorRepository != null
                && courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, user.getId());
        if (!isOwner && !isCollaborator) {
            throw new AccessDeniedException("You are not authorized to manage content for course: " + courseId);
        }
    }

    private void validateInstructorOrAdmin(User user) {
        if (user == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        String role = user.getRole();
        if (!"instructor".equalsIgnoreCase(role) && !"admin".equalsIgnoreCase(role)) {
            throw new AccessDeniedException("Only instructors and administrators can perform this action");
        }
    }
}
