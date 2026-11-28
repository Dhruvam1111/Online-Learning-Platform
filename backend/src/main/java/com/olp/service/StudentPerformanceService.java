package com.olp.service;

import com.olp.dto.CourseProgressResponse;
import com.olp.dto.StudentPerformanceDtos.*;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** FR-S46..FR-S51. No writes, no new database tables. */
@Service
public class StudentPerformanceService {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final double PRACTICE_THRESHOLD = 60.0;
    private final StudentPerformanceReadRepository read;
    private final UserRepository users;
    private final CourseProgressService progress;
    private final QuizAttemptAnswerRepository quizAnswers;
    private final ExamAttemptAnswerRepository examAnswers;
    private final ExamQuestionRepository examQuestions;

    public StudentPerformanceService(StudentPerformanceReadRepository read, UserRepository users,
                                     CourseProgressService progress,
                                     QuizAttemptAnswerRepository quizAnswers,
                                     ExamAttemptAnswerRepository examAnswers,
                                     ExamQuestionRepository examQuestions) {
        this.read=read; this.users=users; this.progress=progress;
        this.quizAnswers=quizAnswers; this.examAnswers=examAnswers; this.examQuestions=examQuestions;
    }

    public User resolveStudent(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null)
            throw new InvalidCredentialsException("Student authentication is required");
        User student;
        if (auth.getPrincipal() instanceof User u) student = u;
        else {
            String email = auth.getName();
            student = users.findByEmail(email).orElseThrow(() ->
                    new InvalidCredentialsException("Authenticated student not found"));
        }
        if (!"STUDENT".equalsIgnoreCase(student.getRole()) || student.getId() == null)
            throw new AccessDeniedException("Student role is required");
        return student;
    }

    @Transactional(readOnly = true)
    public List<Result> history(User student, Integer courseId) {
        Integer id = requireStudent(student);
        if (courseId != null) requireEnrollment(id, courseId);
        List<Result> result = new ArrayList<>();
        for (ExamAttempt a : read.exams(id)) {
            Exam exam = a.getExam();
            if (!visibleExam(a)) continue;
            Result row = new Result(a.getId(), exam.getCourse().getId(), exam.getCourse().getTitle(),
                    "EXAM", exam.getId(), exam.getTitle(), a.getObtainedMarks(),
                    a.getTotalMarks() != null && a.getTotalMarks().signum() > 0 ? a.getTotalMarks() : exam.getMaxMarks(),
                    percent(a.getObtainedMarks(), a.getTotalMarks() != null && a.getTotalMarks().signum() > 0
                            ? a.getTotalMarks() : exam.getMaxMarks()), date(a.getSubmittedAt(), a.getPublishedAt()));
            if (courseId == null || courseId.equals(row.courseId())) result.add(row);
        }
        for (QuizAttempt a : read.quizzes(id)) {
            Quiz quiz = a.getQuiz();
            if (!visibleQuiz(a)) continue;
            Result row = new Result(a.getId(), quiz.getCourse().getId(), quiz.getCourse().getTitle(),
                    "QUIZ", quiz.getId(), quiz.getTitle(), a.getObtainedMarks(), quiz.getTotalMarks(),
                    percent(a.getObtainedMarks(), quiz.getTotalMarks()), date(a.getSubmittedAt(), a.getPublishedAt()));
            if (courseId == null || courseId.equals(row.courseId())) result.add(row);
        }
        for (AssignmentSubmission a : read.assignments(id)) {
            Assignment assignment = a.getAssignment();
            if (!visibleAssignment(a)) continue;
            Result row = new Result(a.getId(), assignment.getCourse().getId(), assignment.getCourse().getTitle(),
                    "ASSIGNMENT", assignment.getId(), assignment.getTitle(), a.getAwardedMarks(),
                    assignment.getMaxMarks(), percent(a.getAwardedMarks(), assignment.getMaxMarks()),
                    date(a.getSubmittedAt(), a.getPublishedAt()));
            if (courseId == null || courseId.equals(row.courseId())) result.add(row);
        }
        result.sort(Comparator.comparing(Result::assessedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(Result::attemptId, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
        return result;
    }

    @Transactional(readOnly = true)
    public Overview overview(User student) {
        Integer id = requireStudent(student);
        List<Enrollment> enrolled = read.enrollments(id);
        List<Result> all = history(student, null);
        List<CourseSummary> courses = new ArrayList<>();
        for (Enrollment enrollment : enrolled) {
            Course course = enrollment.getCourse();
            if (course == null) continue;
            CourseProgressResponse p = progress.getCourseProgress(id, course.getId());
            List<Result> rows = all.stream().filter(r -> course.getId().equals(r.courseId())).toList();
            courses.add(new CourseSummary(course.getId(), course.getTitle(), p.getPercentage(),
                    p.getCompletedCount(), p.getTotalCount(), rows.size(), average(rows)));
        }
        Double completionAverage = courses.isEmpty() ? null : courses.stream()
                .mapToDouble(CourseSummary::lectureCompletionPercentage).average().orElse(0);
        return new Overview(courses.size(), all.size(), average(all), completionAverage, courses);
    }

    @Transactional(readOnly = true)
    public List<Topic> topics(User student, Integer courseId) {
        Integer studentId = requireStudent(student);
        if (courseId != null) requireEnrollment(studentId, courseId);
        Map<String, TopicAccumulator> sums = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (QuizAttempt attempt : read.quizzes(studentId)) {
            if (!visibleQuiz(attempt) || (courseId != null &&
                    !courseId.equals(attempt.getQuiz().getCourse().getId()))) continue;
            for (QuizAttemptAnswer answer : quizAnswers.findByAttemptId(attempt.getId())) {
                if (answer.getQuestion() == null || answer.getAwardedMarks() == null) continue;
                add(sums, answer.getQuestion().getTopic(), answer.getAwardedMarks(), answer.getMaxMarks());
            }
        }
        for (ExamAttempt attempt : read.exams(studentId)) {
            if (!visibleExam(attempt) || (courseId != null &&
                    !courseId.equals(attempt.getExam().getCourse().getId()))) continue;
            for (ExamAttemptAnswer answer : examAnswers.findAnswers(attempt.getId())) {
                if (answer.getQuestion() == null || answer.getMarksAwarded() == null) continue;
                BigDecimal max = examQuestions.findByExamIdAndQuestionId(
                        attempt.getExam().getId(), answer.getQuestion().getId())
                        .map(ExamQuestion::getMarks).orElse(answer.getQuestion().getDefaultMarks());
                add(sums, answer.getQuestion().getTopic(), answer.getMarksAwarded(), max);
            }
        }
        return sums.entrySet().stream().map(e -> {
            TopicAccumulator a = e.getValue();
            double pct = percent(a.awarded, a.maximum);
            return new Topic(e.getKey(), a.count, a.awarded, a.maximum,
                    pct, pct < PRACTICE_THRESHOLD);
        }).sorted(Comparator.comparingDouble(Topic::percentage).thenComparing(Topic::topic)).toList();
    }

    @Transactional(readOnly = true)
    public List<Topic> practiceTopics(User student, Integer courseId) {
        return topics(student, courseId).stream().filter(Topic::needsPractice).toList();
    }

    @Transactional(readOnly = true)
    public List<TrendPoint> trend(User student, Integer courseId) {
        List<Result> rows = new ArrayList<>(history(student, courseId));
        rows.sort(Comparator.comparing(Result::assessedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Result::attemptId));
        List<TrendPoint> points = new ArrayList<>();
        double total = 0;
        for (Result row : rows) {
            if (row.percentage() == null || row.assessedAt() == null) continue;
            total += row.percentage();
            points.add(new TrendPoint(row.assessedAt(), row.type(), row.assessmentId(),
                    row.attemptId(), row.percentage(), round(total / (points.size() + 1))));
        }
        return points;
    }

    private Integer requireStudent(User student) {
        if (student == null || student.getId() == null || !"STUDENT".equalsIgnoreCase(student.getRole()))
            throw new AccessDeniedException("Student role is required");
        return student.getId();
    }

    private void requireEnrollment(Integer studentId, Integer courseId) {
        if (!read.existsByStudentIdAndCourseId(studentId, courseId))
            throw new AccessDeniedException("You are not enrolled in this course");
    }

    private static boolean visibleExam(ExamAttempt a) {
        return a != null && a.getExam() != null && a.getExam().getCourse() != null
                && a.getObtainedMarks() != null && a.getSubmittedAt() != null
                && Boolean.TRUE.equals(a.getIsPublished())
                && Boolean.TRUE.equals(a.getExam().getResultsPublished())
                && !Boolean.TRUE.equals(a.getExam().getResultsWithheld());
    }

    private static boolean visibleQuiz(QuizAttempt a) {
        return a != null && a.getQuiz() != null && a.getQuiz().getCourse() != null
                && a.getObtainedMarks() != null && a.getSubmittedAt() != null
                && Boolean.TRUE.equals(a.getIsPublished())
                && Boolean.TRUE.equals(a.getQuiz().getResultsPublished())
                && !Boolean.TRUE.equals(a.getQuiz().getResultsWithheld());
    }

    private static boolean visibleAssignment(AssignmentSubmission a) {
        return a != null && a.getAssignment() != null && a.getAssignment().getCourse() != null
                && a.getAwardedMarks() != null && a.getSubmittedAt() != null
                && Boolean.TRUE.equals(a.getIsPublished())
                && Boolean.TRUE.equals(a.getAssignment().getResultsPublished())
                && !Boolean.TRUE.equals(a.getAssignment().getResultsWithheld());
    }

    private static LocalDateTime date(LocalDateTime submitted, LocalDateTime published) {
        return submitted != null ? submitted : published;
    }
    private static Double percent(BigDecimal awarded, BigDecimal maximum) {
        if (awarded == null || maximum == null || maximum.signum() <= 0) return null;
        return round(awarded.multiply(HUNDRED).divide(maximum, 4, RoundingMode.HALF_UP).doubleValue());
    }
    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
    private static Double average(List<Result> rows) {
        var known = rows.stream().map(Result::percentage).filter(Objects::nonNull).toList();
        return known.isEmpty() ? null : round(known.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }
    private static void add(Map<String,TopicAccumulator> map, String topic,
                            BigDecimal awarded, BigDecimal maximum) {
        if (topic == null || topic.isBlank() || awarded == null || maximum == null || maximum.signum() <= 0) return;
        TopicAccumulator a = map.computeIfAbsent(topic.trim(), k -> new TopicAccumulator());
        a.awarded = a.awarded.add(awarded); a.maximum = a.maximum.add(maximum); a.count++;
    }
    private static class TopicAccumulator {
        int count;
        BigDecimal awarded = BigDecimal.ZERO;
        BigDecimal maximum = BigDecimal.ZERO;
    }
}
