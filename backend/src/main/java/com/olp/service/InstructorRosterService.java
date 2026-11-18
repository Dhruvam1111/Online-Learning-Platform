package com.olp.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.olp.dto.CourseRosterResponse;
import com.olp.dto.CourseRosterSummaryResponse;
import com.olp.dto.EnrolledStudentResponse;
import com.olp.entity.Certificate;
import com.olp.entity.Course;
import com.olp.entity.Enrollment;
import com.olp.entity.Lecture;
import com.olp.entity.Lesson;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CertificateRepository;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.LectureProgressRepository;
import com.olp.repository.LectureRepository;
import com.olp.repository.LessonProgressRepository;
import com.olp.repository.LessonRepository;
import com.olp.repository.UserRepository;

/**
 * Step 42 / FR-I63: Enrolled Student Roster Service.
 * Provides enrolled students per course with enrollment date, progress %, and last activity.
 * Supports search, pagination, dynamic sorting, and restricted access to course instructors (DR-05).
 */
@Service
public class InstructorRosterService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LectureRepository lectureRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public InstructorRosterService(
            EnrollmentRepository enrollmentRepository,
            CourseRepository courseRepository,
            CourseInstructorRepository courseInstructorRepository,
            LessonRepository lessonRepository,
            LessonProgressRepository lessonProgressRepository,
            LectureRepository lectureRepository,
            LectureProgressRepository lectureProgressRepository,
            CertificateRepository certificateRepository,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.lessonRepository = lessonRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.lectureRepository = lectureRepository;
        this.lectureProgressRepository = lectureProgressRepository;
        this.certificateRepository = certificateRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
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
    public CourseRosterResponse getCourseRoster(
            Integer courseId,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDir,
            User currentUser) {

        Course course = verifyCourseAccess(courseId, currentUser);

        int validPage = Math.max(page, 0);
        int validSize = (size <= 0) ? 10 : Math.min(size, 100);
        String trimmedSearch = (search != null) ? search.trim() : "";

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String normalizedSort = (sortBy != null) ? sortBy.trim() : "enrolledAt";

        List<Lesson> lessons = lessonRepository.findByCourseId(courseId);
        List<Lecture> lectures = lectureRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        int totalItems = lessons.size() + lectures.size();

        Set<Integer> lessonIds = lessons.stream().map(Lesson::getId).collect(Collectors.toSet());
        Set<Integer> lectureIds = lectures.stream().map(Lecture::getId).collect(Collectors.toSet());

        Map<Integer, Certificate> certMap = new HashMap<>();
        List<Certificate> certList = certificateRepository.findByCourseId(courseId);
        for (Certificate c : certList) {
            if (c.getStudent() != null) {
                certMap.put(c.getStudent().getId(), c);
            }
        }

        CourseRosterSummaryResponse summary = calculateSummary(course, lessons, lectures, lessonIds, lectureIds, certMap);

        if ("progress".equalsIgnoreCase(normalizedSort) || "lastActivity".equalsIgnoreCase(normalizedSort)) {
            List<Enrollment> allEnrollments = enrollmentRepository.findAllByCourseIdWithSearch(courseId, trimmedSearch);
            List<EnrolledStudentResponse> allStudentResponses = buildStudentResponses(
                    courseId,
                    allEnrollments,
                    totalItems,
                    lessonIds,
                    lectureIds,
                    certMap
            );

            if ("progress".equalsIgnoreCase(normalizedSort)) {
                Comparator<EnrolledStudentResponse> comp = Comparator.comparing(
                        EnrolledStudentResponse::getProgressPercentage,
                        Comparator.nullsFirst(Double::compareTo)
                );
                if (direction == Sort.Direction.DESC) {
                    comp = comp.reversed();
                }
                allStudentResponses.sort(comp);
            } else {
                Comparator<EnrolledStudentResponse> comp = Comparator.comparing(
                        EnrolledStudentResponse::getLastActivityAt,
                        Comparator.nullsFirst(LocalDateTime::compareTo)
                );
                if (direction == Sort.Direction.DESC) {
                    comp = comp.reversed();
                }
                allStudentResponses.sort(comp);
            }

            int totalElements = allStudentResponses.size();
            int totalPages = (int) Math.ceil((double) totalElements / validSize);
            int fromIndex = validPage * validSize;
            List<EnrolledStudentResponse> pageContent;

            if (fromIndex >= totalElements) {
                pageContent = Collections.emptyList();
            } else {
                int toIndex = Math.min(fromIndex + validSize, totalElements);
                pageContent = allStudentResponses.subList(fromIndex, toIndex);
            }

            boolean isLast = (validPage + 1) >= totalPages || totalPages == 0;

            return new CourseRosterResponse(
                    course.getId(),
                    course.getTitle(),
                    summary,
                    pageContent,
                    validPage,
                    validSize,
                    totalElements,
                    totalPages,
                    isLast
            );
        }

        String sortProperty = switch (normalizedSort.toLowerCase()) {
            case "name" -> "s.name";
            case "email" -> "s.email";
            default -> "enrolledAt";
        };

        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(direction, sortProperty));
        Page<Enrollment> enrollmentPage = enrollmentRepository.findByCourseIdWithSearch(courseId, trimmedSearch, pageable);

        List<EnrolledStudentResponse> studentResponses = buildStudentResponses(
                courseId,
                enrollmentPage.getContent(),
                totalItems,
                lessonIds,
                lectureIds,
                certMap
        );

        return new CourseRosterResponse(
                course.getId(),
                course.getTitle(),
                summary,
                studentResponses,
                enrollmentPage.getNumber(),
                enrollmentPage.getSize(),
                enrollmentPage.getTotalElements(),
                enrollmentPage.getTotalPages(),
                enrollmentPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public CourseRosterSummaryResponse getRosterSummary(Integer courseId, User currentUser) {
        Course course = verifyCourseAccess(courseId, currentUser);

        List<Lesson> lessons = lessonRepository.findByCourseId(courseId);
        List<Lecture> lectures = lectureRepository.findByCourseIdOrderByOrderIndexAsc(courseId);

        Set<Integer> lessonIds = lessons.stream().map(Lesson::getId).collect(Collectors.toSet());
        Set<Integer> lectureIds = lectures.stream().map(Lecture::getId).collect(Collectors.toSet());

        Map<Integer, Certificate> certMap = new HashMap<>();
        List<Certificate> certList = certificateRepository.findByCourseId(courseId);
        for (Certificate c : certList) {
            if (c.getStudent() != null) {
                certMap.put(c.getStudent().getId(), c);
            }
        }

        return calculateSummary(course, lessons, lectures, lessonIds, lectureIds, certMap);
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
            throw new AccessDeniedException("Access denied: you can only view students enrolled in your own courses (DR-05)");
        }
        return course;
    }

    private List<EnrolledStudentResponse> buildStudentResponses(
            Integer courseId,
            List<Enrollment> enrollments,
            int totalItems,
            Set<Integer> lessonIds,
            Set<Integer> lectureIds,
            Map<Integer, Certificate> certMap) {

        if (enrollments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> studentIds = enrollments.stream()
                .filter(e -> e.getStudent() != null)
                .map(e -> e.getStudent().getId())
                .collect(Collectors.toList());

        Map<Integer, LocalDateTime> lastActivityMap = fetchLastActivityMap(courseId, studentIds);

        List<EnrolledStudentResponse> results = new ArrayList<>(enrollments.size());
        for (Enrollment enrollment : enrollments) {
            User student = enrollment.getStudent();
            if (student == null) {
                continue;
            }

            int studentId = student.getId();

            int completedLessons = 0;
            if (!lessonIds.isEmpty()) {
                completedLessons = (int) lessonProgressRepository.countCompletedByStudentIdAndLessonIds(studentId, lessonIds);
            }

            int completedLectures = 0;
            if (!lectureIds.isEmpty()) {
                completedLectures = (int) lectureProgressRepository.countCompletedByStudentIdAndLectureIds(studentId, lectureIds);
            }

            int completedTotal = completedLessons + completedLectures;
            double progressPercentage = (totalItems == 0)
                    ? 0.0
                    : Math.round(((double) completedTotal / totalItems) * 1000.0) / 10.0;

            LocalDateTime lastActivityAt = lastActivityMap.getOrDefault(studentId, enrollment.getEnrolledAt());
            if (lastActivityAt == null || (enrollment.getEnrolledAt() != null && lastActivityAt.isBefore(enrollment.getEnrolledAt()))) {
                lastActivityAt = enrollment.getEnrolledAt();
            }

            Certificate cert = certMap.get(studentId);
            boolean certificateIssued = cert != null;
            String certificateCode = cert != null ? cert.getCertificateCode() : null;
            LocalDateTime certificateIssuedAt = cert != null ? cert.getIssuedAt() : null;

            results.add(new EnrolledStudentResponse(
                    studentId,
                    student.getName(),
                    student.getEmail(),
                    enrollment.getEnrolledAt(),
                    progressPercentage,
                    completedTotal,
                    totalItems,
                    lastActivityAt,
                    certificateIssued,
                    certificateCode,
                    certificateIssuedAt
            ));
        }

        return results;
    }

    private CourseRosterSummaryResponse calculateSummary(
            Course course,
            List<Lesson> lessons,
            List<Lecture> lectures,
            Set<Integer> lessonIds,
            Set<Integer> lectureIds,
            Map<Integer, Certificate> certMap) {

        List<Enrollment> allEnrollments = enrollmentRepository.findByCourseIdOrderByEnrolledAtDesc(course.getId());
        long totalEnrolled = allEnrollments.size();

        if (totalEnrolled == 0) {
            return new CourseRosterSummaryResponse(
                    course.getId(),
                    course.getTitle(),
                    0, 0, 0, 0, 0.0, 0
            );
        }

        int totalItems = lessons.size() + lectures.size();
        long completedCount = 0;
        long inProgressCount = 0;
        long notStartedCount = 0;
        double progressSum = 0.0;

        List<Integer> studentIds = allEnrollments.stream()
                .filter(e -> e.getStudent() != null)
                .map(e -> e.getStudent().getId())
                .collect(Collectors.toList());

        Map<Integer, LocalDateTime> lastActivityMap = fetchLastActivityMap(course.getId(), studentIds);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long activeLast30DaysCount = 0;

        for (Enrollment e : allEnrollments) {
            User student = e.getStudent();
            if (student == null) {
                continue;
            }
            int studentId = student.getId();

            int completedLessons = (!lessonIds.isEmpty())
                    ? (int) lessonProgressRepository.countCompletedByStudentIdAndLessonIds(studentId, lessonIds)
                    : 0;
            int completedLectures = (!lectureIds.isEmpty())
                    ? (int) lectureProgressRepository.countCompletedByStudentIdAndLectureIds(studentId, lectureIds)
                    : 0;
            int completedTotal = completedLessons + completedLectures;

            double progress = (totalItems == 0)
                    ? 0.0
                    : Math.round(((double) completedTotal / totalItems) * 1000.0) / 10.0;

            progressSum += progress;

            boolean hasCertificate = certMap.containsKey(studentId);
            if (progress >= 100.0 || hasCertificate) {
                completedCount++;
            } else if (progress > 0.0) {
                inProgressCount++;
            } else {
                notStartedCount++;
            }

            LocalDateTime lastActivityAt = lastActivityMap.getOrDefault(studentId, e.getEnrolledAt());
            if (lastActivityAt != null && lastActivityAt.isAfter(thirtyDaysAgo)) {
                activeLast30DaysCount++;
            }
        }

        double averageProgress = Math.round((progressSum / totalEnrolled) * 10.0) / 10.0;

        return new CourseRosterSummaryResponse(
                course.getId(),
                course.getTitle(),
                totalEnrolled,
                completedCount,
                inProgressCount,
                notStartedCount,
                averageProgress,
                activeLast30DaysCount
        );
    }

    private Map<Integer, LocalDateTime> fetchLastActivityMap(Integer courseId, List<Integer> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, LocalDateTime> map = new HashMap<>();

        String inClause = String.join(",", Collections.nCopies(studentIds.size(), "?"));

        // 1. lecture_progress
        try {
            String sql = "SELECT lp.student_id, MAX(lp.completed_at) AS max_at " +
                         "FROM lecture_progress lp " +
                         "JOIN lectures l ON lp.lecture_id = l.id " +
                         "WHERE l.course_id = ? AND lp.student_id IN (" + inClause + ") " +
                         "GROUP BY lp.student_id";
            List<Object> params = new ArrayList<>();
            params.add(courseId);
            params.addAll(studentIds);
            jdbcTemplate.query(sql, rs -> {
                int sId = rs.getInt("student_id");
                Timestamp ts = rs.getTimestamp("max_at");
                if (ts != null) {
                    updateMaxTimestamp(map, sId, ts.toLocalDateTime());
                }
            }, params.toArray());
        } catch (Exception ignored) {
        }

        // 2. lesson_progress
        try {
            String sql = "SELECT lp.student_id, MAX(lp.completed_at) AS max_at " +
                         "FROM lesson_progress lp " +
                         "JOIN lessons l ON lp.lesson_id = l.id " +
                         "JOIN course_sections cs ON l.section_id = cs.id " +
                         "JOIN course_modules cm ON cs.module_id = cm.id " +
                         "WHERE cm.course_id = ? AND lp.student_id IN (" + inClause + ") " +
                         "GROUP BY lp.student_id";
            List<Object> params = new ArrayList<>();
            params.add(courseId);
            params.addAll(studentIds);
            jdbcTemplate.query(sql, rs -> {
                int sId = rs.getInt("student_id");
                Timestamp ts = rs.getTimestamp("max_at");
                if (ts != null) {
                    updateMaxTimestamp(map, sId, ts.toLocalDateTime());
                }
            }, params.toArray());
        } catch (Exception ignored) {
        }

        // 3. quiz_attempts
        try {
            String sql = "SELECT qa.student_id, MAX(COALESCE(qa.submitted_at, qa.started_at, qa.updated_at)) AS max_at " +
                         "FROM quiz_attempts qa " +
                         "JOIN quizzes q ON qa.quiz_id = q.id " +
                         "WHERE q.course_id = ? AND qa.student_id IN (" + inClause + ") " +
                         "GROUP BY qa.student_id";
            List<Object> params = new ArrayList<>();
            params.add(courseId);
            params.addAll(studentIds);
            jdbcTemplate.query(sql, rs -> {
                int sId = rs.getInt("student_id");
                Timestamp ts = rs.getTimestamp("max_at");
                if (ts != null) {
                    updateMaxTimestamp(map, sId, ts.toLocalDateTime());
                }
            }, params.toArray());
        } catch (Exception ignored) {
        }

        // 4. assignment_submissions
        try {
            String sql = "SELECT sub.student_id, MAX(COALESCE(sub.submitted_at, sub.updated_at)) AS max_at " +
                         "FROM assignment_submissions sub " +
                         "JOIN assignments a ON sub.assignment_id = a.id " +
                         "WHERE a.course_id = ? AND sub.student_id IN (" + inClause + ") " +
                         "GROUP BY sub.student_id";
            List<Object> params = new ArrayList<>();
            params.add(courseId);
            params.addAll(studentIds);
            jdbcTemplate.query(sql, rs -> {
                int sId = rs.getInt("student_id");
                Timestamp ts = rs.getTimestamp("max_at");
                if (ts != null) {
                    updateMaxTimestamp(map, sId, ts.toLocalDateTime());
                }
            }, params.toArray());
        } catch (Exception ignored) {
        }

        // 5. exam_attempts
        try {
            String sql = "SELECT ea.student_id, MAX(COALESCE(ea.submitted_at, ea.started_at, ea.updated_at)) AS max_at " +
                         "FROM exam_attempts ea " +
                         "JOIN exams e ON ea.exam_id = e.id " +
                         "WHERE e.course_id = ? AND ea.student_id IN (" + inClause + ") " +
                         "GROUP BY ea.student_id";
            List<Object> params = new ArrayList<>();
            params.add(courseId);
            params.addAll(studentIds);
            jdbcTemplate.query(sql, rs -> {
                int sId = rs.getInt("student_id");
                Timestamp ts = rs.getTimestamp("max_at");
                if (ts != null) {
                    updateMaxTimestamp(map, sId, ts.toLocalDateTime());
                }
            }, params.toArray());
        } catch (Exception ignored) {
        }

        return map;
    }

    private void updateMaxTimestamp(Map<Integer, LocalDateTime> map, int studentId, LocalDateTime ts) {
        if (ts == null) {
            return;
        }
        LocalDateTime existing = map.get(studentId);
        if (existing == null || ts.isAfter(existing)) {
            map.put(studentId, ts);
        }
    }
}
