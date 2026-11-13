package com.olp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.CourseSubmissionRequest;
import com.olp.dto.CourseSubmissionResponse;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service orchestrating course submission for admin review (FR-I08).
 * Enforces pre-submission completeness validation, transitions course status to PENDING_REVIEW,
 * generates an immutable course snapshot, and preserves submission history records across resubmissions.
 */
@Service
public class CourseSubmissionService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CourseRepository courseRepository;
    private final CourseSubmissionRepository courseSubmissionRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final LessonRepository lessonRepository;
    private final LectureRepository lectureRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;

    public CourseSubmissionService(
            CourseRepository courseRepository,
            CourseSubmissionRepository courseSubmissionRepository,
            CourseModuleRepository courseModuleRepository,
            CourseSectionRepository courseSectionRepository,
            LessonRepository lessonRepository,
            LectureRepository lectureRepository,
            UserRepository userRepository) {
        this(courseRepository, courseSubmissionRepository, courseModuleRepository,
                courseSectionRepository, lessonRepository, lectureRepository, null, userRepository);
    }

    @Autowired
    public CourseSubmissionService(
            CourseRepository courseRepository,
            CourseSubmissionRepository courseSubmissionRepository,
            @Autowired(required = false) CourseModuleRepository courseModuleRepository,
            @Autowired(required = false) CourseSectionRepository courseSectionRepository,
            @Autowired(required = false) LessonRepository lessonRepository,
            @Autowired(required = false) LectureRepository lectureRepository,
            @Autowired(required = false) CourseInstructorRepository courseInstructorRepository,
            UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.courseSubmissionRepository = courseSubmissionRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.courseSectionRepository = courseSectionRepository;
        this.lessonRepository = lessonRepository;
        this.lectureRepository = lectureRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.userRepository = userRepository;
    }

    /**
     * Resolves the current User entity from Spring Security Authentication.
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
     * FR-I08: Submits a course for admin review.
     * Validates completeness, transitions status to PENDING_REVIEW, creates an immutable
     * snapshot and submission history record, and locks the course against editing.
     */
    @Transactional
    public CourseSubmissionResponse submitCourseForReview(User currentUser, Integer courseId, CourseSubmissionRequest request) {
        validateInstructorOrAdmin(currentUser);

        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        verifyCourseAccess(course, currentUser);

        // 1. Completeness and status validation before state change
        validateCourseForSubmission(course);

        // 2. Fetch modules & content for snapshot
        List<CourseModule> modules = courseModuleRepository != null
                ? courseModuleRepository.findByCourseIdOrderByOrderIndexAsc(courseId)
                : (course.getModules() != null ? course.getModules() : Collections.emptyList());

        List<Lecture> lectures = lectureRepository != null
                ? lectureRepository.findByCourseIdOrderByOrderIndexAsc(courseId)
                : Collections.emptyList();

        // 3. Generate snapshot data
        String snapshotData = generateCourseSnapshot(course, modules, lectures);

        // 4. Create and persist submission history record
        String comments = request != null ? request.getComments() : null;
        CourseSubmission submission = new CourseSubmission(
                course,
                currentUser,
                CourseStatus.PENDING_REVIEW.name(),
                comments,
                snapshotData
        );
        CourseSubmission savedSubmission = courseSubmissionRepository.save(submission);

        // 5. Transition course status to PENDING_REVIEW and clear any previous rejection reason
        course.setStatus(CourseStatus.PENDING_REVIEW);
        course.setRejectionReason(null);
        courseRepository.save(course);

        long submissionCount = courseSubmissionRepository.countByCourseId(courseId);
        return CourseSubmissionResponse.fromEntity(savedSubmission, submissionCount);
    }

    /**
     * Retrieves the chronological submission history for a course.
     */
    @Transactional(readOnly = true)
    public List<CourseSubmissionResponse> getSubmissionHistory(User currentUser, Integer courseId) {
        validateInstructorOrAdmin(currentUser);

        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        verifyCourseAccess(course, currentUser);

        List<CourseSubmission> submissions = courseSubmissionRepository.findByCourseIdOrderBySubmittedAtDesc(courseId);
        long totalSubmissions = submissions.size();

        List<CourseSubmissionResponse> responses = new ArrayList<>();
        for (int i = 0; i < submissions.size(); i++) {
            long submissionNumber = totalSubmissions - i;
            responses.add(CourseSubmissionResponse.fromEntity(submissions.get(i), submissionNumber));
        }
        return responses;
    }

    /**
     * Validates that all required fields and minimum content requirements are met.
     */
    public void validateCourseForSubmission(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Course cannot be null");
        }

        // Validate Title
        if (course.getTitle() == null || course.getTitle().trim().isEmpty()
                || "Untitled Draft".equalsIgnoreCase(course.getTitle().trim())) {
            throw new IllegalArgumentException("Course title must be provided before submission");
        }

        // Validate Description
        if (course.getDescription() == null || course.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Course description must be provided before submission");
        }

        // Validate Pricing
        if (course.getPrice() == null) {
            throw new IllegalStateException("Course price must be configured before submitting for review");
        }
        if (course.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Course price cannot be negative");
        }

        // Validate Status
        if (course.getStatus() == CourseStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Course is already under review (status is PENDING_REVIEW)");
        }
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            throw new IllegalStateException("Course is already published");
        }
        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot submit an archived course for review");
        }

        // Validate Modules & Actual Content
        List<CourseModule> modules = courseModuleRepository != null && course.getId() != null
                ? courseModuleRepository.findByCourseIdOrderByOrderIndexAsc(course.getId())
                : (course.getModules() != null ? course.getModules() : Collections.emptyList());

        if (modules == null || modules.isEmpty()) {
            throw new IllegalStateException("Course must have at least one module before submitting for review");
        }

        boolean hasActualContent = false;
        for (CourseModule mod : modules) {
            List<CourseSection> sections = courseSectionRepository != null && mod.getId() != null
                    ? courseSectionRepository.findByModuleIdOrderByOrderIndexAsc(mod.getId())
                    : (mod.getSections() != null ? mod.getSections() : Collections.emptyList());

            if (sections != null) {
                for (CourseSection sec : sections) {
                    List<Lesson> lessons = lessonRepository != null && sec.getId() != null
                            ? lessonRepository.findBySectionIdOrderByOrderIndexAsc(sec.getId())
                            : (sec.getLessons() != null ? sec.getLessons() : Collections.emptyList());

                    if (lessons != null && !lessons.isEmpty()) {
                        hasActualContent = true;
                        break;
                    }
                }
            }
            if (hasActualContent) {
                break;
            }
        }

        if (!hasActualContent && lectureRepository != null && course.getId() != null) {
            List<Lecture> lectures = lectureRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
            if (lectures != null && !lectures.isEmpty()) {
                hasActualContent = true;
            }
        }

        if (!hasActualContent) {
            throw new IllegalStateException("Course must have at least one module with actual lesson content before submitting for review");
        }
    }

    /**
     * Generates a frozen JSON snapshot of the course structure and details at submission time.
     */
    private String generateCourseSnapshot(Course course, List<CourseModule> modules, List<Lecture> lectures) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("courseId", course.getId());
            snapshot.put("title", course.getTitle());
            snapshot.put("description", course.getDescription());
            snapshot.put("category", course.getCategory());
            snapshot.put("price", course.getPrice());
            snapshot.put("isFree", course.isFree());
            snapshot.put("discountType", course.getDiscountType() != null ? course.getDiscountType().name() : null);
            snapshot.put("discountValue", course.getDiscountValue());
            snapshot.put("learningOutcomes", course.getLearningOutcomesList());
            snapshot.put("totalModules", modules != null ? modules.size() : 0);
            snapshot.put("totalLectures", lectures != null ? lectures.size() : 0);
            snapshot.put("submittedAt", LocalDateTime.now().toString());

            return OBJECT_MAPPER.writeValueAsString(snapshot);
        } catch (Exception e) {
            return "{\"courseId\":" + course.getId() + ",\"title\":\"" + course.getTitle() + "\",\"status\":\"PENDING_REVIEW\"}";
        }
    }

    private void validateInstructorOrAdmin(User currentUser) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        String role = currentUser.getRole();
        if (!"instructor".equalsIgnoreCase(role) && !"admin".equalsIgnoreCase(role)) {
            throw new AccessDeniedException("Access denied: Only instructors or admins can submit courses for review");
        }
        if ("instructor".equalsIgnoreCase(role) && !"active".equalsIgnoreCase(currentUser.getAccountStatus())) {
            throw new AccessDeniedException("Access denied: Instructor account is awaiting admin approval");
        }
    }

    private void verifyCourseAccess(Course course, User currentUser) {
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            return;
        }
        boolean isOwner = course.getInstructor() != null
                && currentUser.getId().equals(course.getInstructor().getId());
        boolean isCoInstructor = courseInstructorRepository != null
                && courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), currentUser.getId());

        if (!isOwner && !isCoInstructor) {
            throw new AccessDeniedException("Cannot submit another instructor's course for review");
        }
    }
}

