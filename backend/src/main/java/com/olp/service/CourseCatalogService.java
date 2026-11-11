package com.olp.service;

import com.olp.dto.CourseDetailResponse;
import com.olp.dto.CourseInstructorResponse;
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

    public CourseCatalogService(CourseRepository courseRepository,
                                LectureRepository lectureRepository,
                                ReviewRepository reviewRepository,
                                BookmarkRepository bookmarkRepository,
                                EnrollmentRepository enrollmentRepository,
                                UserRepository userRepository) {
        this(courseRepository, lectureRepository, reviewRepository, bookmarkRepository,
                enrollmentRepository, userRepository, null, null);
    }

    public CourseCatalogService(CourseRepository courseRepository,
                                LectureRepository lectureRepository,
                                ReviewRepository reviewRepository,
                                BookmarkRepository bookmarkRepository,
                                EnrollmentRepository enrollmentRepository,
                                UserRepository userRepository,
                                CourseInstructorRepository courseInstructorRepository) {
        this(courseRepository, lectureRepository, reviewRepository, bookmarkRepository,
                enrollmentRepository, userRepository, courseInstructorRepository, null);
    }

    @Autowired
    public CourseCatalogService(CourseRepository courseRepository,
                                LectureRepository lectureRepository,
                                ReviewRepository reviewRepository,
                                BookmarkRepository bookmarkRepository,
                                EnrollmentRepository enrollmentRepository,
                                UserRepository userRepository,
                                @Autowired(required = false) CourseInstructorRepository courseInstructorRepository,
                                @Autowired(required = false) InstructorProfileRepository instructorProfileRepository) {
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
        this.reviewRepository = reviewRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.instructorProfileRepository = instructorProfileRepository;
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
                    .filter(c -> c.getStatus() == CourseStatus.PUBLISHED)
                    .filter(c -> c.getTitle().toLowerCase().contains(search.trim().toLowerCase()) ||
                            (c.getDescription() != null && c.getDescription().toLowerCase().contains(search.trim().toLowerCase())))
                    .toList();
        } else {
            courses = courseRepository.findAll().stream()
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
                course.getStatus().name(),
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

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new AccessDeniedException("Cannot enroll in a course that is not published");
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
     * FR-I07: Non-published courses (especially DRAFT) are visible only to associated instructors
     * and authorized admins. Students and anonymous users can never access draft courses.
     */
    private void verifyCourseVisibleToUser(Course course, User currentUser) {
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            return;
        }
        if (isAssociatedInstructorOrAdmin(course, currentUser)) {
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
        if (isAssociatedInstructorOrAdmin(course, currentUser)) {
            return true;
        }
        if (course.getStatus() == CourseStatus.DRAFT) {
            return false;
        }
        return enrollmentRepository.existsByStudentIdAndCourseId(currentUser.getId(), course.getId());
    }
}
