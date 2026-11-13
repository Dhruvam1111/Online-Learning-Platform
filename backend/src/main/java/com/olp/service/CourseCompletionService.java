package com.olp.service;

import com.olp.dto.CertificateResponse;
import com.olp.dto.CompletionEvaluationResult;
import com.olp.dto.CourseCompletionCriteriaRequest;
import com.olp.dto.CourseCompletionCriteriaResponse;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for configuring course certificate completion criteria, evaluating student eligibility,
 * and issuing certificates upon full criteria satisfaction (FR-I56, FR-S77, DR-03).
 */
@Service
public class CourseCompletionService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseCompletionCriteriaRepository completionCriteriaRepository;
    private final CertificateRepository certificateRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LectureRepository lectureRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final CourseAuditService courseAuditService;

    @Autowired
    public CourseCompletionService(
            CourseRepository courseRepository,
            UserRepository userRepository,
            CourseCompletionCriteriaRepository completionCriteriaRepository,
            CertificateRepository certificateRepository,
            EnrollmentRepository enrollmentRepository,
            LessonRepository lessonRepository,
            LessonProgressRepository lessonProgressRepository,
            LectureRepository lectureRepository,
            LectureProgressRepository lectureProgressRepository,
            QuizRepository quizRepository,
            QuizAttemptRepository quizAttemptRepository,
            @Autowired(required = false) CourseInstructorRepository courseInstructorRepository,
            @Autowired(required = false) CourseAuditService courseAuditService) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.completionCriteriaRepository = completionCriteriaRepository;
        this.certificateRepository = certificateRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lessonRepository = lessonRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.lectureRepository = lectureRepository;
        this.lectureProgressRepository = lectureProgressRepository;
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.courseAuditService = courseAuditService;
    }

    /**
     * Configure or update completion criteria for a course (FR-I56).
     * Locks criteria after the first certificate has been issued (DR-03).
     */
    @Transactional
    public CourseCompletionCriteriaResponse configureCompletionCriteria(
            User instructor,
            Integer courseId,
            CourseCompletionCriteriaRequest request) {

        if (instructor == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Completion criteria request cannot be null");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        if (course.isDeleted()) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }

        verifyInstructorAuthorization(course, instructor);

        if (isCriteriaLocked(courseId)) {
            throw new IllegalStateException(
                    "Cannot modify completion criteria: One or more certificates have already been issued for this course. " +
                    "Completion rules are permanently locked to preserve certification integrity (DR-03).");
        }

        validateCriteriaRequest(courseId, request);

        CourseCompletionCriteria criteria = completionCriteriaRepository.findByCourseId(courseId)
                .orElseGet(() -> new CourseCompletionCriteria(course));

        Map<String, Object> prevValues = new HashMap<>();
        if (criteria.getId() != null) {
            prevValues.put("minLessonCompletionPercentage", criteria.getMinLessonCompletionPercentage());
            prevValues.put("minQuizPassingMarks", criteria.getMinQuizPassingMarks());
            prevValues.put("minQuizPassingPercentage", criteria.getMinQuizPassingPercentage());
            prevValues.put("requireAllQuizzesPassed", criteria.isRequireAllQuizzesPassed());
            prevValues.put("mandatoryLessonIds", criteria.getMandatoryLessonIds());
            prevValues.put("mandatoryQuizIds", criteria.getMandatoryQuizIds());
        }

        if (request.getMinLessonCompletionPercentage() != null) {
            criteria.setMinLessonCompletionPercentage(request.getMinLessonCompletionPercentage());
        }
        criteria.setMinQuizPassingMarks(request.getMinQuizPassingMarks());
        criteria.setMinQuizPassingPercentage(request.getMinQuizPassingPercentage());
        if (request.getRequireAllQuizzesPassed() != null) {
            criteria.setRequireAllQuizzesPassed(request.getRequireAllQuizzesPassed());
        }
        criteria.setMandatoryLessonIdList(request.getMandatoryLessonIds());
        criteria.setMandatoryQuizIdList(request.getMandatoryQuizIds());

        CourseCompletionCriteria saved = completionCriteriaRepository.save(criteria);

        long certCount = certificateRepository != null ? certificateRepository.countByCourseId(courseId) : 0;

        if (courseAuditService != null) {
            Map<String, Object> newValues = new HashMap<>();
            newValues.put("minLessonCompletionPercentage", saved.getMinLessonCompletionPercentage());
            newValues.put("minQuizPassingMarks", saved.getMinQuizPassingMarks());
            newValues.put("minQuizPassingPercentage", saved.getMinQuizPassingPercentage());
            newValues.put("requireAllQuizzesPassed", saved.isRequireAllQuizzesPassed());
            newValues.put("mandatoryLessonIds", saved.getMandatoryLessonIds());
            newValues.put("mandatoryQuizIds", saved.getMandatoryQuizIds());

            courseAuditService.recordAudit(
                    course,
                    instructor,
                    "COURSE_COMPLETION_CRITERIA_UPDATE",
                    "completion_criteria",
                    prevValues,
                    newValues);
        }

        return CourseCompletionCriteriaResponse.fromEntity(saved, certCount);
    }

    /**
     * Retrieve configured completion criteria for a course.
     */
    @Transactional(readOnly = true)
    public CourseCompletionCriteriaResponse getCompletionCriteria(Integer courseId, User currentUser) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        if (course.isDeleted()) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }

        long certCount = certificateRepository != null ? certificateRepository.countByCourseId(courseId) : 0;

        CourseCompletionCriteria criteria = completionCriteriaRepository.findByCourseId(courseId)
                .orElseGet(() -> {
                    CourseCompletionCriteria def = new CourseCompletionCriteria(course);
                    def.setLocked(certCount > 0);
                    return def;
                });

        return CourseCompletionCriteriaResponse.fromEntity(criteria, certCount);
    }

    /**
     * Reusable evaluation engine: evaluate(student, course).
     * Checks whether the learner satisfies every configured completion criterion (DR-03).
     * Exposed for both student-facing certification checks (FR-S77) and instructor reviews.
     */
    @Transactional(readOnly = true)
    public CompletionEvaluationResult evaluate(User student, Course course) {
        if (student == null) {
            throw new InvalidCredentialsException("Student is not authenticated");
        }
        if (course == null) {
            throw new IllegalArgumentException("Course cannot be null");
        }
        if (course.isDeleted()) {
            throw new ResourceNotFoundException("Course not found with id: " + course.getId());
        }

        CompletionEvaluationResult result = new CompletionEvaluationResult();
        result.setCourseId(course.getId());
        result.setCourseTitle(course.getTitle());
        result.setStudentId(student.getId());
        result.setStudentName(student.getName());
        result.setCriteriaLocked(isCriteriaLocked(course.getId()));

        // Check if certificate is already issued
        if (certificateRepository != null) {
            List<Certificate> certs = certificateRepository.findByCourseId(course.getId()).stream()
                    .filter(c -> c.getStudent() != null && student.getId().equals(c.getStudent().getId()))
                    .toList();
            if (!certs.isEmpty()) {
                Certificate cert = certs.get(0);
                result.setCertificateIssued(true);
                result.setCertificateId(cert.getId());
                result.setCertificateCode(cert.getCertificateCode());
                result.setCertificateIssuedAt(cert.getIssuedAt());
            }
        }

        // Verify active enrollment
        boolean enrolled = enrollmentRepository != null &&
                enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId());
        if (!enrolled) {
            result.addUnmetReason("Student is not enrolled in this course");
            result.setEligible(false);
            return result;
        }

        CourseCompletionCriteria criteria = completionCriteriaRepository.findByCourseId(course.getId())
                .orElseGet(() -> new CourseCompletionCriteria(course));

        double requiredLessonPct = criteria.getMinLessonCompletionPercentage() != null
                ? criteria.getMinLessonCompletionPercentage().doubleValue()
                : 100.0;
        result.setRequiredLessonCompletionPercentage(requiredLessonPct);
        result.setMinQuizPassingMarks(criteria.getMinQuizPassingMarks());

        // 1. Evaluate lesson / lecture completion
        evaluateLessonCompletion(student, course, criteria, result);

        // 2. Evaluate mandatory lessons
        evaluateMandatoryLessons(student, course, criteria, result);

        // 3. Evaluate quizzes / exams
        evaluateQuizzes(student, course, criteria, result);

        // 4. Overall eligibility determination
        result.setEligible(result.getUnmetCriteriaReasons().isEmpty());
        return result;
    }

    @Transactional(readOnly = true)
    public CompletionEvaluationResult evaluate(Integer studentId, Integer courseId) {
        if (studentId == null || courseId == null) {
            throw new IllegalArgumentException("Student ID and Course ID cannot be null");
        }
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        return evaluate(student, course);
    }

    /**
     * Issues a certificate to the student if and only if all completion criteria are satisfied (DR-03).
     * Locks the course completion criteria after the first certificate is issued.
     */
    @Transactional
    public CertificateResponse issueCertificate(User student, Course course) {
        if (student == null || course == null) {
            throw new IllegalArgumentException("Student and Course cannot be null");
        }

        // Verify criteria fulfillment
        CompletionEvaluationResult evaluation = evaluate(student, course);
        if (!evaluation.isEligible()) {
            throw new IllegalStateException(
                    "Cannot issue certificate: Completion criteria are not satisfied. Reasons: " +
                    String.join("; ", evaluation.getUnmetCriteriaReasons()));
        }

        // Prevent duplicate certificate issuance
        if (certificateRepository != null) {
            Optional<Certificate> existing = certificateRepository.findByCourseId(course.getId()).stream()
                    .filter(c -> c.getStudent() != null && student.getId().equals(c.getStudent().getId()))
                    .findFirst();
            if (existing.isPresent()) {
                return CertificateResponse.fromEntity(existing.get());
            }
        }

        // Generate unique certificate code
        String certCode = String.format("OLP-%d-%d-%s",
                course.getId(),
                student.getId(),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        Certificate cert = new Certificate(course, student, certCode);
        Certificate saved = certificateRepository != null ? certificateRepository.save(cert) : cert;

        // Lock completion criteria configuration
        CourseCompletionCriteria criteria = completionCriteriaRepository.findByCourseId(course.getId()).orElse(null);
        if (criteria != null && !criteria.isLocked()) {
            criteria.setLocked(true);
            completionCriteriaRepository.save(criteria);
        }

        // Record audit
        if (courseAuditService != null) {
            courseAuditService.recordAudit(
                    course,
                    student,
                    "CERTIFICATE_ISSUED",
                    "certificate",
                    Map.of("issued", false),
                    Map.of("issued", true, "certificateCode", certCode, "studentId", student.getId()));
        }

        return CertificateResponse.fromEntity(saved);
    }

    @Transactional
    public CertificateResponse issueCertificate(Integer studentId, Integer courseId) {
        if (studentId == null || courseId == null) {
            throw new IllegalArgumentException("Student ID and Course ID cannot be null");
        }
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        return issueCertificate(student, course);
    }

    /**
     * Checks if the completion criteria for the given course are locked.
     * Criteria lock permanently once the first certificate has been awarded (DR-03).
     */
    public boolean isCriteriaLocked(Integer courseId) {
        if (courseId == null) return false;
        if (certificateRepository != null && certificateRepository.countByCourseId(courseId) > 0) {
            return true;
        }
        return completionCriteriaRepository.findByCourseId(courseId)
                .map(CourseCompletionCriteria::isLocked)
                .orElse(false);
    }

    // ==========================================
    // Internal Evaluation Helper Methods
    // ==========================================

    private void evaluateLessonCompletion(User student, Course course,
                                          CourseCompletionCriteria criteria,
                                          CompletionEvaluationResult result) {
        List<Lesson> lessons = lessonRepository != null ? lessonRepository.findByCourseId(course.getId()) : Collections.emptyList();
        List<Lecture> lectures = lectureRepository != null ? lectureRepository.findByCourseIdOrderByOrderIndexAsc(course.getId()) : Collections.emptyList();

        int totalItems = lessons.size() + lectures.size();
        int completedCount = 0;

        if (!lessons.isEmpty() && lessonProgressRepository != null) {
            Set<Integer> lessonIds = lessons.stream().map(Lesson::getId).collect(Collectors.toSet());
            completedCount += lessonProgressRepository.countCompletedByStudentIdAndLessonIds(student.getId(), lessonIds);
        }

        if (!lectures.isEmpty() && lectureProgressRepository != null) {
            Set<Integer> lectureIds = lectures.stream().map(Lecture::getId).collect(Collectors.toSet());
            completedCount += lectureProgressRepository.countCompletedByStudentIdAndLectureIds(student.getId(), lectureIds);
        }

        result.setTotalLessons(totalItems);
        result.setCompletedLessons(completedCount);

        double actualPercentage = (totalItems == 0) ? 100.0 : ((double) completedCount / totalItems) * 100.0;
        double roundedPercentage = Math.round(actualPercentage * 10.0) / 10.0;
        result.setLessonCompletionPercentage(roundedPercentage);

        double requiredPct = criteria.getMinLessonCompletionPercentage() != null
                ? criteria.getMinLessonCompletionPercentage().doubleValue()
                : 100.0;

        if (roundedPercentage < requiredPct) {
            result.setLessonCriteriaSatisfied(false);
            result.addUnmetReason(String.format(
                    "Lesson completion criteria unmet: Completed %.1f%% of lessons (%d/%d), but %.1f%% is required",
                    roundedPercentage, completedCount, totalItems, requiredPct));
        } else {
            result.setLessonCriteriaSatisfied(true);
        }
    }

    private void evaluateMandatoryLessons(User student, Course course,
                                          CourseCompletionCriteria criteria,
                                          CompletionEvaluationResult result) {
        List<Integer> mandatoryIds = criteria.getMandatoryLessonIdList();
        if (mandatoryIds.isEmpty()) {
            result.setMandatoryLessonsSatisfied(true);
            return;
        }

        List<Lesson> allLessons = lessonRepository != null ? lessonRepository.findByCourseId(course.getId()) : Collections.emptyList();
        List<Lecture> allLectures = lectureRepository != null ? lectureRepository.findByCourseIdOrderByOrderIndexAsc(course.getId()) : Collections.emptyList();

        Map<Integer, String> itemTitles = new HashMap<>();
        for (Lesson l : allLessons) {
            itemTitles.put(l.getId(), l.getTitle());
        }
        for (Lecture lc : allLectures) {
            itemTitles.put(lc.getId(), lc.getTitle());
        }

        Set<Integer> completedLessonIds = new HashSet<>();
        if (lessonProgressRepository != null && !allLessons.isEmpty()) {
            Set<Integer> ids = allLessons.stream().map(Lesson::getId).collect(Collectors.toSet());
            completedLessonIds.addAll(lessonProgressRepository.findCompletedLessonIdsByStudentId(student.getId(), ids));
        }
        if (lectureProgressRepository != null && !allLectures.isEmpty()) {
            Set<Integer> ids = allLectures.stream().map(Lecture::getId).collect(Collectors.toSet());
            completedLessonIds.addAll(lectureProgressRepository.findCompletedLectureIdsByStudentId(student.getId(), ids));
        }

        boolean allMandatoryCompleted = true;
        for (Integer mandatoryId : mandatoryIds) {
            if (!completedLessonIds.contains(mandatoryId)) {
                allMandatoryCompleted = false;
                String title = itemTitles.getOrDefault(mandatoryId, "Item #" + mandatoryId);
                result.getMissingMandatoryLessonIds().add(mandatoryId);
                result.getMissingMandatoryLessonTitles().add(title);
                result.addUnmetReason(String.format("Mandatory curriculum item '%s' (ID: %d) has not been completed", title, mandatoryId));
            }
        }
        result.setMandatoryLessonsSatisfied(allMandatoryCompleted);
    }

    private void evaluateQuizzes(User student, Course course,
                                 CourseCompletionCriteria criteria,
                                 CompletionEvaluationResult result) {
        List<Quiz> quizzes = quizRepository != null
                ? quizRepository.findByCourseIdAndStatusOrderByCreatedAtDesc(course.getId(), QuizStatus.PUBLISHED)
                : Collections.emptyList();

        if (quizzes.isEmpty()) {
            result.setTotalQuizzes(0);
            result.setPassedQuizzes(0);
            result.setQuizCriteriaSatisfied(true);
            result.setMandatoryQuizzesSatisfied(true);
            return;
        }

        result.setTotalQuizzes(quizzes.size());
        int passedCount = 0;
        boolean allPassed = true;

        List<Integer> mandatoryQuizIds = criteria.getMandatoryQuizIdList();
        Set<Integer> passedQuizIds = new HashSet<>();

        for (Quiz quiz : quizzes) {
            List<QuizAttempt> attempts = quizAttemptRepository != null
                    ? quizAttemptRepository.findByQuizIdAndStudentIdAndStatusOrderByAttemptNumberDesc(
                            quiz.getId(), student.getId(), QuizAttemptStatus.SUBMITTED)
                    : Collections.emptyList();

            BigDecimal bestScore = attempts.stream()
                    .map(QuizAttempt::getObtainedMarks)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            boolean passedThisQuiz = true;

            // 1. Quiz's own passing marks
            if (quiz.getPassingMarks() != null && bestScore.compareTo(quiz.getPassingMarks()) < 0) {
                passedThisQuiz = false;
            }

            // 2. Course-level minimum quiz passing marks
            if (criteria.getMinQuizPassingMarks() != null && bestScore.compareTo(criteria.getMinQuizPassingMarks()) < 0) {
                passedThisQuiz = false;
                result.addUnmetReason(String.format(
                        "Quiz '%s' best score %.2f is below the course minimum passing mark of %.2f",
                        quiz.getTitle(), bestScore, criteria.getMinQuizPassingMarks()));
            }

            // 3. Course-level minimum quiz passing percentage
            if (criteria.getMinQuizPassingPercentage() != null && quiz.getTotalMarks() != null && quiz.getTotalMarks().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pct = bestScore.multiply(BigDecimal.valueOf(100)).divide(quiz.getTotalMarks(), 2, RoundingMode.HALF_UP);
                if (pct.compareTo(criteria.getMinQuizPassingPercentage()) < 0) {
                    passedThisQuiz = false;
                    result.addUnmetReason(String.format(
                            "Quiz '%s' score %.1f%% is below the course minimum passing percentage of %.1f%%",
                            quiz.getTitle(), pct.doubleValue(), criteria.getMinQuizPassingPercentage().doubleValue()));
                }
            }

            if (passedThisQuiz) {
                passedCount++;
                passedQuizIds.add(quiz.getId());
            } else {
                allPassed = false;
                if (criteria.isRequireAllQuizzesPassed()) {
                    result.addUnmetReason(String.format(
                            "Course requires all quizzes to be passed, but quiz '%s' (ID: %d) has not been passed",
                            quiz.getTitle(), quiz.getId()));
                }
            }
        }

        result.setPassedQuizzes(passedCount);
        result.setQuizCriteriaSatisfied(!criteria.isRequireAllQuizzesPassed() || allPassed);

        // Evaluate mandatory quizzes
        boolean allMandatoryQuizzesPassed = true;
        if (!mandatoryQuizIds.isEmpty()) {
            Map<Integer, String> quizTitles = quizzes.stream().collect(Collectors.toMap(Quiz::getId, Quiz::getTitle, (a, b) -> a));
            for (Integer mId : mandatoryQuizIds) {
                if (!passedQuizIds.contains(mId)) {
                    allMandatoryQuizzesPassed = false;
                    String qTitle = quizTitles.getOrDefault(mId, "Quiz #" + mId);
                    result.getMissingMandatoryQuizIds().add(mId);
                    result.getMissingMandatoryQuizTitles().add(qTitle);
                    result.addUnmetReason(String.format("Mandatory assessment '%s' (ID: %d) has not been passed", qTitle, mId));
                }
            }
        }
        result.setMandatoryQuizzesSatisfied(allMandatoryQuizzesPassed);
    }

    private void verifyInstructorAuthorization(Course course, User instructor) {
        if ("admin".equalsIgnoreCase(instructor.getRole())) {
            return;
        }
        boolean isOwner = course.getInstructor() != null && instructor.getId().equals(course.getInstructor().getId());
        boolean isAssociated = courseInstructorRepository != null &&
                courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), instructor.getId());

        if (!isOwner && !isAssociated) {
            throw new AccessDeniedException("Access denied: You are not authorized to configure completion criteria for this course");
        }
    }

    private void validateCriteriaRequest(Integer courseId, CourseCompletionCriteriaRequest request) {
        if (request.getMinLessonCompletionPercentage() != null) {
            if (request.getMinLessonCompletionPercentage().compareTo(BigDecimal.ZERO) < 0 ||
                request.getMinLessonCompletionPercentage().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Minimum lesson completion percentage must be between 0% and 100%");
            }
        }
        if (request.getMinQuizPassingMarks() != null && request.getMinQuizPassingMarks().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum quiz passing marks must be non-negative");
        }
        if (request.getMinQuizPassingPercentage() != null) {
            if (request.getMinQuizPassingPercentage().compareTo(BigDecimal.ZERO) < 0 ||
                request.getMinQuizPassingPercentage().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Minimum quiz passing percentage must be between 0% and 100%");
            }
        }

        // Validate mandatory quiz IDs belong to the course
        if (request.getMandatoryQuizIds() != null && !request.getMandatoryQuizIds().isEmpty() && quizRepository != null) {
            List<Quiz> courseQuizzes = quizRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
            Set<Integer> validQuizIds = courseQuizzes.stream().map(Quiz::getId).collect(Collectors.toSet());
            for (Integer mQuizId : request.getMandatoryQuizIds()) {
                if (mQuizId != null && !validQuizIds.contains(mQuizId)) {
                    throw new IllegalArgumentException("Mandatory quiz with ID " + mQuizId + " does not belong to course " + courseId);
                }
            }
        }
    }
}
