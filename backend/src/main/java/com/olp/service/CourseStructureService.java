package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CourseStructureService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final LessonRepository lessonRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;
    private final CourseSubmissionService courseSubmissionService;
    private final CourseAuditService courseAuditService;

    public CourseStructureService(
            CourseRepository courseRepository,
            CourseModuleRepository courseModuleRepository,
            CourseSectionRepository courseSectionRepository,
            LessonRepository lessonRepository,
            UserRepository userRepository) {
        this(courseRepository, courseModuleRepository, courseSectionRepository, lessonRepository, null, userRepository, null, null);
    }

    public CourseStructureService(
            CourseRepository courseRepository,
            CourseModuleRepository courseModuleRepository,
            CourseSectionRepository courseSectionRepository,
            LessonRepository lessonRepository,
            CourseInstructorRepository courseInstructorRepository,
            UserRepository userRepository) {
        this(courseRepository, courseModuleRepository, courseSectionRepository, lessonRepository, courseInstructorRepository, userRepository, null, null);
    }

    @Autowired
    public CourseStructureService(
            CourseRepository courseRepository,
            CourseModuleRepository courseModuleRepository,
            CourseSectionRepository courseSectionRepository,
            LessonRepository lessonRepository,
            @Autowired(required = false) CourseInstructorRepository courseInstructorRepository,
            UserRepository userRepository,
            @Autowired(required = false) CourseSubmissionService courseSubmissionService,
            @Autowired(required = false) CourseAuditService courseAuditService) {
        this.courseRepository = courseRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.courseSectionRepository = courseSectionRepository;
        this.lessonRepository = lessonRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.userRepository = userRepository;
        this.courseSubmissionService = courseSubmissionService;
        this.courseAuditService = courseAuditService;
    }

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

    // =========================================================================
    // FULL HIERARCHY STRUCTURE API
    // =========================================================================

    @Transactional(readOnly = true)
    public CourseStructureResponse getCourseStructure(User currentUser, Integer courseId) {
        Course course = verifyCourseAssociation(courseId, currentUser);
        List<CourseModule> modules = courseModuleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);

        List<CourseModuleResponse> moduleResponses = modules.stream()
                .map(m -> CourseModuleResponse.fromEntity(m, true))
                .toList();

        return new CourseStructureResponse(
                course.getId(),
                course.getTitle(),
                course.getStatus() != null ? course.getStatus().name() : null,
                moduleResponses);
    }

    // =========================================================================
    // MODULE CRUD & REORDER
    // =========================================================================

    @Transactional
    public CourseModuleResponse createModule(User currentUser, Integer courseId, CourseModuleRequest request) {
        Course course = verifyCourseAssociation(courseId, currentUser);
        verifyCourseNotUnderReview(course);

        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Module title cannot be blank");
        }
        String title = request.getTitle().trim();
        if (title.length() > 200) {
            throw new IllegalArgumentException("Module title must be 200 characters or fewer");
        }

        int orderIndex;
        if (request.getOrderIndex() != null && request.getOrderIndex() > 0) {
            orderIndex = request.getOrderIndex();
        } else {
            orderIndex = courseModuleRepository.findMaxOrderIndexByCourseId(courseId) + 1;
        }

        CourseModule module = new CourseModule(
                course,
                title,
                request.getDescription() != null ? request.getDescription().trim() : null,
                orderIndex);

        CourseModule saved = courseModuleRepository.save(module);
        handlePublishedCourseModification(course, currentUser, "Created module: " + saved.getTitle());
        return CourseModuleResponse.fromEntity(saved, false);
    }

    @Transactional(readOnly = true)
    public List<CourseModuleResponse> getModules(User currentUser, Integer courseId) {
        verifyCourseAssociation(courseId, currentUser);
        List<CourseModule> modules = courseModuleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        return modules.stream()
                .map(m -> CourseModuleResponse.fromEntity(m, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseModuleResponse getModule(User currentUser, Integer courseId, Integer moduleId) {
        verifyCourseAssociation(courseId, currentUser);
        CourseModule module = findAndVerifyModule(courseId, moduleId);
        return CourseModuleResponse.fromEntity(module, true);
    }

    @Transactional
    public CourseModuleResponse updateModule(User currentUser, Integer courseId, Integer moduleId,
            CourseModuleRequest request) {
        Course course = verifyCourseAssociation(courseId, currentUser);
        verifyCourseNotUnderReview(course);
        CourseModule module = findAndVerifyModule(courseId, moduleId);

        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Module title cannot be blank");
        }
        String title = request.getTitle().trim();
        if (title.length() > 200) {
            throw new IllegalArgumentException("Module title must be 200 characters or fewer");
        }

        module.setTitle(title);
        if (request.getDescription() != null) {
            module.setDescription(request.getDescription().trim());
        }
        if (request.getOrderIndex() != null && request.getOrderIndex() > 0) {
            module.setOrderIndex(request.getOrderIndex());
        }

        CourseModule updated = courseModuleRepository.save(module);
        handlePublishedCourseModification(course, currentUser, "Updated module: " + updated.getTitle());
        return CourseModuleResponse.fromEntity(updated, true);
    }

    @Transactional
    public ApiResponse deleteModule(User currentUser, Integer courseId, Integer moduleId) {
        Course course = verifyCourseAssociation(courseId, currentUser);
        verifyCourseNotUnderReview(course);
        CourseModule module = findAndVerifyModule(courseId, moduleId);
        courseModuleRepository.delete(module);
        handlePublishedCourseModification(course, currentUser, "Deleted module ID: " + moduleId);
        return new ApiResponse(true, "Module deleted successfully");
    }

    @Transactional
    public List<CourseModuleResponse> reorderModules(User currentUser, Integer courseId,
            ReorderStructureRequest request) {
        Course course = verifyCourseAssociation(courseId, currentUser);
        verifyCourseNotUnderReview(course);

        if (request == null || request.getOrderedIds() == null || request.getOrderedIds().isEmpty()) {
            throw new IllegalArgumentException("Ordered IDs list cannot be empty");
        }

        List<Integer> orderedIds = request.getOrderedIds();
        validateNoDuplicates(orderedIds, "module");

        for (int i = 0; i < orderedIds.size(); i++) {
            Integer moduleId = orderedIds.get(i);
            CourseModule module = findAndVerifyModule(courseId, moduleId);
            module.setOrderIndex(i + 1);
            courseModuleRepository.save(module);
        }

        handlePublishedCourseModification(course, currentUser, "Reordered modules");
        return getModules(currentUser, courseId);
    }

    // =========================================================================
    // SECTION CRUD & REORDER
    // =========================================================================

    @Transactional
    public CourseSectionResponse createSection(User currentUser, Integer courseId, Integer moduleId,
            CourseSectionRequest request) {
        Course course = verifyCourseAssociation(courseId, currentUser);
        verifyCourseNotUnderReview(course);
        CourseModule module = findAndVerifyModule(courseId, moduleId);

        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Section title cannot be blank");
        }
        String title = request.getTitle().trim();
        if (title.length() > 200) {
            throw new IllegalArgumentException("Section title must be 200 characters or fewer");
        }

        int orderIndex;
        if (request.getOrderIndex() != null && request.getOrderIndex() > 0) {
            orderIndex = request.getOrderIndex();
        } else {
            orderIndex = courseSectionRepository.findMaxOrderIndexByModuleId(moduleId) + 1;
        }

        CourseSection section = new CourseSection(
                module,
                title,
                request.getDescription() != null ? request.getDescription().trim() : null,
                orderIndex);

        CourseSection saved = courseSectionRepository.save(section);
        handlePublishedCourseModification(course, currentUser, "Created section: " + saved.getTitle());
        return CourseSectionResponse.fromEntity(saved, false);
    }

    @Transactional(readOnly = true)
    public List<CourseSectionResponse> getSections(User currentUser, Integer courseId, Integer moduleId) {
        verifyCourseAssociation(courseId, currentUser);
        findAndVerifyModule(courseId, moduleId);

        List<CourseSection> sections = courseSectionRepository.findByModuleIdOrderByOrderIndexAsc(moduleId);
        return sections.stream()
                .map(s -> CourseSectionResponse.fromEntity(s, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseSectionResponse getSection(User currentUser, Integer courseId, Integer moduleId, Integer sectionId) {
        verifyCourseAssociation(courseId, currentUser);
        findAndVerifyModule(courseId, moduleId);
        CourseSection section = findAndVerifySection(moduleId, sectionId);
        return CourseSectionResponse.fromEntity(section, true);
    }

    @Transactional
    public CourseSectionResponse updateSection(User currentUser, Integer courseId, Integer moduleId, Integer sectionId,
            CourseSectionRequest request) {
        Course course = verifyCourseAssociation(courseId, currentUser);
        verifyCourseNotUnderReview(course);
        findAndVerifyModule(courseId, moduleId);
        CourseSection section = findAndVerifySection(moduleId, sectionId);

        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Section title cannot be blank");
        }
        String title = request.getTitle().trim();
        if (title.length() > 200) {
            throw new IllegalArgumentException("Section title must be 200 characters or fewer");
        }

        section.setTitle(title);
        if (request.getDescription() != null) {
            section.setDescription(request.getDescription().trim());
        }
        if (request.getOrderIndex() != null && request.getOrderIndex() > 0) {
            section.setOrderIndex(request.getOrderIndex());
        }

        CourseSection updated = courseSectionRepository.save(section);
        handlePublishedCourseModification(course, currentUser, "Updated section: " + updated.getTitle());
        return CourseSectionResponse.fromEntity(updated, true);
    }

    @Transactional
    public ApiResponse deleteSection(User currentUser, Integer courseId, Integer moduleId, Integer sectionId) {
        Course course = verifyCourseAssociation(courseId, currentUser);
        verifyCourseNotUnderReview(course);
        findAndVerifyModule(courseId, moduleId);
        CourseSection section = findAndVerifySection(moduleId, sectionId);
        courseSectionRepository.delete(section);
        handlePublishedCourseModification(course, currentUser, "Deleted section ID: " + sectionId);
        return new ApiResponse(true, "Section deleted successfully");
    }

    @Transactional
    public List<CourseSectionResponse> reorderSections(User currentUser, Integer courseId, Integer moduleId,
            ReorderStructureRequest request) {
        Course course = verifyCourseAssociation(courseId, currentUser);
        verifyCourseNotUnderReview(course);
        findAndVerifyModule(courseId, moduleId);

        if (request == null || request.getOrderedIds() == null || request.getOrderedIds().isEmpty()) {
            throw new IllegalArgumentException("Ordered IDs list cannot be empty");
        }

        List<Integer> orderedIds = request.getOrderedIds();
        validateNoDuplicates(orderedIds, "section");

        for (int i = 0; i < orderedIds.size(); i++) {
            Integer sectionId = orderedIds.get(i);
            CourseSection section = findAndVerifySection(moduleId, sectionId);
            section.setOrderIndex(i + 1);
            courseSectionRepository.save(section);
        }

        handlePublishedCourseModification(course, currentUser, "Reordered sections");
        return getSections(currentUser, courseId, moduleId);
    }

    // =========================================================================
    // LESSON CRUD & REORDER
    // =========================================================================

    @Transactional
    public LessonResponse createLesson(User currentUser, Integer courseId, Integer moduleId, Integer sectionId,
            LessonRequest request) {
        Course course = verifyCourseAssociation(courseId, currentUser);
        verifyCourseNotUnderReview(course);
        findAndVerifyModule(courseId, moduleId);
        CourseSection section = findAndVerifySection(moduleId, sectionId);

        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Lesson title cannot be blank");
        }
        String title = request.getTitle().trim();
        if (title.length() > 200) {
            throw new IllegalArgumentException("Lesson title must be 200 characters or fewer");
        }

        LessonType lessonType = LessonType.fromString(request.getLessonType());

        int orderIndex;
        if (request.getOrderIndex() != null && request.getOrderIndex() > 0) {
            orderIndex = request.getOrderIndex();
        } else {
            orderIndex = lessonRepository.findMaxOrderIndexBySectionId(sectionId) + 1;
        }

        Lesson lesson = new Lesson(
                section,
                title,
                lessonType,
                request.getContent() != null ? request.getContent().trim() : null,
                request.getContentUrl() != null ? request.getContentUrl().trim() : null,
                request.getDurationMinutes(),
                orderIndex,
                request.getMetadata());

        Lesson saved = lessonRepository.save(lesson);
        handlePublishedCourseModification(course, currentUser, "Created lesson: " + saved.getTitle());
        return LessonResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> getLessons(User currentUser, Integer courseId, Integer moduleId, Integer sectionId) {
        verifyCourseAssociation(courseId, currentUser);
        findAndVerifyModule(courseId, moduleId);
        findAndVerifySection(moduleId, sectionId);

        List<Lesson> lessons = lessonRepository.findBySectionIdOrderByOrderIndexAsc(sectionId);
        return lessons.stream()
                .map(LessonResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public LessonResponse getLesson(User currentUser, Integer courseId, Integer moduleId, Integer sectionId,
            Integer lessonId) {
        verifyCourseAssociation(courseId, currentUser);
        findAndVerifyModule(courseId, moduleId);
        findAndVerifySection(moduleId, sectionId);
        Lesson lesson = findAndVerifyLesson(sectionId, lessonId);
        return LessonResponse.fromEntity(lesson);
    }

    @Transactional
    public LessonResponse updateLesson(User currentUser, Integer courseId, Integer moduleId, Integer sectionId,
            Integer lessonId, LessonRequest request) {
        Course course = verifyCourseAssociation(courseId, currentUser);
        verifyCourseNotUnderReview(course);
        findAndVerifyModule(courseId, moduleId);
        findAndVerifySection(moduleId, sectionId);
        Lesson lesson = findAndVerifyLesson(sectionId, lessonId);

        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Lesson title cannot be blank");
        }
        String title = request.getTitle().trim();
        if (title.length() > 200) {
            throw new IllegalArgumentException("Lesson title must be 200 characters or fewer");
        }

        lesson.setTitle(title);
        if (request.getLessonType() != null && !request.getLessonType().trim().isEmpty()) {
            lesson.setLessonType(LessonType.fromString(request.getLessonType()));
        }
        if (request.getContent() != null) {
            lesson.setContent(request.getContent().trim());
        }
        if (request.getContentUrl() != null) {
            lesson.setContentUrl(request.getContentUrl().trim());
        }
        if (request.getDurationMinutes() != null) {
            lesson.setDurationMinutes(request.getDurationMinutes());
        }
        if (request.getMetadata() != null) {
            lesson.setMetadata(request.getMetadata());
        }
        if (request.getOrderIndex() != null && request.getOrderIndex() > 0) {
            lesson.setOrderIndex(request.getOrderIndex());
        }

        Lesson updated = lessonRepository.save(lesson);
        handlePublishedCourseModification(course, currentUser, "Updated lesson: " + updated.getTitle());
        return LessonResponse.fromEntity(updated);
    }

    @Transactional
    public ApiResponse deleteLesson(User currentUser, Integer courseId, Integer moduleId, Integer sectionId,
            Integer lessonId) {
        Course course = verifyCourseAssociation(courseId, currentUser);
        verifyCourseNotUnderReview(course);
        findAndVerifyModule(courseId, moduleId);
        findAndVerifySection(moduleId, sectionId);
        Lesson lesson = findAndVerifyLesson(sectionId, lessonId);
        lessonRepository.delete(lesson);
        handlePublishedCourseModification(course, currentUser, "Deleted lesson ID: " + lessonId);
        return new ApiResponse(true, "Lesson deleted successfully");
    }

    @Transactional
    public List<LessonResponse> reorderLessons(User currentUser, Integer courseId, Integer moduleId, Integer sectionId,
            ReorderStructureRequest request) {
        Course course = verifyCourseAssociation(courseId, currentUser);
        verifyCourseNotUnderReview(course);
        findAndVerifyModule(courseId, moduleId);
        findAndVerifySection(moduleId, sectionId);

        if (request == null || request.getOrderedIds() == null || request.getOrderedIds().isEmpty()) {
            throw new IllegalArgumentException("Ordered IDs list cannot be empty");
        }

        List<Integer> orderedIds = request.getOrderedIds();
        validateNoDuplicates(orderedIds, "lesson");

        for (int i = 0; i < orderedIds.size(); i++) {
            Integer lessonId = orderedIds.get(i);
            Lesson lesson = findAndVerifyLesson(sectionId, lessonId);
            lesson.setOrderIndex(i + 1);
            lessonRepository.save(lesson);
        }

        handlePublishedCourseModification(course, currentUser, "Reordered lessons");
        return getLessons(currentUser, courseId, moduleId, sectionId);
    }

    // =========================================================================
    // ANCESTOR CHAIN AND PERMISSION HELPERS
    // =========================================================================

    public Course verifyCourseAssociation(Integer courseId, User currentUser) {
        validateInstructor(currentUser);

        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        boolean isOwner = course.getInstructor() != null && currentUser.getId().equals(course.getInstructor().getId());
        boolean isCoInstructor = courseInstructorRepository != null &&
                courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, currentUser.getId());
        boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getRole());

        if (!isOwner && !isCoInstructor && !isAdmin) {
            throw new AccessDeniedException(
                    "Cannot modify course structure: you are not an authorized instructor for this course");
        }

        return course;
    }

    private void verifyCourseNotUnderReview(Course course) {
        if (course != null && course.getStatus() == CourseStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Cannot modify course structure while course is under review (status is PENDING_REVIEW)");
        }
    }

    private void handlePublishedCourseModification(Course course, User currentUser, String actionDetails) {
        if (course != null && course.getStatus() == CourseStatus.PUBLISHED) {
            if (courseSubmissionService != null) {
                courseSubmissionService.triggerReReview(
                        currentUser,
                        course,
                        "Course structure modification on published course (" + actionDetails + ") triggered re-review");
            } else {
                course.setStatus(CourseStatus.PENDING_REVIEW);
                courseRepository.save(course);
            }
            if (courseAuditService != null) {
                courseAuditService.recordAudit(
                        course,
                        currentUser,
                        "COURSE_STRUCTURE_EDIT_MAJOR",
                        "curriculum",
                        "status: PUBLISHED",
                        "status: PENDING_REVIEW, details: " + actionDetails);
            }
        }
    }

    private CourseModule findAndVerifyModule(Integer courseId, Integer moduleId) {
        if (moduleId == null) {
            throw new IllegalArgumentException("Module ID cannot be null");
        }
        return courseModuleRepository.findByIdAndCourseId(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Module not found with id: " + moduleId + " in course: " + courseId));
    }

    private CourseSection findAndVerifySection(Integer moduleId, Integer sectionId) {
        if (sectionId == null) {
            throw new IllegalArgumentException("Section ID cannot be null");
        }
        return courseSectionRepository.findByIdAndModuleId(sectionId, moduleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section not found with id: " + sectionId + " in module: " + moduleId));
    }

    private Lesson findAndVerifyLesson(Integer sectionId, Integer lessonId) {
        if (lessonId == null) {
            throw new IllegalArgumentException("Lesson ID cannot be null");
        }
        return lessonRepository.findByIdAndSectionId(lessonId, sectionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lesson not found with id: " + lessonId + " in section: " + sectionId));
    }

    private void validateInstructor(User currentUser) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (!"instructor".equalsIgnoreCase(currentUser.getRole()) && !"admin".equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied: only instructors or admins can manage course structure");
        }
        if ("instructor".equalsIgnoreCase(currentUser.getRole())
                && !"active".equalsIgnoreCase(currentUser.getAccountStatus())) {
            throw new AccessDeniedException("Access denied: Instructor account is awaiting admin approval");
        }
    }

    private void validateNoDuplicates(List<Integer> ids, String entityName) {
        Set<Integer> seen = new HashSet<>();
        for (Integer id : ids) {
            if (id == null) {
                throw new IllegalArgumentException(entityName + " ID in reorder list cannot be null");
            }
            if (!seen.add(id)) {
                throw new IllegalArgumentException("Duplicate " + entityName + " ID in reorder list: " + id);
            }
        }
    }
}
