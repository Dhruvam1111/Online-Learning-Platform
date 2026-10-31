package com.olp.service;

import com.olp.dto.CourseDetailResponse;
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

    public CourseCatalogService(CourseRepository courseRepository,
                                LectureRepository lectureRepository,
                                ReviewRepository reviewRepository,
                                BookmarkRepository bookmarkRepository,
                                EnrollmentRepository enrollmentRepository,
                                UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
        this.reviewRepository = reviewRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
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

            return new PublicCourseResponse(
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
        }).toList();
    }

    /**
     * Course details with lecture curriculum and review info.
     */
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(Integer courseId, User currentUser) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        Integer currentUserId = currentUser != null ? currentUser.getId() : null;

        List<Lecture> lectures = lectureRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        List<LectureResponse> lectureResponses = lectures.stream()
                .map(LectureResponse::fromEntity)
                .toList();

        Double avgRating = reviewRepository.findAverageRatingByCourseId(courseId);
        int totalReviews = reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId).size();
        boolean enrolled = currentUserId != null &&
                enrollmentRepository.existsByStudentIdAndCourseId(currentUserId, courseId);
        boolean bookmarked = currentUserId != null &&
                bookmarkRepository.existsByStudentIdAndCourseId(currentUserId, courseId);

        return new CourseDetailResponse(
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

        if (enrollmentRepository.existsByStudentIdAndCourseId(currentUser.getId(), courseId)) {
            return new EnrollmentResponse(true, "Already enrolled in this course", courseId, true);
        }

        Enrollment enrollment = new Enrollment(currentUser, course);
        enrollmentRepository.save(enrollment);

        return new EnrollmentResponse(true, "Successfully enrolled in " + course.getTitle(), courseId, true);
    }

    /**
     * Returns lectures for an enrolled student or course preview.
     */
    @Transactional(readOnly = true)
    public List<LectureResponse> getCourseLectures(Integer courseId, User currentUser) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        return lectureRepository.findByCourseIdOrderByOrderIndexAsc(course.getId()).stream()
                .map(LectureResponse::fromEntity)
                .toList();
    }
}
