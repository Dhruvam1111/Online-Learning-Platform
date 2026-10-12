package com.olp.service;

import com.olp.dto.AdminCourseResponse;
import com.olp.dto.AdminUserResponse;
import com.olp.dto.PageResponse;
import com.olp.dto.RejectCourseRequest;
import com.olp.entity.Course;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private AdminService adminService;

    private User sampleStudent;
    private User pendingInstructor;
    private User adminUser;
    private Course sampleCourse;

    @BeforeEach
    void setUp() {
        sampleStudent = new User("Alice", "alice@student.com", "hash", "student", "active");
        sampleStudent.setId(1);

        pendingInstructor = new User("Dr. Chen", "chen@instructor.com", "hash", "instructor", "pending");
        pendingInstructor.setId(2);

        adminUser = new User("Admin", "admin@olp.com", "hash", "admin", "active");
        adminUser.setId(3);

        sampleCourse = new Course(pendingInstructor, "Java 101", "Basics of Java", "pending_review");
        sampleCourse.setId(10);
    }

    @Test
    void testGetUsers_WithFilters() {
        Page<User> userPage = new PageImpl<>(List.of(sampleStudent, adminUser));
        when(userRepository.findUsersWithFilters(any(), any(), any(), any(Pageable.class))).thenReturn(userPage);

        PageResponse<AdminUserResponse> result = adminService.getUsers("student", "active", "alice", 0, 10, "createdAt", "desc");

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(userRepository, times(1)).findUsersWithFilters(eq("student"), eq("active"), eq("alice"), any(Pageable.class));
    }

    @Test
    void testGetPendingInstructors() {
        Page<User> pendingPage = new PageImpl<>(List.of(pendingInstructor));
        when(userRepository.findByRoleAndAccountStatus(eq("instructor"), eq("pending"), any(Pageable.class))).thenReturn(pendingPage);

        PageResponse<AdminUserResponse> result = adminService.getPendingInstructors(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("pending", result.getContent().get(0).getAccountStatus());
        verify(userRepository, times(1)).findByRoleAndAccountStatus(eq("instructor"), eq("pending"), any(Pageable.class));
    }

    @Test
    void testApproveInstructor_Success() {
        when(userRepository.findById(2)).thenReturn(Optional.of(pendingInstructor));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response = adminService.approveInstructor(2);

        assertNotNull(response);
        assertEquals("active", response.getAccountStatus());
        verify(userRepository, times(1)).save(pendingInstructor);
    }

    @Test
    void testApproveInstructor_NotAnInstructor_ThrowsException() {
        when(userRepository.findById(1)).thenReturn(Optional.of(sampleStudent));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                adminService.approveInstructor(1)
        );

        assertTrue(exception.getMessage().contains("Only users with role 'instructor'"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRejectInstructor_Success() {
        when(userRepository.findById(2)).thenReturn(Optional.of(pendingInstructor));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response = adminService.rejectInstructor(2);

        assertNotNull(response);
        assertEquals("rejected", response.getAccountStatus());
        verify(userRepository, times(1)).save(pendingInstructor);
    }

    @Test
    void testSuspendUser_Success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(sampleStudent));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response = adminService.suspendUser(1);

        assertNotNull(response);
        assertEquals("suspended", response.getAccountStatus());
        verify(userRepository, times(1)).save(sampleStudent);
    }

    @Test
    void testSuspendAdmin_ThrowsException() {
        when(userRepository.findById(3)).thenReturn(Optional.of(adminUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                adminService.suspendUser(3)
        );

        assertTrue(exception.getMessage().contains("Admin accounts cannot be suspended"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testActivateUser_Success() {
        sampleStudent.setAccountStatus("suspended");
        when(userRepository.findById(1)).thenReturn(Optional.of(sampleStudent));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response = adminService.activateUser(1);

        assertNotNull(response);
        assertEquals("active", response.getAccountStatus());
        verify(userRepository, times(1)).save(sampleStudent);
    }

    @Test
    void testDeleteUser_Success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(sampleStudent));
        doNothing().when(userRepository).delete(sampleStudent);

        adminService.deleteUser(1);

        verify(userRepository, times(1)).delete(sampleStudent);
    }

    @Test
    void testDeleteAdmin_ThrowsException() {
        when(userRepository.findById(3)).thenReturn(Optional.of(adminUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                adminService.deleteUser(3)
        );

        assertTrue(exception.getMessage().contains("Admin accounts cannot be deleted"));
        verify(userRepository, never()).delete(any());
    }

    @Test
    void testUserNotFound_ThrowsException() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                adminService.approveInstructor(999)
        );
    }

    @Test
    void testGetCourses_WithFilters() {
        Page<Course> coursePage = new PageImpl<>(List.of(sampleCourse));
        when(courseRepository.findCoursesWithFilters(any(), any(), any(Pageable.class))).thenReturn(coursePage);

        PageResponse<AdminCourseResponse> result = adminService.getCourses("pending_review", "Java", 0, 10, "createdAt", "desc");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Java 101", result.getContent().get(0).getTitle());
        verify(courseRepository, times(1)).findCoursesWithFilters(eq("pending_review"), eq("java"), any(Pageable.class));
    }

    @Test
    void testGetPendingCourses() {
        Page<Course> coursePage = new PageImpl<>(List.of(sampleCourse));
        when(courseRepository.findByStatus(eq("pending_review"), any(Pageable.class))).thenReturn(coursePage);

        PageResponse<AdminCourseResponse> result = adminService.getPendingCourses(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("pending_review", result.getContent().get(0).getStatus());
        verify(courseRepository, times(1)).findByStatus(eq("pending_review"), any(Pageable.class));
    }

    @Test
    void testApproveCourse_Success() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminCourseResponse response = adminService.approveCourse(10);

        assertNotNull(response);
        assertEquals("published", response.getStatus());
        assertNull(response.getRejectionReason());
        verify(courseRepository, times(1)).save(sampleCourse);
    }

    @Test
    void testRejectCourse_Success() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RejectCourseRequest request = new RejectCourseRequest("Content is incomplete");
        AdminCourseResponse response = adminService.rejectCourse(10, request);

        assertNotNull(response);
        assertEquals("rejected", response.getStatus());
        assertEquals("Content is incomplete", response.getRejectionReason());
        verify(courseRepository, times(1)).save(sampleCourse);
    }

    @Test
    void testUnpublishCourse_Success() {
        sampleCourse.setStatus("published");
        when(courseRepository.findById(10)).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminCourseResponse response = adminService.unpublishCourse(10);

        assertNotNull(response);
        assertEquals("draft", response.getStatus());
        verify(courseRepository, times(1)).save(sampleCourse);
    }

    @Test
    void testDeleteCourse_Success() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(sampleCourse));
        doNothing().when(courseRepository).delete(sampleCourse);

        adminService.deleteCourse(10);

        verify(courseRepository, times(1)).delete(sampleCourse);
    }

    @Test
    void testCourseNotFound_ThrowsException() {
        when(courseRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                adminService.approveCourse(999)
        );
    }
}
