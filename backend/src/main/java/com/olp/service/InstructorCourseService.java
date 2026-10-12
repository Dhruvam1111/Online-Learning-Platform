package com.olp.service;

import com.olp.dto.CourseResponse;
import com.olp.dto.UpdateCourseRequest;
import com.olp.dto.UpdateCourseStatusRequest;
import com.olp.entity.Course;
import com.olp.entity.CourseStatus;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseRepository;
import com.olp.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstructorCourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public InstructorCourseService(CourseRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    /**
     * Resolves the current User entity from the Spring Security Authentication context.
     * Supports both custom User principals and email-based / UserDetails principals.
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
     * FR-I60 — List only courses owned by the authenticated instructor.
     */
    @Transactional(readOnly = true)
    public List<CourseResponse> getInstructorCourses(User currentUser) {
        validateInstructor(currentUser);

        List<Course> courses = courseRepository.findByInstructorIdOrderByIdAsc(currentUser.getId());
        return courses.stream()
                .map(CourseResponse::fromEntity)
                .toList();
    }

    /**
     * FR-I09 — Update title and description for an owned course.
     */
    @Transactional
    public CourseResponse updateCourse(User currentUser, Integer courseId, UpdateCourseRequest request) {
        validateInstructor(currentUser);

        if (request.getInstructorId() != null && !request.getInstructorId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Cannot modify another instructor's course");
        }
        if (request.getId() != null && !request.getId().equals(courseId)) {
            throw new IllegalArgumentException("Course ID in path and body do not match");
        }

        Course course = findCourseAndVerifyOwnership(courseId, currentUser);

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }

        course.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }

        Course updated = courseRepository.save(course);
        return CourseResponse.fromEntity(updated);
    }

    /**
     * FR-I09 — Change status between DRAFT and PUBLISHED for an owned course.
     */
    @Transactional
    public CourseResponse updateCourseStatus(User currentUser, Integer courseId, UpdateCourseStatusRequest request) {
        validateInstructor(currentUser);

        Course course = findCourseAndVerifyOwnership(courseId, currentUser);

        if (request == null || request.getStatus() == null || request.getStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("Course status cannot be blank");
        }

        CourseStatus newStatus = CourseStatus.fromString(request.getStatus());
        course.setStatus(newStatus);

        Course updated = courseRepository.save(course);
        return CourseResponse.fromEntity(updated);
    }

    private Course findCourseAndVerifyOwnership(Integer courseId, User currentUser) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        if (course.getInstructor() == null || !currentUser.getId().equals(course.getInstructor().getId())) {
            throw new AccessDeniedException("Cannot modify another instructor's course");
        }

        return course;
    }

    private void validateInstructor(User currentUser) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (!"instructor".equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied: only instructors can access instructor courses");
        }
    }
}
