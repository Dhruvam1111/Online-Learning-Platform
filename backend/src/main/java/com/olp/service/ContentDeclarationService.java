package com.olp.service;

import com.olp.dto.ContentDeclarationRequest;
import com.olp.dto.ContentDeclarationResponse;
import com.olp.dto.TakedownReplaceRequest;
import com.olp.dto.TakedownResponseRequest;
import com.olp.entity.*;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.ContentDeclarationRepository;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.LessonRepository;
import com.olp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service managing Content Ownership Declarations and Takedown Lifecycle (FR-I64, Step 14).
 * Enforces copyright compliance and intellectual property verification prior to upload.
 */
@Service
@Transactional
public class ContentDeclarationService {

    private final ContentDeclarationRepository declarationRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;

    @Autowired
    public ContentDeclarationService(
            ContentDeclarationRepository declarationRepository,
            CourseRepository courseRepository,
            LessonRepository lessonRepository,
            CourseInstructorRepository courseInstructorRepository,
            UserRepository userRepository) {
        this.declarationRepository = declarationRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.userRepository = userRepository;
    }

    /**
     * Resolves the authenticated User from the security context authentication.
     */
    public User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * Submit an intellectual property and content ownership declaration.
     */
    public ContentDeclarationResponse declareContent(User instructor, ContentDeclarationRequest request) {
        if (instructor == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        if (!"instructor".equalsIgnoreCase(instructor.getRole()) && !"admin".equalsIgnoreCase(instructor.getRole())) {
            throw new AccessDeniedException("Only instructors or administrators can declare content ownership");
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));

        assertInstructorAccess(course, instructor);

        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + request.getLessonId()));

            // Verify lesson belongs to course
            Integer lessonCourseId = extractCourseIdFromLesson(lesson);
            if (!Objects.equals(lessonCourseId, course.getId())) {
                throw new IllegalArgumentException("Lesson with ID " + request.getLessonId() + " does not belong to course with ID " + course.getId());
            }
        }

        ContentDeclaration declaration = new ContentDeclaration(
                course,
                lesson,
                instructor,
                request.getDeclarationText(),
                request.getLicenseType(),
                request.getSourceAttribution()
        );

        ContentDeclaration saved = declarationRepository.save(declaration);
        return ContentDeclarationResponse.fromEntity(saved);
    }

    /**
     * List all declarations associated with a course.
     */
    @Transactional(readOnly = true)
    public List<ContentDeclarationResponse> getDeclarationsByCourse(Integer courseId, User instructor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        assertInstructorAccess(course, instructor);

        return declarationRepository.findByCourseIdOrderByDeclaredAtDesc(courseId)
                .stream()
                .map(ContentDeclarationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve active declaration for a specific lesson (or fallback to course-level).
     */
    @Transactional(readOnly = true)
    public ContentDeclarationResponse getDeclarationByLesson(Integer lessonId, User instructor) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));

        Integer courseId = extractCourseIdFromLesson(lesson);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        assertInstructorAccess(course, instructor);

        ContentDeclaration declaration = declarationRepository.findFirstByLessonIdOrderByDeclaredAtDesc(lessonId)
                .or(() -> declarationRepository.findFirstByCourseIdAndLessonIsNullOrderByDeclaredAtDesc(courseId))
                .orElseThrow(() -> new ResourceNotFoundException("No active content ownership declaration found for lesson ID: " + lessonId));

        return ContentDeclarationResponse.fromEntity(declaration);
    }

    /**
     * Retrieve declaration by unique ID.
     */
    @Transactional(readOnly = true)
    public ContentDeclarationResponse getDeclarationById(Integer id, User instructor) {
        ContentDeclaration declaration = declarationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content declaration not found with id: " + id));

        assertInstructorAccess(declaration.getCourse(), instructor);
        return ContentDeclarationResponse.fromEntity(declaration);
    }

    /**
     * Instructor responds to a takedown notice for flagged content.
     */
    public ContentDeclarationResponse respondToTakedown(Integer id, User instructor, TakedownResponseRequest request) {
        ContentDeclaration declaration = declarationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content declaration not found with id: " + id));

        assertInstructorAccess(declaration.getCourse(), instructor);

        if (declaration.getTakedownStatus() != TakedownStatus.FLAGGED) {
            throw new IllegalStateException("Cannot respond to takedown notice: declaration is currently in status " + declaration.getTakedownStatus());
        }

        declaration.setTakedownStatus(TakedownStatus.RESPONDED);
        declaration.setInstructorResponse(request.getResponse());
        declaration.setInstructorRespondedAt(LocalDateTime.now());

        ContentDeclaration updated = declarationRepository.save(declaration);
        return ContentDeclarationResponse.fromEntity(updated);
    }

    /**
     * Voluntarily remove flagged content in response to takedown notice.
     */
    public ContentDeclarationResponse removeContent(Integer id, User instructor) {
        ContentDeclaration declaration = declarationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content declaration not found with id: " + id));

        assertInstructorAccess(declaration.getCourse(), instructor);

        if (declaration.getTakedownStatus() != TakedownStatus.FLAGGED && declaration.getTakedownStatus() != TakedownStatus.RESPONDED) {
            throw new IllegalStateException("Content can only be removed from FLAGGED or RESPONDED state, current status: " + declaration.getTakedownStatus());
        }

        declaration.setTakedownStatus(TakedownStatus.REMOVED);
        declaration.setResolvedAt(LocalDateTime.now());

        ContentDeclaration updated = declarationRepository.save(declaration);
        return ContentDeclarationResponse.fromEntity(updated);
    }

    /**
     * Replace flagged content with licensed or corrected version.
     */
    public ContentDeclarationResponse replaceContent(Integer id, User instructor, TakedownReplaceRequest request) {
        ContentDeclaration declaration = declarationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content declaration not found with id: " + id));

        assertInstructorAccess(declaration.getCourse(), instructor);

        if (declaration.getTakedownStatus() != TakedownStatus.FLAGGED && declaration.getTakedownStatus() != TakedownStatus.RESPONDED) {
            throw new IllegalStateException("Content can only be replaced from FLAGGED or RESPONDED state, current status: " + declaration.getTakedownStatus());
        }

        if (request != null && request.getSourceAttribution() != null && !request.getSourceAttribution().isBlank()) {
            declaration.setSourceAttribution(request.getSourceAttribution());
        }
        declaration.setTakedownStatus(TakedownStatus.REPLACED);
        declaration.setResolvedAt(LocalDateTime.now());

        ContentDeclaration updated = declarationRepository.save(declaration);
        return ContentDeclarationResponse.fromEntity(updated);
    }

    /**
     * Check whether a lesson or course has a valid active ownership declaration.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> checkDeclarationStatus(Integer courseId, Integer lessonId, User instructor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        assertInstructorAccess(course, instructor);

        boolean declared = isDeclared(courseId, lessonId);
        return Map.of(
                "courseId", courseId,
                "lessonId", lessonId != null ? lessonId : 0,
                "declared", declared
        );
    }

    /**
     * Fast boolean lookup for declaration validity (excluding removed items).
     */
    @Transactional(readOnly = true)
    public boolean isDeclared(Integer courseId, Integer lessonId) {
        if (lessonId != null) {
            boolean lessonDeclared = declarationRepository.existsByLessonIdAndTakedownStatusNot(lessonId, TakedownStatus.REMOVED);
            if (lessonDeclared) {
                return true;
            }
        }
        if (courseId != null) {
            return declarationRepository.existsByCourseIdAndLessonIsNullAndTakedownStatusNot(courseId, TakedownStatus.REMOVED);
        }
        return false;
    }

    /**
     * Gate utility for Step 13 (Video Upload) and Step 15 (Materials Upload).
     * Throws IllegalStateException if no valid declaration exists.
     */
    @Transactional(readOnly = true)
    public void assertDeclared(Integer lessonId) {
        if (lessonId == null) {
            throw new IllegalArgumentException("Lesson ID cannot be null when verifying content declaration");
        }
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));

        Integer courseId = extractCourseIdFromLesson(lesson);
        if (!isDeclared(courseId, lessonId)) {
            throw new IllegalStateException("Content upload blocked: No valid content ownership declaration found for lesson ID: " + lessonId);
        }
    }

    /**
     * Validates that the user is the course owner, an authorized co-instructor, or an administrator.
     */
    private void assertInstructorAccess(Course course, User instructor) {
        if (instructor == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        if ("admin".equalsIgnoreCase(instructor.getRole())) {
            return;
        }
        if (course.getInstructor() != null && Objects.equals(course.getInstructor().getId(), instructor.getId())) {
            return;
        }
        if (courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), instructor.getId())) {
            return;
        }
        throw new AccessDeniedException("You are not authorized to manage declarations for course: " + course.getTitle());
    }

    private Integer extractCourseIdFromLesson(Lesson lesson) {
        if (lesson.getSection() != null &&
            lesson.getSection().getModule() != null &&
            lesson.getSection().getModule().getCourse() != null) {
            return lesson.getSection().getModule().getCourse().getId();
        }
        return null;
    }
}
