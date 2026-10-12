package com.olp.service;

import com.olp.dto.InstructorProfileResponse;
import com.olp.dto.UpdateInstructorProfileRequest;
import com.olp.entity.InstructorProfile;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.repository.InstructorProfileRepository;
import com.olp.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstructorProfileService {

    private final UserRepository userRepository;
    private final InstructorProfileRepository instructorProfileRepository;

    public InstructorProfileService(UserRepository userRepository,
                                    InstructorProfileRepository instructorProfileRepository) {
        this.userRepository = userRepository;
        this.instructorProfileRepository = instructorProfileRepository;
    }

    /**
     * Resolves the current User entity from the Spring Security Authentication context.
     * Supports both custom User principals and standard UserDetails / email-based principals.
     */
    public User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
    }

    /**
     * Retrieves the profile information for the authenticated instructor.
     * Instructors can only view their own profile.
     */
    @Transactional(readOnly = true)
    public InstructorProfileResponse getProfile(User currentUser) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }

        if (!"instructor".equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied: only instructors can access instructor profiles");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElse(currentUser);

        InstructorProfile profile = instructorProfileRepository.findByInstructorId(user.getId())
                .orElse(null);

        return new InstructorProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                profile != null ? profile.getQualifications() : null,
                profile != null ? profile.getExpertiseAreas() : null,
                profile != null ? profile.getBio() : null
        );
    }

    /**
     * Updates the profile information for the authenticated instructor.
     * Only qualifications, expertise areas, bio (and name) can be updated.
     * Privileged fields such as role and password cannot be modified.
     * Prevent modifying another user's profile if an external ID is supplied.
     */
    @Transactional
    public InstructorProfileResponse updateProfile(User currentUser, UpdateInstructorProfileRequest request) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }

        if (!"instructor".equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied: only instructors can update instructor profiles");
        }

        // Prevent callers from attempting to supply another instructor's ID
        if (request.getId() != null && !request.getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Cannot modify another instructor's profile");
        }
        if (request.getInstructorId() != null && !request.getInstructorId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Cannot modify another instructor's profile");
        }
        if (request.getUserId() != null && !request.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Cannot modify another instructor's profile");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        // 1. Update name if provided and not blank (consistent with existing project)
        if (request.getName() != null) {
            if (request.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Name cannot be empty");
            }
            user.setName(request.getName().trim());
            user = userRepository.save(user);
        }

        final User targetUser = user;

        // 2. Fetch or create InstructorProfile entity
        InstructorProfile profile = instructorProfileRepository.findByInstructorId(user.getId())
                .orElseGet(() -> {
                    InstructorProfile newProfile = new InstructorProfile();
                    newProfile.setInstructor(targetUser);
                    return newProfile;
                });

        // 3. Update instructor-specific fields
        if (request.getQualifications() != null) {
            profile.setQualifications(request.getQualifications());
        }
        if (request.getExpertiseAreas() != null) {
            profile.setExpertiseAreas(request.getExpertiseAreas());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }

        profile = instructorProfileRepository.save(profile);

        // 4. Return sanitized profile response (never exposes passwords/secrets)
        return new InstructorProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                profile.getQualifications(),
                profile.getExpertiseAreas(),
                profile.getBio()
        );
    }
}
