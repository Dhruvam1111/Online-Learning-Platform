package com.olp.service;

import com.olp.dto.StudentNotificationResponse;
import com.olp.entity.Course;
import com.olp.entity.StudentNotification;
import com.olp.entity.User;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.StudentNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentNotificationServiceTest {

    @Mock
    private StudentNotificationRepository notificationRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private StudentNotificationService notificationService;

    private User student;
    private Course course;
    private StudentNotification notification;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(10);
        student.setEmail("student@olp.com");

        course = new Course();
        course.setId(100);
        course.setTitle("Operating Systems");

        notification = new StudentNotification(student, course, "ANNOUNCEMENT", "New Assignment", "Due on Friday", 1, "/assignments/1");
        notification.setId(1);
        notification.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testGetNotificationsForStudent_Success() {
        when(notificationRepository.findByStudentIdOrderByCreatedAtDesc(10)).thenReturn(List.of(notification));

        List<StudentNotificationResponse> list = notificationService.getStudentNotifications(student, false);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("New Assignment", list.get(0).getTitle());
        assertFalse(list.get(0).getIsRead());
    }

    @Test
    void testGetUnreadCount_Success() {
        when(notificationRepository.countUnreadByStudentId(10)).thenReturn(3L);

        long count = notificationService.getUnreadCount(student);
        assertEquals(3L, count);
    }

    @Test
    void testMarkAsRead_Success() {
        when(notificationRepository.findById(1)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(StudentNotification.class))).thenAnswer(i -> i.getArgument(0));

        StudentNotificationResponse res = notificationService.markAsRead(student, 1);

        assertNotNull(res);
        assertTrue(res.getIsRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void testMarkAsRead_OtherStudent_ThrowsAccessDenied() {
        User other = new User();
        other.setId(999);

        when(notificationRepository.findById(1)).thenReturn(Optional.of(notification));

        assertThrows(AccessDeniedException.class, () ->
                notificationService.markAsRead(other, 1)
        );
    }

    @Test
    void testMarkAllAsRead_Success() {
        when(notificationRepository.markAllAsRead(10)).thenReturn(2);

        int updated = notificationService.markAllAsRead(student);

        assertEquals(2, updated);
        verify(notificationRepository).markAllAsRead(10);
    }
}
