package com.olp.service;

import com.olp.dto.ForgotPasswordRequest;
import com.olp.dto.LoginRequest;
import com.olp.dto.ResetPasswordTokenRequest;
import com.olp.entity.User;
import com.olp.exception.AccountStatusException;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.InstructorProfileRepository;
import com.olp.repository.UserRepository;
import com.olp.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginLogoutPasswordResetTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private InstructorProfileRepository instructorProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    private TokenBlacklistService tokenBlacklistService;
    private AuthService authService;

    private User activeInstructor;
    private User pendingInstructor;
    private User suspendedUser;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService();
        authService = new AuthService(
                userRepository,
                instructorProfileRepository,
                passwordEncoder,
                jwtUtil,
                tokenBlacklistService,
                emailService
        );

        activeInstructor = new User("Dr. Jane", "jane@olp.com", "$2a$10$hashedPasswordJane", "instructor", "active");
        activeInstructor.setId(10);

        pendingInstructor = new User("Dr. Smith", "smith@olp.com", "$2a$10$hashedPasswordSmith", "instructor", "pending");
        pendingInstructor.setId(20);

        suspendedUser = new User("Bad User", "bad@olp.com", "$2a$10$hashedPasswordBad", "student", "suspended");
        suspendedUser.setId(30);
    }

    // =========================================================================
    // 1. LOGIN TESTS
    // =========================================================================

    @Test
    void testLogin_ActiveInstructor_Success_PasswordsVerifiedWithBCrypt() {
        LoginRequest request = new LoginRequest("jane@olp.com", "SecretPass123!");
        when(userRepository.findByEmail("jane@olp.com")).thenReturn(Optional.of(activeInstructor));
        when(passwordEncoder.matches("SecretPass123!", "$2a$10$hashedPasswordJane")).thenReturn(true);
        when(jwtUtil.generateToken(10, "instructor")).thenReturn("jwt.token.instructor");

        var response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt.token.instructor", response.getToken());
        assertEquals("jane@olp.com", response.getUser().getEmail());
        assertEquals("instructor", response.getUser().getRole());
        verify(passwordEncoder).matches("SecretPass123!", "$2a$10$hashedPasswordJane");
    }

    @Test
    void testLogin_InvalidPassword_ThrowsInvalidCredentials() {
        LoginRequest request = new LoginRequest("jane@olp.com", "WrongPassword");
        when(userRepository.findByEmail("jane@olp.com")).thenReturn(Optional.of(activeInstructor));
        when(passwordEncoder.matches("WrongPassword", "$2a$10$hashedPasswordJane")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void testLogin_PendingInstructor_ThrowsAccountStatusExceptionWithAwaitingApprovalMessage() {
        LoginRequest request = new LoginRequest("smith@olp.com", "SecretPass123!");
        when(userRepository.findByEmail("smith@olp.com")).thenReturn(Optional.of(pendingInstructor));
        when(passwordEncoder.matches("SecretPass123!", "$2a$10$hashedPasswordSmith")).thenReturn(true);

        AccountStatusException ex = assertThrows(AccountStatusException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("Awaiting admin approval"));
    }

    @Test
    void testLogin_SuspendedUser_ThrowsAccountStatusException() {
        LoginRequest request = new LoginRequest("bad@olp.com", "SecretPass123!");
        when(userRepository.findByEmail("bad@olp.com")).thenReturn(Optional.of(suspendedUser));
        when(passwordEncoder.matches("SecretPass123!", "$2a$10$hashedPasswordBad")).thenReturn(true);

        AccountStatusException ex = assertThrows(AccountStatusException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().toLowerCase().contains("suspended"));
    }

    // =========================================================================
    // 2. LOGOUT & TOKEN BLACKLISTING TESTS
    // =========================================================================

    @Test
    void testLogout_BlacklistsTokenAndClearsSession() {
        String token = "valid.jwt.token.to.blacklist";
        assertFalse(tokenBlacklistService.isTokenBlacklisted(token));

        authService.logout(token);

        assertTrue(tokenBlacklistService.isTokenBlacklisted(token));
    }

    @Test
    void testLogout_FromHttpServletRequest_ExtractsBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token123");

        authService.logout(request);

        assertTrue(tokenBlacklistService.isTokenBlacklisted("token123"));
    }

    // =========================================================================
    // 3. PASSWORD RESET TESTS
    // =========================================================================

    @Test
    void testForgotPassword_GeneratesSingleUseTokenAndDispatchesEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("jane@olp.com");
        when(userRepository.findByEmail("jane@olp.com")).thenReturn(Optional.of(activeInstructor));
        when(jwtUtil.generatePasswordResetToken(eq(10), anyLong())).thenReturn("reset.jwt.token.10");

        Map<String, String> result = authService.forgotPassword(request);

        assertNotNull(result);
        assertTrue(result.get("resetLink").contains("reset.jwt.token.10"));
        verify(emailService).sendPasswordResetLinkEmail(eq("jane@olp.com"), eq("Dr. Jane"), anyString(), eq(15));
    }

    @Test
    void testForgotPassword_UserNotFound_ThrowsResourceNotFoundException() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("unknown@olp.com");
        when(userRepository.findByEmail("unknown@olp.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.forgotPassword(request));
        verify(jwtUtil, never()).generatePasswordResetToken(anyInt(), anyLong());
    }

    @Test
    void testResetPasswordWithToken_Success_HashesNewPasswordAndInvalidatesAllUserSessions() {
        String resetToken = "valid.reset.token";
        ResetPasswordTokenRequest request = new ResetPasswordTokenRequest(resetToken, "BrandNewPassword123!", "BrandNewPassword123!");

        when(jwtUtil.validatePasswordResetToken(resetToken)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(resetToken)).thenReturn(10);
        when(userRepository.findById(10)).thenReturn(Optional.of(activeInstructor));
        when(passwordEncoder.encode("BrandNewPassword123!")).thenReturn("$2a$10$newlyHashedPassword");

        Map<String, String> response = authService.resetPasswordWithToken(request);

        assertNotNull(response);
        assertTrue(response.get("message").contains("successfully reset"));
        assertEquals("$2a$10$newlyHashedPassword", activeInstructor.getPasswordHash());
        verify(userRepository).save(activeInstructor);

        // Verify single-use token was blacklisted
        assertTrue(tokenBlacklistService.isTokenBlacklisted(resetToken));

        // Verify previous sessions for user 10 are revoked
        Date oldTokenIssueTime = new Date(System.currentTimeMillis() - 5000);
        assertTrue(tokenBlacklistService.isUserTokenRevoked(10, oldTokenIssueTime));
    }

    @Test
    void testResetPasswordWithToken_MismatchedConfirmPassword_ThrowsIllegalArgument() {
        String resetToken = "valid.reset.token";
        ResetPasswordTokenRequest request = new ResetPasswordTokenRequest(resetToken, "NewPass12345!", "DifferentPass12345!");

        when(jwtUtil.validatePasswordResetToken(resetToken)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.resetPasswordWithToken(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testResetPasswordWithToken_AlreadyUsedToken_ThrowsInvalidCredentials() {
        String resetToken = "consumed.reset.token";
        tokenBlacklistService.blacklistToken(resetToken);

        ResetPasswordTokenRequest request = new ResetPasswordTokenRequest(resetToken, "NewPass12345!", "NewPass12345!");
        when(jwtUtil.validatePasswordResetToken(resetToken)).thenReturn(true);

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> authService.resetPasswordWithToken(request));
        assertTrue(ex.getMessage().contains("already been used"));
    }

    @Test
    void testResetPasswordWithToken_InvalidOrExpiredToken_ThrowsInvalidCredentials() {
        String resetToken = "expired.or.tampered.token";
        ResetPasswordTokenRequest request = new ResetPasswordTokenRequest(resetToken, "NewPass12345!", "NewPass12345!");
        when(jwtUtil.validatePasswordResetToken(resetToken)).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.resetPasswordWithToken(request));
    }
}

