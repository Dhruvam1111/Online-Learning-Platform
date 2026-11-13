package com.olp.service;

import com.olp.dto.CourseDraftRequest;
import com.olp.dto.CourseInstructorResponse;
import com.olp.dto.CoursePreviewResponse;
import com.olp.dto.CoursePricingRequest;
import com.olp.dto.CoursePricingResponse;
import com.olp.dto.CoursePrerequisiteRequest;
import com.olp.dto.CourseResponse;
import com.olp.dto.CourseSubmissionRequest;
import com.olp.dto.CourseSubmissionResponse;
import com.olp.dto.CreateCourseRequest;
import com.olp.dto.PageResponse;
import com.olp.dto.UpdateCourseRequest;
import com.olp.dto.UpdateCourseStatusRequest;
import com.olp.entity.Course;
import com.olp.entity.CourseInstructor;
import com.olp.entity.CourseInstructorRole;
import com.olp.entity.CoursePrerequisite;
import com.olp.entity.CourseStatus;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CoursePrerequisiteRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.InstructorProfileRepository;
import com.olp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final InstructorProfileRepository instructorProfileRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CoursePricingService coursePricingService;
    private final CoursePreviewService coursePreviewService;
    private final CourseSubmissionService courseSubmissionService;

    public InstructorCourseService(CourseRepository courseRepository, UserRepository userRepository) {
        this(courseRepository, userRepository, null, null, null, null, null, null, null);
    }

    public InstructorCourseService(
            CourseRepository courseRepository,
            UserRepository userRepository,
            CourseInstructorRepository courseInstructorRepository,
            CoursePrerequisiteRepository coursePrerequisiteRepository) {
        this(courseRepository, userRepository, courseInstructorRepository, coursePrerequisiteRepository, null, null, null, null, null);
    }

    public InstructorCourseService(
            CourseRepository courseRepository,
            UserRepository userRepository,
            CourseInstructorRepository courseInstructorRepository,
            CoursePrerequisiteRepository coursePrerequisiteRepository,
            InstructorProfileRepository instructorProfileRepository) {
        this(courseRepository, userRepository, courseInstructorRepository, coursePrerequisiteRepository,
                instructorProfileRepository, null, null, null, null);
    }

    public InstructorCourseService(
            CourseRepository courseRepository,
            UserRepository userRepository,
            CourseInstructorRepository courseInstructorRepository,
            CoursePrerequisiteRepository coursePrerequisiteRepository,
            InstructorProfileRepository instructorProfileRepository,
            EnrollmentRepository enrollmentRepository,
            CoursePricingService coursePricingService) {
        this(courseRepository, userRepository, courseInstructorRepository, coursePrerequisiteRepository,
                instructorProfileRepository, enrollmentRepository, coursePricingService, null, null);
    }

    public InstructorCourseService(
            CourseRepository courseRepository,
            UserRepository userRepository,
            CourseInstructorRepository courseInstructorRepository,
            CoursePrerequisiteRepository coursePrerequisiteRepository,
            InstructorProfileRepository instructorProfileRepository,
            EnrollmentRepository enrollmentRepository,
            CoursePricingService coursePricingService,
            CoursePreviewService coursePreviewService) {
        this(courseRepository, userRepository, courseInstructorRepository, coursePrerequisiteRepository,
                instructorProfileRepository, enrollmentRepository, coursePricingService, coursePreviewService, null);
    }

    @Autowired
    public InstructorCourseService(
            CourseRepository courseRepository,
            UserRepository userRepository,
            @Autowired(required = false) CourseInstructorRepository courseInstructorRepository,
            @Autowired(required = false) CoursePrerequisiteRepository coursePrerequisiteRepository,
            @Autowired(required = false) InstructorProfileRepository instructorProfileRepository,
            @Autowired(required = false) EnrollmentRepository enrollmentRepository,
            @Autowired(required = false) CoursePricingService coursePricingService,
            @Autowired(required = false) CoursePreviewService coursePreviewService,
            @Autowired(required = false) CourseSubmissionService courseSubmissionService) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.coursePrerequisiteRepository = coursePrerequisiteRepository;
        this.instructorProfileRepository = instructorProfileRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.coursePricingService = coursePricingService;
        this.coursePreviewService = coursePreviewService;
        this.courseSubmissionService = courseSubmissionService;
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
     * FR-I60 — List courses associated with the authenticated instructor (unpaginated, batch-enriched).
     */
    @Transactional(readOnly = true)
    public List<CourseResponse> getInstructorCourses(User currentUser) {
        validateInstructor(currentUser);

        List<Course> courses = findAllAssociatedCourses(currentUser);
        return enrichCourseResponses(courses);
    }

    /**
     * FR-I60 — Retrieve paginated "My Courses" list for the authenticated instructor (owner or co-instructor),
     * supporting status filtering (DRAFT, PENDING_REVIEW, PUBLISHED, ARCHIVED) and efficient batch enrollment counts.
     */
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getMyCourses(
            User currentUser,
            String statusFilter,
            int page,
            int size) {
        return getMyCourses(currentUser, statusFilter, null, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getMyCourses(
            User currentUser,
            String statusFilter,
            String search,
            int page,
            int size) {
        validateInstructor(currentUser);

        if (page < 0) {
            throw new IllegalArgumentException("Page index must be 0 or greater");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        CourseStatus parsedStatus = null;
        if (statusFilter != null && !statusFilter.trim().isEmpty()) {
            parsedStatus = CourseStatus.fromString(statusFilter);
        }

        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        Page<Course> coursePage;
        if (parsedStatus == null && trimmedSearch == null) {
            coursePage = courseRepository.findAssociatedCoursesByInstructor(currentUser.getId(), pageable);
        } else if (parsedStatus != null && trimmedSearch == null) {
            coursePage = courseRepository.findAssociatedCoursesByInstructorAndStatus(
                    currentUser.getId(), parsedStatus, pageable);
        } else if (parsedStatus == null) {
            coursePage = courseRepository.searchAssociatedCoursesByInstructor(
                    currentUser.getId(), trimmedSearch, pageable);
        } else {
            coursePage = courseRepository.searchAssociatedCoursesByInstructorAndStatus(
                    currentUser.getId(), parsedStatus, trimmedSearch, pageable);
        }

        if (coursePage == null) {
            coursePage = Page.empty(pageable);
        }

        List<CourseResponse> content = enrichCourseResponses(coursePage.getContent());
        return new PageResponse<>(
                content,
                coursePage.getNumber(),
                coursePage.getSize(),
                coursePage.getTotalElements(),
                coursePage.getTotalPages(),
                coursePage.isLast()
        );
    }

    /**
     * FR-I07 — List only DRAFT courses associated with the authenticated instructor.
     */
    @Transactional(readOnly = true)
    public List<CourseResponse> getInstructorDrafts(User currentUser) {
        validateInstructor(currentUser);

        List<Course> drafts = findAllAssociatedCourses(currentUser).stream()
                .filter(c -> c.getStatus() == CourseStatus.DRAFT)
                .toList();
        return enrichCourseResponses(drafts);
    }

    /**
     * FR-I07 — Retrieve a single course (including DRAFT) for an associated instructor.
     */
    @Transactional(readOnly = true)
    public CourseResponse getInstructorCourseById(User currentUser, Integer courseId) {
        validateInstructor(currentUser);
        Course course = findCourseAndVerifyOwnership(courseId, currentUser);
        return enrichCourseResponse(course);
    }

    /**
     * FR-I59 — Retrieve all instructors associated with a course, including roles and basic profile info.
     * Accessible to associated instructors (OWNER / CO_INSTRUCTOR) and admins.
     */
    @Transactional(readOnly = true)
    public List<CourseInstructorResponse> getCourseInstructors(User currentUser, Integer courseId) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getRole());
        Course course;
        if (isAdmin) {
            if (courseId == null) {
                throw new IllegalArgumentException("Course ID cannot be null");
            }
            course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        } else {
            validateInstructor(currentUser);
            course = findCourseAndVerifyOwnership(courseId, currentUser);
        }
        return CourseInstructorResponse.resolveInstructorsForCourse(
                course, courseInstructorRepository, instructorProfileRepository);
    }

    /**
     * FR-I59 — Remove a co-instructor from a course (sensitive operation restricted to OWNER only).
     */
    @Transactional
    public void removeCourseInstructor(User currentUser, Integer courseId, Integer targetInstructorId) {
        validateInstructor(currentUser);
        Course course = findCourseAndVerifyOwnerOnly(courseId, currentUser);

        if (targetInstructorId == null) {
            throw new IllegalArgumentException("Instructor ID cannot be null");
        }
        if (course.getInstructor() != null && targetInstructorId.equals(course.getInstructor().getId())) {
            throw new IllegalStateException("Cannot remove the primary course owner");
        }
        if (courseInstructorRepository == null) {
            throw new IllegalStateException("CourseInstructorRepository is not configured");
        }

        CourseInstructor mapping = courseInstructorRepository
                .findByCourseIdAndInstructorId(courseId, targetInstructorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instructor " + targetInstructorId + " is not associated with course " + courseId));

        if (mapping.isOwner()) {
            throw new IllegalStateException("Cannot remove a course owner");
        }

        courseInstructorRepository.delete(mapping);
        if (course.getInstructors() != null) {
            course.getInstructors().removeIf(ci -> ci.getInstructor() != null
                    && targetInstructorId.equals(ci.getInstructor().getId()));
        }
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

        if (request.getPrice() != null || request.getDiscountValue() != null || request.getEnrollmentLimit() != null
                || request.getDiscountType() != null) {
            CoursePricingRequest pricingReq = new CoursePricingRequest(
                    request.getPrice(),
                    request.getDiscountType(),
                    request.getDiscountValue(),
                    request.getDiscountStartDate(),
                    request.getDiscountEndDate(),
                    request.getEnrollmentLimit()
            );
            if (coursePricingService != null) {
                coursePricingService.validatePricingRequest(pricingReq);
                coursePricingService.applyPricing(course, pricingReq);
            } else {
                course.setPrice(request.getPrice() != null ? request.getPrice() : java.math.BigDecimal.ZERO);
                course.setDiscountType(request.getDiscountType());
                course.setDiscountValue(request.getDiscountValue() != null ? request.getDiscountValue() : java.math.BigDecimal.ZERO);
                course.setDiscountStartDate(request.getDiscountStartDate());
                course.setDiscountEndDate(request.getDiscountEndDate());
                course.setEnrollmentLimit(request.getEnrollmentLimit());
            }
        }

        Course saved = courseRepository.save(course);

        // FR-I05 & FR-I59: Maintain multi-instructor ownership mapping (creator is OWNER)
        if (saved != null && courseInstructorRepository != null) {
            CourseInstructor ownerMapping = new CourseInstructor(saved, currentUser, CourseInstructorRole.OWNER);
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

        if (request.getPrice() != null || request.getDiscountValue() != null || request.getEnrollmentLimit() != null
                || request.getDiscountType() != null) {
            CoursePricingRequest pricingReq = new CoursePricingRequest(
                    request.getPrice(),
                    request.getDiscountType(),
                    request.getDiscountValue(),
                    request.getDiscountStartDate(),
                    request.getDiscountEndDate(),
                    request.getEnrollmentLimit()
            );
            if (coursePricingService != null) {
                coursePricingService.validatePricingRequest(pricingReq);
                coursePricingService.applyPricing(course, pricingReq);
            } else {
                course.setPrice(request.getPrice() != null ? request.getPrice() : java.math.BigDecimal.ZERO);
                course.setDiscountType(request.getDiscountType());
                course.setDiscountValue(request.getDiscountValue() != null ? request.getDiscountValue() : java.math.BigDecimal.ZERO);
                course.setDiscountStartDate(request.getDiscountStartDate());
                course.setDiscountEndDate(request.getDiscountEndDate());
                course.setEnrollmentLimit(request.getEnrollmentLimit());
            }
        }

        Course saved = courseRepository.save(course);

        if (saved != null && courseInstructorRepository != null) {
            CourseInstructor ownerMapping = new CourseInstructor(saved, currentUser, CourseInstructorRole.OWNER);
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

            if (request.getPrice() != null) {
                existing.setPrice(request.getPrice());
            }
            if (request.getDiscountType() != null) {
                existing.setDiscountType(request.getDiscountType());
            }
            if (request.getDiscountValue() != null) {
                existing.setDiscountValue(request.getDiscountValue());
            }
            if (request.getDiscountStartDate() != null) {
                existing.setDiscountStartDate(request.getDiscountStartDate());
            }
            if (request.getDiscountEndDate() != null) {
                existing.setDiscountEndDate(request.getDiscountEndDate());
            }
            if (request.getEnrollmentLimit() != null) {
                existing.setEnrollmentLimit(request.getEnrollmentLimit());
            }
        }

        Course saved = courseRepository.save(existing);
        return CourseResponse.fromEntity(saved);
    }

    /**
     * FR-I09 & FR-I59 — Update course content for an associated instructor (OWNER or CO_INSTRUCTOR).
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

        if (course.getStatus() == CourseStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Cannot edit course while it is under review (status is PENDING_REVIEW)");
        }

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        validateContentPolicy("title", request.getTitle().trim());

        course.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) {
            validateContentPolicy("description", request.getDescription());
            course.setDescription(request.getDescription());
        }

        if (request.getPrice() != null || request.getDiscountType() != null || request.getDiscountValue() != null
                || request.getDiscountStartDate() != null || request.getDiscountEndDate() != null || request.getEnrollmentLimit() != null) {
            CoursePricingRequest pricingReq = new CoursePricingRequest(
                    request.getPrice() != null ? request.getPrice() : course.getPrice(),
                    request.getDiscountType() != null ? request.getDiscountType() : course.getDiscountType(),
                    request.getDiscountValue() != null ? request.getDiscountValue() : course.getDiscountValue(),
                    request.getDiscountStartDate() != null ? request.getDiscountStartDate() : course.getDiscountStartDate(),
                    request.getDiscountEndDate() != null ? request.getDiscountEndDate() : course.getDiscountEndDate(),
                    request.getEnrollmentLimit() != null ? request.getEnrollmentLimit() : course.getEnrollmentLimit()
            );
            if (coursePricingService != null) {
                coursePricingService.validatePricingRequest(pricingReq);
                coursePricingService.applyPricing(course, pricingReq);
            } else {
                if (request.getPrice() != null) course.setPrice(request.getPrice());
                if (request.getDiscountType() != null) course.setDiscountType(request.getDiscountType());
                if (request.getDiscountValue() != null) course.setDiscountValue(request.getDiscountValue());
                if (request.getDiscountStartDate() != null) course.setDiscountStartDate(request.getDiscountStartDate());
                if (request.getDiscountEndDate() != null) course.setDiscountEndDate(request.getDiscountEndDate());
                if (request.getEnrollmentLimit() != null) course.setEnrollmentLimit(request.getEnrollmentLimit());
            }
        }

        Course updated = courseRepository.save(course);
        return enrichCourseResponse(updated);
    }

    /**
     * FR-I10 — Update pricing, discount rules, and enrollment limits for an associated course.
     */
    @Transactional
    public CoursePricingResponse updateCoursePricing(User currentUser, Integer courseId, CoursePricingRequest request) {
        validateInstructor(currentUser);
        if (coursePricingService != null) {
            return coursePricingService.updateCoursePricing(currentUser, courseId, request);
        }
        Course course = findCourseAndVerifyOwnership(courseId, currentUser);
        if (course.getStatus() == CourseStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Cannot update course pricing while it is under review (status is PENDING_REVIEW)");
        }
        if (request.getPrice() != null) {
            course.setPrice(request.getPrice());
        }
        course.setDiscountType(request.getDiscountType());
        course.setDiscountValue(request.getDiscountValue() != null ? request.getDiscountValue() : java.math.BigDecimal.ZERO);
        course.setDiscountStartDate(request.getDiscountStartDate());
        course.setDiscountEndDate(request.getDiscountEndDate());
        course.setEnrollmentLimit(request.getEnrollmentLimit());
        Course saved = courseRepository.save(course);
        CoursePricingResponse resp = new CoursePricingResponse();
        resp.setCourseId(saved.getId());
        resp.setCourseTitle(saved.getTitle());
        resp.setBasePrice(saved.getPrice());
        resp.setInstructorPrice(saved.getPrice());
        resp.setFree(saved.isFree());
        resp.setEnrollmentLimit(saved.getEnrollmentLimit());
        return resp;
    }

    /**
     * FR-I10 — Retrieve pricing, discounts, tax, and final payable amount breakdown for an associated course.
     */
    @Transactional(readOnly = true)
    public CoursePricingResponse getCoursePricing(User currentUser, Integer courseId) {
        validateInstructor(currentUser);
        Course course = findCourseAndVerifyOwnership(courseId, currentUser);
        if (coursePricingService != null) {
            return coursePricingService.calculatePricing(course);
        }
        CoursePricingResponse resp = new CoursePricingResponse();
        resp.setCourseId(course.getId());
        resp.setCourseTitle(course.getTitle());
        resp.setBasePrice(course.getPrice());
        resp.setInstructorPrice(course.getPrice());
        resp.setFree(course.isFree());
        resp.setEnrollmentLimit(course.getEnrollmentLimit());
        return resp;
    }

    /**
     * FR-I09 & FR-I59 — Change course status for an associated course.
     * Transitioning to or from ARCHIVED is a sensitive operation restricted to the OWNER only.
     */
    @Transactional
    public CourseResponse updateCourseStatus(User currentUser, Integer courseId, UpdateCourseStatusRequest request) {
        validateInstructor(currentUser);

        Course course = findCourseAndVerifyOwnership(courseId, currentUser);

        if (course.getStatus() == CourseStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Cannot change status of a course while it is under review (status is PENDING_REVIEW)");
        }

        if (request == null || request.getStatus() == null || request.getStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("Course status cannot be blank");
        }

        CourseStatus newStatus = CourseStatus.fromString(request.getStatus());
        if (newStatus == CourseStatus.ARCHIVED || course.getStatus() == CourseStatus.ARCHIVED) {
            verifyOwnerOnly(course, currentUser, "archive or unarchive");
        }

        course.setStatus(newStatus);

        Course updated = courseRepository.save(course);
        return enrichCourseResponse(updated);
    }

    /**
     * FR-I59 — Archive a course (restricted to OWNER only).
     */
    @Transactional
    public CourseResponse archiveCourse(User currentUser, Integer courseId) {
        validateInstructor(currentUser);
        Course course = findCourseAndVerifyOwnerOnly(courseId, currentUser);
        course.setStatus(CourseStatus.ARCHIVED);
        Course updated = courseRepository.save(course);
        return enrichCourseResponse(updated);
    }

    /**
     * FR-I59 — Delete a course (restricted to OWNER only).
     */
    @Transactional
    public void deleteCourse(User currentUser, Integer courseId) {
        validateInstructor(currentUser);
        Course course = findCourseAndVerifyOwnerOnly(courseId, currentUser);
        if (course.getStatus() == CourseStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Cannot delete course while it is under review (status is PENDING_REVIEW)");
        }
        courseRepository.delete(course);
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

    private CourseResponse enrichCourseResponse(Course course) {
        if (course == null) {
            return null;
        }
        List<CourseResponse> enriched = enrichCourseResponses(List.of(course));
        return enriched.isEmpty() ? CourseResponse.fromEntity(course) : enriched.get(0);
    }

    private List<CourseResponse> enrichCourseResponses(List<Course> courses) {
        if (courses == null || courses.isEmpty()) {
            return List.of();
        }

        List<Integer> courseIds = new ArrayList<>();
        for (Course c : courses) {
            if (c != null && c.getId() != null) {
                courseIds.add(c.getId());
            }
        }

        Map<Integer, Long> enrollmentCounts = new HashMap<>();
        if (enrollmentRepository != null && !courseIds.isEmpty()) {
            if (courseIds.size() == 1) {
                Integer cid = courseIds.get(0);
                enrollmentCounts.put(cid, enrollmentRepository.countByCourseId(cid));
            } else {
                List<Object[]> rows = enrollmentRepository.countEnrollmentsByCourseIds(courseIds);
                if (rows != null) {
                    for (Object[] row : rows) {
                        if (row != null && row.length >= 2
                                && row[0] instanceof Number idNum && row[1] instanceof Number countNum) {
                            enrollmentCounts.put(idNum.intValue(), countNum.longValue());
                        }
                    }
                }
            }
        }

        Map<Integer, List<CourseInstructorResponse>> instructorsByCourse =
                CourseInstructorResponse.resolveInstructorsForCourses(
                        courses, courseInstructorRepository, instructorProfileRepository);

        List<CourseResponse> responses = new ArrayList<>(courses.size());
        for (int idx = 0; idx < courses.size(); idx++) {
            Course course = courses.get(idx);
            if (course == null) {
                continue;
            }
            CourseResponse dto = CourseResponse.fromEntity(course);
            if (course.getId() != null) {
                dto.setEnrollmentCount(enrollmentCounts.getOrDefault(course.getId(), 0L));
            }
            Integer key = course.getId() != null ? course.getId() : -(idx + 1);
            List<CourseInstructorResponse> resolvedTeam = instructorsByCourse.get(key);
            if (resolvedTeam != null) {
                dto.setInstructors(resolvedTeam);
            }
            responses.add(dto);
        }
        return responses;
    }

    /**
     * FR-I73: Previews a course as a student using draft data, in strictly read-only mode.
     */
    @Transactional(readOnly = true)
    public CoursePreviewResponse previewCourseAsStudent(User currentUser, Integer courseId) {
        if (coursePreviewService != null) {
            return coursePreviewService.previewCourseAsStudent(currentUser, courseId);
        }
        validateInstructor(currentUser);
        Course course = findCourseAndVerifyOwnership(courseId, currentUser);
        CoursePreviewResponse response = new CoursePreviewResponse();
        response.setId(course.getId());
        response.setTitle(course.getTitle());
        response.setDescription(course.getDescription());
        response.setStatus(course.getStatus() != null ? course.getStatus().name() : "DRAFT");
        response.setPreview(true);
        response.setReadOnly(true);
        response.setCanEnroll(false);
        response.setEnrolled(false);
        response.setProgressTrackingEnabled(false);
        return response;
    }

    /**
     * FR-I08: Submit a course for admin review.
     */
    @Transactional
    public CourseSubmissionResponse submitCourseForReview(User currentUser, Integer courseId, CourseSubmissionRequest request) {
        validateInstructor(currentUser);
        if (courseSubmissionService != null) {
            return courseSubmissionService.submitCourseForReview(currentUser, courseId, request);
        }
        throw new IllegalStateException("CourseSubmissionService is not configured");
    }

    /**
     * FR-I08: Retrieve submission history for an associated course.
     */
    @Transactional(readOnly = true)
    public List<CourseSubmissionResponse> getCourseSubmissions(User currentUser, Integer courseId) {
        validateInstructor(currentUser);
        if (courseSubmissionService != null) {
            return courseSubmissionService.getSubmissionHistory(currentUser, courseId);
        }
        throw new IllegalStateException("CourseSubmissionService is not configured");
    }

    private boolean isCourseOwner(Course course, User currentUser) {
        if (course == null || currentUser == null || currentUser.getId() == null) {
            return false;
        }
        if (course.getInstructor() != null && currentUser.getId().equals(course.getInstructor().getId())) {
            return true;
        }
        return courseInstructorRepository != null
                && courseInstructorRepository.existsByCourseIdAndInstructorIdAndRole(
                        course.getId(), currentUser.getId(), CourseInstructorRole.OWNER);
    }

    private Course findCourseAndVerifyOwnerOnly(Integer courseId, User currentUser) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        verifyOwnerOnly(course, currentUser, "archive, delete, or manage owners of");
        return course;
    }

    private void verifyOwnerOnly(Course course, User currentUser, String actionDescription) {
        if (!isCourseOwner(course, currentUser)) {
            throw new AccessDeniedException(
                    "Access denied: Only the course OWNER can " + actionDescription + " this course");
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
