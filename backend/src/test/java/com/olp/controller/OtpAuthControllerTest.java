package com.olp.controller;

import com.olp.dto.AuthResponse;
import com.olp.dto.RegisterRequest;
import com.olp.dto.ResetPasswordWithOtpRequest;
import com.olp.dto.SendOtpRequest;
import com.olp.dto.SendOtpResponse;
import com.olp.dto.UserResponse;
import com.olp.dto.VerifyRegistrationOtpRequest;
import com.olp.exception.OtpInvalidException;
import com.olp.service.OtpAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpAuthControllerTest {

    @Mock
    private OtpAuthService otpAuthService;

    @InjectMocks
    private OtpAuthController otpAuthController;

    @Test
    void testSendRegistrationOtp_Returns200() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("sam@example.com");
        SendOtpResponse expected = new SendOtpResponse("Verification code sent", "sa***@example.com");
        when(otpAuthService.sendRegistrationOtp(request)).thenReturn(expected);

        ResponseEntity<SendOtpResponse> response = otpAuthController.sendRegistrationOtp(request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void testVerifyRegistrationOtp_Returns201WithAuthResponse() {
        VerifyRegistrationOtpRequest request = new VerifyRegistrationOtpRequest("sam@example.com", "123456");
        AuthResponse expected = new AuthResponse("Account successfully verified!", "jwt",
                new UserResponse(7, "Sam", "sam@example.com", "student"));
        when(otpAuthService.verifyRegistrationOtp(request)).thenReturn(expected);

        ResponseEntity<AuthResponse> response = otpAuthController.verifyRegistrationOtp(request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("jwt", response.getBody().getToken());
    }

    @Test
    void testVerifyRegistrationOtp_InvalidOtpPropagates() {
        VerifyRegistrationOtpRequest request = new VerifyRegistrationOtpRequest("sam@example.com", "000000");
        when(otpAuthService.verifyRegistrationOtp(request)).thenThrow(new OtpInvalidException("Invalid verification code"));

        assertThrows(OtpInvalidException.class, () -> otpAuthController.verifyRegistrationOtp(request));
    }

    @Test
    void testResendRegistrationOtp_PassesEmailToService() {
        SendOtpRequest request = new SendOtpRequest("sam@example.com");
        SendOtpResponse expected = new SendOtpResponse("A fresh verification code was sent", "sa***@example.com");
        when(otpAuthService.resendRegistrationOtp("sam@example.com")).thenReturn(expected);

        ResponseEntity<SendOtpResponse> response = otpAuthController.resendRegistrationOtp(request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
        verify(otpAuthService, times(1)).resendRegistrationOtp("sam@example.com");
    }

    @Test
    void testSendPasswordResetOtp_PassesEmailToService() {
        SendOtpRequest request = new SendOtpRequest("sam@example.com");
        SendOtpResponse expected = new SendOtpResponse("Reset code sent", "sa***@example.com");
        when(otpAuthService.sendPasswordResetOtp("sam@example.com")).thenReturn(expected);

        ResponseEntity<SendOtpResponse> response = otpAuthController.sendPasswordResetOtp(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("sa***@example.com", response.getBody().getMaskedEmail());
    }

    @Test
    void testResetPasswordWithOtp_Returns200WithSuccessMessage() {
        ResetPasswordWithOtpRequest request =
                new ResetPasswordWithOtpRequest("sam@example.com", "123456", "newpass1", "newpass1");

        ResponseEntity<Map<String, String>> response = otpAuthController.resetPasswordWithOtp(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Password has been successfully reset. You may now log in with your new password.",
                response.getBody().get("message"));
        verify(otpAuthService, times(1)).resetPasswordWithOtp(request);
    }

    @Test
    void testResetPasswordWithOtp_ServiceFailurePropagates() {
        ResetPasswordWithOtpRequest request =
                new ResetPasswordWithOtpRequest("sam@example.com", "123456", "newpass1", "different");
        doThrow(new IllegalArgumentException("New password and confirmation password do not match."))
                .when(otpAuthService).resetPasswordWithOtp(request);

        assertThrows(IllegalArgumentException.class, () -> otpAuthController.resetPasswordWithOtp(request));
    }
}
