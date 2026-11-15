package com.olp.controller;

import com.olp.dto.AuthResponse;
import com.olp.dto.GoogleAuthRequest;
import com.olp.dto.LoginRequest;
import com.olp.dto.RegisterRequest;
import com.olp.dto.UserResponse;
import com.olp.entity.User;
import com.olp.exception.DuplicateEmailException;
import com.olp.exception.InvalidCredentialsException;
import com.olp.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        user = new User("Sam Student", "sam@example.com", "hash", "student");
        user.setId(7);
        userResponse = new UserResponse(7, "Sam Student", "sam@example.com", "student");
    }

    @Test
    void testRegister_Returns201WithAuthResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Sam Student");
        request.setEmail("sam@example.com");
        request.setPassword("password123");
        request.setRole("student");
        AuthResponse expected = new AuthResponse("Registration successful", "jwt", userResponse);
        when(authService.register(request)).thenReturn(expected);

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertEquals(201, response.getStatusCode().value());
        assertSame(expected, response.getBody());
        verify(authService, times(1)).register(request);
    }

    @Test
    void testRegister_DuplicateEmailPropagates() {
        RegisterRequest request = new RegisterRequest();
        when(authService.register(request)).thenThrow(new DuplicateEmailException("An account with this email already exists"));

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class, () -> authController.register(request));

        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void testLogin_Returns200WithAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("sam@example.com");
        request.setPassword("password123");
        AuthResponse expected = new AuthResponse("Login successful", "jwt", userResponse);
        when(authService.login(request)).thenReturn(expected);

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("jwt", Objects.requireNonNull(response.getBody()).getToken());
        assertEquals(7, Objects.requireNonNull(response.getBody()).getUser().getId());
    }

    @Test
    void testLogin_InvalidCredentialsPropagates() {
        LoginRequest request = new LoginRequest();
        when(authService.login(request)).thenThrow(new InvalidCredentialsException("Invalid email or password"));

        assertThrows(InvalidCredentialsException.class, () -> authController.login(request));
    }

    @Test
    void testLoginWithGoogle_Returns200() {
        GoogleAuthRequest request = new GoogleAuthRequest("sam@example.com", "Sam", "id-token");
        AuthResponse expected = new AuthResponse("Authenticated successfully via Google", "jwt", userResponse);
        when(authService.loginOrRegisterWithGoogle(request)).thenReturn(expected);

        ResponseEntity<AuthResponse> response = authController.loginWithGoogle(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Authenticated successfully via Google", Objects.requireNonNull(response.getBody()).getMessage());
        verify(authService, times(1)).loginOrRegisterWithGoogle(request);
    }

    @Test
    void testLogout_Returns200WithMessageAndNoServiceCall() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        ResponseEntity<Map<String, String>> response = authController.logout(httpRequest);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Logged out successfully", Objects.requireNonNull(response.getBody()).get("message"));
        verify(authService, times(1)).logout(httpRequest);
    }

    @Test
    void testMe_ReturnsCurrentUserFromPrincipal() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null);
        when(authService.getCurrentUser(user)).thenReturn(userResponse);

        ResponseEntity<UserResponse> response = authController.me(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertSame(userResponse, response.getBody());
    }

    @Test
    void testMe_NonUserPrincipalThrowsClassCastException() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("not-a-user-entity", null);

        assertThrows(ClassCastException.class, () -> authController.me(authentication));
        verifyNoInteractions(authService);
    }
}
