package com.olp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.CourseAuditResponse;
import com.olp.entity.Course;
import com.olp.entity.CourseAudit;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseAuditRepository;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CourseAuditService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CourseAuditRepository courseAuditRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;

    public CourseAuditService(CourseAuditRepository courseAuditRepository,
                              CourseRepository courseRepository) {
        this(courseAuditRepository, courseRepository, null);
    }

    @Autowired
    public CourseAuditService(CourseAuditRepository courseAuditRepository,
                              CourseRepository courseRepository,
                              @Autowired(required = false) CourseInstructorRepository courseInstructorRepository) {
        this.courseAuditRepository = courseAuditRepository;
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
    }

    /**
     * Record an audit event tracking course modification, unpublishing, or status transitions (FR-I09).
     */
    @Transactional
    public CourseAudit recordAudit(Course course, User actor, String actionType,
                                   String changedFields, String previousValues, String newValues) {
        if (course == null) {
            throw new IllegalArgumentException("Course cannot be null when recording audit");
        }
        if (actor == null) {
            throw new IllegalArgumentException("Actor cannot be null when recording audit");
        }
        if (actionType == null || actionType.trim().isEmpty()) {
            actionType = "COURSE_MODIFICATION";
        }

        CourseAudit audit = new CourseAudit(
                course,
                actor,
                actionType.trim(),
                changedFields,
                previousValues,
                newValues
        );
        return courseAuditRepository.save(audit);
    }

    /**
     * Overload recording audit using Map objects for previous and new values.
     */
    @Transactional
    public CourseAudit recordAudit(Course course, User actor, String actionType,
                                   String changedFields, Map<String, Object> prevMap, Map<String, Object> newMap) {
        String prevJson = toJson(prevMap);
        String newJson = toJson(newMap);
        return recordAudit(course, actor, actionType, changedFields, prevJson, newJson);
    }

    /**
     * Retrieve audit history for a course, ensuring caller is an associated instructor or administrator.
     */
    @Transactional(readOnly = true)
    public List<CourseAuditResponse> getAuditsForCourse(User currentUser, @NonNull Integer courseId) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        Course course = courseRepository.findById(Objects.requireNonNull(courseId))
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        verifyInstructorOrAdminAccess(course, currentUser);

        List<CourseAudit> audits = courseAuditRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
        return audits.stream()
                .map(CourseAuditResponse::fromEntity)
                .toList();
    }

    private void verifyInstructorOrAdminAccess(Course course, User currentUser) {
        boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getRole());
        boolean isOwner = course.getInstructor() != null
                && Objects.equals(course.getInstructor().getId(), currentUser.getId());
        boolean isCoInstructor = courseInstructorRepository != null
                && courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), currentUser.getId());

        if (!isAdmin && !isOwner && !isCoInstructor) {
            throw new AccessDeniedException("Access denied: only course instructors or administrators can view course audits");
        }
    }

    private String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return map.toString();
        }
    }
}

