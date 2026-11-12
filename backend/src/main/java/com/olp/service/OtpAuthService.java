package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.TwoFactorToken;
import com.olp.entity.User;
import com.olp.exception.*;
import com.olp.repository.TwoFactorTokenRepository;
import com.olp.repository.UserRepository;
import com.olp.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Service for OTP-based Authentication and Password Reset.
 *
 * Provides:
 *  1. Password Reset via Email OTP (Forgot Password flow)
 *  2. Passwordless Login via Email OTP (Alternative authentication flow)
 *  3. Password Change via Current Password (Standard authenticated profile flow)
 */
@Service
public class OtpAuthService {

    private static final Logger log = LoggerFactory.getLogger(OtpAuthService.class);

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 3;

    private static final String PURPOSE_PASSWORD_RESET = "PASSWORD_RESET";
    private static final String PURPOSE_LOGIN = "LOGIN";
    private static final String PURPOSE_REGISTRATION = "REGISTRATION";

    private final TwoFactorTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public OtpAuthService(TwoFactorTokenRepository tokenRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          EmailService emailService,
                          TokenBlacklistService tokenBlacklistService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.tokenBlacklistService = tokenBlacklistService != null ? tokenBlacklistService : new TokenBlacklistService();
    }

    public OtpAuthService(TwoFactorTokenRepository tokenRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          EmailService emailService) {
        this(tokenRepository, userRepository, passwordEncoder, jwtUtil, emailService, new TokenBlacklistService());
    }

    // =========================================================================
    // 0. STUDENT SIGNUP WITH EMAIL OTP VERIFICATION
    // =========================================================================

    @Transactional
    public SendOtpResponse sendRegistrationOtp(RegisterRequest request) {
        String cleanEmail = request.getEmail().toLowerCase().trim();

        if (!isDomainDeliverable(cleanEmail)) {
            throw new IllegalArgumentException("Email does not exist. The domain is invalid or cannot receive emails.");
        }

        java.util.Optional<User> existingUserOpt = userRepository.findByEmail(cleanEmail);

        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if ("active".equalsIgnoreCase(user.getAccountStatus())) {
                throw new DuplicateEmailException("An account with this email already exists");
            }
            // Existing pending registration: update info
            user.setName(request.getName().trim());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setRole("student");
            user = userRepository.save(user);
        } else {
            // New user registered with 'pending' status until email is verified
            user = new User(
                    request.getName().trim(),
                    cleanEmail,
                    passwordEncoder.encode(request.getPassword()),
                    "student",
                    "pending"
            );
            user = userRepository.save(user);
        }

        // Invalidate previous registration tokens for this user
        tokenRepository.invalidateAllForUserAndPurpose(user, PURPOSE_REGISTRATION);

        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now();
        TwoFactorToken token = new TwoFactorToken(
                user,
                otpCode,
                now,
                now.plusMinutes(OTP_EXPIRY_MINUTES),
                PURPOSE_REGISTRATION
        );
        tokenRepository.save(token);

        printOtpBox("STUDENT REGISTRATION OTP", cleanEmail, otpCode);

        // Send real email to recipient's inbox
        emailService.sendOtpEmail(cleanEmail, user.getName(), otpCode, "Student Account Verification", OTP_EXPIRY_MINUTES);

        return new SendOtpResponse(
                "Verification code sent to " + cleanEmail + ". It is valid for " + OTP_EXPIRY_MINUTES + " minutes.",
                maskEmail(cleanEmail)
        );
    }

    @Transactional
    public SendOtpResponse resendRegistrationOtp(String email) {
        String cleanEmail = email.toLowerCase().trim();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Email does not exist. No registration found for this email address."));

        if ("active".equalsIgnoreCase(user.getAccountStatus())) {
            throw new IllegalArgumentException("Account is already verified and active. Please log in.");
        }

        tokenRepository.invalidateAllForUserAndPurpose(user, PURPOSE_REGISTRATION);

        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now();
        TwoFactorToken token = new TwoFactorToken(
                user,
                otpCode,
                now,
                now.plusMinutes(OTP_EXPIRY_MINUTES),
                PURPOSE_REGISTRATION
        );
        tokenRepository.save(token);

        printOtpBox("STUDENT REGISTRATION OTP (RESEND)", cleanEmail, otpCode);

        emailService.sendOtpEmail(cleanEmail, user.getName(), otpCode, "Student Account Verification", OTP_EXPIRY_MINUTES);

        return new SendOtpResponse(
                "A fresh verification code was sent to your email.",
                maskEmail(cleanEmail)
        );
    }

    @Transactional(noRollbackFor = {OtpInvalidException.class, OtpExpiredException.class})
    public AuthResponse verifyRegistrationOtp(VerifyRegistrationOtpRequest request) {
        String cleanEmail = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No registration found for this email address"));

        if ("active".equalsIgnoreCase(user.getAccountStatus())) {
            throw new IllegalArgumentException("Account is already verified and active. Please log in.");
        }

        TwoFactorToken token = tokenRepository.findLatestUnusedByUserAndPurpose(user, PURPOSE_REGISTRATION)
                .orElseThrow(() -> new OtpInvalidException("No active registration verification code found. Please request a new code."));

        if (token.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new TooManyOtpAttemptsException("Maximum OTP verification attempts exceeded. Please request a new verification code.");
        }

        if (token.isExpired()) {
            token.setUsed(true);
            tokenRepository.save(token);
            throw new OtpExpiredException("Your verification code has expired. Please request a new one.");
        }

        token.incrementAttempt();
        if (!token.getOtpCode().equals(request.getOtpCode().trim())) {
            tokenRepository.save(token);
            int remaining = MAX_ATTEMPTS - token.getAttemptCount();
            throw new OtpInvalidException("Invalid verification code. " + remaining + " attempt(s) remaining.");
        }

        token.setUsed(true);
        tokenRepository.save(token);

        // Activate user account
        user.setAccountStatus("active");
        userRepository.save(user);

        log.info("[OTP Auth] Student account successfully verified via email OTP for: {}", cleanEmail);

        // Generate JWT token
        String tokenString = jwtUtil.generateToken(user.getId(), user.getRole());
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );

        return new AuthResponse("Account successfully verified! Welcome to Focotech.", tokenString, userResponse);
    }

    // =========================================================================
    // 1. PASSWORD RESET VIA EMAIL OTP
    // =========================================================================

    @Transactional
    public SendOtpResponse sendPasswordResetOtp(String email) {
        String cleanEmail = email.toLowerCase().trim();

        if (!isDomainDeliverable(cleanEmail)) {
            throw new IllegalArgumentException("Email does not exist. The domain is invalid or cannot receive emails.");
        }

        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Email does not exist. No account is registered with this email address."));

        validateAccountStatus(user);

        // Invalidate previous unused password reset tokens
        tokenRepository.invalidateAllForUserAndPurpose(user, PURPOSE_PASSWORD_RESET);

        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now();
        TwoFactorToken token = new TwoFactorToken(
                user,
                otpCode,
                now,
                now.plusMinutes(OTP_EXPIRY_MINUTES),
                PURPOSE_PASSWORD_RESET
        );
        tokenRepository.save(token);

        printOtpBox("PASSWORD RESET OTP", cleanEmail, otpCode);

        // Send real email to recipient's inbox
        emailService.sendOtpEmail(cleanEmail, user.getName(), otpCode, "Password Reset", OTP_EXPIRY_MINUTES);

        return new SendOtpResponse(
                "OTP sent to your registered email address. It is valid for " + OTP_EXPIRY_MINUTES + " minutes.",
                maskEmail(cleanEmail)
        );
    }

    @Transactional(noRollbackFor = {OtpInvalidException.class, OtpExpiredException.class})
    public void resetPasswordWithOtp(ResetPasswordWithOtpRequest request) {
        String cleanEmail = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email address"));

        validateAccountStatus(user);

        TwoFactorToken token = tokenRepository.findLatestUnusedByUserAndPurpose(user, PURPOSE_PASSWORD_RESET)
                .orElseThrow(() -> new OtpInvalidException("No active OTP found. Please request a new OTP first."));

        if (token.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new TooManyOtpAttemptsException("Maximum OTP verification attempts exceeded. Please request a new OTP.");
        }

        if (token.isExpired()) {
            token.setUsed(true);
            tokenRepository.save(token);
            throw new OtpExpiredException("Your OTP has expired. Please request a new one.");
        }

        token.incrementAttempt();
        if (!token.getOtpCode().equals(request.getOtpCode().trim())) {
            tokenRepository.save(token);
            int remaining = MAX_ATTEMPTS - token.getAttemptCount();
            throw new OtpInvalidException("Invalid OTP code. " + remaining + " attempt(s) remaining.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation password do not match.");
        }

        token.setUsed(true);
        tokenRepository.save(token);

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        if (tokenBlacklistService != null) {
            tokenBlacklistService.revokeAllUserTokens(user.getId());
        }

        log.info("[OTP Auth] Password successfully reset via OTP for email: {}", cleanEmail);
    }

    // =========================================================================
    // 2. LOGIN VIA EMAIL OTP (PASSWORDLESS AUTHENTICATION)
    // =========================================================================

    @Transactional
    public SendOtpResponse sendLoginOtp(String email) {
        String cleanEmail = email.toLowerCase().trim();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email address"));

        validateAccountStatus(user);

        tokenRepository.invalidateAllForUserAndPurpose(user, PURPOSE_LOGIN);

        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now();
        TwoFactorToken token = new TwoFactorToken(
                user,
                otpCode,
                now,
                now.plusMinutes(OTP_EXPIRY_MINUTES),
                PURPOSE_LOGIN
        );
        tokenRepository.save(token);

        printOtpBox("LOGIN AUTHENTICATION OTP", cleanEmail, otpCode);

        // Send real email to recipient's inbox
        emailService.sendOtpEmail(cleanEmail, user.getName(), otpCode, "Login Authentication", OTP_EXPIRY_MINUTES);

        return new SendOtpResponse(
                "Login OTP sent to your registered email address. It is valid for " + OTP_EXPIRY_MINUTES + " minutes.",
                maskEmail(cleanEmail)
        );
    }

    @Transactional(noRollbackFor = {OtpInvalidException.class, OtpExpiredException.class})
    public AuthResponse loginWithOtp(VerifyOtpLoginRequest request) {
        String cleanEmail = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email address"));

        validateAccountStatus(user);

        TwoFactorToken token = tokenRepository.findLatestUnusedByUserAndPurpose(user, PURPOSE_LOGIN)
                .orElseThrow(() -> new OtpInvalidException("No active login OTP found. Please request an OTP first."));

        if (token.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new TooManyOtpAttemptsException("Maximum OTP verification attempts exceeded. Please request a new OTP.");
        }

        if (token.isExpired()) {
            token.setUsed(true);
            tokenRepository.save(token);
            throw new OtpExpiredException("Your OTP has expired. Please request a new one.");
        }

        token.incrementAttempt();
        if (!token.getOtpCode().equals(request.getOtpCode().trim())) {
            tokenRepository.save(token);
            int remaining = MAX_ATTEMPTS - token.getAttemptCount();
            throw new OtpInvalidException("Invalid OTP code. " + remaining + " attempt(s) remaining.");
        }

        token.setUsed(true);
        tokenRepository.save(token);

        String tokenString = jwtUtil.generateToken(user.getId(), user.getRole());
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );

        return new AuthResponse("Login successful via OTP verification", tokenString, userResponse);
    }

    // =========================================================================
    // 3. CHANGE PASSWORD VIA CURRENT PASSWORD (AUTHENTICATED)
    // =========================================================================

    @Transactional
    public void changePasswordWithCurrentPassword(User currentUser, ChangePasswordRequest request) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation password do not match.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        if (tokenBlacklistService != null) {
            tokenBlacklistService.revokeAllUserTokens(user.getId());
        }

        log.info("[Password Change] User {} successfully changed password using current password", user.getEmail());
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void validateAccountStatus(User user) {
        String status = user.getAccountStatus();
        if ("pending".equalsIgnoreCase(status)) {
            throw new AccountStatusException("Your instructor account is pending administrator approval");
        } else if ("suspended".equalsIgnoreCase(status)) {
            throw new AccountStatusException("Your account has been suspended. Please contact support");
        } else if ("rejected".equalsIgnoreCase(status)) {
            throw new AccountStatusException("Your instructor application was rejected");
        }
    }

    private void printOtpBox(String title, String email, String otpCode) {
        log.info("=============================================================");
        log.info("  [{}] Email : {}", title, email);
        log.info("  [{}] OTP   : {}", title, otpCode);
        log.info("  [{}] Expiry: {} minutes", title, OTP_EXPIRY_MINUTES);
        log.info("=============================================================");

        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.printf ("║  %-44s║%n", title);
        System.out.printf ("║  To      : %-34s║%n", email);
        System.out.printf ("║  OTP     : %-34s║%n", otpCode);
        System.out.printf ("║  Expires : %-34s║%n", "in " + OTP_EXPIRY_MINUTES + " minutes");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + domain;
        }
        return local.substring(0, 2) + "***@" + domain;
    }

    private boolean isDomainDeliverable(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }
        String domain = email.substring(email.indexOf('@') + 1).trim().toLowerCase();
        if (domain.isBlank() || !domain.contains(".")) {
            return false;
        }

        // Recognized domains and test environments
        if (domain.equals("gmail.com") ||
            domain.equals("yahoo.com") ||
            domain.equals("outlook.com") ||
            domain.equals("hotmail.com") ||
            domain.equals("icloud.com") ||
            domain.equals("dau.ac.in") ||
            domain.equals("example.com") ||
            domain.equals("random.com") ||
            domain.equals("focotech.com") ||
            domain.equals("test.com")) {
            return true;
        }

        try {
            java.util.Hashtable<String, String> env = new java.util.Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", "2000");
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            javax.naming.directory.DirContext ictx = new javax.naming.directory.InitialDirContext(env);
            javax.naming.directory.Attributes attrs = ictx.getAttributes(domain, new String[]{"MX"});
            javax.naming.directory.Attribute attr = attrs.get("MX");
            if (attr != null && attr.size() > 0) {
                return true;
            }
            attrs = ictx.getAttributes(domain, new String[]{"A"});
            javax.naming.directory.Attribute aAttr = attrs.get("A");
            return aAttr != null && aAttr.size() > 0;
        } catch (Exception e) {
            log.warn("[OTP Auth] DNS MX lookup failed for domain {}: {}", domain, e.getMessage());
            return false;
        }
    }
}
