package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.event.AssignmentDueEvent;
import com.olp.exception.AssignmentLateException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AssignmentService (FR-I68, Step 28).
 * Tests deadline enforcement in UTC, late submission policies (REJECT, PENALTY, ALLOW),
 * student enrollment validation, instructor course association, and grading queue integration.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

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
    private User collaborator;
    private User unauthorizedInstructor;
    private User student;
    private User unenrolledStudent;
    private Course course;

    private final LocalDateTime deadlineUtc = LocalDateTime.of(2026, 11, 20, 15, 0, 0);

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

        instructor = new User("Dr. Owner", "owner@olp.com", "hash", "instructor", "active");
        instructor.setId(10);

        collaborator = new User("Prof. Collab", "collab@olp.com", "hash", "instructor", "active");
        collaborator.setId(11);

        unauthorizedInstructor = new User("Dr. Stranger", "stranger@olp.com", "hash", "instructor", "active");
        unauthorizedInstructor.setId(99);

        student = new User("Alice Student", "alice@olp.com", "hash", "student", "active");
        student.setId(20);

        unenrolledStudent = new User("Bob Student", "bob@olp.com", "hash", "student", "active");
        unenrolledStudent.setId(21);

        course = new Course(instructor, "Operating Systems", "OS Architecture", CourseStatus.PUBLISHED);
        course.setId(100);
    }

    private void setServerTimeUtc(LocalDateTime utcTime) {
        Clock fixedClock = Clock.fixed(utcTime.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        assignmentService.setClock(fixedClock);
    }

    @Test
    void testCreateAssignment_Success_PublishesReminderEvent() {
        setServerTimeUtc(LocalDateTime.of(2026, 11, 15, 10, 0, 0));

        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> {
            Assignment a = inv.getArgument(0);
            a.setId(501);
            return a;
        });

        AssignmentCreateRequest request = new AssignmentCreateRequest();
        request.setCourseId(100);
        request.setTitle("Kernel Memory Allocator");
        request.setInstructions("Implement a buddy memory allocator in C");
        request.setDeadline(deadlineUtc);
        request.setMaxMarks(new BigDecimal("100.00"));
        request.setSubmissionType("FILE");
        request.setLatePolicy("PENALTY");
        request.setLatePenaltyPercentage(new BigDecimal("10.00"));
        request.setStatus("PUBLISHED");

        AssignmentResponse response = assignmentService.createAssignment(instructor, request);

        assertNotNull(response);
        assertEquals(501, response.getId());
        assertEquals("Kernel Memory Allocator", response.getTitle());
        assertEquals(deadlineUtc, response.getDeadline());
        assertEquals("PENALTY", response.getLatePolicy());
        assertEquals(new BigDecimal("10.00"), response.getLatePenaltyPercentage());
        assertFalse(response.getIsPastDeadline());

        // Verify reminder event published for notification listener (FR-I41 / Person 3)
        ArgumentCaptor<AssignmentDueEvent> captor = ArgumentCaptor.forClass(AssignmentDueEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        AssignmentDueEvent event = captor.getValue();
        assertEquals(501, event.getAssignmentId());
        assertEquals(100, event.getCourseId());
        assertEquals("Kernel Memory Allocator", event.getTitle());
    }

    @Test
    void testCreateAssignment_CollaboratorInstructor_Success() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 11)).thenReturn(true);
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> {
            Assignment a = inv.getArgument(0);
            a.setId(502);
            return a;
        });

        AssignmentCreateRequest request = new AssignmentCreateRequest();
        request.setCourseId(100);
        request.setTitle("Concurrency Lab");
        request.setDeadline(deadlineUtc);

        AssignmentResponse response = assignmentService.createAssignment(collaborator, request);
        assertNotNull(response);
        assertEquals(502, response.getId());
    }

    @Test
    void testCreateAssignment_UnauthorizedInstructor_ThrowsAccessDenied() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 99)).thenReturn(false);

        AssignmentCreateRequest request = new AssignmentCreateRequest();
        request.setCourseId(100);
        request.setTitle("Unauthorized Lab");
        request.setDeadline(deadlineUtc);

        assertThrows(AccessDeniedException.class, () ->
                assignmentService.createAssignment(unauthorizedInstructor, request));
    }

    @Test
    void testCreateAssignment_InvalidLatePenalty_ThrowsIllegalArgument() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));

        AssignmentCreateRequest request = new AssignmentCreateRequest();
        request.setCourseId(100);
        request.setTitle("Invalid Penalty Lab");
        request.setDeadline(deadlineUtc);
        request.setLatePenaltyPercentage(new BigDecimal("150.00")); // > 100%

        assertThrows(IllegalArgumentException.class, () ->
                assignmentService.createAssignment(instructor, request));
    }

    @Test
    void testSubmitAssignment_BeforeDeadline_AcceptedWithoutPenalty() {
        // Server time: 10 minutes before deadline
        setServerTimeUtc(deadlineUtc.minusMinutes(10));

        Assignment assignment = new Assignment(
                course, null, null, instructor, "Lab 1", "Desc", deadlineUtc,
                new BigDecimal("100.00"), "Rubric", "FILE", "REJECT", BigDecimal.ZERO, "PUBLISHED");
        assignment.setId(501);

        when(assignmentRepository.findById(501)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);
        when(assignmentSubmissionRepository.findByAssignmentIdAndStudentId(501, 20)).thenReturn(Optional.empty());
        when(assignmentSubmissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(inv -> {
            AssignmentSubmission sub = inv.getArgument(0);
            sub.setId(901);
            return sub;
        });

        AssignmentSubmissionRequest request = new AssignmentSubmissionRequest();
        request.setSubmissionType("FILE");
        request.setFileUrl("https://storage.olp.com/subs/lab1.tar.gz");
        request.setFileName("lab1.tar.gz");
        request.setFileSizeBytes(1024L * 1024L);

        AssignmentSubmissionResponse response = assignmentService.submitAssignment(student, 501, request);

        assertNotNull(response);
        assertEquals(901, response.getId());
        assertFalse(response.getIsLate(), "Submission on or before deadline must not be marked late");
        assertEquals(BigDecimal.ZERO, response.getAppliedPenaltyPercentage());
        assertEquals("PENDING", response.getGradingStatus());
        assertEquals("lab1.tar.gz", response.getFileName());
    }

    @Test
    void testSubmitAssignment_AfterDeadline_RejectPolicy_ThrowsAssignmentLateException() {
        // Server time: 5 seconds after deadline
        setServerTimeUtc(deadlineUtc.plusSeconds(5));

        Assignment assignment = new Assignment(
                course, null, null, instructor, "Strict Lab", "Desc", deadlineUtc,
                new BigDecimal("100.00"), "Rubric", "FILE", "REJECT", BigDecimal.ZERO, "PUBLISHED");
        assignment.setId(502);

        when(assignmentRepository.findById(502)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);

        AssignmentSubmissionRequest request = new AssignmentSubmissionRequest();
        request.setSubmissionType("FILE");
        request.setFileUrl("https://storage.olp.com/subs/late.zip");

        AssignmentLateException ex = assertThrows(AssignmentLateException.class, () ->
                assignmentService.submitAssignment(student, 502, request));

        assertTrue(ex.getMessage().contains("deadline"));
        assertTrue(ex.getMessage().contains("Late submissions are not accepted"));
    }

    @Test
    void testSubmitAssignment_AfterDeadline_PenaltyPolicy_AppliesConfiguredPenalty() {
        // Server time: 1 hour after deadline
        setServerTimeUtc(deadlineUtc.plusHours(1));

        Assignment assignment = new Assignment(
                course, null, null, instructor, "Flexible Lab", "Desc", deadlineUtc,
                new BigDecimal("100.00"), "Rubric", "FILE", "PENALTY", new BigDecimal("15.00"), "PUBLISHED");
        assignment.setId(503);

        when(assignmentRepository.findById(503)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);
        when(assignmentSubmissionRepository.findByAssignmentIdAndStudentId(503, 20)).thenReturn(Optional.empty());
        when(assignmentSubmissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(inv -> {
            AssignmentSubmission sub = inv.getArgument(0);
            sub.setId(903);
            return sub;
        });

        AssignmentSubmissionRequest request = new AssignmentSubmissionRequest();
        request.setSubmissionType("FILE");
        request.setFileUrl("https://storage.olp.com/subs/flexible.pdf");

        AssignmentSubmissionResponse response = assignmentService.submitAssignment(student, 503, request);

        assertNotNull(response);
        assertTrue(response.getIsLate(), "Submission after deadline must be marked late");
        assertEquals(new BigDecimal("15.00"), response.getAppliedPenaltyPercentage());
        assertEquals("PENDING", response.getGradingStatus());
    }

    @Test
    void testSubmitAssignment_AfterDeadline_AllowPolicy_NoPenalty() {
        // Server time: 2 hours after deadline
        setServerTimeUtc(deadlineUtc.plusHours(2));

        Assignment assignment = new Assignment(
                course, null, null, instructor, "Open Lab", "Desc", deadlineUtc,
                new BigDecimal("100.00"), "Rubric", "TEXT", "ALLOW", BigDecimal.ZERO, "PUBLISHED");
        assignment.setId(504);

        when(assignmentRepository.findById(504)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);
        when(assignmentSubmissionRepository.findByAssignmentIdAndStudentId(504, 20)).thenReturn(Optional.empty());
        when(assignmentSubmissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(inv -> {
            AssignmentSubmission sub = inv.getArgument(0);
            sub.setId(904);
            return sub;
        });

        AssignmentSubmissionRequest request = new AssignmentSubmissionRequest();
        request.setSubmissionType("TEXT");
        request.setTextContent("Summary of virtual memory paging");

        AssignmentSubmissionResponse response = assignmentService.submitAssignment(student, 504, request);

        assertNotNull(response);
        assertTrue(response.getIsLate());
        assertEquals(BigDecimal.ZERO, response.getAppliedPenaltyPercentage());
    }

    @Test
    void testSubmitAssignment_UnenrolledStudent_ThrowsAccessDenied() {
        Assignment assignment = new Assignment(
                course, null, null, instructor, "Enrolled Only Lab", "Desc", deadlineUtc,
                new BigDecimal("100.00"), "Rubric", "FILE", "REJECT", BigDecimal.ZERO, "PUBLISHED");
        assignment.setId(505);

        when(assignmentRepository.findById(505)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsByStudentIdAndCourseId(21, 100)).thenReturn(false);

        AssignmentSubmissionRequest request = new AssignmentSubmissionRequest();
        request.setFileUrl("https://storage.olp.com/subs/test.pdf");

        assertThrows(AccessDeniedException.class, () ->
                assignmentService.submitAssignment(unenrolledStudent, 505, request));
    }

    @Test
    void testSubmitAssignment_MissingFileUrlForFileSubmission_ThrowsIllegalArgument() {
        Assignment assignment = new Assignment(
                course, null, null, instructor, "File Lab", "Desc", deadlineUtc,
                new BigDecimal("100.00"), "Rubric", "FILE", "REJECT", BigDecimal.ZERO, "PUBLISHED");
        assignment.setId(506);

        when(assignmentRepository.findById(506)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(true);

        AssignmentSubmissionRequest request = new AssignmentSubmissionRequest();
        request.setSubmissionType("FILE");
        request.setFileUrl("   "); // blank

        assertThrows(IllegalArgumentException.class, () ->
                assignmentService.submitAssignment(student, 506, request));
    }

    @Test
    void testGradeSubmission_ComputesEffectiveMarksWithLatePenalty() {
        setServerTimeUtc(LocalDateTime.of(2026, 11, 21, 12, 0, 0));

        Assignment assignment = new Assignment(
                course, null, null, instructor, "Graded Lab", "Desc", deadlineUtc,
                new BigDecimal("100.00"), "Rubric", "FILE", "PENALTY", new BigDecimal("20.00"), "PUBLISHED");
        assignment.setId(507);

        AssignmentSubmission sub = new AssignmentSubmission(
                assignment, student, "FILE", "https://storage.olp.com/file.zip", "file.zip", 2048L,
                null, deadlineUtc.plusHours(2), true, new BigDecimal("20.00"));
        sub.setId(907);

        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(assignmentSubmissionRepository.findById(907)).thenReturn(Optional.of(sub));
        when(assignmentSubmissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        // Award 90.00 marks; with 20% penalty, effective marks = 90 - 18 = 72.00
        AssignmentSubmissionResponse response = assignmentService.gradeSubmission(
                instructor, 907, new BigDecimal("90.00"), "Good work, penalty deducted for late submission");

        assertNotNull(response);
        assertEquals(new BigDecimal("90.00"), response.getAwardedMarks());
        assertEquals(new BigDecimal("72.00"), response.getEffectiveMarks());
        assertEquals("GRADED", response.getGradingStatus());
        assertEquals(10, response.getGraderId());
        assertEquals("Dr. Owner", response.getGraderName());
    }

    @Test
    void testExactUtcDeadlineBoundary_Behavior() {
        Assignment assignment = new Assignment(
                course, null, null, instructor, "Boundary Lab", "Desc", deadlineUtc,
                new BigDecimal("100.00"), "Rubric", "FILE", "REJECT", BigDecimal.ZERO, "PUBLISHED");
        assignment.setId(508);

        // Exactly at deadlineUtc
        assertFalse(assignment.isSubmissionLate(deadlineUtc), "Exact deadline match is not late");

        // 1 millisecond before deadlineUtc
        assertFalse(assignment.isSubmissionLate(deadlineUtc.minusNanos(1000000)));

        // 1 second after deadlineUtc
        assertTrue(assignment.isSubmissionLate(deadlineUtc.plusSeconds(1)), "Past deadline is late");
    }
}

