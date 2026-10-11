package com.olp.service;

import com.olp.dto.UpdateProfileRequest;
import com.olp.dto.UserResponse;
import com.olp.entity.User;
import com.olp.exception.DuplicateEmailException;
import com.olp.exception.InvalidCredentialsException;
import com.olp.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // =========================================================================
    // DEVELOPER A RESPONSIBILITY: User Model & Profile Retrieval
    // =========================================================================

    /**
     * Retrieves the profile information for the authenticated user.
     * Developer A: Profile retrieval logic & sanitized DTO response.
     */
    public UserResponse getProfile(User currentUser) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        return new UserResponse(
                currentUser.getId(),
                currentUser.getName(),
                currentUser.getEmail(),
                currentUser.getRole()
        );
    }

    // =========================================================================
    // DEVELOPER B RESPONSIBILITY: Profile Update & Authorization
    // =========================================================================

    /**
     * Updates profile information for the authenticated user.
     * Developer B: Profile update, duplicate check, and authorization validation.
     */
    public UserResponse updateProfile(User currentUser, UpdateProfileRequest request) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }

        // Fetch the fresh entity from database
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        // 1. Update name if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }

        // 2. Update email if provided and changed
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().toLowerCase().trim();
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                // Duplicate check
                if (userRepository.existsByEmail(newEmail)) {
                    throw new DuplicateEmailException("An account with this email already exists");
                }
                user.setEmail(newEmail);
            }
        }

        // 3. Save changes
        user = userRepository.save(user);

        // 4. Return updated sanitized response
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
