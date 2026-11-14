package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.event.ExamScheduledEvent;
import com.olp.exception.ExamIneligibleException;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service managing examination creation, scheduling, server-authoritative timer enforcement (DR-ED06),
 * and student eligibility verification (DR-ED07) (FR-I25, Step 38).
 */
@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CoursePrerequisiteRepository coursePrerequisiteRepository;
    private final CourseProgressService courseProgressService;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    private Clock clock = Clock.systemUTC();

    public ExamService(ExamRepository examRepository,
                       ExamQuestionRepository examQuestionRepository,
                       ExamAttemptRepository examAttemptRepository,
                       CourseRepository courseRepository,
                       CourseInstructorRepository courseInstructorRepository,
                       EnrollmentRepository enrollmentRepository,
                       CoursePrerequisiteRepository coursePrerequisiteRepository,
                       CourseProgressService courseProgressService,
                       QuestionRepository questionRepository,
                       UserRepository userRepository,
                       ApplicationEventPublisher eventPublisher) {
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.coursePrerequisiteRepository = coursePrerequisiteRepository;
        this.courseProgressService = courseProgressService;
        this.questionRepository = questionRepository;
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
                    .orElseThrow(() -> new InvalidCredentialsException("User not found: " + email));
        }
        throw new InvalidCredentialsException("Invalid authentication principal");
    }

    @Transactional
    public ExamResponse createExam(User currentUser, ExamCreateRequest request) {
        validateInstructorOrAdmin(currentUser);
        if (request == null || request.getCourseId() == null) {
            throw new IllegalArgumentException("Course ID is required to create an exam");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Exam title cannot be blank");
        }
        if (request.getStartWindow() == null) {
            throw new IllegalArgumentException("Exam start window is required");
        }
        if (request.getEndWindow() == null) {
            throw new IllegalArgumentException("Exam end window is required");
        }
        if (request.getEndWindow().isBefore(request.getStartWindow())) {
            throw new IllegalArgumentException("Exam end window must be after start window");
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + request.getCourseId()));
        verifyCourseAssociation(course.getId(), currentUser);

        Integer durationMinutes = request.getDurationMinutes() != null ? request.getDurationMinutes() : 60;
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Exam duration must be at least 1 minute");
        }

        BigDecimal maxMarks = request.getMaxMarks() != null ? request.getMaxMarks() : new BigDecimal("100.00");
        if (maxMarks.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Max marks cannot be negative");
        }

        BigDecimal minProgress = request.getMinProgressPercentage() != null ? request.getMinProgressPercentage() : BigDecimal.ZERO;
        if (minProgress.compareTo(BigDecimal.ZERO) < 0 || minProgress.compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException("Minimum progress percentage must be between 0 and 100");
        }

        Exam exam = new Exam(
                course,
                currentUser,
                request.getTitle().trim(),
                trimToNull(request.getDescription()),
                trimToNull(request.getInstructions()),
                request.getStartWindow(),
                request.getEndWindow(),
                durationMinutes,
                request.getTimeZone() != null ? request.getTimeZone() : "UTC",
                maxMarks,
                request.getPassingMarks(),
                request.getMaxAttempts() != null ? request.getMaxAttempts() : 1,
                minProgress,
                request.getRequirePrerequisites() != null ? request.getRequirePrerequisites() : true,
                request.getRequireEnrollment() != null ? request.getRequireEnrollment() : true,
                request.getStatus() != null ? request.getStatus().toUpperCase() : "SCHEDULED"
        );

        Exam saved = examRepository.save(exam);

        // Attach questions from question bank if provided
        if (request.getQuestionIds() != null && !request.getQuestionIds().isEmpty()) {
            int order = 1;
            for (Integer questionId : request.getQuestionIds()) {
                Question question = questionRepository.findById(questionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
                ExamQuestion eq = new ExamQuestion(
                        saved,
                        question,
                        1,
                        question.getDefaultMarks() != null ? question.getDefaultMarks() : BigDecimal.ONE,
                        order++
                );
                examQuestionRepository.save(eq);
            }
        }

        // Publish event for downstream notifications (FR-I41)
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new ExamScheduledEvent(
                    this,
                    saved.getId(),
                    course.getId(),
                    saved.getTitle(),
                    saved.getStartWindow(),
                    saved.getEndWindow(),
                    saved.getDurationMinutes()
            ));
        }

        LocalDateTime serverNow = LocalDateTime.now(clock);
        return mapToExamResponse(saved, serverNow);
    }

    @Transactional
    public ExamResponse updateExam(User currentUser, Integer examId, ExamUpdateRequest request) {
        validateInstructorOrAdmin(currentUser);
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
        verifyCourseAssociation(exam.getCourse().getId(), currentUser);

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            exam.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            exam.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getInstructions() != null) {
            exam.setInstructions(trimToNull(request.getInstructions()));
        }
        if (request.getStartWindow() != null) {
            exam.setStartWindow(request.getStartWindow());
        }
        if (request.getEndWindow() != null) {
            exam.setEndWindow(request.getEndWindow());
        }
        if (exam.getEndWindow().isBefore(exam.getStartWindow())) {
            throw new IllegalArgumentException("Exam end window must be after start window");
        }
        if (request.getDurationMinutes() != null) {
            if (request.getDurationMinutes() <= 0) {
                throw new IllegalArgumentException("Duration must be at least 1 minute");
            }
            exam.setDurationMinutes(request.getDurationMinutes());
        }
        if (request.getTimeZone() != null) {
            exam.setTimeZone(request.getTimeZone());
        }
        if (request.getMaxMarks() != null) {
            if (request.getMaxMarks().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Max marks cannot be negative");
            }
            exam.setMaxMarks(request.getMaxMarks());
        }
        if (request.getPassingMarks() != null) {
            exam.setPassingMarks(request.getPassingMarks());
        }
        if (request.getMaxAttempts() != null) {
            if (request.getMaxAttempts() < 1) {
                throw new IllegalArgumentException("Max attempts must be at least 1");
            }
            exam.setMaxAttempts(request.getMaxAttempts());
        }
        if (request.getMinProgressPercentage() != null) {
            if (request.getMinProgressPercentage().compareTo(BigDecimal.ZERO) < 0 ||
                    request.getMinProgressPercentage().compareTo(new BigDecimal("100.00")) > 0) {
                throw new IllegalArgumentException("Min progress percentage must be between 0 and 100");
            }
            exam.setMinProgressPercentage(request.getMinProgressPercentage());
        }
        if (request.getRequirePrerequisites() != null) {
            exam.setRequirePrerequisites(request.getRequirePrerequisites());
        }
        if (request.getRequireEnrollment() != null) {
            exam.setRequireEnrollment(request.getRequireEnrollment());
        }
        if (request.getStatus() != null) {
            exam.setStatus(request.getStatus().toUpperCase());
        }

        Exam updated = examRepository.save(exam);
        LocalDateTime serverNow = LocalDateTime.now(clock);
        return mapToExamResponse(updated, serverNow);
    }

    public ExamResponse getExam(User currentUser, Integer examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));

        if (currentUser != null && !"admin".equalsIgnoreCase(currentUser.getRole())) {
            boolean isInstructor = "instructor".equalsIgnoreCase(currentUser.getRole());
            if (isInstructor) {
                verifyCourseAssociation(exam.getCourse().getId(), currentUser);
            } else {
                if (Boolean.TRUE.equals(exam.getRequireEnrollment())) {
                    boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(currentUser.getId(), exam.getCourse().getId());
                    if (!enrolled) {
                        throw new AccessDeniedException("Student is not enrolled in course: " + exam.getCourse().getId());
                    }
                }
            }
        }

        LocalDateTime serverNow = LocalDateTime.now(clock);
        return mapToExamResponse(exam, serverNow);
    }

    public List<ExamResponse> getCourseExams(User currentUser, Integer courseId) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        if (currentUser != null && "instructor".equalsIgnoreCase(currentUser.getRole())) {
            verifyCourseAssociation(courseId, currentUser);
        }

        List<Exam> exams = examRepository.findByCourseId(courseId);
        LocalDateTime serverNow = LocalDateTime.now(clock);
        return exams.stream()
                .map(e -> mapToExamResponse(e, serverNow))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ExamQuestionItemDto> linkQuestionsToExam(User currentUser, Integer examId, List<Integer> questionIds) {
        validateInstructorOrAdmin(currentUser);
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
        verifyCourseAssociation(exam.getCourse().getId(), currentUser);

        examQuestionRepository.deleteByExamId(examId);

        List<ExamQuestionItemDto> result = new ArrayList<>();
        if (questionIds != null) {
            int order = 1;
            for (Integer qId : questionIds) {
                Question question = questionRepository.findById(qId)
                        .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + qId));
                ExamQuestion eq = new ExamQuestion(
                        exam,
                        question,
                        1,
                        question.getDefaultMarks() != null ? question.getDefaultMarks() : BigDecimal.ONE,
                        order++
                );
                ExamQuestion savedEq = examQuestionRepository.save(eq);
                result.add(new ExamQuestionItemDto(
                        savedEq.getId(),
                        question.getId(),
                        question.getQuestionText(),
                        question.getQuestionType() != null ? question.getQuestionType().name() : "MULTIPLE_CHOICE",
                        question.getOptionsJson(),
                        savedEq.getMarks(),
                        savedEq.getOrderIndex()
                ));
            }
        }
        return result;
    }

    public ExamEligibilityResponse checkEligibility(User currentUser, Integer examId) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
        Integer courseId = exam.getCourse().getId();
        Integer studentId = currentUser.getId();
        LocalDateTime serverNow = LocalDateTime.now(clock);

        // 1. Enrollment check
        boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
        if (Boolean.TRUE.equals(exam.getRequireEnrollment()) && !enrolled) {
            return ExamEligibilityResponse.denied(
                    examId, studentId, "NOT_ENROLLED",
                    "Student is not enrolled in this course",
                    BigDecimal.ZERO, exam.getMinProgressPercentage(),
                    (int) examAttemptRepository.countByExamIdAndStudentId(examId, studentId),
                    exam.getMaxAttempts(), exam.getStartWindow(), exam.getEndWindow(), serverNow
            );
        }

        // 2. Prerequisites check
        if (Boolean.TRUE.equals(exam.getRequirePrerequisites()) && coursePrerequisiteRepository != null) {
            List<CoursePrerequisite> prerequisites = coursePrerequisiteRepository.findByCourseIdOrderByIdAsc(courseId);
            for (CoursePrerequisite prereq : prerequisites) {
                if ("COURSE".equalsIgnoreCase(prereq.getPrerequisiteType()) && prereq.getRequiredCourse() != null) {
                    CourseProgressResponse reqCourseProgress = courseProgressService != null
                            ? courseProgressService.getCourseProgress(studentId, prereq.getRequiredCourse().getId())
                            : null;
                    if (reqCourseProgress == null || reqCourseProgress.getPercentage() < 100.0) {
                        return ExamEligibilityResponse.denied(
                                examId, studentId, "PREREQUISITE_UNMET",
                                "Prerequisite course incomplete: " + prereq.getTitle(),
                                BigDecimal.valueOf(reqCourseProgress != null ? reqCourseProgress.getPercentage() : 0.0),
                                exam.getMinProgressPercentage(),
                                (int) examAttemptRepository.countByExamIdAndStudentId(examId, studentId),
                                exam.getMaxAttempts(), exam.getStartWindow(), exam.getEndWindow(), serverNow
                        );
                    }
                }
            }
        }

        // 3. Minimum course progress check
        CourseProgressResponse progress = courseProgressService != null
                ? courseProgressService.getCourseProgress(studentId, courseId)
                : null;
        double currentProgressDouble = (progress != null) ? progress.getPercentage() : 0.0;
        BigDecimal studentProgress = BigDecimal.valueOf(currentProgressDouble);
        BigDecimal minRequired = exam.getMinProgressPercentage() != null ? exam.getMinProgressPercentage() : BigDecimal.ZERO;
        if (minRequired.compareTo(BigDecimal.ZERO) > 0 && studentProgress.compareTo(minRequired) < 0) {
            return ExamEligibilityResponse.denied(
                    examId, studentId, "BELOW_MIN_PROGRESS",
                    "Current progress of " + studentProgress + "% is below required minimum of " + minRequired + "%",
                    studentProgress, minRequired,
                    (int) examAttemptRepository.countByExamIdAndStudentId(examId, studentId),
                    exam.getMaxAttempts(), exam.getStartWindow(), exam.getEndWindow(), serverNow
            );
        }

        // 4. Window timing checks (Start Window and End Window)
        if (exam.getStartWindow() != null && serverNow.isBefore(exam.getStartWindow())) {
            return ExamEligibilityResponse.denied(
                    examId, studentId, "BEFORE_START_WINDOW",
                    "Examination window has not opened yet. Starts at " + exam.getStartWindow(),
                    studentProgress, minRequired,
                    (int) examAttemptRepository.countByExamIdAndStudentId(examId, studentId),
                    exam.getMaxAttempts(), exam.getStartWindow(), exam.getEndWindow(), serverNow
            );
        }
        if (exam.getEndWindow() != null && serverNow.isAfter(exam.getEndWindow())) {
            return ExamEligibilityResponse.denied(
                    examId, studentId, "AFTER_END_WINDOW",
                    "Examination window closed at " + exam.getEndWindow(),
                    studentProgress, minRequired,
                    (int) examAttemptRepository.countByExamIdAndStudentId(examId, studentId),
                    exam.getMaxAttempts(), exam.getStartWindow(), exam.getEndWindow(), serverNow
            );
        }

        // 5. Max attempts exhausted check
        int attemptsUsed = (int) examAttemptRepository.countByExamIdAndStudentId(examId, studentId);
        if (exam.getMaxAttempts() != null && attemptsUsed >= exam.getMaxAttempts()) {
            return ExamEligibilityResponse.denied(
                    examId, studentId, "MAX_ATTEMPTS_EXHAUSTED",
                    "Maximum attempts (" + exam.getMaxAttempts() + ") exhausted for this examination",
                    studentProgress, minRequired,
                    attemptsUsed, exam.getMaxAttempts(),
                    exam.getStartWindow(), exam.getEndWindow(), serverNow
            );
        }

        // Student is eligible to take the exam
        return ExamEligibilityResponse.eligible(
                examId, studentId,
                studentProgress, minRequired,
                attemptsUsed, exam.getMaxAttempts(),
                exam.getStartWindow(), exam.getEndWindow(), serverNow
        );
    }

    @Transactional
    public ExamStartResponse startExam(User currentUser, Integer examId) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));

        LocalDateTime serverNow = LocalDateTime.now(clock);

        // Check if an existing in-progress attempt is already ongoing
        List<ExamAttempt> activeAttempts = examAttemptRepository.findActiveAttempts(examId, currentUser.getId());
        if (!activeAttempts.isEmpty()) {
            ExamAttempt active = activeAttempts.get(0);
            if (!active.isExpired(serverNow)) {
                return buildExamStartResponse(exam, active, serverNow);
            } else {
                active.setStatus("EXPIRED");
                examAttemptRepository.save(active);
            }
        }

        // Verify eligibility before creating a new attempt
        ExamEligibilityResponse eligibility = checkEligibility(currentUser, examId);
        if (!Boolean.TRUE.equals(eligibility.getIsEligible())) {
            throw new ExamIneligibleException(eligibility.getDenialReason(), eligibility.getDenialMessage());
        }

        int nextAttemptNumber = (int) examAttemptRepository.countByExamIdAndStudentId(examId, currentUser.getId()) + 1;
        LocalDateTime startedAt = serverNow;
        LocalDateTime expiresAt = exam.computeExpiresAt(startedAt);

        ExamAttempt attempt = new ExamAttempt(exam, currentUser, nextAttemptNumber, startedAt, expiresAt);
        attempt.setTotalMarks(exam.getMaxMarks());
        ExamAttempt savedAttempt = examAttemptRepository.save(attempt);

        return buildExamStartResponse(exam, savedAttempt, serverNow);
    }

    private ExamStartResponse buildExamStartResponse(Exam exam, ExamAttempt attempt, LocalDateTime serverNow) {
        ExamStartResponse res = new ExamStartResponse();
        res.setAttemptId(attempt.getId());
        res.setExamId(exam.getId());
        res.setTitle(exam.getTitle());
        res.setAttemptNumber(attempt.getAttemptNumber());
        res.setStartedAt(attempt.getStartedAt());
        res.setExpiresAt(attempt.getExpiresAt());
        long remaining = Math.max(0, Duration.between(serverNow, attempt.getExpiresAt()).toSeconds());
        res.setRemainingSeconds(remaining);
        res.setStatus(attempt.getStatus());
        res.setServerTime(serverNow);
        res.setDurationMinutes(exam.getDurationMinutes());
        res.setTotalMarks(exam.getMaxMarks());
        res.setInstructions(exam.getInstructions());

        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(exam.getId());
        List<ExamQuestionItemDto> questionDtos = examQuestions.stream()
                .map(eq -> {
                    Question q = eq.getQuestion();
                    return new ExamQuestionItemDto(
                            eq.getId(),
                            q.getId(),
                            q.getQuestionText(),
                            q.getQuestionType() != null ? q.getQuestionType().name() : "MULTIPLE_CHOICE",
                            q.getOptionsJson(),
                            eq.getMarks(),
                            eq.getOrderIndex()
                    );
                })
                .collect(Collectors.toList());
        res.setQuestions(questionDtos);
        return res;
    }

    public ExamResponse mapToExamResponse(Exam exam, LocalDateTime serverNow) {
        ExamResponse res = new ExamResponse();
        res.setId(exam.getId());
        res.setCourseId(exam.getCourse() != null ? exam.getCourse().getId() : null);
        res.setCourseTitle(exam.getCourse() != null ? exam.getCourse().getTitle() : null);
        res.setCreatedById(exam.getCreatedBy() != null ? exam.getCreatedBy().getId() : null);
        res.setCreatedByName(exam.getCreatedBy() != null ? exam.getCreatedBy().getName() : null);
        res.setTitle(exam.getTitle());
        res.setDescription(exam.getDescription());
        res.setInstructions(exam.getInstructions());
        res.setStartWindow(exam.getStartWindow());
        res.setEndWindow(exam.getEndWindow());
        res.setDurationMinutes(exam.getDurationMinutes());
        res.setTimeZone(exam.getTimeZone());
        res.setMaxMarks(exam.getMaxMarks());
        res.setPassingMarks(exam.getPassingMarks());
        res.setMaxAttempts(exam.getMaxAttempts());
        res.setMinProgressPercentage(exam.getMinProgressPercentage());
        res.setRequirePrerequisites(exam.getRequirePrerequisites());
        res.setRequireEnrollment(exam.getRequireEnrollment());
        res.setStatus(exam.getStatus());
        res.setQuestionCount((int) examQuestionRepository.countByExamId(exam.getId()));
        res.setCreatedAt(exam.getCreatedAt());
        res.setUpdatedAt(exam.getUpdatedAt());
        res.setIsInsideWindow(exam.isInsideWindow(serverNow));
        res.setServerTime(serverNow);
        return res;
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

    private String trimToNull(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return text.trim();
    }
}

