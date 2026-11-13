package com.olp.service;

import com.olp.dto.CreateReviewRequest;
import com.olp.dto.ReviewResponse;
import com.olp.entity.Review;
import com.olp.entity.User;
import com.olp.repository.EnrollmentCheckRepository;
import com.olp.repository.ReviewRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private EnrollmentCheckRepository enrollmentCheckRepository;

    @Mock
    private UserRepository userRepository;

    private ReviewService reviewService;

    private User student;
    private CreateReviewRequest request;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, enrollmentCheckRepository, userRepository);

        student = new User("John Doe", "john@olp.com", "hash", "student", "active");
        student.setId(101);

        request = new CreateReviewRequest();
        request.setRating(5);
        request.setComment("Great course!");
    }

    @Test
    void testAddReview_Success() {
        when(enrollmentCheckRepository.isStudentEnrolled(101, 1)).thenReturn(true);
        when(reviewRepository.existsByCourseIdAndStudentId(1, 101)).thenReturn(false);

        Review savedReview = new Review(101, 1, 5, "Great course!");
        savedReview.setId(10);
        savedReview.setCreatedAt(LocalDateTime.now());
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(userRepository.findById(101)).thenReturn(Optional.of(student));

        ReviewResponse response = reviewService.addReview(1, 101, request);

        assertNotNull(response);
        assertEquals(10, response.getId());
        assertEquals(1, response.getCourseId());
        assertEquals(101, response.getStudentId());
        assertEquals("John Doe", response.getStudentName());
        assertEquals(5, response.getRating());
        assertEquals("Great course!", response.getComment());
    }

    @Test
    void testAddReview_UserNotFound_DefaultsToStudent() {
        when(enrollmentCheckRepository.isStudentEnrolled(101, 1)).thenReturn(true);
        when(reviewRepository.existsByCourseIdAndStudentId(1, 101)).thenReturn(false);

        Review savedReview = new Review(101, 1, 5, "Great course!");
        savedReview.setId(10);
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(userRepository.findById(101)).thenReturn(Optional.empty());

        ReviewResponse response = reviewService.addReview(1, 101, request);

        assertNotNull(response);
        assertEquals("Student", response.getStudentName());
    }

    @Test
    void testAddReview_NotEnrolled_ThrowsForbidden() {
        when(enrollmentCheckRepository.isStudentEnrolled(101, 1)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewService.addReview(1, 101, request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testAddReview_AlreadyReviewed_ThrowsConflict() {
        when(enrollmentCheckRepository.isStudentEnrolled(101, 1)).thenReturn(true);
        when(reviewRepository.existsByCourseIdAndStudentId(1, 101)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewService.addReview(1, 101, request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void testAddReview_NullArguments_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> reviewService.addReview(null, 101, request));
        assertThrows(IllegalArgumentException.class, () -> reviewService.addReview(1, null, request));
        assertThrows(IllegalArgumentException.class, () -> reviewService.addReview(1, 101, null));
    }

    @Test
    void testGetReviewsForCourse_Success() {
        Review r1 = new Review(101, 1, 5, "Nice");
        r1.setId(1);
        r1.setCreatedAt(LocalDateTime.now());

        Review r2 = new Review(null, 1, 4, "Good");
        r2.setId(2);
        r2.setCreatedAt(LocalDateTime.now());

        when(reviewRepository.findByCourseIdOrderByCreatedAtDesc(1)).thenReturn(List.of(r1, r2));
        when(userRepository.findById(101)).thenReturn(Optional.of(student));

        List<ReviewResponse> list = reviewService.getReviewsForCourse(1);

        assertEquals(2, list.size());
        assertEquals("John Doe", list.get(0).getStudentName());
        assertEquals("Student", list.get(1).getStudentName());
    }

    @Test
    void testGetReviewsForCourse_NullCourseId_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> reviewService.getReviewsForCourse(null));
    }

    @Test
    void testGetAverageRating_Success() {
        when(reviewRepository.findAverageRatingByCourseId(1)).thenReturn(4.567);
        Double avg = reviewService.getAverageRating(1);
        assertEquals(4.6, avg);

        when(reviewRepository.findAverageRatingByCourseId(2)).thenReturn(null);
        Double zeroAvg = reviewService.getAverageRating(2);
        assertEquals(0.0, zeroAvg);
    }

    @Test
    void testGetAverageRating_NullCourseId_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> reviewService.getAverageRating(null));
    }
}

