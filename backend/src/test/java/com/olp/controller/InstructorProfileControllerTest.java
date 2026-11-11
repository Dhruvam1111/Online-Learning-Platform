package com.olp.controller;

import com.olp.dto.InstructorProfileResponse;
import com.olp.dto.InstructorSummaryResponse;
import com.olp.dto.UpdateInstructorProfileRequest;
import com.olp.entity.User;
import com.olp.service.InstructorProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorProfileControllerTest {

    @Mock
    private InstructorProfileService instructorProfileService;

    @InjectMocks
    private InstructorProfileController controller;

    private User instructor;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        instructor = new User("Dr. Alan Turing", "turing@olp.com", "hash", "instructor", "active");
        instructor.setId(10);
        authentication = new UsernamePasswordAuthenticationToken(instructor, null);
    }

    @Test
    void testGetProfile_Success() {
        InstructorProfileResponse expected = new InstructorProfileResponse(
                10, "Dr. Alan Turing", "turing@olp.com", "instructor",
                "Ph.D.", "AI", "Bio", LocalDateTime.now(), LocalDateTime.now()
        );

        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);
        when(instructorProfileService.getProfile(instructor)).thenReturn(expected);

        ResponseEntity<InstructorProfileResponse> response = controller.getProfile(authentication, null, null, null);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void testGetProfile_DifferentIdInQuery_ThrowsAccessDenied() {
        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);

        assertThrows(AccessDeniedException.class,
                () -> controller.getProfile(authentication, 999, null, null));
    }

    @Test
    void testUpdateProfile_Success() {
        UpdateInstructorProfileRequest request = new UpdateInstructorProfileRequest("Prof. Alan", "D.Sc.", "ML", "New Bio");
        InstructorProfileResponse expected = new InstructorProfileResponse(
                10, "Prof. Alan", "turing@olp.com", "instructor",
                "D.Sc.", "ML", "New Bio", LocalDateTime.now(), LocalDateTime.now()
        );

        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);
        when(instructorProfileService.updateProfile(instructor, request)).thenReturn(expected);

        ResponseEntity<InstructorProfileResponse> response = controller.updateProfile(authentication, request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
        verify(instructorProfileService).updateProfile(instructor, request);
    }

    @Test
    void testGetInstructorSummary_PublicEndpoint_ReturnsSummary() {
        InstructorSummaryResponse expected = new InstructorSummaryResponse(
                10, "Dr. Alan Turing", "turing@olp.com",
                "Ph.D.", "AI", "Bio", LocalDateTime.now(), LocalDateTime.now()
        );

        when(instructorProfileService.getInstructorSummary(10)).thenReturn(expected);

        ResponseEntity<InstructorSummaryResponse> response = controller.getInstructorSummary(10);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void testGetCurrentInstructorSummary_Authenticated_ReturnsSummary() {
        InstructorSummaryResponse expected = new InstructorSummaryResponse(
                10, "Dr. Alan Turing", "turing@olp.com",
                "Ph.D.", "AI", "Bio", LocalDateTime.now(), LocalDateTime.now()
        );

        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);
        when(instructorProfileService.getInstructorSummary(10)).thenReturn(expected);

        ResponseEntity<InstructorSummaryResponse> response = controller.getCurrentInstructorSummary(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }
}

