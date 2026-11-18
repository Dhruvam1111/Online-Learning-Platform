package com.olp.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.olp.dto.AssessmentPerformanceSummaryDto;
import com.olp.dto.CourseAssessmentsOverviewResponse;
import com.olp.dto.PageResponse;
import com.olp.dto.StudentAssessmentAttemptDto;
import com.olp.dto.StudentAssessmentPerformanceDto;
import com.olp.dto.StudentCoursePerformanceReport;
import com.olp.entity.Course;
import com.olp.entity.Enrollment;
import com.olp.entity.Exam;
import com.olp.entity.ExamAttempt;
import com.olp.entity.Quiz;
import com.olp.entity.QuizAttempt;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.ExamAttemptRepository;
import com.olp.repository.ExamRepository;
import com.olp.repository.QuizAttemptRepository;
import com.olp.repository.QuizRepository;
import com.olp.repository.UserRepository;

/**
 * Step 57 / FR-I44: Student Performance View Service.
 * Provides aggregate views (average, highest, lowest, pass rate) and individual views (attempts and marks)
 * across quizzes and examinations using published results only.
 * Optimized for performance (NFR-05) and strictly scoped to course instructors (DR-05).
 */
@Service
public class InstructorPerformanceService {

    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public InstructorPerformanceService(
            CourseRepository courseRepository,
            CourseInstructorRepository courseInstructorRepository,
            QuizRepository quizRepository,
            QuizAttemptRepository quizAttemptRepository,
            ExamRepository examRepository,
            ExamAttemptRepository examAttemptRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.examRepository = examRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    public User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public CourseAssessmentsOverviewResponse getCourseAssessmentsOverview(Integer courseId, User currentUser) {
        Course course = verifyCourseAccess(courseId, currentUser);

        List<Quiz> quizzes = quizRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
        List<Exam> exams = examRepository.findByCourseId(courseId);

        List<AssessmentPerformanceSummaryDto> summaries = new ArrayList<>();
        double passRateSum = 0.0;
        int passRateCount = 0;

        for (Quiz q : quizzes) {
            AssessmentPerformanceSummaryDto summary = getQuizPerformanceSummary(q);
            summaries.add(summary);
            if (summary.getTotalPublishedAttempts() > 0 && summary.getPassRatePercentage() != null) {
                passRateSum += summary.getPassRatePercentage();
                passRateCount++;
            }
        }

        for (Exam e : exams) {
            AssessmentPerformanceSummaryDto summary = getExamPerformanceSummary(e);
            summaries.add(summary);
            if (summary.getTotalPublishedAttempts() > 0 && summary.getPassRatePercentage() != null) {
                passRateSum += summary.getPassRatePercentage();
                passRateCount++;
            }
        }

        Double overallPassRate = passRateCount > 0
                ? Math.round((passRateSum / passRateCount) * 10.0) / 10.0
                : null;

        return new CourseAssessmentsOverviewResponse(
                course.getId(),
                course.getTitle(),
                summaries.size(),
                overallPassRate,
                summaries
        );
    }

    @Transactional(readOnly = true)
    public AssessmentPerformanceSummaryDto getAssessmentSummary(
            Integer courseId,
            String assessmentType,
            Integer assessmentId,
            User currentUser) {

        verifyCourseAccess(courseId, currentUser);

        if ("EXAM".equalsIgnoreCase(assessmentType)) {
            Exam exam = examRepository.findByIdAndCourseId(assessmentId, courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + assessmentId));
            return getExamPerformanceSummary(exam);
        } else {
            Quiz quiz = quizRepository.findById(assessmentId)
                    .filter(q -> q.getCourse() != null && q.getCourse().getId().equals(courseId))
                    .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + assessmentId));
            return getQuizPerformanceSummary(quiz);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentAssessmentPerformanceDto> getStudentsPerformanceForAssessment(
            Integer courseId,
            String assessmentType,
            Integer assessmentId,
            String search,
            int page,
            int size,
            User currentUser) {

        verifyCourseAccess(courseId, currentUser);

        List<StudentAssessmentPerformanceDto> allStudentStats;
        if ("EXAM".equalsIgnoreCase(assessmentType)) {
            Exam exam = examRepository.findByIdAndCourseId(assessmentId, courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + assessmentId));
            allStudentStats = buildStudentStatsForExam(exam);
        } else {
            Quiz quiz = quizRepository.findById(assessmentId)
                    .filter(q -> q.getCourse() != null && q.getCourse().getId().equals(courseId))
                    .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + assessmentId));
            allStudentStats = buildStudentStatsForQuiz(quiz);
        }

        if (search != null && !search.trim().isEmpty()) {
            String term = search.trim().toLowerCase();
            allStudentStats = allStudentStats.stream()
                    .filter(s -> (s.getStudentName() != null && s.getStudentName().toLowerCase().contains(term))
                            || (s.getStudentEmail() != null && s.getStudentEmail().toLowerCase().contains(term)))
                    .collect(Collectors.toList());
        }

        allStudentStats.sort(Comparator.comparing(
                StudentAssessmentPerformanceDto::getBestPercentage,
                Comparator.nullsFirst(Double::compareTo)
        ).reversed());

        int validPage = Math.max(page, 0);
        int validSize = (size <= 0) ? 10 : Math.min(size, 100);
        int totalElements = allStudentStats.size();
        int totalPages = (int) Math.ceil((double) totalElements / validSize);
        int fromIndex = validPage * validSize;

        List<StudentAssessmentPerformanceDto> pageContent;
        if (fromIndex >= totalElements) {
            pageContent = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + validSize, totalElements);
            pageContent = allStudentStats.subList(fromIndex, toIndex);
        }

        boolean isLast = (validPage + 1) >= totalPages || totalPages == 0;

        return new PageResponse<>(
                pageContent,
                validPage,
                validSize,
                totalElements,
                totalPages,
                isLast
        );
    }

    @Transactional(readOnly = true)
    public StudentAssessmentPerformanceDto getStudentAttemptsForAssessment(
            Integer courseId,
            String assessmentType,
            Integer assessmentId,
            Integer studentId,
            User currentUser) {

        verifyCourseAccess(courseId, currentUser);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        if ("EXAM".equalsIgnoreCase(assessmentType)) {
            Exam exam = examRepository.findByIdAndCourseId(assessmentId, courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + assessmentId));
            return buildIndividualStudentExamReport(exam, student);
        } else {
            Quiz quiz = quizRepository.findById(assessmentId)
                    .filter(q -> q.getCourse() != null && q.getCourse().getId().equals(courseId))
                    .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + assessmentId));
            return buildIndividualStudentQuizReport(quiz, student);
        }
    }

    @Transactional(readOnly = true)
    public StudentCoursePerformanceReport getStudentCoursePerformanceReport(
            Integer courseId,
            Integer studentId,
            User currentUser) {

        Course course = verifyCourseAccess(courseId, currentUser);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Student is not enrolled in course id: " + courseId));

        List<Quiz> quizzes = quizRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
        List<Exam> exams = examRepository.findByCourseId(courseId);

        List<StudentAssessmentPerformanceDto> assessmentList = new ArrayList<>();
        int quizzesAttempted = 0;
        int quizzesPassed = 0;
        int examsAttempted = 0;
        int examsPassed = 0;
        double percentageSum = 0.0;
        int scoredCount = 0;

        for (Quiz q : quizzes) {
            StudentAssessmentPerformanceDto dto = buildIndividualStudentQuizReport(q, student);
            if (dto.getTotalAttemptsCount() > 0) {
                quizzesAttempted++;
                if (dto.isPassed()) {
                    quizzesPassed++;
                }
                if (dto.getBestPercentage() != null) {
                    percentageSum += dto.getBestPercentage();
                    scoredCount++;
                }
                assessmentList.add(dto);
            }
        }

        for (Exam e : exams) {
            StudentAssessmentPerformanceDto dto = buildIndividualStudentExamReport(e, student);
            if (dto.getTotalAttemptsCount() > 0) {
                examsAttempted++;
                if (dto.isPassed()) {
                    examsPassed++;
                }
                if (dto.getBestPercentage() != null) {
                    percentageSum += dto.getBestPercentage();
                    scoredCount++;
                }
                assessmentList.add(dto);
            }
        }

        Double overallAverage = scoredCount > 0
                ? Math.round((percentageSum / scoredCount) * 10.0) / 10.0
                : null;

        return new StudentCoursePerformanceReport(
                course.getId(),
                course.getTitle(),
                student.getId(),
                student.getName(),
                student.getEmail(),
                enrollment.getEnrolledAt(),
                overallAverage,
                quizzesAttempted,
                quizzesPassed,
                examsAttempted,
                examsPassed,
                assessmentList
        );
    }

    private Course verifyCourseAccess(Integer courseId, User currentUser) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        boolean isOwner = course.getInstructor() != null
                && course.getInstructor().getId().equals(currentUser.getId());
        boolean isCoInstructor = courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, currentUser.getId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole())
                || "ROLE_ADMIN".equalsIgnoreCase(currentUser.getRole());

        if (!isOwner && !isCoInstructor && !isAdmin) {
            throw new AccessDeniedException("Access denied: you can only view performance for your own courses (DR-05)");
        }
        return course;
    }

    private AssessmentPerformanceSummaryDto getQuizPerformanceSummary(Quiz quiz) {
        double maxMarks = (quiz.getTotalMarks() != null && quiz.getTotalMarks().doubleValue() > 0)
                ? quiz.getTotalMarks().doubleValue()
                : 100.0;
        double passingMarks = (quiz.getPassingMarks() != null)
                ? quiz.getPassingMarks().doubleValue()
                : (maxMarks * 0.5);

        List<QuizAttempt> publishedAttempts = quizAttemptRepository.findPublishedAttemptsByQuizId(quiz.getId());
        return computeAggregateSummary(
                quiz.getId(),
                "QUIZ",
                quiz.getTitle(),
                quiz.getCourse() != null ? quiz.getCourse().getId() : null,
                maxMarks,
                passingMarks,
                publishedAttempts.stream().map(a -> new AttemptMarksRecord(
                        a.getStudent() != null ? a.getStudent().getId() : null,
                        a.getObtainedMarks() != null ? a.getObtainedMarks().doubleValue() : null
                )).collect(Collectors.toList())
        );
    }

    private AssessmentPerformanceSummaryDto getExamPerformanceSummary(Exam exam) {
        double maxMarks = (exam.getMaxMarks() != null && exam.getMaxMarks().doubleValue() > 0)
                ? exam.getMaxMarks().doubleValue()
                : 100.0;
        double passingMarks = (exam.getPassingMarks() != null)
                ? exam.getPassingMarks().doubleValue()
                : (maxMarks * 0.5);

        List<ExamAttempt> publishedAttempts = examAttemptRepository.findPublishedAttemptsByExamId(exam.getId());
        return computeAggregateSummary(
                exam.getId(),
                "EXAM",
                exam.getTitle(),
                exam.getCourse() != null ? exam.getCourse().getId() : null,
                maxMarks,
                passingMarks,
                publishedAttempts.stream().map(a -> new AttemptMarksRecord(
                        a.getStudent() != null ? a.getStudent().getId() : null,
                        a.getObtainedMarks() != null ? a.getObtainedMarks().doubleValue() : null
                )).collect(Collectors.toList())
        );
    }

    private AssessmentPerformanceSummaryDto computeAggregateSummary(
            Integer assessmentId,
            String assessmentType,
            String title,
            Integer courseId,
            double maxMarks,
            double passingMarks,
            List<AttemptMarksRecord> records) {

        long totalAttempts = records.size();
        if (totalAttempts == 0) {
            return new AssessmentPerformanceSummaryDto(
                    assessmentId, assessmentType, title, courseId, maxMarks, passingMarks,
                    false, 0, 0, null, null, null, null, null, null, 0, 0, null, buildEmptyDistribution()
            );
        }

        Map<Integer, Double> studentBestMarks = new HashMap<>();
        double sumMarks = 0.0;
        double maxObtained = Double.NEGATIVE_INFINITY;
        double minObtained = Double.POSITIVE_INFINITY;
        long validMarksCount = 0;

        Map<String, Long> distribution = buildEmptyDistribution();

        for (AttemptMarksRecord r : records) {
            Double marks = r.marks();
            if (marks != null) {
                sumMarks += marks;
                validMarksCount++;
                if (marks > maxObtained) {
                    maxObtained = marks;
                }
                if (marks < minObtained) {
                    minObtained = marks;
                }

                double pct = maxMarks > 0 ? (marks / maxMarks) * 100.0 : 0.0;
                bucketScore(distribution, pct);

                if (r.studentId() != null) {
                    Double currentBest = studentBestMarks.get(r.studentId());
                    if (currentBest == null || marks > currentBest) {
                        studentBestMarks.put(r.studentId(), marks);
                    }
                }
            }
        }

        long uniqueStudents = studentBestMarks.size();
        Double averageMarks = validMarksCount > 0 ? Math.round((sumMarks / validMarksCount) * 10.0) / 10.0 : null;
        Double averagePercentage = (averageMarks != null && maxMarks > 0)
                ? Math.round((averageMarks / maxMarks) * 1000.0) / 10.0
                : null;
        Double highestMarks = validMarksCount > 0 ? maxObtained : null;
        Double highestPercentage = (highestMarks != null && maxMarks > 0)
                ? Math.round((highestMarks / maxMarks) * 1000.0) / 10.0
                : null;
        Double lowestMarks = validMarksCount > 0 ? minObtained : null;
        Double lowestPercentage = (lowestMarks != null && maxMarks > 0)
                ? Math.round((lowestMarks / maxMarks) * 1000.0) / 10.0
                : null;

        long passedCount = studentBestMarks.values().stream()
                .filter(m -> m >= passingMarks)
                .count();
        long failedCount = uniqueStudents - passedCount;
        Double passRate = uniqueStudents > 0
                ? Math.round(((double) passedCount / uniqueStudents) * 1000.0) / 10.0
                : null;

        return new AssessmentPerformanceSummaryDto(
                assessmentId,
                assessmentType,
                title,
                courseId,
                maxMarks,
                passingMarks,
                true,
                totalAttempts,
                uniqueStudents,
                averageMarks,
                averagePercentage,
                highestMarks,
                highestPercentage,
                lowestMarks,
                lowestPercentage,
                passedCount,
                failedCount,
                passRate,
                distribution
        );
    }

    private List<StudentAssessmentPerformanceDto> buildStudentStatsForQuiz(Quiz quiz) {
        double maxMarks = (quiz.getTotalMarks() != null && quiz.getTotalMarks().doubleValue() > 0)
                ? quiz.getTotalMarks().doubleValue()
                : 100.0;
        double passingMarks = (quiz.getPassingMarks() != null)
                ? quiz.getPassingMarks().doubleValue()
                : (maxMarks * 0.5);

        List<QuizAttempt> attempts = quizAttemptRepository.findPublishedAttemptsByQuizId(quiz.getId());
        Map<Integer, List<QuizAttempt>> byStudent = attempts.stream()
                .filter(a -> a.getStudent() != null)
                .collect(Collectors.groupingBy(a -> a.getStudent().getId()));

        List<StudentAssessmentPerformanceDto> result = new ArrayList<>();
        for (Map.Entry<Integer, List<QuizAttempt>> entry : byStudent.entrySet()) {
            List<QuizAttempt> studentAttempts = entry.getValue();
            studentAttempts.sort(Comparator.comparing(QuizAttempt::getAttemptNumber));

            User student = studentAttempts.get(0).getStudent();
            List<StudentAssessmentAttemptDto> attemptDtos = studentAttempts.stream()
                    .map(a -> mapQuizAttemptToDto(a, maxMarks, passingMarks))
                    .collect(Collectors.toList());

            Double bestMarks = attemptDtos.stream()
                    .map(StudentAssessmentAttemptDto::getObtainedMarks)
                    .filter(java.util.Objects::nonNull)
                    .max(Double::compareTo)
                    .orElse(null);

            Double bestPercentage = (bestMarks != null && maxMarks > 0)
                    ? Math.round((bestMarks / maxMarks) * 1000.0) / 10.0
                    : null;

            StudentAssessmentAttemptDto latestAttempt = attemptDtos.get(attemptDtos.size() - 1);
            Double latestMarks = latestAttempt.getObtainedMarks();
            Double latestPercentage = latestAttempt.getPercentage();
            boolean passed = bestMarks != null && bestMarks >= passingMarks;

            result.add(new StudentAssessmentPerformanceDto(
                    student.getId(),
                    student.getName(),
                    student.getEmail(),
                    quiz.getId(),
                    "QUIZ",
                    quiz.getTitle(),
                    maxMarks,
                    passingMarks,
                    bestMarks,
                    bestPercentage,
                    latestMarks,
                    latestPercentage,
                    passed,
                    attemptDtos.size(),
                    latestAttempt.getSubmittedAt(),
                    attemptDtos
            ));
        }

        return result;
    }

    private List<StudentAssessmentPerformanceDto> buildStudentStatsForExam(Exam exam) {
        double maxMarks = (exam.getMaxMarks() != null && exam.getMaxMarks().doubleValue() > 0)
                ? exam.getMaxMarks().doubleValue()
                : 100.0;
        double passingMarks = (exam.getPassingMarks() != null)
                ? exam.getPassingMarks().doubleValue()
                : (maxMarks * 0.5);

        List<ExamAttempt> attempts = examAttemptRepository.findPublishedAttemptsByExamId(exam.getId());
        Map<Integer, List<ExamAttempt>> byStudent = attempts.stream()
                .filter(a -> a.getStudent() != null)
                .collect(Collectors.groupingBy(a -> a.getStudent().getId()));

        List<StudentAssessmentPerformanceDto> result = new ArrayList<>();
        for (Map.Entry<Integer, List<ExamAttempt>> entry : byStudent.entrySet()) {
            List<ExamAttempt> studentAttempts = entry.getValue();
            studentAttempts.sort(Comparator.comparing(ExamAttempt::getAttemptNumber));

            User student = studentAttempts.get(0).getStudent();
            List<StudentAssessmentAttemptDto> attemptDtos = studentAttempts.stream()
                    .map(a -> mapExamAttemptToDto(a, maxMarks, passingMarks))
                    .collect(Collectors.toList());

            Double bestMarks = attemptDtos.stream()
                    .map(StudentAssessmentAttemptDto::getObtainedMarks)
                    .filter(java.util.Objects::nonNull)
                    .max(Double::compareTo)
                    .orElse(null);

            Double bestPercentage = (bestMarks != null && maxMarks > 0)
                    ? Math.round((bestMarks / maxMarks) * 1000.0) / 10.0
                    : null;

            StudentAssessmentAttemptDto latestAttempt = attemptDtos.get(attemptDtos.size() - 1);
            Double latestMarks = latestAttempt.getObtainedMarks();
            Double latestPercentage = latestAttempt.getPercentage();
            boolean passed = bestMarks != null && bestMarks >= passingMarks;

            result.add(new StudentAssessmentPerformanceDto(
                    student.getId(),
                    student.getName(),
                    student.getEmail(),
                    exam.getId(),
                    "EXAM",
                    exam.getTitle(),
                    maxMarks,
                    passingMarks,
                    bestMarks,
                    bestPercentage,
                    latestMarks,
                    latestPercentage,
                    passed,
                    attemptDtos.size(),
                    latestAttempt.getSubmittedAt(),
                    attemptDtos
            ));
        }

        return result;
    }

    private StudentAssessmentPerformanceDto buildIndividualStudentQuizReport(Quiz quiz, User student) {
        double maxMarks = (quiz.getTotalMarks() != null && quiz.getTotalMarks().doubleValue() > 0)
                ? quiz.getTotalMarks().doubleValue()
                : 100.0;
        double passingMarks = (quiz.getPassingMarks() != null)
                ? quiz.getPassingMarks().doubleValue()
                : (maxMarks * 0.5);

        List<QuizAttempt> attempts = quizAttemptRepository.findPublishedAttemptsByQuizIdAndStudentId(quiz.getId(), student.getId());
        List<StudentAssessmentAttemptDto> attemptDtos = attempts.stream()
                .map(a -> mapQuizAttemptToDto(a, maxMarks, passingMarks))
                .collect(Collectors.toList());

        Double bestMarks = attemptDtos.stream()
                .map(StudentAssessmentAttemptDto::getObtainedMarks)
                .filter(java.util.Objects::nonNull)
                .max(Double::compareTo)
                .orElse(null);

        Double bestPercentage = (bestMarks != null && maxMarks > 0)
                ? Math.round((bestMarks / maxMarks) * 1000.0) / 10.0
                : null;

        Double latestMarks = attemptDtos.isEmpty() ? null : attemptDtos.get(attemptDtos.size() - 1).getObtainedMarks();
        Double latestPercentage = attemptDtos.isEmpty() ? null : attemptDtos.get(attemptDtos.size() - 1).getPercentage();
        LocalDateTime latestSubmittedAt = attemptDtos.isEmpty() ? null : attemptDtos.get(attemptDtos.size() - 1).getSubmittedAt();
        boolean passed = bestMarks != null && bestMarks >= passingMarks;

        return new StudentAssessmentPerformanceDto(
                student.getId(),
                student.getName(),
                student.getEmail(),
                quiz.getId(),
                "QUIZ",
                quiz.getTitle(),
                maxMarks,
                passingMarks,
                bestMarks,
                bestPercentage,
                latestMarks,
                latestPercentage,
                passed,
                attemptDtos.size(),
                latestSubmittedAt,
                attemptDtos
        );
    }

    private StudentAssessmentPerformanceDto buildIndividualStudentExamReport(Exam exam, User student) {
        double maxMarks = (exam.getMaxMarks() != null && exam.getMaxMarks().doubleValue() > 0)
                ? exam.getMaxMarks().doubleValue()
                : 100.0;
        double passingMarks = (exam.getPassingMarks() != null)
                ? exam.getPassingMarks().doubleValue()
                : (maxMarks * 0.5);

        List<ExamAttempt> attempts = examAttemptRepository.findPublishedAttemptsByExamIdAndStudentId(exam.getId(), student.getId());
        List<StudentAssessmentAttemptDto> attemptDtos = attempts.stream()
                .map(a -> mapExamAttemptToDto(a, maxMarks, passingMarks))
                .collect(Collectors.toList());

        Double bestMarks = attemptDtos.stream()
                .map(StudentAssessmentAttemptDto::getObtainedMarks)
                .filter(java.util.Objects::nonNull)
                .max(Double::compareTo)
                .orElse(null);

        Double bestPercentage = (bestMarks != null && maxMarks > 0)
                ? Math.round((bestMarks / maxMarks) * 1000.0) / 10.0
                : null;

        Double latestMarks = attemptDtos.isEmpty() ? null : attemptDtos.get(attemptDtos.size() - 1).getObtainedMarks();
        Double latestPercentage = attemptDtos.isEmpty() ? null : attemptDtos.get(attemptDtos.size() - 1).getPercentage();
        LocalDateTime latestSubmittedAt = attemptDtos.isEmpty() ? null : attemptDtos.get(attemptDtos.size() - 1).getSubmittedAt();
        boolean passed = bestMarks != null && bestMarks >= passingMarks;

        return new StudentAssessmentPerformanceDto(
                student.getId(),
                student.getName(),
                student.getEmail(),
                exam.getId(),
                "EXAM",
                exam.getTitle(),
                maxMarks,
                passingMarks,
                bestMarks,
                bestPercentage,
                latestMarks,
                latestPercentage,
                passed,
                attemptDtos.size(),
                latestSubmittedAt,
                attemptDtos
        );
    }

    private StudentAssessmentAttemptDto mapQuizAttemptToDto(QuizAttempt a, double maxMarks, double passingMarks) {
        Double marks = a.getObtainedMarks() != null ? a.getObtainedMarks().doubleValue() : null;
        Double pct = (marks != null && maxMarks > 0) ? Math.round((marks / maxMarks) * 1000.0) / 10.0 : null;
        boolean passed = marks != null && marks >= passingMarks;

        return new StudentAssessmentAttemptDto(
                a.getId(),
                a.getAttemptNumber() != null ? a.getAttemptNumber() : 1,
                marks,
                maxMarks,
                pct,
                passed,
                a.getStatus() != null ? a.getStatus().name() : "SUBMITTED",
                a.isPublished(),
                a.getSubmittedAt() != null ? a.getSubmittedAt() : a.getUpdatedAt(),
                a.getOverallFeedback()
        );
    }

    private StudentAssessmentAttemptDto mapExamAttemptToDto(ExamAttempt a, double maxMarks, double passingMarks) {
        Double marks = a.getObtainedMarks() != null ? a.getObtainedMarks().doubleValue() : null;
        Double pct = (marks != null && maxMarks > 0) ? Math.round((marks / maxMarks) * 1000.0) / 10.0 : null;
        boolean passed = marks != null && marks >= passingMarks;

        return new StudentAssessmentAttemptDto(
                a.getId(),
                a.getAttemptNumber() != null ? a.getAttemptNumber() : 1,
                marks,
                maxMarks,
                pct,
                passed,
                a.getStatus() != null ? a.getStatus() : "SUBMITTED",
                Boolean.TRUE.equals(a.getIsPublished()),
                a.getSubmittedAt() != null ? a.getSubmittedAt() : a.getUpdatedAt(),
                a.getReviewNotes()
        );
    }

    private void bucketScore(Map<String, Long> dist, double pct) {
        if (pct <= 20.0) {
            dist.put("0-20%", dist.get("0-20%") + 1L);
        } else if (pct <= 40.0) {
            dist.put("21-40%", dist.get("21-40%") + 1L);
        } else if (pct <= 60.0) {
            dist.put("41-60%", dist.get("41-60%") + 1L);
        } else if (pct <= 80.0) {
            dist.put("61-80%", dist.get("61-80%") + 1L);
        } else {
            dist.put("81-100%", dist.get("81-100%") + 1L);
        }
    }

    private Map<String, Long> buildEmptyDistribution() {
        Map<String, Long> dist = new LinkedHashMap<>();
        dist.put("0-20%", 0L);
        dist.put("21-40%", 0L);
        dist.put("41-60%", 0L);
        dist.put("61-80%", 0L);
        dist.put("81-100%", 0L);
        return dist;
    }

    private record AttemptMarksRecord(Integer studentId, Double marks) {}
}
