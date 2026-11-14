package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.event.AssignmentDueEvent;
import com.olp.exception.AssignmentLateException;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Service managing assignment creation, deadlines, late policy enforcement,
 * student submissions, and grading queue integration (FR-I68, Step 28).
 */
@Service
public class AssignmentService {

    private static final Set<String> ALLOWED_SUBMISSION_TYPES = Set.of("FILE", "TEXT", "BOTH");
    private static final Set<String> ALLOWED_LATE_POLICIES = Set.of("ALLOW", "REJECT", "PENALTY");
    private static final Set<String> ALLOWED_STATUSES = Set.of("DRAFT", "PUBLISHED", "ARCHIVED");

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final LessonRepository lessonRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    private Clock clock = Clock.systemUTC();

    public AssignmentService(AssignmentRepository assignmentRepository,
                             AssignmentSubmissionRepository assignmentSubmissionRepository,
                             CourseRepository courseRepository,
                             CourseModuleRepository courseModuleRepository,
                             LessonRepository lessonRepository,
                             CourseInstructorRepository courseInstructorRepository,
                             EnrollmentRepository enrollmentRepository,
                             UserRepository userRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.assignmentRepository = assignmentRepository;
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
        this.courseRepository = courseRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.lessonRepository = lessonRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    public void setClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public User resolveUser(org.springframework.security.core.Authentication authentication) {
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
    public AssignmentResponse createAssignment(User currentUser, AssignmentCreateRequest request) {
        validateInstructorOrAdmin(currentUser);
        if (request == null || request.getCourseId() == null) {
            throw new IllegalArgumentException("Course ID is required to create an assignment");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Assignment title cannot be blank");
        }
        if (request.getDeadline() == null) {
            throw new IllegalArgumentException("Assignment deadline is required");
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + request.getCourseId()));
        verifyCourseAssociation(course.getId(), currentUser);

        CourseModule module = null;
        if (request.getModuleId() != null) {
            module = courseModuleRepository.findById(request.getModuleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Module not found: " + request.getModuleId()));
            if (!Objects.equals(module.getCourse().getId(), course.getId())) {
                throw new IllegalArgumentException("Module does not belong to course " + course.getId());
            }
        }

        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + request.getLessonId()));
        }

        String submissionType = normalizeSubmissionType(request.getSubmissionType());
        String latePolicy = normalizeLatePolicy(request.getLatePolicy());
        String status = normalizeStatus(request.getStatus());

        BigDecimal maxMarks = request.getMaxMarks() != null ? request.getMaxMarks() : new BigDecimal("100.00");
        if (maxMarks.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Max marks cannot be negative");
        }

        BigDecimal latePenalty = request.getLatePenaltyPercentage() != null ? request.getLatePenaltyPercentage() : BigDecimal.ZERO;
        if (latePenalty.compareTo(BigDecimal.ZERO) < 0 || latePenalty.compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException("Late penalty percentage must be between 0 and 100");
        }

        Assignment assignment = new Assignment(
                course,
                module,
                lesson,
                currentUser,
                request.getTitle().trim(),
                trimToNull(request.getInstructions()),
                request.getDeadline(),
                maxMarks,
                trimToNull(request.getRubric()),
                submissionType,
                latePolicy,
                latePenalty,
                status
        );

        Assignment saved = assignmentRepository.save(assignment);

        // Raise reminder / due event for downstream notification listeners (FR-I41 / Person 3)
        if (eventPublisher != null) {
            LocalDateTime serverNow = LocalDateTime.now(clock);
            long hoursUntilDue = Duration.between(serverNow, saved.getDeadline()).toHours();
            eventPublisher.publishEvent(new AssignmentDueEvent(
                    this,
                    saved.getId(),
                    course.getId(),
                    saved.getTitle(),
                    saved.getDeadline(),
                    hoursUntilDue
            ));
        }

        return mapToAssignmentResponse(saved);
    }

    @Transactional
    public AssignmentResponse updateAssignment(User currentUser, Integer assignmentId, AssignmentUpdateRequest request) {
        validateInstructorOrAdmin(currentUser);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));
        verifyCourseAssociation(assignment.getCourse().getId(), currentUser);

        if (request.getTitle() != null) {
            if (request.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Assignment title cannot be blank");
            }
            assignment.setTitle(request.getTitle().trim());
        }
        if (request.getInstructions() != null) {
            assignment.setInstructions(trimToNull(request.getInstructions()));
        }
        if (request.getDeadline() != null) {
            assignment.setDeadline(request.getDeadline());
        }
        if (request.getMaxMarks() != null) {
            if (request.getMaxMarks().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Max marks cannot be negative");
            }
            assignment.setMaxMarks(request.getMaxMarks());
        }
        if (request.getRubric() != null) {
            assignment.setRubric(trimToNull(request.getRubric()));
        }
        if (request.getSubmissionType() != null) {
            assignment.setSubmissionType(normalizeSubmissionType(request.getSubmissionType()));
        }
        if (request.getLatePolicy() != null) {
            assignment.setLatePolicy(normalizeLatePolicy(request.getLatePolicy()));
        }
        if (request.getLatePenaltyPercentage() != null) {
            BigDecimal penalty = request.getLatePenaltyPercentage();
            if (penalty.compareTo(BigDecimal.ZERO) < 0 || penalty.compareTo(new BigDecimal("100.00")) > 0) {
                throw new IllegalArgumentException("Late penalty percentage must be between 0 and 100");
            }
            assignment.setLatePenaltyPercentage(penalty);
        }
        if (request.getStatus() != null) {
            assignment.setStatus(normalizeStatus(request.getStatus()));
        }

        Assignment updated = assignmentRepository.save(assignment);
        return mapToAssignmentResponse(updated);
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignment(User currentUser, Integer assignmentId) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        boolean isStudent = "student".equalsIgnoreCase(currentUser.getRole());
        if (isStudent) {
            verifyStudentEnrolled(assignment.getCourse().getId(), currentUser);
        } else if (!"admin".equalsIgnoreCase(currentUser.getRole())) {
            verifyCourseAssociation(assignment.getCourse().getId(), currentUser);
        }

        return mapToAssignmentResponse(assignment);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getCourseAssignments(User currentUser, Integer courseId) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        boolean isStudent = "student".equalsIgnoreCase(currentUser.getRole());
        if (isStudent) {
            verifyStudentEnrolled(courseId, currentUser);
            return assignmentRepository.findByCourseIdAndStatus(courseId, "PUBLISHED").stream()
                    .map(this::mapToAssignmentResponse)
                    .toList();
        } else if (!"admin".equalsIgnoreCase(currentUser.getRole())) {
            verifyCourseAssociation(courseId, currentUser);
        }

        return assignmentRepository.findByCourseId(courseId).stream()
                .map(this::mapToAssignmentResponse)
                .toList();
    }

    @Transactional
    public AssignmentSubmissionResponse submitAssignment(User currentUser, Integer assignmentId, AssignmentSubmissionRequest request) {
        validateActiveStudent(currentUser);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        verifyStudentEnrolled(assignment.getCourse().getId(), currentUser);

        if (request == null) {
            throw new IllegalArgumentException("Submission request payload cannot be null");
        }

        // Validate submission matches allowed type
        String requiredType = assignment.getSubmissionType() != null ? assignment.getSubmissionType() : "FILE";
        String submittedType = request.getSubmissionType() != null ? request.getSubmissionType().toUpperCase().trim() : "FILE";

        if ("FILE".equalsIgnoreCase(requiredType)) {
            if (request.getFileUrl() == null || request.getFileUrl().trim().isEmpty()) {
                throw new IllegalArgumentException("File submission required: fileUrl must not be blank");
            }
        } else if ("TEXT".equalsIgnoreCase(requiredType)) {
            if (request.getTextContent() == null || request.getTextContent().trim().isEmpty()) {
                throw new IllegalArgumentException("Text submission required: textContent must not be blank");
            }
        } else if ("BOTH".equalsIgnoreCase(requiredType)) {
            boolean hasFile = request.getFileUrl() != null && !request.getFileUrl().trim().isEmpty();
            boolean hasText = request.getTextContent() != null && !request.getTextContent().trim().isEmpty();
            if (!hasFile && !hasText) {
                throw new IllegalArgumentException("Submission required: provide fileUrl or textContent");
            }
        }

        LocalDateTime serverNow = LocalDateTime.now(clock);
        boolean isLate = assignment.isSubmissionLate(serverNow);
        BigDecimal appliedPenalty = BigDecimal.ZERO;

        if (isLate) {
            String policy = assignment.getLatePolicy() != null ? assignment.getLatePolicy().toUpperCase().trim() : "REJECT";
            if ("REJECT".equals(policy)) {
                throw new AssignmentLateException("Assignment deadline (" + assignment.getDeadline() + " UTC) has passed. Late submissions are not accepted.");
            } else if ("PENALTY".equals(policy)) {
                appliedPenalty = assignment.getLatePenaltyPercentage() != null ? assignment.getLatePenaltyPercentage() : BigDecimal.ZERO;
            }
        }

        AssignmentSubmission submission = assignmentSubmissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, currentUser.getId())
                .orElse(null);

        if (submission == null) {
            submission = new AssignmentSubmission(
                    assignment,
                    currentUser,
                    submittedType,
                    trimToNull(request.getFileUrl()),
                    trimToNull(request.getFileName()),
                    request.getFileSizeBytes(),
                    trimToNull(request.getTextContent()),
                    serverNow,
                    isLate,
                    appliedPenalty
            );
        } else {
            // Update existing submission before deadline or resubmit
            submission.setSubmissionType(submittedType);
            submission.setFileUrl(trimToNull(request.getFileUrl()));
            submission.setFileName(trimToNull(request.getFileName()));
            submission.setFileSizeBytes(request.getFileSizeBytes());
            submission.setTextContent(trimToNull(request.getTextContent()));
            submission.setSubmittedAt(serverNow);
            submission.setIsLate(isLate);
            submission.setAppliedPenaltyPercentage(appliedPenalty);
            submission.setGradingStatus("PENDING");
        }

        AssignmentSubmission saved = assignmentSubmissionRepository.save(submission);
        return mapToSubmissionResponse(saved);
    }

    @Transactional(readOnly = true)
    public AssignmentSubmissionResponse getMySubmission(User currentUser, Integer assignmentId) {
        validateActiveStudent(currentUser);
        AssignmentSubmission submission = assignmentSubmissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No submission found for assignment id: " + assignmentId));
        return mapToSubmissionResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> getAssignmentSubmissions(User currentUser, Integer assignmentId) {
        validateInstructorOrAdmin(currentUser);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));
        verifyCourseAssociation(assignment.getCourse().getId(), currentUser);

        return assignmentSubmissionRepository.findByAssignmentId(assignmentId).stream()
                .map(this::mapToSubmissionResponse)
                .toList();
    }

    @Transactional
    public AssignmentSubmissionResponse gradeSubmission(User currentUser, Integer submissionId,
                                                         BigDecimal awardedMarks, String feedback) {
        validateInstructorOrAdmin(currentUser);
        AssignmentSubmission submission = assignmentSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + submissionId));
        Assignment assignment = submission.getAssignment();
        verifyCourseAssociation(assignment.getCourse().getId(), currentUser);

        if (awardedMarks == null || awardedMarks.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Awarded marks cannot be negative");
        }
        if (assignment.getMaxMarks() != null && awardedMarks.compareTo(assignment.getMaxMarks()) > 0) {
            throw new IllegalArgumentException("Awarded marks cannot exceed max marks (" + assignment.getMaxMarks() + ")");
        }

        submission.setAwardedMarks(awardedMarks);
        submission.setFeedback(trimToNull(feedback));
        submission.setGrader(currentUser);
        submission.setGradedAt(LocalDateTime.now(clock));
        submission.setGradingStatus("GRADED");

        AssignmentSubmission saved = assignmentSubmissionRepository.save(submission);
        return mapToSubmissionResponse(saved);
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

    public void verifyStudentEnrolled(Integer courseId, User student) {
        boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), courseId);
        if (!enrolled) {
            throw new AccessDeniedException("Student is not enrolled in course: " + courseId);
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

    private void validateActiveStudent(User user) {
        if (user == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (!"student".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only students can perform this action");
        }
    }

    private String normalizeSubmissionType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "FILE";
        }
        String upper = type.trim().toUpperCase();
        if (!ALLOWED_SUBMISSION_TYPES.contains(upper)) {
            throw new IllegalArgumentException("Invalid submission type: " + type + ". Allowed: " + ALLOWED_SUBMISSION_TYPES);
        }
        return upper;
    }

    private String normalizeLatePolicy(String policy) {
        if (policy == null || policy.trim().isEmpty()) {
            return "REJECT";
        }
        String upper = policy.trim().toUpperCase();
        if (!ALLOWED_LATE_POLICIES.contains(upper)) {
            throw new IllegalArgumentException("Invalid late policy: " + policy + ". Allowed: " + ALLOWED_LATE_POLICIES);
        }
        return upper;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "PUBLISHED";
        }
        String upper = status.trim().toUpperCase();
        if (!ALLOWED_STATUSES.contains(upper)) {
            throw new IllegalArgumentException("Invalid status: " + status + ". Allowed: " + ALLOWED_STATUSES);
        }
        return upper;
    }

    private String trimToNull(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return text.trim();
    }

    public AssignmentResponse mapToAssignmentResponse(Assignment assignment) {
        LocalDateTime serverNow = LocalDateTime.now(clock);
        AssignmentResponse response = new AssignmentResponse();
        response.setId(assignment.getId());
        response.setCourseId(assignment.getCourse().getId());
        response.setCourseTitle(assignment.getCourse().getTitle());
        if (assignment.getModule() != null) {
            response.setModuleId(assignment.getModule().getId());
        }
        if (assignment.getLesson() != null) {
            response.setLessonId(assignment.getLesson().getId());
        }
        if (assignment.getCreatedBy() != null) {
            response.setCreatedById(assignment.getCreatedBy().getId());
            response.setCreatedByName(assignment.getCreatedBy().getName());
        }
        response.setTitle(assignment.getTitle());
        response.setInstructions(assignment.getInstructions());
        response.setDeadline(assignment.getDeadline());
        response.setMaxMarks(assignment.getMaxMarks());
        response.setRubric(assignment.getRubric());
        response.setSubmissionType(assignment.getSubmissionType());
        response.setLatePolicy(assignment.getLatePolicy());
        response.setLatePenaltyPercentage(assignment.getLatePenaltyPercentage());
        response.setStatus(assignment.getStatus());
        response.setCreatedAt(assignment.getCreatedAt());
        response.setUpdatedAt(assignment.getUpdatedAt());
        response.setIsPastDeadline(assignment.isSubmissionLate(serverNow));
        response.setServerTime(serverNow);
        return response;
    }

    public AssignmentSubmissionResponse mapToSubmissionResponse(AssignmentSubmission submission) {
        AssignmentSubmissionResponse response = new AssignmentSubmissionResponse();
        response.setId(submission.getId());
        response.setAssignmentId(submission.getAssignment().getId());
        response.setAssignmentTitle(submission.getAssignment().getTitle());
        response.setStudentId(submission.getStudent().getId());
        response.setStudentName(submission.getStudent().getName());
        response.setSubmissionType(submission.getSubmissionType());
        response.setFileUrl(submission.getFileUrl());
        response.setFileName(submission.getFileName());
        response.setFileSizeBytes(submission.getFileSizeBytes());
        response.setTextContent(submission.getTextContent());
        response.setSubmittedAt(submission.getSubmittedAt());
        response.setIsLate(submission.getIsLate());
        response.setAppliedPenaltyPercentage(submission.getAppliedPenaltyPercentage());
        response.setGradingStatus(submission.getGradingStatus());
        response.setAwardedMarks(submission.getAwardedMarks());

        // Calculate effective marks if marks have been awarded
        if (submission.getAwardedMarks() != null) {
            BigDecimal penaltyPct = submission.getAppliedPenaltyPercentage() != null ? submission.getAppliedPenaltyPercentage() : BigDecimal.ZERO;
            if (penaltyPct.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal deduction = submission.getAwardedMarks()
                        .multiply(penaltyPct)
                        .divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);
                BigDecimal effective = submission.getAwardedMarks().subtract(deduction);
                response.setEffectiveMarks(effective.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : effective);
            } else {
                response.setEffectiveMarks(submission.getAwardedMarks());
            }
        }

        response.setFeedback(submission.getFeedback());
        if (submission.getGrader() != null) {
            response.setGraderId(submission.getGrader().getId());
            response.setGraderName(submission.getGrader().getName());
        }
        response.setGradedAt(submission.getGradedAt());
        response.setCreatedAt(submission.getCreatedAt());
        response.setUpdatedAt(submission.getUpdatedAt());
        return response;
    }
}

