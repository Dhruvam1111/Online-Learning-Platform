package com.olp.service;

import com.olp.dto.AdminAuditLogResponse;
import com.olp.dto.CourseReviewRequest;
import com.olp.dto.PageResponse;
import com.olp.dto.RejectCourseRequest;
import com.olp.entity.AdminAuditLog;
import com.olp.entity.Course;
import com.olp.entity.CourseStatus;
import com.olp.entity.User;
import com.olp.repository.AdminAuditLogRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminAuditServiceTest {

    @Mock
    private AdminAuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private AdminAuditService auditService;

    private User adminUser;
    private User studentUser;
    private Course course;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1);
        adminUser.setName("Super Admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole("admin");
        adminUser.setAccountStatus("active");

        studentUser = new User();
        studentUser.setId(2);
        studentUser.setName("Jane Doe");
        studentUser.setEmail("jane@example.com");
        studentUser.setRole("student");
        studentUser.setAccountStatus("active");

        course = new Course();
        course.setId(10);
        course.setTitle("Java Fundamentals");
        course.setStatus(CourseStatus.PENDING_REVIEW);
        course.setInstructor(studentUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, Collections.emptyList())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testRecordLog_WithExplicitActor() {
        when(auditLogRepository.save(any(AdminAuditLog.class))).thenAnswer(i -> {
            AdminAuditLog log = i.getArgument(0);
            log.setId(100L);
            return log;
        });

        AdminAuditLog result = auditService.recordLog(
                adminUser, "USER_SUSPENDED", "USER", "2", "Jane Doe",
                "Violation of rules", "active", "suspended", "127.0.0.1"
        );

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("USER_SUSPENDED", result.getAction());
        assertEquals("USER", result.getTargetType());
        assertEquals("2", result.getTargetId());
        assertEquals(adminUser, result.getActor());
        assertEquals("active", result.getPreviousState());
        assertEquals("suspended", result.getNewState());

        verify(auditLogRepository, times(1)).save(any(AdminAuditLog.class));
    }

    @Test
    void testRecordLog_ResolvesFromSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, Collections.emptyList())
        );

        when(auditLogRepository.save(any(AdminAuditLog.class))).thenAnswer(i -> i.getArgument(0));

        AdminAuditLog result = auditService.recordLog(
                "INSTRUCTOR_APPROVED", "USER", "3", "Prof Smith",
                "Credentials verified", "pending", "active"
        );

        assertNotNull(result);
        assertEquals(adminUser, result.getActor());
        assertEquals("INSTRUCTOR_APPROVED", result.getAction());
    }

    @Test
    void testRecordLog_FallbackToFirstAdminWhenNoAuth() {
        SecurityContextHolder.clearContext();
        when(userRepository.findAll()).thenReturn(List.of(studentUser, adminUser));
        when(auditLogRepository.save(any(AdminAuditLog.class))).thenAnswer(i -> i.getArgument(0));

        AdminAuditLog result = auditService.recordLog(
                "SYSTEM_ACTION", "PLATFORM", "0", "System",
                "Auto maintenance", null, null
        );

        assertNotNull(result);
        assertEquals(adminUser, result.getActor());
    }

    @Test
    void testGetAuditLogs_WithFilters() {
        AdminAuditLog logEntry = new AdminAuditLog(
                adminUser, "COURSE_APPROVED", "COURSE", "10", "Java Fundamentals",
                "Course approved", "PENDING_REVIEW", "PUBLISHED", null
        );
        logEntry.setId(1L);

        Page<AdminAuditLog> page = new PageImpl<>(List.of(logEntry));
        when(auditLogRepository.findWithFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<AdminAuditLogResponse> response = auditService.getAuditLogs(
                1, "COURSE_APPROVED", "COURSE", null, null, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        AdminAuditLogResponse item = response.getContent().get(0);
        assertEquals(1L, item.getId());
        assertEquals("COURSE_APPROVED", item.getAction());
        assertEquals("Java Fundamentals", item.getTargetName());
        assertEquals(adminUser.getId(), item.getActorId());
    }

    @Test
    void testAdminServiceHooks_ApproveAndRejectInstructor() {
        AdminService adminService = new AdminService(userRepository, courseRepository, null, null, auditService);

        User instructorUser = new User();
        instructorUser.setId(5);
        instructorUser.setName("Dr. Instructor");
        instructorUser.setEmail("dr@example.com");
        instructorUser.setRole("instructor");
        instructorUser.setAccountStatus("pending");

        when(userRepository.findById(5)).thenReturn(Optional.of(instructorUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any(AdminAuditLog.class))).thenAnswer(i -> i.getArgument(0));

        // Test approve
        adminService.approveInstructor(5);
        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository, atLeastOnce()).save(captor.capture());
        AdminAuditLog captured = captor.getValue();
        assertEquals("INSTRUCTOR_APPROVED", captured.getAction());
        assertEquals("5", captured.getTargetId());
        assertEquals("active", captured.getNewState());

        // Test reject
        instructorUser.setAccountStatus("pending");
        adminService.rejectInstructor(5);
        verify(auditLogRepository, atLeast(2)).save(captor.capture());
        assertEquals("INSTRUCTOR_REJECTED", captor.getValue().getAction());
        assertEquals("rejected", captor.getValue().getNewState());
    }

    @Test
    void testAdminServiceHooks_SuspendAndActivateUser() {
        AdminService adminService = new AdminService(userRepository, courseRepository, null, null, auditService);

        when(userRepository.findById(2)).thenReturn(Optional.of(studentUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any(AdminAuditLog.class))).thenAnswer(i -> i.getArgument(0));

        adminService.suspendUser(2);
        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());
        assertEquals("USER_SUSPENDED", captor.getValue().getAction());

        adminService.activateUser(2);
        verify(auditLogRepository, times(2)).save(captor.capture());
        assertEquals("USER_ACTIVATED", captor.getValue().getAction());
    }

    @Test
    void testAdminServiceHooks_CourseLifecycleAudit() {
        AdminService adminService = new AdminService(userRepository, courseRepository, null, null, auditService);

        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any(AdminAuditLog.class))).thenAnswer(i -> i.getArgument(0));

        // Course approved
        adminService.approveCourse(10);
        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());
        assertEquals("COURSE_APPROVED", captor.getValue().getAction());

        // Reject course
        RejectCourseRequest rejectReq = new RejectCourseRequest();
        rejectReq.setReason("Lacks quizzes");
        adminService.rejectCourse(10, rejectReq);
        verify(auditLogRepository, times(2)).save(captor.capture());
        assertEquals("COURSE_REJECTED", captor.getValue().getAction());
        assertTrue(captor.getValue().getDetails().contains("Lacks quizzes"));

        // Request changes
        CourseReviewRequest changeReq = new CourseReviewRequest();
        changeReq.setComments("Improve audio quality");
        adminService.requestChanges(10, changeReq);
        verify(auditLogRepository, times(3)).save(captor.capture());
        assertEquals("COURSE_CHANGES_REQUESTED", captor.getValue().getAction());

        // Unpublish
        adminService.unpublishCourse(10);
        verify(auditLogRepository, times(4)).save(captor.capture());
        assertEquals("COURSE_UNPUBLISHED", captor.getValue().getAction());

        // Delete course
        adminService.deleteCourse(10);
        verify(auditLogRepository, times(5)).save(captor.capture());
        assertEquals("COURSE_DELETED", captor.getValue().getAction());
    }
}
