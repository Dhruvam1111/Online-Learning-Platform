package com.olp.controller;

import com.olp.dto.StudentProfileResponse;
import com.olp.dto.UpdateStudentProfileRequest;
import com.olp.entity.User;
import com.olp.service.StudentProfileService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentProfileControllerTest {

    @Mock
    private StudentProfileService studentProfileService;

    @InjectMocks
    private StudentProfileController studentProfileController;

    private User student;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        student = new User("Sam Student", "sam@example.com", "hash", "student");
        student.setId(7);
        authentication = new UsernamePasswordAuthenticationToken(student, null);
    }

    @Test
    void testGetProfile_ReturnsProfile() {
        StudentProfileResponse profile = new StudentProfileResponse(
                7, "Sam Student", "sam@example.com", "student", "Bachelor", "Java", "Get a job", "Bio");
        when(studentProfileService.resolveUser(authentication)).thenReturn(student);
        when(studentProfileService.getProfile(student)).thenReturn(profile);

        ResponseEntity<StudentProfileResponse> response = studentProfileController.getProfile(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertSame(profile, response.getBody());
        assertEquals("Bachelor", response.getBody().educationLevel());
    }

    @Test
    void testGetProfile_NonStudentPropagatesAccessDenied() {
        when(studentProfileService.resolveUser(authentication)).thenReturn(student);
        when(studentProfileService.getProfile(student))
                .thenThrow(new AccessDeniedException("Access denied: only students can view student profiles"));

        assertThrows(AccessDeniedException.class, () -> studentProfileController.getProfile(authentication));
    }

    @Test
    void testUpdateProfile_ReturnsUpdatedProfile() {
        UpdateStudentProfileRequest request = new UpdateStudentProfileRequest("Samuel", "Master", "AI", "Research", "New bio");
        StudentProfileResponse updated = new StudentProfileResponse(
                7, "Samuel", "sam@example.com", "student", "Master", "AI", "Research", "New bio");
        when(studentProfileService.resolveUser(authentication)).thenReturn(student);
        when(studentProfileService.updateProfile(student, request)).thenReturn(updated);

        ResponseEntity<StudentProfileResponse> response = studentProfileController.updateProfile(authentication, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Samuel", response.getBody().name());
        assertEquals("Master", response.getBody().educationLevel());
        verify(studentProfileService, times(1)).updateProfile(student, request);
    }

    @Test
    void testUpdateProfile_BlankNameRejectedByServicePropagates() {
        UpdateStudentProfileRequest request = new UpdateStudentProfileRequest("   ", null, null, null, null);
        when(studentProfileService.resolveUser(authentication)).thenReturn(student);
        when(studentProfileService.updateProfile(student, request))
                .thenThrow(new IllegalArgumentException("Name cannot be empty"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> studentProfileController.updateProfile(authentication, request));

        assertEquals("Name cannot be empty", ex.getMessage());
    }
}
