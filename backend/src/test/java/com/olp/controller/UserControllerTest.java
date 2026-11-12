package com.olp.controller;

import com.olp.dto.UpdateProfileRequest;
import com.olp.dto.UserResponse;
import com.olp.entity.User;
import com.olp.exception.DuplicateEmailException;
import com.olp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User user;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        user = new User("Sam Student", "sam@example.com", "hash", "student");
        user.setId(7);
        authentication = new UsernamePasswordAuthenticationToken(user, null);
    }

    @Test
    void testGetProfile_ReturnsProfileForPrincipal() {
        UserResponse profile = new UserResponse(7, "Sam Student", "sam@example.com", "student");
        when(userService.getProfile(user)).thenReturn(profile);

        ResponseEntity<UserResponse> response = userController.getProfile(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertSame(profile, response.getBody());
    }

    @Test
    void testGetProfile_NonUserPrincipalThrowsClassCastException() {
        Authentication wrong = new UsernamePasswordAuthenticationToken("someone", null);

        assertThrows(ClassCastException.class, () -> userController.getProfile(wrong));
        verifyNoInteractions(userService);
    }

    @Test
    void testUpdateProfile_ReturnsUpdatedProfile() {
        UpdateProfileRequest request = new UpdateProfileRequest("Samuel", "samuel@example.com");
        UserResponse updated = new UserResponse(7, "Samuel", "samuel@example.com", "student");
        when(userService.updateProfile(user, request)).thenReturn(updated);

        ResponseEntity<UserResponse> response = userController.updateProfile(authentication, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Samuel", response.getBody().getName());
        assertEquals("samuel@example.com", response.getBody().getEmail());
        verify(userService, times(1)).updateProfile(user, request);
    }

    @Test
    void testUpdateProfile_DuplicateEmailPropagates() {
        UpdateProfileRequest request = new UpdateProfileRequest("Sam", "taken@example.com");
        when(userService.updateProfile(user, request))
                .thenThrow(new DuplicateEmailException("An account with this email already exists"));

        assertThrows(DuplicateEmailException.class, () -> userController.updateProfile(authentication, request));
    }
}
