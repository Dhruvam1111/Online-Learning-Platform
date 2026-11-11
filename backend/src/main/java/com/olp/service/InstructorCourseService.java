package com.olp.service;

import com.olp.dto.CourseDraftRequest;
import com.olp.dto.CoursePrerequisiteRequest;
import com.olp.dto.CourseResponse;
import com.olp.dto.CreateCourseRequest;
import com.olp.dto.UpdateCourseRequest;
import com.olp.dto.UpdateCourseStatusRequest;
import com.olp.entity.Course;
import com.olp.entity.CourseInstructor;
import com.olp.entity.CoursePrerequisite;
import com.olp.entity.CourseStatus;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CoursePrerequisiteRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class InstructorCourseService {

    private static final List<Pattern> MISLEADING_CLAIM_PATTERNS = List.of(
            Pattern.compile("\\baccredited\\s+(degree|university|college|institution)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(degree|university|college)\\s+equivalence\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bequivalent\\s+to\\s+(a\\s+)?(degree|university\\s+degree|bachelor'?s|master'?s|phd|doctorate)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(official|national|government|regional|state)\\s+accreditation\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(officially|nationally|regionally|government)\\s+accredited\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(grants?|awards?|confers?|earns?)\\s+(a\\s+)?(degree|bachelor'?s|master'?s|phd|doctorate)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(bachelor'?s|master'?s|phd|doctorate)\\s+degree\\s+equivalent\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcollege\\s+credit\\s+(guaranteed|equivalent)\\b", Pattern.CASE_INSENSITIVE)
    );

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final CoursePrerequisiteRepository coursePrerequisiteRepository;

    public InstructorCourseService(CourseRepository courseRepository, UserRepository userRepository) {
        this(courseRepository, userRepository, null, null);
    }

    @Autowired
    public InstructorCourseService(
            CourseRepository courseRepository,
            UserRepository userRepository,
            @Autowired(required = false) CourseInstructorRepository courseInstructorRepository,
            @Autowired(required = false) CoursePrerequisiteRepository coursePrerequisiteRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.coursePrerequisiteRepository = coursePrerequisiteRepository;
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
     * FR-I60 — List courses associated with the authenticated instructor.
     */
    @Transactional(readOnly = true)
    public List<CourseResponse> getInstructorCourses(User currentUser) {
        validateInstructor(currentUser);

        return findAllAssociatedCourses(currentUser).stream()
                .map(CourseResponse::fromEntity)
                .toList();
    }

    /**
     * FR-I07 — List only DRAFT courses associated with the authenticated instructor.
     */
    @Transactional(readOnly = true)
    public List<CourseResponse> getInstructorDrafts(User currentUser) {
        validateInstructor(currentUser);

        return findAllAssociatedCourses(currentUser).stream()
                .filter(c -> c.getStatus() == CourseStatus.DRAFT)
                .map(CourseResponse::fromEntity)
                .toList();
    }

    /**
     * FR-I07 — Retrieve a single course (including DRAFT) for an associated instructor.
     */
    @Transactional(readOnly = true)
    public CourseResponse getInstructorCourseById(User currentUser, Integer courseId) {
        validateInstructor(currentUser);
        Course course = findCourseAndVerifyOwnership(courseId, currentUser);
        return CourseResponse.fromEntity(course);
    }

    /**
     * FR-I05 — Create a new course for the authenticated instructor (starts in DRAFT status).
     * Automatically associates the creator as instructor and owner.
     * Validates required fields, structured prerequisites, and prevents misleading accreditation claims.
     */
    @Transactional
    public CourseResponse createCourse(User currentUser, CreateCourseRequest request) {
        validateInstructor(currentUser);

        if (request == null) {
            throw new IllegalArgumentException("Course request cannot be null");
        }

        // FR-I05: Never accept the instructor ID directly from the request body
        if (request.getInstructorId() != null && !request.getInstructorId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Cannot create a course for another instructor");
        }

        // Validate title
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Course title cannot be blank");
        }
        String trimmedTitle = request.getTitle().trim();
        if (trimmedTitle.length() > 200) {
            throw new IllegalArgumentException("Title must be 200 characters or fewer");
        }
        validateContentPolicy("title", trimmedTitle);

        // Validate description
        String trimmedDescription = request.getDescription() != null ? request.getDescription().trim() : null;
        if (trimmedDescription != null) {
            validateContentPolicy("description", trimmedDescription);
        }

        // Validate category
        String category = "General";
        if (request.getCategory() != null) {
            if (request.getCategory().trim().isEmpty()) {
                throw new IllegalArgumentException("Course category cannot be blank");
            }
            category = request.getCategory().trim();
            if (category.length() > 100) {
                throw new IllegalArgumentException("Category must be 100 characters or fewer");
            }
            validateContentPolicy("category", category);
        }

        List<String> cleanOutcomes = validateAndCleanOutcomes(request.getLearningOutcomes(), false);
        List<CoursePrerequisiteRequest> prereqRequests = request.getPrerequisites();
        validatePrerequisites(prereqRequests, null);

        // FR-I05: Every newly created course must automatically have DRAFT status
        Course course = new Course(
                currentUser,
                trimmedTitle,
                trimmedDescription,
                category,
                CourseStatus.DRAFT
        );
        if (!cleanOutcomes.isEmpty()) {
            course.setLearningOutcomesList(cleanOutcomes);
        }

        Course saved = courseRepository.save(course);

        // FR-I05 & FR-I59: Maintain multi-instructor ownership mapping
        if (saved != null && courseInstructorRepository != null) {
            CourseInstructor ownerMapping = new CourseInstructor(saved, currentUser, "owner");
            courseInstructorRepository.save(ownerMapping);
            saved.getInstructors().add(ownerMapping);
        }

        // FR-I05: Store structured prerequisites for future enrollment eligibility checks
        if (saved != null && prereqRequests != null && coursePrerequisiteRepository != null) {
            persistPrerequisites(saved, prereqRequests);
        }

        return CourseResponse.fromEntity(saved);
    }

    /**
     * FR-I07 — Save a new or existing course as DRAFT without requiring all mandatory fields.
     */
    @Transactional
    public CourseResponse saveDraft(User currentUser, CourseDraftRequest request) {
        Integer targetCourseId = request != null ? request.getId() : null;
        return saveDraft(currentUser, targetCourseId, request);
    }

    /**
     * FR-I07 — Save a new (courseId == null) or partially update an existing course as DRAFT.
     * Skips course-completeness validation while validating data types/bounds and content policy.
     * Preserves existing fields when omitted and protects published/submitted/archived courses.
     */
    @Transactional
    public CourseResponse saveDraft(User currentUser, Integer courseId, CourseDraftRequest request) {
        validateInstructor(currentUser);

        if (request != null && request.getInstructorId() != null
                && !request.getInstructorId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Cannot save a draft for another instructor");
        }
        if (courseId != null && request != null && request.getId() != null && !request.getId().equals(courseId)) {
            throw new IllegalArgumentException("Course ID in path and body do not match");
        }

        if (courseId == null) {
            return createNewDraft(currentUser, request);
        }
        return updateExistingDraft(currentUser, courseId, request);
    }

    private CourseResponse createNewDraft(User currentUser, CourseDraftRequest request) {
        String title = "Untitled Draft";
        if (request != null && request.getTitle() != null) {
            String trimmedTitle = request.getTitle().trim();
            if (trimmedTitle.length() > 200) {
                throw new IllegalArgumentException("Title must be 200 characters or fewer");
            }
            if (!trimmedTitle.isEmpty()) {
                validateContentPolicy("title", trimmedTitle);
                title = trimmedTitle;
            }
        }

        String description = null;
        if (request != null && request.getDescription() != null) {
            String trimmedDescription = request.getDescription().trim();
            validateContentPolicy("description", trimmedDescription);
            description = trimmedDescription.isEmpty() ? null : trimmedDescription;
        }

        String category = "General";
        if (request != null && request.getCategory() != null) {
            String trimmedCategory = request.getCategory().trim();
            if (trimmedCategory.length() > 100) {
                throw new IllegalArgumentException("Category must be 100 characters or fewer");
            }
            if (!trimmedCategory.isEmpty()) {
                validateContentPolicy("category", trimmedCategory);
                category = trimmedCategory;
            }
        }

        List<String> cleanOutcomes = request != null
                ? validateAndCleanOutcomes(request.getLearningOutcomes(), true)
                : new ArrayList<>();

        List<CoursePrerequisiteRequest> prereqRequests = request != null ? request.getPrerequisites() : null;
        validatePrerequisites(prereqRequests, null);

        Course course = new Course(currentUser, title, description, category, CourseStatus.DRAFT);
        if (!cleanOutcomes.isEmpty()) {
            course.setLearningOutcomesList(cleanOutcomes);
        }

        Course saved = courseRepository.save(course);

        if (saved != null && courseInstructorRepository != null) {
            CourseInstructor ownerMapping = new CourseInstructor(saved, currentUser, "owner");
            courseInstructorRepository.save(ownerMapping);
            saved.getInstructors().add(ownerMapping);
        }

        if (saved != null && prereqRequests != null && coursePrerequisiteRepository != null) {
            persistPrerequisites(saved, prereqRequests);
        }

        return CourseResponse.fromEntity(saved);
    }

    private CourseResponse updateExistingDraft(User currentUser, Integer courseId, CourseDraftRequest request) {
        Course existing = findCourseAndVerifyOwnership(courseId, currentUser);

        // FR-I07: Preserve existing data and never change status of published, submitted, or archived courses to DRAFT
        CourseStatus currentStatus = existing.getStatus();
        if (currentStatus == CourseStatus.PUBLISHED
                || currentStatus == CourseStatus.PENDING_REVIEW
                || currentStatus == CourseStatus.ARCHIVED) {
            throw new IllegalStateException(
                    "Cannot save course as draft when status is " + currentStatus.name()
                            + ". Existing course data and status are preserved.");
        }

        if (currentStatus == null || currentStatus == CourseStatus.REJECTED) {
            existing.setStatus(CourseStatus.DRAFT);
        }

        if (request != null) {
            if (request.getTitle() != null) {
                String trimmedTitle = request.getTitle().trim();
                if (trimmedTitle.length() > 200) {
                    throw new IllegalArgumentException("Title must be 200 characters or fewer");
                }
                if (!trimmedTitle.isEmpty()) {
                    validateContentPolicy("title", trimmedTitle);
                    existing.setTitle(trimmedTitle);
                } else if (existing.getTitle() == null || existing.getTitle().trim().isEmpty()) {
                    existing.setTitle("Untitled Draft");
                }
            }

            if (request.getDescription() != null) {
                String trimmedDescription = request.getDescription().trim();
                validateContentPolicy("description", trimmedDescription);
                existing.setDescription(trimmedDescription.isEmpty() ? null : trimmedDescription);
            }

            if (request.getCategory() != null) {
                String trimmedCategory = request.getCategory().trim();
                if (trimmedCategory.length() > 100) {
                    throw new IllegalArgumentException("Category must be 100 characters or fewer");
                }
                if (!trimmedCategory.isEmpty()) {
                    validateContentPolicy("category", trimmedCategory);
                    existing.setCategory(trimmedCategory);
                }
            }

            if (request.getLearningOutcomes() != null) {
                List<String> cleanOutcomes = validateAndCleanOutcomes(request.getLearningOutcomes(), true);
                existing.setLearningOutcomesList(cleanOutcomes);
            }

            if (request.getPrerequisites() != null) {
                validatePrerequisites(request.getPrerequisites(), courseId);
                if (existing.getPrerequisites() != null) {
                    existing.getPrerequisites().clear();
                }
                if (coursePrerequisiteRepository != null) {
                    persistPrerequisites(existing, request.getPrerequisites());
                }
            }
        }

        Course saved = courseRepository.save(existing);
        return CourseResponse.fromEntity(saved);
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
        validateContentPolicy("title", request.getTitle().trim());

        course.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) {
            validateContentPolicy("description", request.getDescription());
            course.setDescription(request.getDescription());
        }

        Course updated = courseRepository.save(course);
        return CourseResponse.fromEntity(updated);
    }

    /**
     * FR-I09 — Change course status for an owned course.
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

    /**
     * Enforces content policies against misleading claims about accreditation or degree equivalence (DR-ED02).
     */
    public void validateContentPolicy(String fieldName, String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        for (Pattern pattern : MISLEADING_CLAIM_PATTERNS) {
            if (pattern.matcher(text).find()) {
                throw new IllegalArgumentException(
                        "Course " + fieldName + " must not make misleading claims regarding accreditation or degree equivalence");
            }
        }
    }

    private List<Course> findAllAssociatedCourses(User currentUser) {
        Map<Integer, Course> uniqueCourses = new LinkedHashMap<>();
        List<Course> primaryCourses = courseRepository.findByInstructorIdOrderByIdAsc(currentUser.getId());
        if (primaryCourses != null) {
            for (Course c : primaryCourses) {
                if (c != null && c.getId() != null) {
                    uniqueCourses.put(c.getId(), c);
                }
            }
        }
        if (courseInstructorRepository != null) {
            List<CourseInstructor> associations = courseInstructorRepository.findByInstructorId(currentUser.getId());
            if (associations != null) {
                for (CourseInstructor ci : associations) {
                    if (ci != null && ci.getCourse() != null && ci.getCourse().getId() != null) {
                        uniqueCourses.putIfAbsent(ci.getCourse().getId(), ci.getCourse());
                    }
                }
            }
        }
        return new ArrayList<>(uniqueCourses.values());
    }

    private List<String> validateAndCleanOutcomes(List<String> outcomes, boolean skipBlankItems) {
        List<String> cleanOutcomes = new ArrayList<>();
        if (outcomes == null) {
            return cleanOutcomes;
        }
        for (String outcome : outcomes) {
            if (outcome == null || outcome.trim().isEmpty()) {
                if (skipBlankItems) {
                    continue;
                }
                throw new IllegalArgumentException("Learning outcome cannot be blank");
            }
            String trimmedOutcome = outcome.trim();
            validateContentPolicy("learning outcome", trimmedOutcome);
            cleanOutcomes.add(trimmedOutcome);
        }
        return cleanOutcomes;
    }

    private void validatePrerequisites(List<CoursePrerequisiteRequest> prereqRequests, Integer selfCourseId) {
        if (prereqRequests == null) {
            return;
        }
        for (CoursePrerequisiteRequest prereq : prereqRequests) {
            if (prereq == null) {
                continue;
            }

            String type = prereq.getType() != null ? prereq.getType().trim().toUpperCase() : null;
            if (type == null || type.isEmpty()) {
                type = prereq.getRequiredCourseId() != null ? "COURSE" : "SKILL";
            }
            if (!"COURSE".equals(type) && !"SKILL".equals(type)) {
                throw new IllegalArgumentException("Invalid prerequisite type: must be COURSE or SKILL");
            }

            if (prereq.getTitle() != null) {
                if (prereq.getTitle().trim().length() > 200) {
                    throw new IllegalArgumentException("Prerequisite title must be 200 characters or fewer");
                }
                validateContentPolicy("prerequisite title", prereq.getTitle());
            }
            if (prereq.getDescription() != null) {
                if (prereq.getDescription().trim().length() > 500) {
                    throw new IllegalArgumentException("Prerequisite description must be 500 characters or fewer");
                }
                validateContentPolicy("prerequisite description", prereq.getDescription());
            }

            if ("COURSE".equals(type)) {
                if (prereq.getRequiredCourseId() == null && (prereq.getTitle() == null || prereq.getTitle().trim().isEmpty())) {
                    throw new IllegalArgumentException("Course prerequisite must specify either requiredCourseId or title");
                }
                if (prereq.getRequiredCourseId() != null) {
                    if (prereq.getRequiredCourseId() <= 0) {
                        throw new IllegalArgumentException("Prerequisite course ID must be positive");
                    }
                    if (selfCourseId != null && selfCourseId.equals(prereq.getRequiredCourseId())) {
                        throw new IllegalArgumentException("A course cannot be a prerequisite of itself");
                    }
                    courseRepository.findById(prereq.getRequiredCourseId())
                            .orElseThrow(() -> new ResourceNotFoundException("Prerequisite course not found with id: " + prereq.getRequiredCourseId()));
                }
            } else {
                if (prereq.getTitle() == null || prereq.getTitle().trim().isEmpty()) {
                    throw new IllegalArgumentException("Prerequisite skill title cannot be blank");
                }
            }
        }
    }

    private void persistPrerequisites(Course course, List<CoursePrerequisiteRequest> prereqRequests) {
        for (CoursePrerequisiteRequest prereq : prereqRequests) {
            if (prereq == null) {
                continue;
            }

            String type = prereq.getType() != null ? prereq.getType().trim().toUpperCase() : null;
            if (type == null || type.isEmpty()) {
                type = prereq.getRequiredCourseId() != null ? "COURSE" : "SKILL";
            }

            Course requiredCourseEntity = null;
            String title = prereq.getTitle() != null ? prereq.getTitle().trim() : null;

            if ("COURSE".equals(type) && prereq.getRequiredCourseId() != null) {
                requiredCourseEntity = courseRepository.findById(prereq.getRequiredCourseId()).orElse(null);
                if (title == null || title.isEmpty()) {
                    title = requiredCourseEntity != null ? requiredCourseEntity.getTitle() : "Course #" + prereq.getRequiredCourseId();
                }
            }

            String desc = prereq.getDescription() != null ? prereq.getDescription().trim() : null;
            CoursePrerequisite cp = new CoursePrerequisite(course, type, requiredCourseEntity, title, desc);
            CoursePrerequisite savedCp = coursePrerequisiteRepository.save(cp);
            course.getPrerequisites().add(savedCp);
        }
    }

    private Course findCourseAndVerifyOwnership(Integer courseId, User currentUser) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        boolean isOwner = course.getInstructor() != null && currentUser.getId().equals(course.getInstructor().getId());
        if (!isOwner && (courseInstructorRepository == null
                || !courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, currentUser.getId()))) {
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
        // FR-I01: Unapproved / pending instructors cannot access course management/creation
        if (!"active".equalsIgnoreCase(currentUser.getAccountStatus())) {
            throw new AccessDeniedException("Access denied: Instructor account is awaiting admin approval");
        }
    }
}
