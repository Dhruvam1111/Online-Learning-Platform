package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.AssignmentLateException;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests targeting branch coverage for AssignmentService (FR-I68, Step 28).
 */
@ExtendWith(MockitoExtension.class)
class AssignmentServiceBranchTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseModuleRepository courseModuleRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AssignmentService assignmentService;

    private User instructor;
    private User student;
    private User admin;
    private Course course;
    private Assignment assignment;

    private final LocalDateTime baseNow = LocalDateTime.of(2026, 11, 20, 10, 0, 0);

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentService(
                assignmentRepository,
                assignmentSubmissionRepository,
                courseRepository,
                courseModuleRepository,
                lessonRepository,
                courseInstructorRepository,
                enrollmentRepository,
                userRepository,
                eventPublisher
        );

        Clock fixedClock = Clock.fixed(baseNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        assignmentService.setClock(fixedClock);

        instructor = new User("Instructor", "instructor@olp.com", "hash", "instructor", "active");
        instructor.setId(1);

        student = new User("Student", "student@olp.com", "hash", "student", "active");
        student.setId(2);

        admin = new User("Admin", "admin@olp.com", "hash", "admin", "active");
        admin.setId(3);

        course = new Course();
        course.setId(100);
        course.setInstructor(instructor);

        assignment = new Assignment();
        assignment.setId(10);
        assignment.setCourse(course);
        assignment.setCreatedBy(instructor);
        assignment.setTitle("Lab 1");
        assignment.setDeadline(baseNow.plusDays(2));
        assignment.setMaxMarks(new BigDecimal("100.00"));
        assignment.setSubmissionType("FILE");
        assignment.setLatePolicy("REJECT");
        assignment.setStatus("PUBLISHED");

        lenient().when(courseRepository.findById(100)).thenReturn(Optional.of(course));
    }

    @Test
    @DisplayName("createAssignment validates invalid inputs (nulls, blank title, negative marks)")
    void testCreateAssignmentValidation() {
        // null payload
        assertThrows(IllegalArgumentException.class, () -> assignmentService.createAssignment(instructor, null));

        // null title
        AssignmentCreateRequest r1 = new AssignmentCreateRequest();
        r1.setCourseId(100);
        r1.setDeadline(baseNow.plusDays(1));
        assertThrows(IllegalArgumentException.class, () -> assignmentService.createAssignment(instructor, r1));

        // blank title
        r1.setTitle("  ");
        assertThrows(IllegalArgumentException.class, () -> assignmentService.createAssignment(instructor, r1));

        // null deadline
        r1.setTitle("Assignment 1");
        r1.setDeadline(null);
        assertThrows(IllegalArgumentException.class, () -> assignmentService.createAssignment(instructor, r1));

        // negative max marks
        r1.setDeadline(baseNow.plusDays(1));
        r1.setMaxMarks(new BigDecimal("-10.00"));
        assertThrows(IllegalArgumentException.class, () -> assignmentService.createAssignment(instructor, r1));

        // invalid late penalty
        r1.setMaxMarks(new BigDecimal("100.00"));
        r1.setLatePenaltyPercentage(new BigDecimal("150.00"));
        assertThrows(IllegalArgumentException.class, () -> assignmentService.createAssignment(instructor, r1));

        // invalid submission type
        r1.setLatePenaltyPercentage(new BigDecimal("10.00"));
        r1.setSubmissionType("INVALID_TYPE");
        assertThrows(IllegalArgumentException.class, () -> assignmentService.createAssignment(instructor, r1));

        // invalid late policy
        r1.setSubmissionType("FILE");
        r1.setLatePolicy("INVALID_POLICY");
        assertThrows(IllegalArgumentException.class, () -> assignmentService.createAssignment(instructor, r1));
    }

    @Test
    @DisplayName("createAssignment validates module and lesson associations")
    void testCreateAssignmentModuleAndLesson() {
        AssignmentCreateRequest req = new AssignmentCreateRequest();
        req.setCourseId(100);
        req.setTitle("Module Assignment");
        req.setDeadline(baseNow.plusDays(3));
        req.setModuleId(200);

        Course foreignCourse = new Course();
        foreignCourse.setId(999);
        CourseModule foreignModule = new CourseModule();
        foreignModule.setId(200);
        foreignModule.setCourse(foreignCourse);

        when(courseModuleRepository.findById(200)).thenReturn(Optional.of(foreignModule));

        // Module belongs to different course -> IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> assignmentService.createAssignment(instructor, req));

        // Lesson not found
        CourseModule correctModule = new CourseModule();
        correctModule.setId(200);
        correctModule.setCourse(course);
        when(courseModuleRepository.findById(200)).thenReturn(Optional.of(correctModule));

        req.setLessonId(300);
        when(lessonRepository.findById(300)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> assignmentService.createAssignment(instructor, req));
    }

    @Test
    @DisplayName("updateAssignment updates all optional fields and validates bounds")
    void testUpdateAssignmentFields() {
        when(assignmentRepository.findById(10)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(i -> i.getArgument(0));

        AssignmentUpdateRequest req = new AssignmentUpdateRequest();
        req.setTitle("Updated Title");
        req.setInstructions("Do this carefully");
        req.setDeadline(baseNow.plusDays(5));
        req.setMaxMarks(new BigDecimal("80.00"));
        req.setRubric("Clear rubric");
        req.setSubmissionType("TEXT");
        req.setLatePolicy("PENALTY");
        req.setLatePenaltyPercentage(new BigDecimal("20.00"));
        req.setStatus("ARCHIVED");

        AssignmentResponse res = assignmentService.updateAssignment(instructor, 10, req);

        assertNotNull(res);
        assertEquals("Updated Title", res.getTitle());
        assertEquals("Do this carefully", res.getInstructions());
        assertEquals(new BigDecimal("80.00"), res.getMaxMarks());
        assertEquals("TEXT", res.getSubmissionType());
        assertEquals("PENALTY", res.getLatePolicy());
        assertEquals(new BigDecimal("20.00"), res.getLatePenaltyPercentage());
        assertEquals("ARCHIVED", res.getStatus());
    }

    @Test
    @DisplayName("submitAssignment enforces submission type matching (FILE, TEXT, BOTH)")
    void testSubmitAssignmentTypeEnforcement() {
        when(assignmentRepository.findById(10)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsByStudentIdAndCourseId(2, 100)).thenReturn(true);

        // Required FILE but fileUrl blank
        AssignmentSubmissionRequest r1 = new AssignmentSubmissionRequest();
        r1.setSubmissionType("FILE");
        assertThrows(IllegalArgumentException.class, () -> assignmentService.submitAssignment(student, 10, r1));

        // Required TEXT but textContent blank
        assignment.setSubmissionType("TEXT");
        AssignmentSubmissionRequest r2 = new AssignmentSubmissionRequest();
        r2.setSubmissionType("TEXT");
        assertThrows(IllegalArgumentException.class, () -> assignmentService.submitAssignment(student, 10, r2));

        // Required BOTH but neither provided
        assignment.setSubmissionType("BOTH");
        AssignmentSubmissionRequest r3 = new AssignmentSubmissionRequest();
        r3.setSubmissionType("BOTH");
        assertThrows(IllegalArgumentException.class, () -> assignmentService.submitAssignment(student, 10, r3));
    }

    @Test
    @DisplayName("submitAssignment rejects late submission when late policy is REJECT")
    void testSubmitAssignmentLateRejected() {
        assignment.setDeadline(baseNow.minusHours(1)); // passed
        assignment.setLatePolicy("REJECT");

        when(assignmentRepository.findById(10)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsByStudentIdAndCourseId(2, 100)).thenReturn(true);

        AssignmentSubmissionRequest req = new AssignmentSubmissionRequest();
        req.setFileUrl("https://files.com/submission.pdf");

        assertThrows(AssignmentLateException.class, () -> assignmentService.submitAssignment(student, 10, req));
    }

    @Test
    @DisplayName("submitAssignment accepts late submission and applies penalty when late policy is PENALTY")
    void testSubmitAssignmentLateWithPenalty() {
        assignment.setDeadline(baseNow.minusHours(1));
        assignment.setLatePolicy("PENALTY");
        assignment.setLatePenaltyPercentage(new BigDecimal("15.00"));

        when(assignmentRepository.findById(10)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsByStudentIdAndCourseId(2, 100)).thenReturn(true);
        when(assignmentSubmissionRepository.findByAssignmentIdAndStudentId(10, 2)).thenReturn(Optional.empty());
        when(assignmentSubmissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(i -> {
            AssignmentSubmission s = i.getArgument(0);
            s.setId(101);
            return s;
        });

        AssignmentSubmissionRequest req = new AssignmentSubmissionRequest();
        req.setFileUrl("https://files.com/submission.pdf");

        AssignmentSubmissionResponse res = assignmentService.submitAssignment(student, 10, req);

        assertNotNull(res);
        assertTrue(res.getIsLate());
        assertEquals(new BigDecimal("15.00"), res.getAppliedPenaltyPercentage());
    }

    @Test
    @DisplayName("gradeSubmission validates awarded marks within [0, maxMarks]")
    void testGradeSubmissionValidation() {
        AssignmentSubmission sub = new AssignmentSubmission();
        sub.setId(101);
        sub.setAssignment(assignment);

        when(assignmentSubmissionRepository.findById(101)).thenReturn(Optional.of(sub));

        // Negative marks
        assertThrows(IllegalArgumentException.class, () ->
                assignmentService.gradeSubmission(instructor, 101, new BigDecimal("-5.00"), "Bad")
        );

        // Exceeds max marks
        assertThrows(IllegalArgumentException.class, () ->
                assignmentService.gradeSubmission(instructor, 101, new BigDecimal("120.00"), "Too high")
        );
    }

    @Test
    @DisplayName("resolveUser handles authentication variants properly")
    void testResolveUser() {
        // Null auth
        assertThrows(InvalidCredentialsException.class, () -> assignmentService.resolveUser(null));

        // User directly in principal
        Authentication auth1 = new UsernamePasswordAuthenticationToken(student, "credentials");
        assertEquals(student, assignmentService.resolveUser(auth1));

        // Email in principal
        Authentication auth2 = mock(Authentication.class);
        when(auth2.getPrincipal()).thenReturn("instructor@olp.com");
        when(userRepository.findByEmail("instructor@olp.com")).thenReturn(Optional.of(instructor));
        assertEquals(instructor, assignmentService.resolveUser(auth2));

        // Unknown email
        when(userRepository.findByEmail("instructor@olp.com")).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class, () -> assignmentService.resolveUser(auth2));
    }
}
