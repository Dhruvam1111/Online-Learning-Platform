package com.olp.controller;

import com.olp.dto.*;
import com.olp.service.OtpAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for Email OTP Authentication & Password Recovery.
 */
@RestController
@RequestMapping("/api/auth")
public class OtpAuthController {

    private final OtpAuthService otpAuthService;

    public OtpAuthController(OtpAuthService otpAuthService) {
        this.otpAuthService = otpAuthService;
    }

    /**
     * POST /api/auth/register/send-otp
     * Public — initiates student registration and sends 6-digit verification OTP.
     */
    @PostMapping("/register/send-otp")
    public ResponseEntity<SendOtpResponse> sendRegistrationOtp(@Valid @RequestBody RegisterRequest request) {
        SendOtpResponse response = otpAuthService.sendRegistrationOtp(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/register/verify-otp
     * Public — verifies OTP and activates student account (returns JWT token).
     */
    @PostMapping("/register/verify-otp")
    public ResponseEntity<AuthResponse> verifyRegistrationOtp(@Valid @RequestBody VerifyRegistrationOtpRequest request) {
        AuthResponse response = otpAuthService.verifyRegistrationOtp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/register/resend-otp
     * Public — resends verification OTP for an unverified student account.
     */
    @PostMapping("/register/resend-otp")
    public ResponseEntity<SendOtpResponse> resendRegistrationOtp(@Valid @RequestBody SendOtpRequest request) {
        SendOtpResponse response = otpAuthService.resendRegistrationOtp(request.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/password-reset/send-otp
     * Public — send 6-digit OTP to email for password reset.
     */
    @PostMapping("/password-reset/send-otp")
    public ResponseEntity<SendOtpResponse> sendPasswordResetOtp(@Valid @RequestBody SendOtpRequest request) {
        SendOtpResponse response = otpAuthService.sendPasswordResetOtp(request.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/password-reset/reset
     * Public — verify OTP and reset password without knowing old password.
     */
    @PostMapping("/password-reset/reset")
    public ResponseEntity<Map<String, String>> resetPasswordWithOtp(@Valid @RequestBody ResetPasswordWithOtpRequest request) {
        otpAuthService.resetPasswordWithOtp(request);
        return ResponseEntity.ok(Map.of("message", "Password has been successfully reset. You may now log in with your new password."));
    }
}
