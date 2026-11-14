package com.olp.controller;

import com.olp.dto.CreateReviewRequest;
import com.olp.dto.ReviewResponse;
import com.olp.entity.User;
import com.olp.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        User student = new User("Sam Student", "sam@example.com", "hash", "student");
        student.setId(7);
        authentication = new UsernamePasswordAuthenticationToken(student, null);
    }

    @Test
    void testCreateReview_Returns201WithSavedReview() {
        CreateReviewRequest request = new CreateReviewRequest(5, "Great course");
        ReviewResponse saved = new ReviewResponse(1, 3, 7, "Sam Student", 5, "Great course", LocalDateTime.now());
        when(reviewService.addReview(3, 7, request)).thenReturn(saved);

        ResponseEntity<ReviewResponse> response = reviewController.createReview(3, request, authentication);

        assertEquals(201, response.getStatusCode().value());
        assertSame(saved, response.getBody());
        verify(reviewService, times(1)).addReview(3, 7, request);
    }

    @Test
    void testCreateReview_DuplicateReviewPropagatesConflict() {
        CreateReviewRequest request = new CreateReviewRequest(4, "Again");
        when(reviewService.addReview(3, 7, request))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "You have already reviewed this course."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewController.createReview(3, request, authentication));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void testGetReviews_ReturnsReviewsAverageAndCount() {
        ReviewResponse r1 = new ReviewResponse(1, 3, 7, "Sam", 5, "Great", LocalDateTime.now());
        ReviewResponse r2 = new ReviewResponse(2, 3, 8, "Alex", 4, "Good", LocalDateTime.now());
        when(reviewService.getReviewsForCourse(3)).thenReturn(List.of(r1, r2));
        when(reviewService.getAverageRating(3)).thenReturn(4.5);

        ResponseEntity<Map<String, Object>> response = reviewController.getReviews(3);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(3, response.getBody().get("courseId"));
        assertEquals(4.5, response.getBody().get("averageRating"));
        assertEquals(2, response.getBody().get("totalReviews"));
        assertEquals(List.of(r1, r2), response.getBody().get("reviews"));
    }

    @Test
    void testGetReviews_NoReviewsReturnsZeroCountAndRating() {
        when(reviewService.getReviewsForCourse(9)).thenReturn(List.of());
        when(reviewService.getAverageRating(9)).thenReturn(0.0);

        ResponseEntity<Map<String, Object>> response = reviewController.getReviews(9);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(0, response.getBody().get("totalReviews"));
        assertEquals(0.0, response.getBody().get("averageRating"));
        assertTrue(((List<?>) response.getBody().get("reviews")).isEmpty());
    }

    @Test
    void testGetReviews_NullAverageRatingFailsBecauseMapOfRejectsNulls() {
        when(reviewService.getReviewsForCourse(9)).thenReturn(List.of());
        when(reviewService.getAverageRating(9)).thenReturn(null);

        assertThrows(NullPointerException.class, () -> reviewController.getReviews(9));
    }
}
