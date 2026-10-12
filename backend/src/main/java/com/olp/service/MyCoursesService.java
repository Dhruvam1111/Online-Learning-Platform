package com.olp.service;

import com.olp.dto.MyCourseResponse;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MyCoursesService {

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    public MyCoursesService(UserRepository userRepository,
                            EnrollmentRepository enrollmentRepository) {
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
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

    /** Returns all courses the given student is enrolled in (empty list if none). */
    @Transactional(readOnly = true)
    public List<MyCourseResponse> getMyCourses(User currentUser) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (!"student".equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied: only students can view enrolled courses");
        }

        return enrollmentRepository.findMyCourses(currentUser.getId());
    }
}