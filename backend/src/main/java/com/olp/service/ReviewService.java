package com.olp.service;

import com.olp.dto.CreateReviewRequest;
import com.olp.dto.ReviewResponse;
import com.olp.entity.Review;
import com.olp.entity.User;
import com.olp.repository.EnrollmentCheckRepository;
import com.olp.repository.ReviewRepository;
import com.olp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final EnrollmentCheckRepository enrollmentCheckRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         EnrollmentCheckRepository enrollmentCheckRepository,
                         UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.enrollmentCheckRepository = enrollmentCheckRepository;
        this.userRepository = userRepository;
    }

    /**
     * Submit a rating & review for a course.
     * Enforces:
     * 1. Student must be enrolled in the course.
     * 2. Student can only submit one review per course.
     */
    public ReviewResponse addReview(Integer courseId, Integer studentId, CreateReviewRequest request) {
        // 1. Verify student is enrolled
        if (!enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be enrolled in this course to leave a review.");
        }

        // 2. Check if already reviewed
        if (reviewRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already reviewed this course.");
        }

        // 3. Save new review
        Review review = new Review(
                studentId,
                courseId,
                request.getRating(),
                request.getComment()
        );
        review = reviewRepository.save(review);

        // 4. Retrieve student name
        String studentName = userRepository.findById(studentId)
                .map(User::getName)
                .orElse("Student");

        return new ReviewResponse(
                review.getId(),
                review.getCourseId(),
                review.getStudentId(),
                studentName,
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }

    /**
     * Get all reviews for a course (newest first) with student names.
     */
    public List<ReviewResponse> getReviewsForCourse(Integer courseId) {
        List<Review> reviews = reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId);

        // Fetch student names to attach to responses
        return reviews.stream().map(r -> {
            String studentName = userRepository.findById(r.getStudentId())
                    .map(User::getName)
                    .orElse("Student");
            return new ReviewResponse(
                    r.getId(),
                    r.getCourseId(),
                    r.getStudentId(),
                    studentName,
                    r.getRating(),
                    r.getComment(),
                    r.getCreatedAt()
            );
        }).collect(Collectors.toList());
    }

    /**
     * Get average rating for a course.
     */
    public Double getAverageRating(Integer courseId) {
        Double avg = reviewRepository.findAverageRatingByCourseId(courseId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }
}