package com.olp.service;

import com.olp.dto.InstructorProfileResponse;
import com.olp.dto.InstructorSummaryResponse;
import com.olp.dto.UpdateInstructorProfileRequest;
import com.olp.entity.Course;
import com.olp.entity.InstructorProfile;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseRepository;
import com.olp.repository.InstructorProfileRepository;
import com.olp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class InstructorProfileService {

    private final UserRepository userRepository;
    private final InstructorProfileRepository instructorProfileRepository;
    private final CourseRepository courseRepository;

    public InstructorProfileService(UserRepository userRepository,
                                    InstructorProfileRepository instructorProfileRepository) {
        this(userRepository, instructorProfileRepository, null);
    }

    @Autowired
    public InstructorProfileService(UserRepository userRepository,
                                    InstructorProfileRepository instructorProfileRepository,
                                    @Autowired(required = false) CourseRepository courseRepository) {
        this.userRepository = userRepository;
        this.instructorProfileRepository = instructorProfileRepository;
        this.courseRepository = courseRepository;
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
                profile != null ? profile.getBio() : null,
                profile != null ? profile.getProfilePictureUrl() : null,
                profile != null ? profile.getCreatedAt() : null,
                profile != null ? profile.getUpdatedAt() : null
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

        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
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
            if (request.getName().length() > 100) {
                throw new IllegalArgumentException("Name must be 100 characters or fewer");
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
                    newProfile.setCreatedAt(LocalDateTime.now());
                    newProfile.setUpdatedAt(LocalDateTime.now());
                    return newProfile;
                });

        // 3. Update instructor-specific fields with validations
        if (request.getQualifications() != null) {
            if (request.getQualifications().length() > 1000) {
                throw new IllegalArgumentException("Qualifications must be 1000 characters or fewer");
            }
            profile.setQualifications(request.getQualifications());
        }
        if (request.getExpertiseAreas() != null) {
            if (request.getExpertiseAreas().length() > 1000) {
                throw new IllegalArgumentException("Expertise areas must be 1000 characters or fewer");
            }
            profile.setExpertiseAreas(request.getExpertiseAreas());
        }
        if (request.getBio() != null) {
            if (request.getBio().length() > 5000) {
                throw new IllegalArgumentException("Bio must be 5000 characters or fewer");
            }
            profile.setBio(request.getBio());
        }

        // FR-I03: Automatically update the updatedAt timestamp whenever the profile is modified
        profile.setUpdatedAt(LocalDateTime.now());
        profile = instructorProfileRepository.save(profile);

        // 4. Return sanitized profile response (never exposes passwords/secrets)
        return new InstructorProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                profile.getQualifications(),
                profile.getExpertiseAreas(),
                profile.getBio(),
                profile.getProfilePictureUrl(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    /**
     * FR-I03: Reusable instructor summary API for instructor cards on course pages,
     * course listings, and public instructor overview.
     */
    @Transactional(readOnly = true)
    public InstructorSummaryResponse getInstructorSummary(Integer instructorId) {
        if (instructorId == null) {
            throw new IllegalArgumentException("Instructor ID cannot be null");
        }

        User user = userRepository.findById(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found with id: " + instructorId));

        if (!"instructor".equalsIgnoreCase(user.getRole())) {
            throw new ResourceNotFoundException("User with id " + instructorId + " is not an instructor");
        }

        InstructorProfile profile = instructorProfileRepository.findByInstructorId(instructorId)
                .orElse(null);

        return new InstructorSummaryResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                profile != null ? profile.getQualifications() : null,
                profile != null ? profile.getExpertiseAreas() : null,
                profile != null ? profile.getBio() : null,
                profile != null ? profile.getProfilePictureUrl() : null,
                profile != null ? profile.getCreatedAt() : null,
                profile != null ? profile.getUpdatedAt() : null
        );
    }

    /**
     * FR-I03: Helper to retrieve an instructor summary directly for a given course.
     */
    @Transactional(readOnly = true)
    public InstructorSummaryResponse getInstructorSummaryForCourse(Integer courseId) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        if (courseRepository == null) {
            throw new IllegalStateException("Course repository is not configured");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        if (course.getInstructor() == null) {
            throw new ResourceNotFoundException("No instructor associated with course id: " + courseId);
        }

        return getInstructorSummary(course.getInstructor().getId());
    }
}
