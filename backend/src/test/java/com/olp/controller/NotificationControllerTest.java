package com.olp.controller;

import com.olp.dto.NotificationResponse;
import com.olp.entity.User;
import com.olp.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private User user;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        user = new User("Dr. Sarah Chen", "sarah@instructor.com", "hash", "instructor");
        user.setId(2);
        authentication = new UsernamePasswordAuthenticationToken(user, null);
    }

    @Test
    void testGetMyNotifications_ReturnsList() {
        NotificationResponse item = new NotificationResponse(
                1, 2, "ANNOUNCEMENT", "Title", "Body", "#dashboard", false, null, LocalDateTime.now()
        );
        when(notificationService.resolveUser(authentication)).thenReturn(user);
        when(notificationService.getUserNotifications(user, false)).thenReturn(List.of(item));

        ResponseEntity<List<NotificationResponse>> response =
                notificationController.getMyNotifications(false, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("Title", response.getBody().get(0).getTitle());
    }

    @Test
    void testGetUnreadCount_ReturnsBadgeCount() {
        when(notificationService.resolveUser(authentication)).thenReturn(user);
        when(notificationService.getUnreadCount(user)).thenReturn(3L);

        ResponseEntity<Map<String, Long>> response = notificationController.getUnreadCount(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(3L, response.getBody().get("unreadCount"));
    }

    @Test
    void testMarkAsRead_UpdatesSingleNotification() {
        NotificationResponse item = new NotificationResponse(
                5, 2, "SYSTEM", "Title", "Body", null, true, LocalDateTime.now(), LocalDateTime.now()
        );
        when(notificationService.resolveUser(authentication)).thenReturn(user);
        when(notificationService.markAsRead(user, 5)).thenReturn(item);

        ResponseEntity<NotificationResponse> response = notificationController.markAsRead(5, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isRead());
    }

    @Test
    void testMarkAllAsRead_MarksAllAndReturnsCount() {
        when(notificationService.resolveUser(authentication)).thenReturn(user);
        when(notificationService.markAllAsRead(user)).thenReturn(6);

        ResponseEntity<Map<String, Object>> response = notificationController.markAllAsRead(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(6, response.getBody().get("updatedCount"));
    }
}

