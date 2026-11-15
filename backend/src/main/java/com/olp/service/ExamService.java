package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.event.ExamScheduledEvent;
import com.olp.exception.ExamIneligibleException;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
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
    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamRandomizationService examRandomizationService;

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
                       ApplicationEventPublisher eventPublisher,
                       ExamBlueprintRepository examBlueprintRepository,
                       ExamRandomizationService examRandomizationService) {
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
        this.examBlueprintRepository = examBlueprintRepository;
        this.examRandomizationService = examRandomizationService;
    }

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
        this(examRepository, examQuestionRepository, examAttemptRepository,
                courseRepository, courseInstructorRepository, enrollmentRepository,
                coursePrerequisiteRepository, courseProgressService, questionRepository,
                userRepository, eventPublisher, null, null);
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

        if (Boolean.TRUE.equals(request.getIsRandomized())) {
            exam.setIsRandomized(true);
            exam.setRandomSeed(request.getRandomSeed());
            if (request.getTotalSets() != null && request.getTotalSets() >= 1) {
                exam.setTotalSets(request.getTotalSets());
            }
        }

        if (request.getQuestionCorrectionPolicy() != null) {
            exam.setQuestionCorrectionPolicy(request.getQuestionCorrectionPolicy());
        }

        Exam saved = examRepository.save(exam);

        // Save blueprints if provided
        if (request.getBlueprints() != null && !request.getBlueprints().isEmpty() && examBlueprintRepository != null) {
            int bpOrder = 1;
            for (ExamBlueprintItemDto bpDto : request.getBlueprints()) {
                ExamBlueprint bp = new ExamBlueprint(
                        saved,
                        trimToNull(bpDto.getTopic()),
                        trimToNull(bpDto.getDifficulty()),
                        bpDto.getQuestionCount() != null ? bpDto.getQuestionCount() : 1,
                        bpDto.getMarksPerQuestion() != null ? bpDto.getMarksPerQuestion() : BigDecimal.ONE,
                        bpOrder++
                );
                examBlueprintRepository.save(bp);
            }
        }

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
        if (request.getQuestionCorrectionPolicy() != null) {
            if (!request.getQuestionCorrectionPolicy().equals(exam.getQuestionCorrectionPolicy())) {
                long attemptCount = examAttemptRepository.countByExamId(examId);
                if (attemptCount > 0) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Cannot modify question correction policy once attempts have started (DR-ED05). Policy is locked.");
                }
                exam.setQuestionCorrectionPolicy(request.getQuestionCorrectionPolicy());
            }
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

        // Randomized exam set handling (FR-I27, Step 39)
        String setLabel = "SET_A";
        Long attemptSeed = null;
        List<ExamQuestionItemDto> questionDtos;

        if (Boolean.TRUE.equals(exam.getIsRandomized()) && examRandomizationService != null) {
            long baseSeed = exam.getRandomSeed() != null ? exam.getRandomSeed() : 42L;
            attemptSeed = baseSeed + ((long) currentUser.getId() * 10007L) + nextAttemptNumber;
            int totalSets = Math.max(1, exam.getTotalSets() != null ? exam.getTotalSets() : 1);
            int variantIdx = Math.abs((int) (attemptSeed % totalSets));
            setLabel = "SET_" + (char) ('A' + variantIdx);

            List<ExamBlueprint> blueprints = examBlueprintRepository != null
                    ? examBlueprintRepository.findByExamIdOrderByOrderIndexAsc(exam.getId())
                    : Collections.emptyList();

            if (!blueprints.isEmpty()) {
                ExamSetVariantDto variant = examRandomizationService.generateSingleSet(exam, blueprints, setLabel, attemptSeed);
                questionDtos = variant.getQuestions();
            } else {
                List<ExamQuestion> examQuestions = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(exam.getId());
                List<ExamQuestion> shuffled = new ArrayList<>(examQuestions);
                Collections.shuffle(shuffled, new Random(attemptSeed));
                questionDtos = new ArrayList<>();
                int idx = 1;
                for (ExamQuestion eq : shuffled) {
                    Question q = eq.getQuestion();
                    questionDtos.add(new ExamQuestionItemDto(
                            eq.getId(), q.getId(), q.getQuestionText(),
                            q.getQuestionType() != null ? q.getQuestionType().name() : "MULTIPLE_CHOICE",
                            q.getOptionsJson(), eq.getMarks(), idx++
                    ));
                }
            }

            // Shuffle option order per question
            Map<String, List<Integer>> shuffleMap = new LinkedHashMap<>();
            for (ExamQuestionItemDto qDto : questionDtos) {
                ExamRandomizationService.ShuffledOptionsResult optRes = examRandomizationService.shuffleOptions(
                        qDto.getOptionsJson(), attemptSeed + qDto.getQuestionId()
                );
                qDto.setOptionsJson(optRes.getShuffledJson());
                if (!optRes.getPermutation().isEmpty()) {
                    shuffleMap.put(String.valueOf(qDto.getQuestionId()), optRes.getPermutation());
                }
            }

            attempt.setSetLabel(setLabel);
            attempt.setRandomSeed(attemptSeed);
            attempt.setQuestionOrder(questionDtos.stream().map(q -> String.valueOf(q.getQuestionId())).collect(Collectors.joining(",")));
            if (!shuffleMap.isEmpty()) {
                try {
                    attempt.setOptionShuffleMap(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(shuffleMap));
                } catch (Exception ignored) {}
            }
        } else {
            List<ExamQuestion> examQuestions = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(exam.getId());
            questionDtos = examQuestions.stream()
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
        }

        ExamAttempt savedAttempt = examAttemptRepository.save(attempt);

        return buildExamStartResponse(exam, savedAttempt, questionDtos, serverNow);
    }

    private ExamStartResponse buildExamStartResponse(Exam exam, ExamAttempt attempt, LocalDateTime serverNow) {
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
        return buildExamStartResponse(exam, attempt, questionDtos, serverNow);
    }

    private ExamStartResponse buildExamStartResponse(Exam exam, ExamAttempt attempt, List<ExamQuestionItemDto> questionDtos, LocalDateTime serverNow) {
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
        res.setSetLabel(attempt.getSetLabel());
        res.setRandomSeed(attempt.getRandomSeed());
        res.setServerTime(serverNow);
        res.setDurationMinutes(exam.getDurationMinutes());
        res.setTotalMarks(exam.getMaxMarks());
        res.setInstructions(exam.getInstructions());
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
        res.setIsRandomized(exam.getIsRandomized());
        res.setRandomSeed(exam.getRandomSeed());
        res.setTotalSets(exam.getTotalSets());
        res.setQuestionCorrectionPolicy(exam.getQuestionCorrectionPolicy());
        if (examBlueprintRepository != null) {
            List<ExamBlueprint> blueprints = examBlueprintRepository.findByExamIdOrderByOrderIndexAsc(exam.getId());
            res.setBlueprints(blueprints.stream()
                    .map(b -> new ExamBlueprintItemDto(b.getId(), b.getTopic(), b.getDifficulty(), b.getQuestionCount(), b.getMarksPerQuestion(), b.getOrderIndex()))
                    .collect(Collectors.toList()));
        }
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

    public QuestionCorrectionPolicyResponse getCorrectionPolicy(User currentUser, Integer examId) {
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

        long attemptCount = examAttemptRepository.countByExamId(examId);
        boolean isLocked = attemptCount > 0;
        String message = isLocked
                ? "Question correction policy is locked because examination attempts have started (DR-ED05)."
                : "Question correction policy is unlocked and can be modified.";

        return new QuestionCorrectionPolicyResponse(
                exam.getId(),
                exam.getTitle(),
                exam.getQuestionCorrectionPolicy(),
                isLocked,
                attemptCount,
                message
        );
    }

    @Transactional
    public QuestionCorrectionPolicyResponse updateCorrectionPolicy(User currentUser, Integer examId, QuestionCorrectionPolicyRequest request) {
        validateInstructorOrAdmin(currentUser);
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
        verifyCourseAssociation(exam.getCourse().getId(), currentUser);

        if (request == null || request.getPolicy() == null) {
            throw new IllegalArgumentException("Question correction policy is required");
        }

        long attemptCount = examAttemptRepository.countByExamId(examId);
        boolean isLocked = attemptCount > 0;

        if (isLocked && !request.getPolicy().equals(exam.getQuestionCorrectionPolicy())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot modify question correction policy once attempts have started (DR-ED05). Policy is locked.");
        }

        exam.setQuestionCorrectionPolicy(request.getPolicy());
        examRepository.save(exam);

        return new QuestionCorrectionPolicyResponse(
                exam.getId(),
                exam.getTitle(),
                exam.getQuestionCorrectionPolicy(),
                isLocked,
                attemptCount,
                "Question correction policy updated successfully."
        );
    }

    public QuestionCorrectionPolicy getCorrectionPolicyForCorrection(Integer examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
        return exam.getQuestionCorrectionPolicy();
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

