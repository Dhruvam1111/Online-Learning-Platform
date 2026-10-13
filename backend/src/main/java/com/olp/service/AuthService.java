package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.exception.DuplicateEmailException;
import com.olp.exception.InvalidCredentialsException;
import com.olp.repository.UserRepository;
import com.olp.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Register a new user.
     * Throws DuplicateEmailException if the email already exists.
     */
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
                initialStatus
        );
        user = userRepository.save(user);

        // 5. Generate a JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        // 6. Build and return the response
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );

        String message = "pending".equals(initialStatus)
                ? "Registration submitted. Your instructor account is pending administrator approval."
                : "Registration successful";

        return new AuthResponse(message, token, userResponse);
    }

    /**
     * Authenticate a user.
     * Throws InvalidCredentialsException on failure.
     * Throws AccountStatusException if account is pending or suspended.
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
            throw new com.olp.exception.AccountStatusException("Your instructor account is pending administrator approval");
        } else if ("suspended".equalsIgnoreCase(status)) {
            throw new com.olp.exception.AccountStatusException("Your account has been suspended. Please contact support");
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
                user.getRole()
        );
        return new AuthResponse("Login successful", token, userResponse);
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
                user.getRole()
        );
    }
}
