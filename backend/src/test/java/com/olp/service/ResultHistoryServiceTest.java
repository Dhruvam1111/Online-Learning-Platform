package com.olp.service;

import com.olp.dto.CreateResultVersionRequest;
import com.olp.dto.ResultVersionResponse;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.ResultVersionRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit and branch coverage tests for ResultHistoryService (FR-I74, Step 49).
 */
@ExtendWith(MockitoExtension.class)
class ResultHistoryServiceTest {

    @Mock
    private ResultVersionRepository resultVersionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ResultHistoryService service;

    private User admin;
    private User courseOwner;
    private User collaborator;
    private User unauthorizedInstructor;
    private User student;
    private Course course;
    private ResultVersion resultVersion;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1);
        admin.setEmail("admin@example.com");
        admin.setName("Admin User");
        admin.setRole("ADMIN");

        courseOwner = new User();
        courseOwner.setId(2);
        courseOwner.setEmail("owner@example.com");
        courseOwner.setName("Owner Instructor");
        courseOwner.setRole("INSTRUCTOR");

        collaborator = new User();
        collaborator.setId(3);
        collaborator.setEmail("collab@example.com");
        collaborator.setName("Collaborator Instructor");
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
        course.setTitle("Software Architecture");
        course.setInstructor(courseOwner);

        resultVersion = new ResultVersion(
                course,
                AssessmentType.EXAM,
                101,
                "Final Exam",
                student,
                500,
                BigDecimal.valueOf(80.00),
                BigDecimal.valueOf(90.00),
                "80.00",
                "90.00",
                "Question 4 correction applied full marks",
                courseOwner
        );
        resultVersion.setId(1);
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
        when(authentication.getPrincipal()).thenReturn(courseOwner);
        User resolved = service.resolveUser(authentication);
        assertSame(courseOwner, resolved);
    }

    @Test
    @DisplayName("resolveUser: email string principal resolved via userRepository")
    void testResolveUser_EmailPrincipal() {
        when(authentication.getPrincipal()).thenReturn("owner@example.com");
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(courseOwner));

        User resolved = service.resolveUser(authentication);
        assertSame(courseOwner, resolved);
    }

    @Test
    @DisplayName("resolveUser: email string principal not found throws InvalidCredentialsException")
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
    // Course Access & RBAC Tests
    // ==========================================

    @Test
    @DisplayName("verifyCourseAccess: null courseId throws IllegalArgumentException")
    void testVerifyCourseAccess_NullId() {
        assertThrows(IllegalArgumentException.class, () -> service.verifyCourseAccess(null, courseOwner));
    }

    @Test
    @DisplayName("verifyCourseAccess: non-existent course throws ResourceNotFoundException")
    void testVerifyCourseAccess_NotFound() {
        when(courseRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.verifyCourseAccess(999, courseOwner));
    }

    @Test
    @DisplayName("verifyCourseAccess: admin can access any course")
    void testVerifyCourseAccess_Admin() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        Course result = service.verifyCourseAccess(10, admin);
        assertSame(course, result);
    }

    @Test
    @DisplayName("verifyCourseAccess: course owner instructor can access")
    void testVerifyCourseAccess_Owner() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        Course result = service.verifyCourseAccess(10, courseOwner);
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
    @DisplayName("getResultHistory: student role throws AccessDeniedException")
    void testGetResultHistory_StudentRoleDenied() {
        assertThrows(AccessDeniedException.class,
                () -> service.getResultHistory(student, 10, null, null, null));
    }

    @Test
    @DisplayName("getResultHistory: null user throws InvalidCredentialsException")
    void testGetResultHistory_NullUserDenied() {
        assertThrows(InvalidCredentialsException.class,
                () -> service.getResultHistory(null, 10, null, null, null));
    }

    // ==========================================
    // getResultHistory Query Tests
    // ==========================================

    @Test
    @DisplayName("getResultHistory: returns filtered results for authorized course owner")
    void testGetResultHistory_Success() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(resultVersionRepository.findFiltered(10, 50, AssessmentType.EXAM, 101))
                .thenReturn(List.of(resultVersion));

        List<ResultVersionResponse> history = service.getResultHistory(
                courseOwner, 10, 50, AssessmentType.EXAM, 101);

        assertNotNull(history);
        assertEquals(1, history.size());
        ResultVersionResponse item = history.get(0);
        assertEquals(1, item.getId());
        assertEquals("80.00", item.getOldValue());
        assertEquals("90.00", item.getNewValue());
        assertEquals("Question 4 correction applied full marks", item.getReason());
        assertEquals("Owner Instructor", item.getActorName());
        assertEquals("owner@example.com", item.getActorEmail());
        assertEquals("Student Alex", item.getStudentName());
        assertEquals("Final Exam", item.getAssessmentTitle());
    }

    // ==========================================
    // recordResultVersion Tests
    // ==========================================

    @Test
    @DisplayName("recordResultVersion: null request throws IllegalArgumentException")
    void testRecordResultVersion_NullRequest() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        assertThrows(IllegalArgumentException.class,
                () -> service.recordResultVersion(courseOwner, 10, null));
    }

    @Test
    @DisplayName("recordResultVersion: missing studentId throws IllegalArgumentException")
    void testRecordResultVersion_MissingStudent() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        CreateResultVersionRequest req = new CreateResultVersionRequest();
        assertThrows(IllegalArgumentException.class,
                () -> service.recordResultVersion(courseOwner, 10, req));
    }

    @Test
    @DisplayName("recordResultVersion: missing assessmentType throws IllegalArgumentException")
    void testRecordResultVersion_MissingType() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        CreateResultVersionRequest req = new CreateResultVersionRequest();
        req.setStudentId(50);
        assertThrows(IllegalArgumentException.class,
                () -> service.recordResultVersion(courseOwner, 10, req));
    }

    @Test
    @DisplayName("recordResultVersion: missing assessmentId throws IllegalArgumentException")
    void testRecordResultVersion_MissingAssessmentId() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        CreateResultVersionRequest req = new CreateResultVersionRequest();
        req.setStudentId(50);
        req.setAssessmentType(AssessmentType.QUIZ);
        assertThrows(IllegalArgumentException.class,
                () -> service.recordResultVersion(courseOwner, 10, req));
    }

    @Test
    @DisplayName("recordResultVersion: missing oldValue throws IllegalArgumentException")
    void testRecordResultVersion_MissingOldValue() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        CreateResultVersionRequest req = new CreateResultVersionRequest();
        req.setStudentId(50);
        req.setAssessmentType(AssessmentType.QUIZ);
        req.setAssessmentId(101);
        req.setOldValue("   ");
        assertThrows(IllegalArgumentException.class,
                () -> service.recordResultVersion(courseOwner, 10, req));
    }

    @Test
    @DisplayName("recordResultVersion: missing newValue throws IllegalArgumentException")
    void testRecordResultVersion_MissingNewValue() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        CreateResultVersionRequest req = new CreateResultVersionRequest();
        req.setStudentId(50);
        req.setAssessmentType(AssessmentType.QUIZ);
        req.setAssessmentId(101);
        req.setOldValue("50.00");
        req.setNewValue("");
        assertThrows(IllegalArgumentException.class,
                () -> service.recordResultVersion(courseOwner, 10, req));
    }

    @Test
    @DisplayName("recordResultVersion: missing reason throws IllegalArgumentException")
    void testRecordResultVersion_MissingReason() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        CreateResultVersionRequest req = new CreateResultVersionRequest();
        req.setStudentId(50);
        req.setAssessmentType(AssessmentType.QUIZ);
        req.setAssessmentId(101);
        req.setOldValue("50.00");
        req.setNewValue("60.00");
        req.setReason("   ");
        assertThrows(IllegalArgumentException.class,
                () -> service.recordResultVersion(courseOwner, 10, req));
    }

    @Test
    @DisplayName("recordResultVersion: non-existent student throws ResourceNotFoundException")
    void testRecordResultVersion_StudentNotFound() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        CreateResultVersionRequest req = new CreateResultVersionRequest(
                AssessmentType.QUIZ, 101, "Quiz 1", 999, null,
                BigDecimal.valueOf(50), BigDecimal.valueOf(60),
                "50.00", "60.00", "Regrading"
        );

        assertThrows(ResourceNotFoundException.class,
                () -> service.recordResultVersion(courseOwner, 10, req));
    }

    @Test
    @DisplayName("recordResultVersion: valid input saves and returns ResultVersionResponse")
    void testRecordResultVersion_Success() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(userRepository.findById(50)).thenReturn(Optional.of(student));
        when(resultVersionRepository.save(any(ResultVersion.class))).thenAnswer(invocation -> {
            ResultVersion v = invocation.getArgument(0);
            v.setId(99);
            return v;
        });

        CreateResultVersionRequest req = new CreateResultVersionRequest(
                AssessmentType.EXAM, 101, "Final Exam", 50, 500,
                BigDecimal.valueOf(80), BigDecimal.valueOf(90),
                "80.00", "90.00", "Recalculation verified"
        );

        ResultVersionResponse res = service.recordResultVersion(courseOwner, 10, req);

        assertNotNull(res);
        assertEquals(99, res.getId());
        assertEquals("80.00", res.getOldValue());
        assertEquals("90.00", res.getNewValue());
        assertEquals("Recalculation verified", res.getReason());
        verify(resultVersionRepository).save(any(ResultVersion.class));
    }

    @Test
    @DisplayName("ResultVersionResponse.fromEntity returns null on null input")
    void testResultVersionResponse_FromEntityNull() {
        assertNull(ResultVersionResponse.fromEntity(null));
    }
}
