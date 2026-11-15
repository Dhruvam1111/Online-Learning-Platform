package com.olp.service;

import com.olp.dto.NotificationPayload;
import com.olp.dto.NotificationResponse;
import com.olp.entity.Notification;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.NotificationRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    private User student;

    @BeforeEach
    void setUp() {
        student = new User("Alice Student", "alice@student.com", "hash", "student");
        student.setId(10);
    }

    @Test
    void testNotify_SavesInAppAndSendsEmail() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(101);
            return n;
        });

        NotificationPayload payload = NotificationPayload.of(
                "Course Update",
                "Module 2 has been published.",
                "#dashboard"
        );

        NotificationResponse res = notificationService.notify(student, "announcement", payload);

        assertNotNull(res);
        assertEquals(101, res.getId());
        assertEquals(10, res.getUserId());
        assertEquals("ANNOUNCEMENT", res.getType());
        assertEquals("Course Update", res.getTitle());
        assertEquals("Module 2 has been published.", res.getMessage());
        assertFalse(res.isRead());

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(emailService, times(1)).sendNotificationEmail(
                eq("alice@student.com"),
                eq("Alice Student"),
                eq("ANNOUNCEMENT"),
                eq("Course Update"),
                eq("Module 2 has been published."),
                eq("#dashboard")
        );
    }

    @Test
    void testNotify_MapOverloadInAppOnly_SkipsEmail() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(102);
            return n;
        });

        Map<String, Object> mapPayload = Map.of(
                "message", "Instructor replied to your question",
                "link", "#dashboard",
                "sendEmail", false
        );

        NotificationResponse res = notificationService.notify(student, "DISCUSSION_REPLY", mapPayload);

        assertEquals(102, res.getId());
        assertEquals("New Discussion Reply", res.getTitle());
        verify(emailService, never()).sendNotificationEmail(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testMarkAsRead_MarksUnreadNotification() {
        Notification notification = new Notification(student, "SYSTEM", "Welcome", "Welcome to OLP", null);
        notification.setId(55);
        when(notificationRepository.findByIdAndUserId(55, 10)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse res = notificationService.markAsRead(student, 55);

        assertTrue(res.isRead());
        assertNotNull(res.getReadAt());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void testMarkAsRead_NotFoundThrowsResourceNotFound() {
        when(notificationRepository.findByIdAndUserId(999, 10)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(student, 999));
    }

    @Test
    void testGetUnreadCountAndMarkAllAsRead() {
        when(notificationRepository.countUnreadByUserId(10)).thenReturn(4L);
        when(notificationRepository.markAllAsReadForUser(eq(10), any(LocalDateTime.class))).thenReturn(4);

        assertEquals(4L, notificationService.getUnreadCount(student));
        assertEquals(4, notificationService.markAllAsRead(student));
    }

    @Test
    void testNotifyUsers_BatchDispatchesToAllValidUsers() {
        User student2 = new User("Bob", "bob@student.com", "hash", "student");
        student2.setId(11);

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(200);
            return n;
        });

        List<NotificationResponse> responses = notificationService.notifyUsers(
                List.of(student, student2),
                "ANNOUNCEMENT",
                NotificationPayload.inAppOnly("Exam Notice", "Midterm is on Friday", "#dashboard")
        );

        assertEquals(2, responses.size());
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }
}

