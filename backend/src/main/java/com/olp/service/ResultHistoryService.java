package com.olp.service;

import com.olp.dto.CreateResultVersionRequest;
import com.olp.dto.ResultVersionResponse;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.ResultVersionRepository;
import com.olp.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing immutable result version change history (FR-I74, Step 49).
 * Provides read-only querying for course instructors and admins, and append-only version recording.
 * Strictly upholds DR-ED04: history entries can never be modified or deleted.
 */
@Service
public class ResultHistoryService {

    private final ResultVersionRepository resultVersionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseInstructorRepository courseInstructorRepository;

    public ResultHistoryService(ResultVersionRepository resultVersionRepository,
                                CourseRepository courseRepository,
                                UserRepository userRepository,
                                CourseInstructorRepository courseInstructorRepository) {
        this.resultVersionRepository = resultVersionRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.courseInstructorRepository = courseInstructorRepository;
    }

    public User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        if (principal instanceof String email) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new InvalidCredentialsException("User not found: " + email));
        }
        throw new InvalidCredentialsException("Invalid authentication principal");
    }

    @Transactional(readOnly = true)
    public List<ResultVersionResponse> getResultHistory(User currentUser,
                                                        Integer courseId,
                                                        Integer studentId,
                                                        AssessmentType assessmentType,
                                                        Integer assessmentId) {
        validateInstructorOrAdmin(currentUser);
        verifyCourseAccess(courseId, currentUser);

        List<ResultVersion> versions = resultVersionRepository.findFiltered(courseId, studentId, assessmentType, assessmentId);
        return versions.stream()
                .map(ResultVersionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ResultVersionResponse recordResultVersion(User currentUser,
                                                    Integer courseId,
                                                    CreateResultVersionRequest request) {
        validateInstructorOrAdmin(currentUser);
        Course course = verifyCourseAccess(courseId, currentUser);

        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }
        if (request.getStudentId() == null) {
            throw new IllegalArgumentException("Student ID is required");
        }
        if (request.getAssessmentType() == null) {
            throw new IllegalArgumentException("Assessment type is required");
        }
        if (request.getAssessmentId() == null) {
            throw new IllegalArgumentException("Assessment ID is required");
        }
        if (request.getOldValue() == null || request.getOldValue().trim().isEmpty()) {
            throw new IllegalArgumentException("Old value is required");
        }
        if (request.getNewValue() == null || request.getNewValue().trim().isEmpty()) {
            throw new IllegalArgumentException("New value is required");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Reason/justification is required");
        }

        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + request.getStudentId()));

        ResultVersion resultVersion = new ResultVersion(
                course,
                request.getAssessmentType(),
                request.getAssessmentId(),
                request.getAssessmentTitle(),
                student,
                request.getAttemptId(),
                request.getOldScore(),
                request.getNewScore(),
                request.getOldValue().trim(),
                request.getNewValue().trim(),
                request.getReason().trim(),
                currentUser
        );

        ResultVersion saved = resultVersionRepository.save(resultVersion);
        return ResultVersionResponse.fromEntity(saved);
    }

    public Course verifyCourseAccess(Integer courseId, User user) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        if ("admin".equalsIgnoreCase(user.getRole())) {
            return course;
        }

        boolean isOwner = course.getInstructor() != null && course.getInstructor().getId().equals(user.getId());
        boolean isCollaborator = courseInstructorRepository != null
                && courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, user.getId());

        if (!isOwner && !isCollaborator) {
            throw new AccessDeniedException("You are not authorized to access result history for course: " + courseId);
        }
        return course;
    }

    private void validateInstructorOrAdmin(User user) {
        if (user == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        String role = user.getRole();
        if (role == null || (!role.equalsIgnoreCase("INSTRUCTOR") && !role.equalsIgnoreCase("ADMIN"))) {
            throw new AccessDeniedException("Only instructors and administrators can access result change history");
        }
    }
}
