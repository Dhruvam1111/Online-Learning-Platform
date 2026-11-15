package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit and branch coverage tests for PendingGradingAlertService (FR-I33, Step 50).
 */
@ExtendWith(MockitoExtension.class)
class PendingGradingAlertServiceTest {

    @Mock
    private QuizAttemptAnswerRepository quizAttemptAnswerRepository;

    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Mock
    private PendingGradingAlertRepository pendingGradingAlertRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PendingGradingAlertService service;

    private User admin;
    private User owner;
    private User collaborator;
    private User unauthorizedInstructor;
    private User student;
    private Course course;
    private PendingGradingAlert sampleAlert;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1);
        admin.setEmail("admin@example.com");
        admin.setName("Admin User");
        admin.setRole("ADMIN");

        owner = new User();
        owner.setId(2);
        owner.setEmail("owner@example.com");
        owner.setName("Owner Instructor");
        owner.setRole("INSTRUCTOR");

        collaborator = new User();
        collaborator.setId(3);
        collaborator.setEmail("collab@example.com");
        collaborator.setName("Collab Instructor");
        collaborator.setRole("INSTRUCTOR");

        unauthorizedInstructor = new User();
        unauthorizedInstructor.setId(4);
        unauthorizedInstructor.setEmail("unauth@example.com");
        unauthorizedInstructor.setName("Unauthorized Instructor");
        unauthorizedInstructor.setRole("INSTRUCTOR");

        student = new User();
        student.setId(50);
        student.setEmail("student@example.com");
        student.setName("Student Alex");
        student.setRole("STUDENT");

        course = new Course();
        course.setId(10);
        course.setTitle("Distributed Systems");
        course.setInstructor(owner);

        sampleAlert = new PendingGradingAlert(
                owner, course, GradingAlertType.NEW_SUBMISSION, 4,
                "New Submission Awaiting Grading", "Work submitted", null);
        sampleAlert.setId(1);
    }

    // ==========================================
    // resolveUser Tests
    // ==========================================

    @Test
    @DisplayName("resolveUser: null authentication throws InvalidCredentialsException")
    void testResolveUser_NullAuth() {
        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(null));
    }

    @Test
    @DisplayName("resolveUser: null principal throws InvalidCredentialsException")
    void testResolveUser_NullPrincipal() {
        when(authentication.getPrincipal()).thenReturn(null);
        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(authentication));
    }

    @Test
    @DisplayName("resolveUser: User principal returns User")
    void testResolveUser_UserPrincipal() {
        when(authentication.getPrincipal()).thenReturn(owner);
        User resolved = service.resolveUser(authentication);
        assertSame(owner, resolved);
    }

    @Test
    @DisplayName("resolveUser: email string principal resolved via userRepository")
    void testResolveUser_EmailPrincipal() {
        when(authentication.getPrincipal()).thenReturn("owner@example.com");
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        User resolved = service.resolveUser(authentication);
        assertSame(owner, resolved);
    }

    @Test
    @DisplayName("resolveUser: email not found throws InvalidCredentialsException")
    void testResolveUser_EmailNotFound() {
        when(authentication.getPrincipal()).thenReturn("unknown@example.com");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(authentication));
    }

    @Test
    @DisplayName("resolveUser: unexpected principal object throws InvalidCredentialsException")
    void testResolveUser_UnexpectedPrincipal() {
        when(authentication.getPrincipal()).thenReturn(Integer.valueOf(123));
        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(authentication));
    }

    // ==========================================
    // RBAC & Course Access Tests
    // ==========================================

    @Test
    @DisplayName("verifyCourseAccess: null courseId throws IllegalArgumentException")
    void testVerifyCourseAccess_NullId() {
        assertThrows(IllegalArgumentException.class, () -> service.verifyCourseAccess(null, owner));
    }

    @Test
    @DisplayName("verifyCourseAccess: non-existent course throws ResourceNotFoundException")
    void testVerifyCourseAccess_NotFound() {
        when(courseRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.verifyCourseAccess(999, owner));
    }

    @Test
    @DisplayName("verifyCourseAccess: admin can access any course")
    void testVerifyCourseAccess_Admin() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        Course result = service.verifyCourseAccess(10, admin);
        assertSame(course, result);
    }

    @Test
    @DisplayName("verifyCourseAccess: owner instructor can access")
    void testVerifyCourseAccess_Owner() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        Course result = service.verifyCourseAccess(10, owner);
        assertSame(course, result);
    }

    @Test
    @DisplayName("verifyCourseAccess: collaborator instructor can access")
    void testVerifyCourseAccess_Collaborator() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, collaborator.getId())).thenReturn(true);

        Course result = service.verifyCourseAccess(10, collaborator);
        assertSame(course, result);
    }

    @Test
    @DisplayName("verifyCourseAccess: unauthorized instructor throws AccessDeniedException")
    void testVerifyCourseAccess_Unauthorized() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, unauthorizedInstructor.getId())).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.verifyCourseAccess(10, unauthorizedInstructor));
    }

    @Test
    @DisplayName("validateInstructorOrAdmin: student role throws AccessDeniedException")
    void testValidateRole_StudentDenied() {
        assertThrows(AccessDeniedException.class, () -> service.getPendingGradingCount(student));
    }

    @Test
    @DisplayName("validateInstructorOrAdmin: null user throws InvalidCredentialsException")
    void testValidateRole_NullUserDenied() {
        assertThrows(InvalidCredentialsException.class, () -> service.getPendingGradingCount(null));
    }

    // ==========================================
    // getPendingGradingCount Tests
    // ==========================================

    @Test
    @DisplayName("getPendingGradingCount: instructor with no courses returns zero counts")
    void testGetPendingGradingCount_NoCourses() {
        when(courseRepository.findByInstructorId(owner.getId())).thenReturn(Collections.emptyList());
        when(courseInstructorRepository.findByInstructorId(owner.getId())).thenReturn(Collections.emptyList());

        PendingGradingCountResponse res = service.getPendingGradingCount(owner);

        assertNotNull(res);
        assertEquals(0, res.getTotalPendingCount());
        assertEquals(0, res.getDescriptiveQuizPendingCount());
        assertEquals(0, res.getAssignmentPendingCount());
        assertTrue(res.getCourses().isEmpty());
    }

    @Test
    @DisplayName("getPendingGradingCount: instructor with owned and collaborated courses computes correct totals")
    void testGetPendingGradingCount_Success() {
        Course collabCourse = new Course();
        collabCourse.setId(20);
        collabCourse.setTitle("Cloud Computing");

        CourseInstructor ci = new CourseInstructor();
        ci.setCourse(collabCourse);
        ci.setInstructor(collaborator);

        when(courseRepository.findByInstructorId(collaborator.getId())).thenReturn(Collections.emptyList());
        when(courseInstructorRepository.findByInstructorId(collaborator.getId())).thenReturn(List.of(ci));

        when(quizAttemptAnswerRepository.countPendingDescriptiveByCourseId(20)).thenReturn(3L);
        when(assignmentSubmissionRepository.countPendingByCourseId(20)).thenReturn(2L);

        PendingGradingCountResponse res = service.getPendingGradingCount(collaborator);

        assertNotNull(res);
        assertEquals(5L, res.getTotalPendingCount());
        assertEquals(3L, res.getDescriptiveQuizPendingCount());
        assertEquals(2L, res.getAssignmentPendingCount());
        assertEquals(1, res.getCourses().size());
        assertEquals(20, res.getCourses().get(0).getCourseId());
        assertEquals("Cloud Computing", res.getCourses().get(0).getCourseTitle());
    }

    @Test
    @DisplayName("getPendingGradingCount: admin role inspects all courses in platform")
    void testGetPendingGradingCount_Admin() {
        when(courseRepository.findAll()).thenReturn(List.of(course));
        when(quizAttemptAnswerRepository.countPendingDescriptiveByCourseId(10)).thenReturn(4L);
        when(assignmentSubmissionRepository.countPendingByCourseId(10)).thenReturn(1L);

        PendingGradingCountResponse res = service.getPendingGradingCount(admin);

        assertNotNull(res);
        assertEquals(5L, res.getTotalPendingCount());
        assertEquals(4L, res.getDescriptiveQuizPendingCount());
        assertEquals(1L, res.getAssignmentPendingCount());
    }

    // ==========================================
    // getCoursePendingDetails Tests
    // ==========================================

    @Test
    @DisplayName("getCoursePendingDetails: maps pending descriptive and assignment items")
    void testGetCoursePendingDetails_Success() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));

        QuizAttemptAnswer answer = new QuizAttemptAnswer();
        answer.setId(101);
        Question question = new Question();
        question.setQuestionText("Explain Paxos consensus");
        question.setQuestionType(QuestionType.DESCRIPTIVE);
        answer.setQuestion(question);
        answer.setEvaluationStatus(EvaluationStatus.MANUAL_GRADING_REQUIRED);

        QuizAttempt attempt = new QuizAttempt();
        attempt.setStudent(student);
        Quiz quiz = new Quiz();
        quiz.setCourse(course);
        attempt.setQuiz(quiz);
        answer.setAttempt(attempt);

        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setId(201);
        Assignment assignment = new Assignment();
        assignment.setId(301);
        assignment.setTitle("Project 1");
        assignment.setCourse(course);
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setGradingStatus("PENDING");

        AssignmentSubmissionResponse mappedSub = new AssignmentSubmissionResponse();
        mappedSub.setId(201);
        mappedSub.setAssignmentTitle("Project 1");

        when(quizAttemptAnswerRepository.findPendingDescriptiveByCourseId(10)).thenReturn(List.of(answer));
        when(assignmentSubmissionRepository.findPendingByCourseId(10)).thenReturn(List.of(submission));
        when(assignmentService.mapToSubmissionResponse(submission)).thenReturn(mappedSub);

        CoursePendingDetailsResponse res = service.getCoursePendingDetails(owner, 10);

        assertNotNull(res);
        assertEquals(10, res.getCourseId());
        assertEquals(2L, res.getTotalPending());
        assertEquals(1L, res.getDescriptivePending());
        assertEquals(1L, res.getAssignmentPending());
        assertEquals(1, res.getDescriptiveSubmissions().size());
        assertEquals(1, res.getAssignmentSubmissions().size());
    }

    // ==========================================
    // Alert Notification Retrieval & Read Tests
    // ==========================================

    @Test
    @DisplayName("getInstructorAlerts: retrieves unread-only or all alerts")
    void testGetInstructorAlerts() {
        when(pendingGradingAlertRepository.findByInstructorIdAndIsReadOrderBySentAtDesc(owner.getId(), false))
                .thenReturn(List.of(sampleAlert));
        when(pendingGradingAlertRepository.findByInstructorIdOrderBySentAtDesc(owner.getId()))
                .thenReturn(List.of(sampleAlert));

        List<PendingGradingAlertResponse> unread = service.getInstructorAlerts(owner, true);
        assertEquals(1, unread.size());

        List<PendingGradingAlertResponse> all = service.getInstructorAlerts(owner, false);
        assertEquals(1, all.size());
    }

    @Test
    @DisplayName("markAlertAsRead: null alertId throws IllegalArgumentException")
    void testMarkAlertAsRead_NullId() {
        assertThrows(IllegalArgumentException.class, () -> service.markAlertAsRead(owner, null));
    }

    @Test
    @DisplayName("markAlertAsRead: non-existent alert throws ResourceNotFoundException")
    void testMarkAlertAsRead_NotFound() {
        when(pendingGradingAlertRepository.findByIdAndInstructorId(999, owner.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.markAlertAsRead(owner, 999));
    }

    @Test
    @DisplayName("markAlertAsRead: marks alert as read and returns DTO")
    void testMarkAlertAsRead_Success() {
        when(pendingGradingAlertRepository.findByIdAndInstructorId(1, owner.getId()))
                .thenReturn(Optional.of(sampleAlert));
        when(pendingGradingAlertRepository.save(any(PendingGradingAlert.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PendingGradingAlertResponse res = service.markAlertAsRead(owner, 1);

        assertNotNull(res);
        assertTrue(res.getIsRead());
        verify(pendingGradingAlertRepository).save(sampleAlert);
    }

    // ==========================================
    // triggerNewSubmissionAlert Tests
    // ==========================================

    @Test
    @DisplayName("triggerNewSubmissionAlert: null courseId throws IllegalArgumentException")
    void testTriggerNewSubmissionAlert_NullCourse() {
        assertThrows(IllegalArgumentException.class,
                () -> service.triggerNewSubmissionAlert(null, "Quiz 1", "Alex"));
    }

    @Test
    @DisplayName("triggerNewSubmissionAlert: non-existent course throws ResourceNotFoundException")
    void testTriggerNewSubmissionAlert_NotFound() {
        when(courseRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.triggerNewSubmissionAlert(999, "Quiz 1", "Alex"));
    }

    @Test
    @DisplayName("triggerNewSubmissionAlert: generates alerts for primary instructor and collaborators")
    void testTriggerNewSubmissionAlert_Success() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));

        CourseInstructor ci = new CourseInstructor();
        ci.setCourse(course);
        ci.setInstructor(collaborator);
        when(courseInstructorRepository.findByCourseIdWithInstructor(10)).thenReturn(List.of(ci));

        when(quizAttemptAnswerRepository.countPendingDescriptiveByCourseId(10)).thenReturn(2L);
        when(assignmentSubmissionRepository.countPendingByCourseId(10)).thenReturn(1L);

        when(pendingGradingAlertRepository.save(any(PendingGradingAlert.class)))
                .thenAnswer(inv -> {
                    PendingGradingAlert a = inv.getArgument(0);
                    a.setId(100);
                    return a;
                });

        List<PendingGradingAlertResponse> alerts = service.triggerNewSubmissionAlert(
                10, "Final Project", "Alex");

        assertNotNull(alerts);
        assertEquals(2, alerts.size()); // 1 for owner, 1 for collaborator
        verify(pendingGradingAlertRepository, times(2)).save(any(PendingGradingAlert.class));
    }

    // ==========================================
    // sendDailyReminderDigests Tests
    // ==========================================

    @Test
    @DisplayName("sendDailyReminderDigests: sends digests only once per day per instructor")
    void testSendDailyReminderDigests() {
        when(courseRepository.findAll()).thenReturn(List.of(course));
        when(quizAttemptAnswerRepository.countPendingDescriptiveByCourseId(10)).thenReturn(3L);
        when(assignmentSubmissionRepository.countPendingByCourseId(10)).thenReturn(1L);

        when(courseInstructorRepository.findByCourseIdWithInstructor(10)).thenReturn(Collections.emptyList());

        LocalDate today = LocalDate.now();
        // First check: not sent yet
        when(pendingGradingAlertRepository.existsByInstructorIdAndCourseIdAndAlertTypeAndDigestDate(
                owner.getId(), 10, GradingAlertType.DAILY_DIGEST, today))
                .thenReturn(false);

        when(pendingGradingAlertRepository.save(any(PendingGradingAlert.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        int sent = service.sendDailyReminderDigests();
        assertEquals(1, sent);
        verify(pendingGradingAlertRepository).save(any(PendingGradingAlert.class));

        // Second run: already sent
        when(pendingGradingAlertRepository.existsByInstructorIdAndCourseIdAndAlertTypeAndDigestDate(
                owner.getId(), 10, GradingAlertType.DAILY_DIGEST, today))
                .thenReturn(true);

        int sentSecond = service.sendDailyReminderDigests();
        assertEquals(0, sentSecond);
    }

    @Test
    @DisplayName("PendingGradingAlertResponse.fromEntity returns null on null input")
    void testPendingGradingAlertResponse_FromEntityNull() {
        assertNull(PendingGradingAlertResponse.fromEntity(null));
    }
}
