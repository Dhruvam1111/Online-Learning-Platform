package com.olp.service;

import com.olp.dto.StudySessionRequest;
import com.olp.dto.StudySessionResponse;
import com.olp.dto.StudySuggestionResponse;
import com.olp.entity.*;
import com.olp.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

/** Student-owned study calendar and rule-based suggestions (FR-S52). */
@Service
public class StudentStudyScheduleService {
    private final UserRepository users;
    private final EnrollmentRepository enrollments;
    private final StudentScheduleEnrollmentRepository studentEnrollments;
    private final CourseRepository courses;
    private final StudentStudySessionRepository sessions;
    private final AssignmentRepository assignments;
    private final ExamRepository exams;
    private final QuizRepository quizzes;
    private final LectureRepository lectures;
    private final LectureProgressRepository lectureProgress;
    private final Clock clock;

    public StudentStudyScheduleService(UserRepository users, EnrollmentRepository enrollments, StudentScheduleEnrollmentRepository studentEnrollments,
                                       CourseRepository courses, StudentStudySessionRepository sessions,
                                       AssignmentRepository assignments, ExamRepository exams,
                                       QuizRepository quizzes, LectureRepository lectures,
                                       LectureProgressRepository lectureProgress) {
        this(users, enrollments, studentEnrollments, courses, sessions, assignments, exams, quizzes, lectures,
                lectureProgress, Clock.systemUTC());
    }

    /** Package-private clock injection makes time-dependent tests deterministic. */
    StudentStudyScheduleService(UserRepository users, EnrollmentRepository enrollments, StudentScheduleEnrollmentRepository studentEnrollments,
                                CourseRepository courses, StudentStudySessionRepository sessions,
                                AssignmentRepository assignments, ExamRepository exams,
                                QuizRepository quizzes, LectureRepository lectures,
                                LectureProgressRepository lectureProgress, Clock clock) {
        this.users = users;
        this.enrollments = enrollments;
        this.studentEnrollments = studentEnrollments;
        this.courses = courses;
        this.sessions = sessions;
        this.assignments = assignments;
        this.exams = exams;
        this.quizzes = quizzes;
        this.lectures = lectures;
        this.lectureProgress = lectureProgress;
        this.clock = clock;
    }

    public User student(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        User user;
        if (auth.getPrincipal() instanceof User principal) {
            user = principal;
        } else {
            String email = auth.getName();
            user = users.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Student not found"));
        }
        if (user.getId() == null || !"STUDENT".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student role required");
        }
        return user;
    }

    private Course enrolledCourse(Integer studentId, Integer courseId) {
        if (courseId == null || !enrollments.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student is not enrolled in this course");
        }
        return courses.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    private static LocalDateTime utc(OffsetDateTime instant) {
        return instant.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private static OffsetDateTime offset(LocalDateTime utc) {
        return utc.atOffset(ZoneOffset.UTC);
    }

    private void validate(StudySessionRequest request) {
        if (request == null || request.courseId() == null || request.title() == null
                || request.title().isBlank() || request.title().length() > 200
                || request.startsAt() == null || request.endsAt() == null
                || (request.notes() != null && request.notes().length() > 4000)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid study session request");
        }
        Duration duration = Duration.between(request.startsAt(), request.endsAt());
        if (duration.compareTo(Duration.ofMinutes(15)) < 0 || duration.compareTo(Duration.ofHours(8)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be 15 minutes to 8 hours");
        }
    }

    private StudentStudySession owned(Integer id, User user) {
        return sessions.findByIdAndStudentId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Study session not found"));
    }

    private StudySessionResponse view(StudentStudySession s) {
        return new StudySessionResponse(s.getId(), s.getCourse().getId(), s.getCourse().getTitle(),
                s.getTitle(), s.getNotes(), offset(s.getStartsAtUtc()), offset(s.getEndsAtUtc()), s.isCompleted());
    }

    private void fill(StudentStudySession s, StudySessionRequest request, Course course) {
        s.setCourse(course);
        s.setTitle(request.title().trim());
        s.setNotes(request.notes() == null ? null : request.notes().trim());
        s.setStartsAtUtc(utc(request.startsAt()));
        s.setEndsAtUtc(utc(request.endsAt()));
    }

    @Transactional
    public StudySessionResponse create(Authentication auth, StudySessionRequest request) {
        User user = student(auth);
        validate(request);
        Course course = enrolledCourse(user.getId(), request.courseId());
        StudentStudySession s = new StudentStudySession();
        s.setStudent(user);
        fill(s, request, course);
        return view(sessions.save(s));
    }

    @Transactional(readOnly = true)
    public List<StudySessionResponse> list(Authentication auth, OffsetDateTime from, OffsetDateTime to) {
        User user = student(auth);
        if (from == null || to == null || !from.isBefore(to)
                || Duration.between(from, to).compareTo(Duration.ofDays(120)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date range must be at most 120 days");
        }
        return sessions.findByStudentIdAndStartsAtUtcGreaterThanEqualAndStartsAtUtcLessThanOrderByStartsAtUtcAsc(
                user.getId(), utc(from), utc(to)).stream().map(this::view).toList();
    }

    @Transactional
    public StudySessionResponse update(Authentication auth, Integer id, StudySessionRequest request) {
        User user = student(auth);
        StudentStudySession s = owned(id, user);
        validate(request);
        Course course = enrolledCourse(user.getId(), request.courseId());
        fill(s, request, course);
        return view(sessions.save(s));
    }

    @Transactional
    public StudySessionResponse markCompleted(Authentication auth, Integer id, boolean completed) {
        StudentStudySession s = owned(id, student(auth));
        s.setCompleted(completed);
        return view(sessions.save(s));
    }

    @Transactional
    public void delete(Authentication auth, Integer id) {
        sessions.delete(owned(id, student(auth)));
    }

    @Transactional(readOnly = true)
    public List<StudySuggestionResponse> suggestions(Authentication auth, int days) {
        User user = student(auth);
        if (days < 1 || days > 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "days must be between 1 and 60");
        }
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDateTime limit = now.plusDays(days);
        List<StudySuggestionResponse> result = new ArrayList<>();
        // A student's enrollment is the single source of truth for accessible courses.
        for (Enrollment enrollment : studentEnrollments.findByStudentId(user.getId())) {
            if (enrollment.getStudent() == null || !user.getId().equals(enrollment.getStudent().getId())) continue;
            Course course = enrollment.getCourse();
            if (course == null || course.getId() == null) continue;
            Integer courseId = course.getId();

            for (Assignment a : assignments.findByCourseId(courseId)) {
                LocalDateTime deadline = a.getDeadline();
                if (deadline != null && !deadline.isBefore(now) && !deadline.isAfter(limit)
                        && "PUBLISHED".equalsIgnoreCase(a.getStatus())) {
                    result.add(new StudySuggestionResponse(courseId, course.getTitle(), "ASSIGNMENT", a.getId(),
                            "Prepare assignment: " + a.getTitle(), offset(deadline), 60,
                            "Upcoming assignment deadline"));
                }
            }
            for (Exam exam : exams.findByCourseId(courseId)) {
                LocalDateTime start = exam.getStartWindow();
                if (start != null && !start.isBefore(now) && !start.isAfter(limit)
                        && !"DRAFT".equalsIgnoreCase(exam.getStatus())) {
                    result.add(new StudySuggestionResponse(courseId, course.getTitle(), "EXAM", exam.getId(),
                            "Revise for exam: " + exam.getTitle(), offset(start), 90,
                            "Upcoming examination window"));
                }
            }
            for (Quiz q : quizzes.findByCourseIdOrderByCreatedAtDesc(courseId)) {
                LocalDateTime end = q.getAvailableUntil();
                if (q.getStatus() == QuizStatus.PUBLISHED && end != null && !end.isBefore(now) && !end.isAfter(limit)) {
                    result.add(new StudySuggestionResponse(courseId, course.getTitle(), "QUIZ", q.getId(),
                            "Prepare quiz: " + q.getTitle(), offset(end), 45,
                            "Quiz availability ends soon"));
                }
            }
            for (Lecture lecture : lectures.findByCourseIdOrderByOrderIndexAsc(courseId)) {
                if (lectureProgress.existsByStudentIdAndLectureId(user.getId(), lecture.getId())) continue;
                result.add(new StudySuggestionResponse(courseId, course.getTitle(), "LECTURE", lecture.getId(),
                        "Study lecture: " + lecture.getTitle(), null, 45,
                        "Unfinished course lecture"));
                // Limit non-deadline suggestions per course for an actionable response.
                if (result.stream().filter(v -> courseId.equals(v.courseId()) && "LECTURE".equals(v.sourceType())).count() >= 3) break;
            }
        }
        result.sort(Comparator.comparing(StudySuggestionResponse::dueAt,
                Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StudySuggestionResponse::courseId)
                .thenComparing(StudySuggestionResponse::sourceType));
        return result;
    }
}
