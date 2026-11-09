package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.TwoFactorToken;
import com.olp.entity.User;
import com.olp.exception.*;
import com.olp.repository.TwoFactorTokenRepository;
import com.olp.repository.UserRepository;
import com.olp.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpAuthServiceTest {

    @Mock
    private TwoFactorTokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private OtpAuthService otpAuthService;

    // ---------- helpers ----------

    private User user(Integer id, String email, String role, String status) {
        User u = new User("Test User", email, "oldhash", role, status);
        u.setId(id);
        return u;
    }

    private TwoFactorToken token(User u, String code, String purpose, int attempts, boolean expired) {
        LocalDateTime now = LocalDateTime.now();
        TwoFactorToken t = new TwoFactorToken(
                u, code, now.minusMinutes(5), expired ? now.minusMinutes(1) : now.plusMinutes(10), purpose);
        t.setAttemptCount(attempts);
        return t;
    }

    private RegisterRequest registerRequest(String name, String email, String password) {
        RegisterRequest r = new RegisterRequest();
        r.setName(name);
        r.setEmail(email);
        r.setPassword(password);
        r.setRole("student");
        return r;
    }

    private void assertFreshToken(TwoFactorToken t, User owner, String purpose, LocalDateTime before, LocalDateTime after) {
        assertSame(owner, t.getUser());
        assertEquals(purpose, t.getPurpose());
        assertTrue(t.getOtpCode().matches("\\d{6}"), "OTP must be 6 digits but was " + t.getOtpCode());
        assertFalse(t.isUsed());
        assertEquals(0, t.getAttemptCount());
        assertFalse(t.getExpiresAt().isBefore(before.plusMinutes(10)));
        assertFalse(t.getExpiresAt().isAfter(after.plusMinutes(10)));
    }

    // =====================================================================
    // sendRegistrationOtp
    // =====================================================================

    @Test
    void testSendRegistrationOtp_DomainWithoutDot_ThrowsAndTouchesNothing() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> otpAuthService.sendRegistrationOtp(registerRequest("Al", "user@localhost", "pw")));

        assertTrue(ex.getMessage().contains("Email does not exist"));
        verifyNoInteractions(userRepository, tokenRepository, emailService, passwordEncoder);
    }

    @Test
    void testSendRegistrationOtp_EmailWithoutAtSign_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> otpAuthService.sendRegistrationOtp(registerRequest("Al", "plainaddress", "pw")));

        verifyNoInteractions(userRepository, tokenRepository, emailService);
    }

    @Test
    void testSendRegistrationOtp_ActiveAccountExists_ThrowsDuplicateEmail() {
        when(userRepository.findByEmail("taken@example.com"))
                .thenReturn(Optional.of(user(1, "taken@example.com", "student", "active")));

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class,
                () -> otpAuthService.sendRegistrationOtp(registerRequest("Al", "taken@example.com", "pw")));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any());
        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void testSendRegistrationOtp_ActiveStatusCheckIsCaseInsensitive() {
        when(userRepository.findByEmail("taken@example.com"))
                .thenReturn(Optional.of(user(1, "taken@example.com", "student", "ACTIVE")));

        assertThrows(DuplicateEmailException.class,
                () -> otpAuthService.sendRegistrationOtp(registerRequest("Al", "taken@example.com", "pw")));
    }

    @Test
    void testSendRegistrationOtp_NewUser_CreatesPendingStudentAndSendsOtp() {
        when(userRepository.findByEmail("new.user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        LocalDateTime before = LocalDateTime.now();

        SendOtpResponse response = otpAuthService.sendRegistrationOtp(
                registerRequest("  Alice  ", "  New.User@Example.COM ", "pw"));

        LocalDateTime after = LocalDateTime.now();
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("Alice", saved.getName());
        assertEquals("new.user@example.com", saved.getEmail());
        assertEquals("hashed", saved.getPasswordHash());
        assertEquals("student", saved.getRole());
        assertEquals("pending", saved.getAccountStatus());

        verify(tokenRepository).invalidateAllForUserAndPurpose(saved, "REGISTRATION");
        ArgumentCaptor<TwoFactorToken> tokenCaptor = ArgumentCaptor.forClass(TwoFactorToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertFreshToken(tokenCaptor.getValue(), saved, "REGISTRATION", before, after);

        verify(emailService).sendOtpEmail("new.user@example.com", "Alice",
                tokenCaptor.getValue().getOtpCode(), "Student Account Verification", 10);
        assertEquals("Verification code sent to new.user@example.com. It is valid for 10 minutes.", response.getMessage());
        assertEquals("ne***@example.com", response.getMaskedEmail());
    }

    @Test
    void testSendRegistrationOtp_ShortLocalPart_IsMaskedWithSingleCharacter() {
        when(userRepository.findByEmail("ab@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        SendOtpResponse response = otpAuthService.sendRegistrationOtp(registerRequest("Ab", "ab@example.com", "pw"));

        assertEquals("a***@example.com", response.getMaskedEmail());
    }

    @Test
    void testSendRegistrationOtp_PendingUserExists_UpdatesDetailsInsteadOfCreating() {
        User pending = user(8, "pending@gmail.com", "instructor", "pending");
        when(userRepository.findByEmail("pending@gmail.com")).thenReturn(Optional.of(pending));
        when(passwordEncoder.encode("newpw")).thenReturn("newhash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        otpAuthService.sendRegistrationOtp(registerRequest(" Bob ", "pending@gmail.com", "newpw"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertSame(pending, userCaptor.getValue());
        assertEquals("Bob", pending.getName());
        assertEquals("newhash", pending.getPasswordHash());
        assertEquals("student", pending.getRole());
        verify(tokenRepository).invalidateAllForUserAndPurpose(pending, "REGISTRATION");
        verify(tokenRepository).save(any(TwoFactorToken.class));
    }

    @Test
    void testSendRegistrationOtp_EmailDeliveryFailure_Propagates() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new IllegalArgumentException("Email delivery failed: smtp down"))
                .when(emailService).sendOtpEmail(anyString(), anyString(), anyString(), anyString(), anyInt());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> otpAuthService.sendRegistrationOtp(registerRequest("Nia", "new@example.com", "pw")));

        assertTrue(ex.getMessage().contains("smtp down"));
    }

    // =====================================================================
    // resendRegistrationOtp
    // =====================================================================

    @Test
    void testResendRegistrationOtp_UnknownEmail_ThrowsNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> otpAuthService.resendRegistrationOtp("ghost@example.com"));

        assertTrue(ex.getMessage().contains("Email does not exist"));
        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void testResendRegistrationOtp_AlreadyActive_ThrowsIllegalArgument() {
        when(userRepository.findByEmail("done@example.com"))
                .thenReturn(Optional.of(user(1, "done@example.com", "student", "active")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> otpAuthService.resendRegistrationOtp("done@example.com"));

        assertTrue(ex.getMessage().contains("already verified"));
        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void testResendRegistrationOtp_PendingUser_IssuesFreshToken() {
        User pending = user(3, "pending@example.com", "student", "pending");
        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(pending));
        LocalDateTime before = LocalDateTime.now();

        SendOtpResponse response = otpAuthService.resendRegistrationOtp("  Pending@Example.com ");

        LocalDateTime after = LocalDateTime.now();
        verify(tokenRepository).invalidateAllForUserAndPurpose(pending, "REGISTRATION");
        ArgumentCaptor<TwoFactorToken> captor = ArgumentCaptor.forClass(TwoFactorToken.class);
        verify(tokenRepository).save(captor.capture());
        assertFreshToken(captor.getValue(), pending, "REGISTRATION", before, after);
        verify(emailService).sendOtpEmail("pending@example.com", "Test User",
                captor.getValue().getOtpCode(), "Student Account Verification", 10);
        assertEquals("A fresh verification code was sent to your email.", response.getMessage());
        assertEquals("pe***@example.com", response.getMaskedEmail());
    }

    // =====================================================================
    // verifyRegistrationOtp
    // =====================================================================

    @Test
    void testVerifyRegistrationOtp_UnknownEmail_ThrowsNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> otpAuthService.verifyRegistrationOtp(new VerifyRegistrationOtpRequest("ghost@example.com", "123456")));
    }

    @Test
    void testVerifyRegistrationOtp_AlreadyActive_ThrowsIllegalArgument() {
        when(userRepository.findByEmail("done@example.com"))
                .thenReturn(Optional.of(user(1, "done@example.com", "student", "active")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> otpAuthService.verifyRegistrationOtp(new VerifyRegistrationOtpRequest("done@example.com", "123456")));

        assertTrue(ex.getMessage().contains("already verified"));
        verifyNoInteractions(tokenRepository);
    }

    @Test
    void testVerifyRegistrationOtp_NoActiveToken_ThrowsOtpInvalid() {
        User pending = user(2, "p@example.com", "student", "pending");
        when(userRepository.findByEmail("p@example.com")).thenReturn(Optional.of(pending));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(pending, "REGISTRATION")).thenReturn(Optional.empty());

        OtpInvalidException ex = assertThrows(OtpInvalidException.class,
                () -> otpAuthService.verifyRegistrationOtp(new VerifyRegistrationOtpRequest("p@example.com", "123456")));

        assertTrue(ex.getMessage().contains("No active registration verification code"));
    }

    @Test
    void testVerifyRegistrationOtp_ThreeAttemptsUsed_ThrowsTooManyAttemptsWithoutSaving() {
        User pending = user(2, "p@example.com", "student", "pending");
        TwoFactorToken t = token(pending, "123456", "REGISTRATION", 3, false);
        when(userRepository.findByEmail("p@example.com")).thenReturn(Optional.of(pending));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(pending, "REGISTRATION")).thenReturn(Optional.of(t));

        assertThrows(TooManyOtpAttemptsException.class,
                () -> otpAuthService.verifyRegistrationOtp(new VerifyRegistrationOtpRequest("p@example.com", "123456")));

        verify(tokenRepository, never()).save(any());
        assertEquals(3, t.getAttemptCount());
    }

    @Test
    void testVerifyRegistrationOtp_FourAttemptsUsed_StillThrowsTooManyAttempts() {
        User pending = user(2, "p@example.com", "student", "pending");
        when(userRepository.findByEmail("p@example.com")).thenReturn(Optional.of(pending));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(pending, "REGISTRATION"))
                .thenReturn(Optional.of(token(pending, "123456", "REGISTRATION", 4, false)));

        assertThrows(TooManyOtpAttemptsException.class,
                () -> otpAuthService.verifyRegistrationOtp(new VerifyRegistrationOtpRequest("p@example.com", "123456")));
    }

    @Test
    void testVerifyRegistrationOtp_AttemptLimitIsCheckedBeforeExpiry() {
        User pending = user(2, "p@example.com", "student", "pending");
        TwoFactorToken t = token(pending, "123456", "REGISTRATION", 3, true);
        when(userRepository.findByEmail("p@example.com")).thenReturn(Optional.of(pending));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(pending, "REGISTRATION")).thenReturn(Optional.of(t));

        assertThrows(TooManyOtpAttemptsException.class,
                () -> otpAuthService.verifyRegistrationOtp(new VerifyRegistrationOtpRequest("p@example.com", "123456")));

        assertFalse(t.isUsed());
    }

    @Test
    void testVerifyRegistrationOtp_ExpiredToken_MarksUsedAndThrowsExpired() {
        User pending = user(2, "p@example.com", "student", "pending");
        TwoFactorToken t = token(pending, "123456", "REGISTRATION", 0, true);
        when(userRepository.findByEmail("p@example.com")).thenReturn(Optional.of(pending));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(pending, "REGISTRATION")).thenReturn(Optional.of(t));

        assertThrows(OtpExpiredException.class,
                () -> otpAuthService.verifyRegistrationOtp(new VerifyRegistrationOtpRequest("p@example.com", "123456")));

        assertTrue(t.isUsed());
        verify(tokenRepository).save(t);
        verify(userRepository, never()).save(any());
    }

    @Test
    void testVerifyRegistrationOtp_WrongCodeFirstAttempt_ReportsTwoRemaining() {
        User pending = user(2, "p@example.com", "student", "pending");
        TwoFactorToken t = token(pending, "123456", "REGISTRATION", 0, false);
        when(userRepository.findByEmail("p@example.com")).thenReturn(Optional.of(pending));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(pending, "REGISTRATION")).thenReturn(Optional.of(t));

        OtpInvalidException ex = assertThrows(OtpInvalidException.class,
                () -> otpAuthService.verifyRegistrationOtp(new VerifyRegistrationOtpRequest("p@example.com", "000000")));

        assertTrue(ex.getMessage().contains("2 attempt(s) remaining"));
        assertEquals(1, t.getAttemptCount());
        assertFalse(t.isUsed());
        verify(tokenRepository).save(t);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void testVerifyRegistrationOtp_WrongCodeOnThirdAttempt_ReportsZeroRemaining() {
        User pending = user(2, "p@example.com", "student", "pending");
        TwoFactorToken t = token(pending, "123456", "REGISTRATION", 2, false);
        when(userRepository.findByEmail("p@example.com")).thenReturn(Optional.of(pending));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(pending, "REGISTRATION")).thenReturn(Optional.of(t));

        OtpInvalidException ex = assertThrows(OtpInvalidException.class,
                () -> otpAuthService.verifyRegistrationOtp(new VerifyRegistrationOtpRequest("p@example.com", "000000")));

        assertTrue(ex.getMessage().contains("0 attempt(s) remaining"));
        assertEquals(3, t.getAttemptCount());
    }

    @Test
    void testVerifyRegistrationOtp_CorrectCodeOnThirdAttempt_StillSucceeds() {
        User pending = user(2, "p@example.com", "student", "pending");
        TwoFactorToken t = token(pending, "123456", "REGISTRATION", 2, false);
        when(userRepository.findByEmail("p@example.com")).thenReturn(Optional.of(pending));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(pending, "REGISTRATION")).thenReturn(Optional.of(t));
        when(jwtUtil.generateToken(2, "student")).thenReturn("jwt");

        AuthResponse response = otpAuthService.verifyRegistrationOtp(new VerifyRegistrationOtpRequest("p@example.com", "123456"));

        assertEquals("jwt", response.getToken());
        assertTrue(t.isUsed());
    }

    @Test
    void testVerifyRegistrationOtp_Success_ActivatesUserAndReturnsJwt() {
        User pending = user(2, "p@example.com", "student", "pending");
        TwoFactorToken t = token(pending, "123456", "REGISTRATION", 0, false);
        when(userRepository.findByEmail("p@example.com")).thenReturn(Optional.of(pending));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(pending, "REGISTRATION")).thenReturn(Optional.of(t));
        when(jwtUtil.generateToken(2, "student")).thenReturn("jwt-token");

        AuthResponse response = otpAuthService.verifyRegistrationOtp(
                new VerifyRegistrationOtpRequest("  P@Example.com ", " 123456 "));

        assertEquals("Account successfully verified! Welcome to Focotech.", response.getMessage());
        assertEquals("jwt-token", response.getToken());
        assertEquals(2, response.getUser().getId());
        assertEquals("p@example.com", response.getUser().getEmail());
        assertEquals("student", response.getUser().getRole());
        assertEquals("active", pending.getAccountStatus());
        assertTrue(t.isUsed());
        assertEquals(1, t.getAttemptCount());
        verify(tokenRepository).save(t);
        verify(userRepository).save(pending);
    }

    // =====================================================================
    // sendPasswordResetOtp
    // =====================================================================

    @Test
    void testSendPasswordResetOtp_InvalidDomain_ThrowsBeforeLookingUpUser() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> otpAuthService.sendPasswordResetOtp("user@localhost"));

        assertTrue(ex.getMessage().contains("Email does not exist"));
        verifyNoInteractions(userRepository, tokenRepository, emailService);
    }

    @Test
    void testSendPasswordResetOtp_UnknownEmail_ThrowsNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> otpAuthService.sendPasswordResetOtp("ghost@example.com"));

        assertTrue(ex.getMessage().contains("No account is registered"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "pending|Your instructor account is pending administrator approval",
            "suspended|Your account has been suspended. Please contact support",
            "rejected|Your instructor application was rejected",
            "SUSPENDED|Your account has been suspended. Please contact support"})
    void testSendPasswordResetOtp_BlockedStatuses_ThrowAccountStatusException(String status, String message) {
        when(userRepository.findByEmail("acct@example.com"))
                .thenReturn(Optional.of(user(4, "acct@example.com", "instructor", status)));

        AccountStatusException ex = assertThrows(AccountStatusException.class,
                () -> otpAuthService.sendPasswordResetOtp("acct@example.com"));

        assertEquals(message, ex.getMessage());
        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void testSendPasswordResetOtp_ActiveUser_IssuesPasswordResetToken() {
        User active = user(4, "acct@example.com", "student", "active");
        when(userRepository.findByEmail("acct@example.com")).thenReturn(Optional.of(active));
        LocalDateTime before = LocalDateTime.now();

        SendOtpResponse response = otpAuthService.sendPasswordResetOtp("  ACCT@example.com ");

        LocalDateTime after = LocalDateTime.now();
        verify(tokenRepository).invalidateAllForUserAndPurpose(active, "PASSWORD_RESET");
        ArgumentCaptor<TwoFactorToken> captor = ArgumentCaptor.forClass(TwoFactorToken.class);
        verify(tokenRepository).save(captor.capture());
        assertFreshToken(captor.getValue(), active, "PASSWORD_RESET", before, after);
        verify(emailService).sendOtpEmail("acct@example.com", "Test User",
                captor.getValue().getOtpCode(), "Password Reset", 10);
        assertEquals("OTP sent to your registered email address. It is valid for 10 minutes.", response.getMessage());
        assertEquals("ac***@example.com", response.getMaskedEmail());
    }

    @Test
    void testSendPasswordResetOtp_UnrecognisedStatusIsNotBlocked() {
        User odd = user(4, "acct@example.com", "student", "active-ish");
        when(userRepository.findByEmail("acct@example.com")).thenReturn(Optional.of(odd));

        assertDoesNotThrow(() -> otpAuthService.sendPasswordResetOtp("acct@example.com"));

        verify(tokenRepository).save(any(TwoFactorToken.class));
    }

    // =====================================================================
    // resetPasswordWithOtp
    // =====================================================================

    private ResetPasswordWithOtpRequest resetRequest(String code, String newPw, String confirmPw) {
        return new ResetPasswordWithOtpRequest("acct@example.com", code, newPw, confirmPw);
    }

    @Test
    void testResetPasswordWithOtp_UnknownEmail_ThrowsNotFound() {
        when(userRepository.findByEmail("acct@example.com")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> otpAuthService.resetPasswordWithOtp(resetRequest("123456", "a", "a")));

        assertTrue(ex.getMessage().contains("No account found"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "pending|Your instructor account is pending administrator approval",
            "suspended|Your account has been suspended. Please contact support",
            "rejected|Your instructor application was rejected"})
    void testResetPasswordWithOtp_BlockedStatuses_ThrowAccountStatusException(String status, String message) {
        when(userRepository.findByEmail("acct@example.com"))
                .thenReturn(Optional.of(user(4, "acct@example.com", "instructor", status)));

        AccountStatusException ex = assertThrows(AccountStatusException.class,
                () -> otpAuthService.resetPasswordWithOtp(resetRequest("123456", "a", "a")));

        assertEquals(message, ex.getMessage());
        verifyNoInteractions(tokenRepository);
    }

    @Test
    void testResetPasswordWithOtp_NoActiveToken_ThrowsOtpInvalid() {
        User u = user(4, "acct@example.com", "student", "active");
        when(userRepository.findByEmail("acct@example.com")).thenReturn(Optional.of(u));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(u, "PASSWORD_RESET")).thenReturn(Optional.empty());

        OtpInvalidException ex = assertThrows(OtpInvalidException.class,
                () -> otpAuthService.resetPasswordWithOtp(resetRequest("123456", "a", "a")));

        assertTrue(ex.getMessage().contains("No active OTP"));
    }

    @Test
    void testResetPasswordWithOtp_ThreeAttemptsUsed_ThrowsTooManyAttempts() {
        User u = user(4, "acct@example.com", "student", "active");
        when(userRepository.findByEmail("acct@example.com")).thenReturn(Optional.of(u));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(u, "PASSWORD_RESET"))
                .thenReturn(Optional.of(token(u, "123456", "PASSWORD_RESET", 3, false)));

        assertThrows(TooManyOtpAttemptsException.class,
                () -> otpAuthService.resetPasswordWithOtp(resetRequest("123456", "a", "a")));

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void testResetPasswordWithOtp_ExpiredToken_MarksUsedAndThrowsExpired() {
        User u = user(4, "acct@example.com", "student", "active");
        TwoFactorToken t = token(u, "123456", "PASSWORD_RESET", 0, true);
        when(userRepository.findByEmail("acct@example.com")).thenReturn(Optional.of(u));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(u, "PASSWORD_RESET")).thenReturn(Optional.of(t));

        assertThrows(OtpExpiredException.class,
                () -> otpAuthService.resetPasswordWithOtp(resetRequest("123456", "a", "a")));

        assertTrue(t.isUsed());
        verify(tokenRepository).save(t);
    }

    @Test
    void testResetPasswordWithOtp_WrongCode_ReportsRemainingAttemptsAndKeepsPassword() {
        User u = user(4, "acct@example.com", "student", "active");
        TwoFactorToken t = token(u, "123456", "PASSWORD_RESET", 0, false);
        when(userRepository.findByEmail("acct@example.com")).thenReturn(Optional.of(u));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(u, "PASSWORD_RESET")).thenReturn(Optional.of(t));

        OtpInvalidException ex = assertThrows(OtpInvalidException.class,
                () -> otpAuthService.resetPasswordWithOtp(resetRequest("999999", "a", "a")));

        assertTrue(ex.getMessage().contains("2 attempt(s) remaining"));
        assertEquals(1, t.getAttemptCount());
        assertFalse(t.isUsed());
        assertEquals("oldhash", u.getPasswordHash());
        verify(tokenRepository).save(t);
        verify(userRepository, never()).save(any());
    }

    @Test
    void testResetPasswordWithOtp_PasswordMismatch_ThrowsAndDoesNotConsumeToken() {
        User u = user(4, "acct@example.com", "student", "active");
        TwoFactorToken t = token(u, "123456", "PASSWORD_RESET", 0, false);
        when(userRepository.findByEmail("acct@example.com")).thenReturn(Optional.of(u));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(u, "PASSWORD_RESET")).thenReturn(Optional.of(t));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> otpAuthService.resetPasswordWithOtp(resetRequest("123456", "NewPass1", "Different")));

        assertTrue(ex.getMessage().contains("do not match"));
        assertFalse(t.isUsed());
        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    void testResetPasswordWithOtp_Success_ConsumesTokenAndStoresEncodedPassword() {
        User u = user(4, "acct@example.com", "student", "active");
        TwoFactorToken t = token(u, "123456", "PASSWORD_RESET", 0, false);
        when(userRepository.findByEmail("acct@example.com")).thenReturn(Optional.of(u));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(u, "PASSWORD_RESET")).thenReturn(Optional.of(t));
        when(passwordEncoder.encode("NewPass1")).thenReturn("newhash");

        otpAuthService.resetPasswordWithOtp(resetRequest(" 123456 ", "NewPass1", "NewPass1"));

        assertTrue(t.isUsed());
        assertEquals("newhash", u.getPasswordHash());
        verify(tokenRepository).save(t);
        verify(userRepository).save(u);
    }

    @Test
    void testResetPasswordWithOtp_CorrectCodeOnThirdAttempt_StillSucceeds() {
        User u = user(4, "acct@example.com", "student", "active");
        TwoFactorToken t = token(u, "123456", "PASSWORD_RESET", 2, false);
        when(userRepository.findByEmail("acct@example.com")).thenReturn(Optional.of(u));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(u, "PASSWORD_RESET")).thenReturn(Optional.of(t));
        when(passwordEncoder.encode("NewPass1")).thenReturn("newhash");

        otpAuthService.resetPasswordWithOtp(resetRequest("123456", "NewPass1", "NewPass1"));

        assertTrue(t.isUsed());
        assertEquals("newhash", u.getPasswordHash());
    }

    // =====================================================================
    // sendLoginOtp
    // =====================================================================

    @Test
    void testSendLoginOtp_UnknownEmail_ThrowsNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> otpAuthService.sendLoginOtp("ghost@example.com"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "pending|Your instructor account is pending administrator approval",
            "suspended|Your account has been suspended. Please contact support",
            "rejected|Your instructor application was rejected"})
    void testSendLoginOtp_BlockedStatuses_ThrowAccountStatusException(String status, String message) {
        when(userRepository.findByEmail("acct@example.com"))
                .thenReturn(Optional.of(user(4, "acct@example.com", "student", status)));

        AccountStatusException ex = assertThrows(AccountStatusException.class,
                () -> otpAuthService.sendLoginOtp("acct@example.com"));

        assertEquals(message, ex.getMessage());
        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void testSendLoginOtp_ActiveUser_IssuesLoginToken() {
        User active = user(4, "acct@example.com", "student", "active");
        when(userRepository.findByEmail("acct@example.com")).thenReturn(Optional.of(active));
        LocalDateTime before = LocalDateTime.now();

        SendOtpResponse response = otpAuthService.sendLoginOtp(" Acct@Example.com ");

        LocalDateTime after = LocalDateTime.now();
        verify(tokenRepository).invalidateAllForUserAndPurpose(active, "LOGIN");
        ArgumentCaptor<TwoFactorToken> captor = ArgumentCaptor.forClass(TwoFactorToken.class);
        verify(tokenRepository).save(captor.capture());
        assertFreshToken(captor.getValue(), active, "LOGIN", before, after);
        verify(emailService).sendOtpEmail("acct@example.com", "Test User",
                captor.getValue().getOtpCode(), "Login Authentication", 10);
        assertEquals("Login OTP sent to your registered email address. It is valid for 10 minutes.", response.getMessage());
        assertEquals("ac***@example.com", response.getMaskedEmail());
    }

    @Test
    void testSendLoginOtp_DoesNotPerformDomainDeliverabilityCheck() {
        User active = user(5, "x@localhost", "student", "active");
        when(userRepository.findByEmail("x@localhost")).thenReturn(Optional.of(active));

        SendOtpResponse response = otpAuthService.sendLoginOtp("x@localhost");

        assertEquals("x***@localhost", response.getMaskedEmail());
    }

    // =====================================================================
    // loginWithOtp
    // =====================================================================

    @Test
    void testLoginWithOtp_UnknownEmail_ThrowsNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> otpAuthService.loginWithOtp(new VerifyOtpLoginRequest("ghost@example.com", "123456")));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "pending|Your instructor account is pending administrator approval",
            "suspended|Your account has been suspended. Please contact support",
            "rejected|Your instructor application was rejected"})
    void testLoginWithOtp_BlockedStatuses_ThrowAccountStatusException(String status, String message) {
        when(userRepository.findByEmail("acct@example.com"))
                .thenReturn(Optional.of(user(4, "acct@example.com", "student", status)));

        AccountStatusException ex = assertThrows(AccountStatusException.class,
                () -> otpAuthService.loginWithOtp(new VerifyOtpLoginRequest("acct@example.com", "123456")));

        assertEquals(message, ex.getMessage());
        verifyNoInteractions(tokenRepository, jwtUtil);
    }

    @Test
    void testLoginWithOtp_NoActiveToken_ThrowsOtpInvalid() {
        User u = user(4, "acct@example.com", "student", "active");
        when(userRepository.findByEmail("acct@example.com")).thenReturn(Optional.of(u));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(u, "LOGIN")).thenReturn(Optional.empty());

        OtpInvalidException ex = assertThrows(OtpInvalidException.class,
                () -> otpAuthService.loginWithOtp(new VerifyOtpLoginRequest("acct@example.com", "123456")));

        assertTrue(ex.getMessage().contains("No active login OTP"));
    }

    @Test
    void testLoginWithOtp_ThreeAttemptsUsed_ThrowsTooManyAttempts() {
        User u = user(4, "acct@example.com", "student", "active");
        when(userRepository.findByEmail("acct@example.com")).thenReturn(Optional.of(u));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(u, "LOGIN"))
                .thenReturn(Optional.of(token(u, "123456", "LOGIN", 3, false)));

        assertThrows(TooManyOtpAttemptsException.class,
                () -> otpAuthService.loginWithOtp(new VerifyOtpLoginRequest("acct@example.com", "123456")));

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void testLoginWithOtp_ExpiredToken_MarksUsedAndThrowsExpired() {
        User u = user(4, "acct@example.com", "student", "active");
        TwoFactorToken t = token(u, "123456", "LOGIN", 0, true);
        when(userRepository.findByEmail("acct@example.com")).thenReturn(Optional.of(u));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(u, "LOGIN")).thenReturn(Optional.of(t));

        assertThrows(OtpExpiredException.class,
                () -> otpAuthService.loginWithOtp(new VerifyOtpLoginRequest("acct@example.com", "123456")));

        assertTrue(t.isUsed());
        verify(tokenRepository).save(t);
    }

    @Test
    void testLoginWithOtp_WrongCode_ReportsRemainingAttempts() {
        User u = user(4, "acct@example.com", "student", "active");
        TwoFactorToken t = token(u, "123456", "LOGIN", 1, false);
        when(userRepository.findByEmail("acct@example.com")).thenReturn(Optional.of(u));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(u, "LOGIN")).thenReturn(Optional.of(t));

        OtpInvalidException ex = assertThrows(OtpInvalidException.class,
                () -> otpAuthService.loginWithOtp(new VerifyOtpLoginRequest("acct@example.com", "000000")));

        assertTrue(ex.getMessage().contains("1 attempt(s) remaining"));
        assertEquals(2, t.getAttemptCount());
        assertFalse(t.isUsed());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void testLoginWithOtp_Success_ReturnsJwtAndConsumesToken() {
        User u = user(4, "acct@example.com", "instructor", "active");
        TwoFactorToken t = token(u, "123456", "LOGIN", 0, false);
        when(userRepository.findByEmail("acct@example.com")).thenReturn(Optional.of(u));
        when(tokenRepository.findLatestUnusedByUserAndPurpose(u, "LOGIN")).thenReturn(Optional.of(t));
        when(jwtUtil.generateToken(4, "instructor")).thenReturn("jwt-login");

        AuthResponse response = otpAuthService.loginWithOtp(new VerifyOtpLoginRequest(" ACCT@example.com ", " 123456 "));

        assertEquals("Login successful via OTP verification", response.getMessage());
        assertEquals("jwt-login", response.getToken());
        assertEquals(4, response.getUser().getId());
        assertEquals("instructor", response.getUser().getRole());
        assertTrue(t.isUsed());
        verify(tokenRepository).save(t);
        verify(userRepository, never()).save(any());
    }

    // =====================================================================
    // changePasswordWithCurrentPassword
    // =====================================================================

    @Test
    void testChangePassword_NullUser_ThrowsInvalidCredentials() {
        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> otpAuthService.changePasswordWithCurrentPassword(null, new ChangePasswordRequest("a", "b", "b")));

        assertTrue(ex.getMessage().contains("not authenticated"));
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void testChangePassword_UserMissingFromDatabase_ThrowsNotFound() {
        User current = user(6, "me@example.com", "student", "active");
        when(userRepository.findById(6)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> otpAuthService.changePasswordWithCurrentPassword(current, new ChangePasswordRequest("a", "b", "b")));
    }

    @Test
    void testChangePassword_WrongCurrentPassword_ThrowsInvalidCredentials() {
        User current = user(6, "me@example.com", "student", "active");
        when(userRepository.findById(6)).thenReturn(Optional.of(current));
        when(passwordEncoder.matches("wrong", "oldhash")).thenReturn(false);

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> otpAuthService.changePasswordWithCurrentPassword(current, new ChangePasswordRequest("wrong", "b", "b")));

        assertEquals("Current password is incorrect.", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testChangePassword_ConfirmationMismatch_ThrowsAfterCurrentPasswordCheck() {
        User current = user(6, "me@example.com", "student", "active");
        when(userRepository.findById(6)).thenReturn(Optional.of(current));
        when(passwordEncoder.matches("old", "oldhash")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> otpAuthService.changePasswordWithCurrentPassword(current, new ChangePasswordRequest("old", "new1", "new2")));

        assertTrue(ex.getMessage().contains("do not match"));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testChangePassword_Success_StoresEncodedNewPassword() {
        User current = user(6, "me@example.com", "student", "active");
        when(userRepository.findById(6)).thenReturn(Optional.of(current));
        when(passwordEncoder.matches("old", "oldhash")).thenReturn(true);
        when(passwordEncoder.encode("new1")).thenReturn("newhash");

        otpAuthService.changePasswordWithCurrentPassword(current, new ChangePasswordRequest("old", "new1", "new1"));

        assertEquals("newhash", current.getPasswordHash());
        verify(userRepository).save(current);
    }
}
