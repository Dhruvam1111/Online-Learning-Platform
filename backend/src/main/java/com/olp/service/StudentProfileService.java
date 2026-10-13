package com.olp.service;

import com.olp.dto.StudentProfileResponse;
import com.olp.dto.UpdateStudentProfileRequest;
import com.olp.entity.StudentProfile;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.repository.StudentProfileRepository;
import com.olp.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentProfileService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;

    public StudentProfileService(UserRepository userRepository,
                                 StudentProfileRepository studentProfileRepository) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    /** Resolves the current User from the Spring Security Authentication. */
    public User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
    }

    /** Returns the authenticated student's own profile (profile fields are null if not yet created). */
    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile(User currentUser) {
        requireStudent(currentUser, "view");

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        StudentProfile profile = studentProfileRepository.findByStudentId(user.getId()).orElse(null);
        return toResponse(user, profile);
    }

    /** Updates name and student-specific fields. Null fields in the request are left unchanged. */
    @Transactional
    public StudentProfileResponse updateProfile(User currentUser, UpdateStudentProfileRequest request) {
        requireStudent(currentUser, "update");

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (request.name() != null) {
            String trimmed = request.name().trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Name cannot be empty");
            }
            user.setName(trimmed);
            user = userRepository.save(user);
        }

        final User targetUser = user;
        StudentProfile profile = studentProfileRepository.findByStudentId(user.getId())
                .orElseGet(() -> {
                    StudentProfile p = new StudentProfile();
                    p.setStudent(targetUser);
                    return p;
                });

        if (request.educationLevel() != null) profile.setEducationLevel(request.educationLevel().trim());
        if (request.interests() != null) profile.setInterests(request.interests().trim());
        if (request.learningGoals() != null) profile.setLearningGoals(request.learningGoals().trim());
        if (request.bio() != null) profile.setBio(request.bio().trim());

        profile = studentProfileRepository.save(profile);
        return toResponse(user, profile);
    }

    private void requireStudent(User user, String action) {
        if (user == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (!"student".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Access denied: only students can " + action + " student profiles");
        }
    }

    /** Never exposes passwordHash. */
    private StudentProfileResponse toResponse(User user, StudentProfile profile) {
        return new StudentProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                profile != null ? profile.getEducationLevel() : null,
                profile != null ? profile.getInterests() : null,
                profile != null ? profile.getLearningGoals() : null,
                profile != null ? profile.getBio() : null
        );
    }
}