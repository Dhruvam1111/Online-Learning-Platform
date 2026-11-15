package com.olp.controller;

import com.olp.dto.AnnouncementRequest;
import com.olp.dto.AnnouncementResponse;
import com.olp.security.CustomUserDetails;
import com.olp.service.InstructorAnnouncementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorAnnouncementControllerTest {

    @Mock
    private InstructorAnnouncementService announcementService;

    @InjectMocks
    private InstructorAnnouncementController controller;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getId()).thenReturn(10);
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
    }

    @Test
    void createAnnouncement_returns201Created() {
        AnnouncementRequest req = new AnnouncementRequest("Week 2 Live Session", "Join us on Sunday at 6 PM.");
        AnnouncementResponse mockRes = new AnnouncementResponse();
        mockRes.setId(1);
        mockRes.setCourseId(101);
        mockRes.setTitle("Week 2 Live Session");
        mockRes.setNotifiedStudentCount(15);

        when(announcementService.createAnnouncement(10, 101, req)).thenReturn(mockRes);

        ResponseEntity<AnnouncementResponse> response = controller.createAnnouncement(101, req, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(15, response.getBody().getNotifiedStudentCount());
    }

    @Test
    void getInstructorCourseAnnouncements_returnsList() {
        AnnouncementResponse item = new AnnouncementResponse();
        item.setId(2);
        item.setTitle("Welcome to the Course");
        when(announcementService.getCourseAnnouncementsForInstructor(10, 101)).thenReturn(List.of(item));

        ResponseEntity<List<AnnouncementResponse>> response = controller.getInstructorCourseAnnouncements(101, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }
}

