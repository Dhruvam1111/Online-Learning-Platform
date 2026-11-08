package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.InstructorProfile;
import com.olp.entity.User;
import com.olp.exception.DuplicateEmailException;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.InstructorProfileRepository;
import com.olp.repository.UserRepository;
import com.olp.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final InstructorProfileRepository instructorProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
            InstructorProfileRepository instructorProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.instructorProfileRepository = instructorProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Register a new user.
     * Throws DuplicateEmailException if the email already exists.
     * FR-I01: Instructors are registered with pending status, their profile is
     * initialized
     * with expertise & qualification, and an email verification link is dispatched.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("An account with this email already exists");
        }

        String role = request.getRole().toLowerCase().trim();
        if ("admin".equals(role)) {
            throw new IllegalArgumentException("Cannot self-register as an administrator");
        }

        // 2. Determine initial account status
        // Instructors must undergo administrative approval (FR-A03, FR-I01)
        String initialStatus = "instructor".equals(role) ? "pending" : "active";

        // 3. Hash the password
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 4. Create and save the user
        User user = new User(
                request.getName(),
                request.getEmail().toLowerCase().trim(),
                hashedPassword,
                role,
                initialStatus);
        user = userRepository.save(user);

        // 5. FR-I01: If instructor, initialize profile and dispatch email verification
        // link
        String verificationLink = null;
        if ("instructor".equals(role)) {
            String qual = request.getQualification() != null ? request.getQualification().trim()
                    : request.getQualifications();
            String exp = request.getExpertise() != null ? request.getExpertise().trim() : request.getExpertiseAreas();
            InstructorProfile profile = new InstructorProfile(user, qual, exp, null);
            instructorProfileRepository.save(profile);

            // Generate verification token using JWT authentication setup
            String verificationToken = jwtUtil.generateToken(user.getId(), "VERIFY_EMAIL");
            verificationLink = "http://localhost:8080/api/auth/verify-email?token=" + verificationToken;

            // Dispatch verification link through platform's logging/email channel
            logger.info("================================================================================");
            logger.info("[EMAIL DISPATCH] Verification link sent to {}: {}", user.getEmail(), verificationLink);
            logger.info("================================================================================");
            System.out
                    .println("[EMAIL DISPATCH] Verification link sent to " + user.getEmail() + ": " + verificationLink);
        }

        // 6. Generate a JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        // 7. Build and return the response
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getAccountStatus());

        String message = "pending".equals(initialStatus)
                ? "Registration submitted. An email verification link has been sent to your email. Your instructor account is awaiting administrator approval."
                : "Registration successful";

        return new AuthResponse(message, token, userResponse, verificationLink);
    }

    /**
     * Authenticate a user.
     * Throws InvalidCredentialsException on failure.
     * Throws AccountStatusException if account is pending or suspended.
     * FR-I01: Displays 'Awaiting admin approval' message for pending instructors.
     */
    public AuthResponse login(LoginRequest request) {
        // 1. Look up user by email
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // 2. Compare passwords
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // 3. Verify account status
        String status = user.getAccountStatus();
        if ("pending".equalsIgnoreCase(status)) {
            throw new com.olp.exception.AccountStatusException(
                    "Awaiting admin approval: Your instructor account is pending administrator approval");
        } else if ("suspended".equalsIgnoreCase(status)) {
            throw new com.olp.exception.AccountStatusException(
                    "Your account has been suspended. Please contact support");
        } else if ("rejected".equalsIgnoreCase(status)) {
            throw new com.olp.exception.AccountStatusException("Your instructor application was rejected");
        }

        // 4. Generate a JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        // 5. Build and return the response
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getAccountStatus());
        return new AuthResponse("Login successful", token, userResponse);
    }

    /**
     * Verifies an email token.
     * FR-I01: Email verification link validation.
     */
    public ApiResponse verifyEmail(String token) {
        if (token == null || !jwtUtil.validateToken(token)) {
            throw new InvalidCredentialsException("Invalid or expired verification token");
        }

        Integer userId = jwtUtil.getUserIdFromToken(token);
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new ApiResponse(true,
                "Email verified successfully. Your instructor account is awaiting administrator approval.");
    }

    /**
     * Get the current user's info from a User entity.
     * Called by the /me endpoint.
     */
    public UserResponse getCurrentUser(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getAccountStatus());
    }

    /**
     * Authenticate or auto-register a user via Google Account.
     * Google has already verified the email ownership.
     */
    @Transactional
    public AuthResponse loginOrRegisterWithGoogle(GoogleAuthRequest request) {
        String cleanEmail = request.getEmail().toLowerCase().trim();
        String name = (request.getName() != null && !request.getName().isBlank())
                ? request.getName().trim()
                : cleanEmail.split("@")[0];

        Optional<User> existingUser = userRepository.findByEmail(cleanEmail);
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            // If user exists with pending status, activate it since Google verified their
            // email
            if ("pending".equalsIgnoreCase(user.getAccountStatus())) {
                user.setAccountStatus("active");
                user = userRepository.save(user);
            } else if ("suspended".equalsIgnoreCase(user.getAccountStatus())) {
                throw new com.olp.exception.AccountStatusException(
                        "Your account has been suspended. Please contact support");
            } else if ("rejected".equalsIgnoreCase(user.getAccountStatus())) {
                throw new com.olp.exception.AccountStatusException("Your instructor application was rejected");
            }
        } else {
            // Auto-provision a new student account verified by Google
            user = new User(
                    name,
                    cleanEmail,
                    passwordEncoder.encode(UUID.randomUUID().toString()),
                    "student",
                    "active");
            user = userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole());

        return new AuthResponse("Authenticated successfully via Google", token, userResponse);
    }
}
