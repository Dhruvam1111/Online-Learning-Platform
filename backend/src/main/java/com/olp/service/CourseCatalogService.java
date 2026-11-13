package com.olp.service;

import com.olp.dto.CertificateResponse;
import com.olp.dto.CourseDetailResponse;
import com.olp.dto.CourseInstructorResponse;
import com.olp.dto.CoursePricingResponse;
import com.olp.dto.EnrollmentResponse;
import com.olp.dto.LectureResponse;
import com.olp.dto.PublicCourseResponse;
import com.olp.entity.Course;
import com.olp.entity.CourseStatus;
import com.olp.entity.Enrollment;
import com.olp.entity.Lecture;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CourseCatalogService {

    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final ReviewRepository reviewRepository;
    private final BookmarkRepository bookmarkRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final InstructorProfileRepository instructorProfileRepository;
    private final CoursePricingService coursePricingService;
    private final CertificateRepository certificateRepository;

    public CourseCatalogService(CourseRepository courseRepository,
                                LectureRepository lectureRepository,
                                ReviewRepository reviewRepository,
                                BookmarkRepository bookmarkRepository,
                                EnrollmentRepository enrollmentRepository,
                                UserRepository userRepository) {
        this(courseRepository, lectureRepository, reviewRepository, bookmarkRepository,
                enrollmentRepository, userRepository, null, null, null, null);
    }

    public CourseCatalogService(CourseRepository courseRepository,
                                LectureRepository lectureRepository,
                                ReviewRepository reviewRepository,
                                BookmarkRepository bookmarkRepository,
                                EnrollmentRepository enrollmentRepository,
                                UserRepository userRepository,
                                CourseInstructorRepository courseInstructorRepository) {
        this(courseRepository, lectureRepository, reviewRepository, bookmarkRepository,
                enrollmentRepository, userRepository, courseInstructorRepository, null, null, null);
    }

    public CourseCatalogService(CourseRepository courseRepository,
                                LectureRepository lectureRepository,
                                ReviewRepository reviewRepository,
                                BookmarkRepository bookmarkRepository,
                                EnrollmentRepository enrollmentRepository,
                                UserRepository userRepository,
                                CourseInstructorRepository courseInstructorRepository,
                                InstructorProfileRepository instructorProfileRepository) {
        this(courseRepository, lectureRepository, reviewRepository, bookmarkRepository,
                enrollmentRepository, userRepository, courseInstructorRepository, instructorProfileRepository, null, null);
    }

    public CourseCatalogService(CourseRepository courseRepository,
                                LectureRepository lectureRepository,
                                ReviewRepository reviewRepository,
                                BookmarkRepository bookmarkRepository,
                                EnrollmentRepository enrollmentRepository,
                                UserRepository userRepository,
                                CourseInstructorRepository courseInstructorRepository,
                                InstructorProfileRepository instructorProfileRepository,
                                CoursePricingService coursePricingService) {
        this(courseRepository, lectureRepository, reviewRepository, bookmarkRepository,
                enrollmentRepository, userRepository, courseInstructorRepository, instructorProfileRepository, coursePricingService, null);
    }

    @Autowired
    public CourseCatalogService(CourseRepository courseRepository,
                                LectureRepository lectureRepository,
                                ReviewRepository reviewRepository,
                                BookmarkRepository bookmarkRepository,
                                EnrollmentRepository enrollmentRepository,
                                UserRepository userRepository,
                                @Autowired(required = false) CourseInstructorRepository courseInstructorRepository,
                                @Autowired(required = false) InstructorProfileRepository instructorProfileRepository,
                                @Autowired(required = false) CoursePricingService coursePricingService,
                                @Autowired(required = false) CertificateRepository certificateRepository) {
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
        this.reviewRepository = reviewRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.instructorProfileRepository = instructorProfileRepository;
        this.coursePricingService = coursePricingService;
        this.certificateRepository = certificateRepository;
    }

    /**
     * Resolves the current User entity from Authentication if available, or null for anonymous users.
     */
    public User resolveUserOptional(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * Resolves authenticated User or throws InvalidCredentialsException.
     */
    public User resolveUser(Authentication authentication) {
        User user = resolveUserOptional(authentication);
        if (user == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        return user;
    }

    /**
     * Public course catalog — returns all published courses.
     */
    @Transactional(readOnly = true)
    public List<PublicCourseResponse> getPublishedCourses(String search, User currentUser) {
        List<Course> courses;

        if (search != null && !search.trim().isEmpty()) {
            courses = courseRepository.findAll().stream()
                    .filter(c -> !c.isDeleted())
                    .filter(c -> c.getStatus() == CourseStatus.PUBLISHED)
                    .filter(c -> c.getTitle().toLowerCase().contains(search.trim().toLowerCase()) ||
                            (c.getDescription() != null && c.getDescription().toLowerCase().contains(search.trim().toLowerCase())))
                    .toList();
        } else {
            courses = courseRepository.findAll().stream()
                    .filter(c -> !c.isDeleted())
                    .filter(c -> c.getStatus() == CourseStatus.PUBLISHED)
                    .toList();
        }

        Integer currentUserId = currentUser != null ? currentUser.getId() : null;

        return courses.stream().map(c -> {
            int totalLectures = lectureRepository.countByCourseId(c.getId());
            Double avgRating = reviewRepository.findAverageRatingByCourseId(c.getId());
            int totalReviews = reviewRepository.findByCourseIdOrderByCreatedAtDesc(c.getId()).size();
            boolean enrolled = currentUserId != null &&
                    enrollmentRepository.existsByStudentIdAndCourseId(currentUserId, c.getId());
            boolean bookmarked = currentUserId != null &&
                    bookmarkRepository.existsByStudentIdAndCourseId(currentUserId, c.getId());

            PublicCourseResponse item = new PublicCourseResponse(
                    c.getId(),
                    c.getTitle(),
                    c.getDescription(),
                    c.getStatus().name(),
                    c.getInstructor() != null ? c.getInstructor().getId() : null,
                    c.getInstructor() != null ? c.getInstructor().getName() : "Instructor",
                    c.getCreatedAt(),
                    totalLectures,
                    avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 4.8,
                    totalReviews,
                    enrolled,
                    bookmarked
            );
            item.setInstructors(CourseInstructorResponse.resolveInstructorsForCourse(
                    c, courseInstructorRepository, instructorProfileRepository));
            item.setPrice(c.getPrice() != null ? c.getPrice() : BigDecimal.ZERO);
            item.setFree(c.isFree());
            item.setEnrollmentLimit(c.getEnrollmentLimit());
            if (coursePricingService != null) {
                CoursePricingResponse pr = coursePricingService.calculatePricing(c);
                if (pr != null) {
                    item.setFinalPayableAmount(pr.getFinalPayableAmount());
                }
            }
            return item;
        }).toList();
    }

    /**
     * Course details with lecture curriculum and review info.
     */
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(Integer courseId, User currentUser) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        verifyCourseVisibleToUser(course, currentUser);

        Integer currentUserId = currentUser != null ? currentUser.getId() : null;

        List<Lecture> lectures = lectureRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        boolean canViewContent = canViewContent(course, currentUser);
        List<LectureResponse> lectureResponses = lectures.stream()
                .map(l -> canViewContent ? LectureResponse.fromEntity(l) : LectureResponse.previewOf(l))
                .toList();

        Double avgRating = reviewRepository.findAverageRatingByCourseId(courseId);
        int totalReviews = reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId).size();
        boolean enrolled = currentUserId != null &&
                enrollmentRepository.existsByStudentIdAndCourseId(currentUserId, courseId);
        boolean bookmarked = currentUserId != null &&
                bookmarkRepository.existsByStudentIdAndCourseId(currentUserId, courseId);

        CourseDetailResponse detail = new CourseDetailResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getStatus() != null ? course.getStatus().name() : null,
                course.getInstructor() != null ? course.getInstructor().getId() : null,
                course.getInstructor() != null ? course.getInstructor().getName() : "Instructor",
                course.getCreatedAt(),
                lectureResponses.size(),
                avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 4.8,
                totalReviews,
                enrolled,
                bookmarked,
                lectureResponses
        );
        detail.setInstructors(CourseInstructorResponse.resolveInstructorsForCourse(
                course, courseInstructorRepository, instructorProfileRepository));
        detail.setPrice(course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO);
        detail.setFree(course.isFree());
        detail.setDiscountType(course.getDiscountType());
        detail.setDiscountValue(course.getDiscountValue());
        detail.setDiscountStartDate(course.getDiscountStartDate());
        detail.setDiscountEndDate(course.getDiscountEndDate());
        detail.setEnrollmentLimit(course.getEnrollmentLimit());
        if (coursePricingService != null) {
            CoursePricingResponse pr = coursePricingService.calculatePricing(course);
            if (pr != null) {
                detail.setDiscountAmount(pr.getDiscountAmount());
                detail.setDiscountedPrice(pr.getDiscountedPrice());
                detail.setTaxRate(pr.getTaxRate());
                detail.setTaxAmount(pr.getTaxAmount());
                detail.setFinalPayableAmount(pr.getFinalPayableAmount());
                detail.setEnrollmentLimitReached(pr.isEnrollmentLimitReached());
            }
        }
        return detail;
    }

    /**
     * FR-I59 — Retrieve all instructors associated with a course, including roles and basic profile info.
     * Respects draft privacy: non-published courses require associated instructor or admin access.
     */
    @Transactional(readOnly = true)
    public List<CourseInstructorResponse> getCourseInstructors(Integer courseId, User currentUser) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        verifyCourseVisibleToUser(course, currentUser);

        return CourseInstructorResponse.resolveInstructorsForCourse(
                course, courseInstructorRepository, instructorProfileRepository);
    }

    /**
     * Enrolls an authenticated student in the specified course.
     */
    @Transactional
    public EnrollmentResponse enrollCourse(User currentUser, Integer courseId) {
        if (!"student".equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("Only students can enroll in courses");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        if (course.isDeleted() || course.getStatus() != CourseStatus.PUBLISHED) {
            throw new AccessDeniedException("Cannot enroll in a course that is not published");
        }

        // FR-I10: Prevent enrollment when course enrollment limit is reached
        if (coursePricingService != null && coursePricingService.isEnrollmentLimitReached(course)) {
            throw new IllegalStateException("Course enrollment limit has been reached");
        } else if (course.getEnrollmentLimit() != null && course.getEnrollmentLimit() > 0) {
            long currentCount = enrollmentRepository.countByCourseId(course.getId());
            if (currentCount >= course.getEnrollmentLimit()) {
                throw new IllegalStateException("Course enrollment limit has been reached");
            }
        }

        if (enrollmentRepository.existsByStudentIdAndCourseId(currentUser.getId(), courseId)) {
            return new EnrollmentResponse(true, "Already enrolled in this course", courseId, true);
        }

        Enrollment enrollment = new Enrollment(currentUser, course);
        enrollmentRepository.save(enrollment);

        return new EnrollmentResponse(true, "Successfully enrolled in " + course.getTitle(), courseId, true);
    }

    /**
     * Returns lectures for a course. Video fields are only populated for the course's
     * instructor or an enrolled student; everyone else receives the syllabus outline only.
     */
    @Transactional(readOnly = true)
    public List<LectureResponse> getCourseLectures(Integer courseId, User currentUser) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        verifyCourseVisibleToUser(course, currentUser);

        boolean canViewContent = canViewContent(course, currentUser);
        return lectureRepository.findByCourseIdOrderByOrderIndexAsc(course.getId()).stream()
                .map(l -> canViewContent ? LectureResponse.fromEntity(l) : LectureResponse.previewOf(l))
                .toList();
    }

    /**
     * FR-I07, FR-I09 & FR-I12: Non-published courses are visible only to associated instructors
     * and authorized admins. However, for UNPUBLISHED and ARCHIVED courses, already-enrolled students
     * retain access to course details and learning materials. Deleted courses are inaccessible to all.
     */
    private void verifyCourseVisibleToUser(Course course, User currentUser) {
        if (course.isDeleted()) {
            throw new AccessDeniedException("Access denied: this course has been deleted");
        }
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            return;
        }
        if (isAssociatedInstructorOrAdmin(course, currentUser)) {
            return;
        }
        if (currentUser != null && (course.getStatus() == CourseStatus.UNPUBLISHED || course.getStatus() == CourseStatus.ARCHIVED)
                && enrollmentRepository != null
                && enrollmentRepository.existsByStudentIdAndCourseId(currentUser.getId(), course.getId())) {
            return;
        }
        throw new AccessDeniedException("Access denied: this course is not publicly available");
    }

    private boolean isAssociatedInstructorOrAdmin(Course course, User currentUser) {
        if (currentUser == null) {
            return false;
        }
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            return true;
        }
        if (course.getInstructor() != null
                && course.getInstructor().getId().equals(currentUser.getId())) {
            return true;
        }
        return courseInstructorRepository != null
                && courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), currentUser.getId());
    }

    /**
     * True when the user may see lecture video data: an admin, an associated instructor,
     * or a student enrolled in a published/non-draft course. Anonymous users never can.
     */
    private boolean canViewContent(Course course, User currentUser) {
        if (currentUser == null) {
            return false;
        }
        if (course.isDeleted()) {
            return false;
        }
        if (isAssociatedInstructorOrAdmin(course, currentUser)) {
            return true;
        }
        if (course.getStatus() == CourseStatus.DRAFT) {
            return false;
        }
        return enrollmentRepository.existsByStudentIdAndCourseId(currentUser.getId(), course.getId());
    }

    /**
     * FR-I12: Retrieve issued certificates for the authenticated student.
     */
    @Transactional(readOnly = true)
    public List<CertificateResponse> getStudentCertificates(User currentUser) {
        if (currentUser == null || certificateRepository == null) {
            return List.of();
        }
        return certificateRepository.findByStudentId(currentUser.getId()).stream()
                .map(CertificateResponse::fromEntity)
                .toList();
    }

    /**
     * FR-I12: Retrieve certificates issued for a course (subject to course visibility checks).
     */
    @Transactional(readOnly = true)
    public List<CertificateResponse> getCourseCertificates(Integer courseId, User currentUser) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        verifyCourseVisibleToUser(course, currentUser);
        if (certificateRepository == null) {
            return List.of();
        }
        return certificateRepository.findByCourseId(courseId).stream()
                .map(CertificateResponse::fromEntity)
                .toList();
    }

    /**
     * FR-I10 — Retrieve pricing breakdown, active discounts, taxes, and final payable amount (DR-PY03).
     */
    @Transactional(readOnly = true)
    public CoursePricingResponse getCoursePricing(Integer courseId) {
        if (coursePricingService != null) {
            return coursePricingService.calculatePricing(courseId);
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        CoursePricingResponse resp = new CoursePricingResponse();
        resp.setCourseId(course.getId());
        resp.setCourseTitle(course.getTitle());
        resp.setBasePrice(course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO);
        resp.setInstructorPrice(course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO);
        resp.setFree(course.isFree());
        resp.setEnrollmentLimit(course.getEnrollmentLimit());
        return resp;
    }
}
