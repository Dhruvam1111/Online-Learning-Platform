package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentStudyScheduleServiceTest {
    @Mock UserRepository users;
    @Mock EnrollmentRepository enrollments;
    @Mock StudentScheduleEnrollmentRepository studentEnrollments;
    @Mock CourseRepository courses;
    @Mock StudentStudySessionRepository sessions;
    @Mock AssignmentRepository assignments;
    @Mock ExamRepository exams;
    @Mock QuizRepository quizzes;
    @Mock LectureRepository lectures;
    @Mock LectureProgressRepository lectureProgress;
    @Mock Authentication auth;

    private StudentStudyScheduleService service;
    private User student;
    private Course course;
    private static final OffsetDateTime START = OffsetDateTime.parse("2026-12-02T18:00:00+05:30");
    private static final OffsetDateTime END = OffsetDateTime.parse("2026-12-02T19:00:00+05:30");

    @BeforeEach
    void setUp() {
        service = new StudentStudyScheduleService(users, enrollments, studentEnrollments, courses,
                sessions, assignments, exams, quizzes, lectures, lectureProgress,
                Clock.fixed(Instant.parse("2026-12-01T00:00:00Z"), ZoneOffset.UTC));
        student = new User("Learner", "learner@test.com", "hash", "STUDENT");
        student.setId(12);
        course = new Course();
        course.setId(3);
        course.setTitle("Networking");
    }

    private void authenticated() {
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(student);
    }

    private void enrolled() {
        when(enrollments.existsByStudentIdAndCourseId(12, 3)).thenReturn(true);
        when(courses.findById(3)).thenReturn(Optional.of(course));
    }

    private StudySessionRequest request() {
        return new StudySessionRequest(3, "Revise routing", "Chapter 4", START, END);
    }

    private StudentStudySession existing() {
        StudentStudySession s = new StudentStudySession();
        s.setId(55);
        s.setStudent(student);
        s.setCourse(course);
        s.setTitle("Revise routing");
        s.setStartsAtUtc(LocalDateTime.of(2026, 12, 2, 12, 30));
        s.setEndsAtUtc(LocalDateTime.of(2026, 12, 2, 13, 30));
        return s;
    }

    @Test
    void createsOwnedSessionWithUtcNormalization() {
        authenticated(); enrolled();
        when(sessions.save(any(StudentStudySession.class))).thenAnswer(i -> {
            StudentStudySession s = i.getArgument(0);
            s.setId(55);
            return s;
        });
        StudySessionResponse out = service.create(auth, request());
        assertEquals(55, out.id());
        assertEquals(OffsetDateTime.parse("2026-12-02T12:30:00Z"), out.startsAt());
        assertEquals(3, out.courseId());
        verify(sessions).save(argThat(s -> s.getStudent().getId().equals(12)));
    }

    @Test
    void rejectsSessionForUnenrolledCourse() {
        authenticated();
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.create(auth, request()));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        verifyNoInteractions(sessions);
    }

    @Test
    void rejectsShortAndReversedTimes() {
        authenticated();
        StudySessionRequest shortRequest = new StudySessionRequest(3, "Revision", null, START, START.plusMinutes(5));
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.create(auth, shortRequest)).getStatusCode());
        StudySessionRequest reverse = new StudySessionRequest(3, "Revision", null, END, START);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.create(auth, reverse)).getStatusCode());
    }

    @Test
    void deniesUnauthenticatedRequests() {
        when(auth.isAuthenticated()).thenReturn(false);
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ResponseStatusException.class,
                () -> service.list(auth, START, END)).getStatusCode());
    }

    @Test
    void rejectsInstructor() {
        student.setRole("INSTRUCTOR");
        authenticated();
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> service.create(auth, request())).getStatusCode());
    }

    @Test
    void preventsAccessToOtherStudentSession() {
        authenticated();
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.markCompleted(auth, 55, true)).getStatusCode());
        verify(sessions).findByIdAndStudentId(55, 12);
        verify(sessions, never()).save(any());
    }

    @Test
    void togglesCompletionForOwnedSession() {
        authenticated();
        when(sessions.findByIdAndStudentId(55, 12)).thenReturn(Optional.of(existing()));
        when(sessions.save(any(StudentStudySession.class))).thenAnswer(i -> i.getArgument(0));
        assertTrue(service.markCompleted(auth, 55, true).completed());
    }

    @Test
    void listsOnlyCurrentStudentSessions() {
        authenticated();
        when(sessions.findByStudentIdAndStartsAtUtcGreaterThanEqualAndStartsAtUtcLessThanOrderByStartsAtUtcAsc(
                eq(12), any(), any())).thenReturn(List.of(existing()));
        List<StudySessionResponse> out = service.list(auth, START.minusDays(1), END.plusDays(1));
        assertEquals(1, out.size());
        assertEquals(55, out.get(0).id());
    }

    @Test
    void rejectsTooLargeCalendarRange() {
        authenticated();
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.list(auth, START, START.plusDays(121))).getStatusCode());
    }

    @Test
    void updateChecksOwnershipBeforeChangingSchedule() {
        authenticated();
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.update(auth, 99, request())).getStatusCode());
        verifyNoInteractions(courses);
    }

    @Test
    void deleteOnlyOwnedSession() {
        authenticated();
        StudentStudySession existing = existing();
        when(sessions.findByIdAndStudentId(55, 12)).thenReturn(Optional.of(existing));
        service.delete(auth, 55);
        verify(sessions).delete(existing);
    }

    @Test
    void suggestionsCombineDeadlinesAndUnfinishedLecture() {
        authenticated();
        when(studentEnrollments.findByStudentId(12)).thenReturn(List.of(new Enrollment(student, course)));
        Assignment a = new Assignment();
        a.setId(40); a.setTitle("Lab report"); a.setStatus("PUBLISHED");
        a.setDeadline(LocalDateTime.parse("2026-12-03T12:00:00"));
        when(assignments.findByCourseId(3)).thenReturn(List.of(a));
        when(exams.findByCourseId(3)).thenReturn(List.of());
        when(quizzes.findByCourseIdOrderByCreatedAtDesc(3)).thenReturn(List.of());
        Lecture lecture = new Lecture(); lecture.setId(8); lecture.setTitle("Subnetting");
        when(lectures.findByCourseIdOrderByOrderIndexAsc(3)).thenReturn(List.of(lecture));
        List<StudySuggestionResponse> out = service.suggestions(auth, 7);
        assertEquals(2, out.size());
        assertEquals("ASSIGNMENT", out.get(0).sourceType());
        assertEquals("LECTURE", out.get(1).sourceType());
        verify(lectureProgress).existsByStudentIdAndLectureId(12, 8);
    }

    @Test
    void suggestionsRejectInvalidHorizon() {
        authenticated();
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.suggestions(auth, 61)).getStatusCode());
    }
}
