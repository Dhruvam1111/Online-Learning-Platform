package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for Core User Authentication & Google OAuth SSO.
 * Maintained by Person 1 (Developer A: Identity, JWT & Modern OAuth Provider).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register
     * Public — direct credentials registration.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     * Public — authenticate with Email + Password.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/auth/verify-email?token=...
     * Public — no token header required.
     * FR-I01: Validates instructor email verification link.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse> verifyEmail(@RequestParam("token") String token) {
        ApiResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/google
     * Public — authenticate or auto-register via official Google account.
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse response = authService.loginOrRegisterWithGoogle(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/password-reset/request
     * Public — request a single-use password reset link sent to email.
     */
    @PostMapping("/password-reset/request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@Valid @RequestBody ForgotPasswordRequest request) {
        Map<String, String> response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/forgot-password
     * Public — alias for password reset request.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        Map<String, String> response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/password-reset/confirm
     * Public — confirm password reset with single-use token and invalidate all previous sessions.
     */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(@Valid @RequestBody ResetPasswordTokenRequest request) {
        Map<String, String> response = authService.resetPasswordWithToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/reset-password
     * Public — alias for password reset confirmation.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordTokenRequest request) {
        Map<String, String> response = authService.resetPasswordWithToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/logout
     * Authenticated — terminates user session, blacklists token, and clears SecurityContext.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /**
     * GET /api/auth/me
     * Authenticated — returns the currently authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        UserResponse response = authService.getCurrentUser(currentUser);
        return ResponseEntity.ok(response);
    }
}
