package com.olp.controller;

import com.olp.dto.CreateReviewRequest;
import com.olp.dto.ReviewResponse;
import com.olp.entity.User;
import com.olp.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses/{courseId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * POST /api/courses/{courseId}/reviews
     * Only logged-in students can submit reviews.
     */
    @PostMapping
    @PreAuthorize("hasRole('student')")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Integer courseId,
            @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        ReviewResponse response = reviewService.addReview(courseId, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/courses/{courseId}/reviews
     * Public — anyone browsing can view the reviews and average rating.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getReviews(@PathVariable Integer courseId) {
        List<ReviewResponse> reviews = reviewService.getReviewsForCourse(courseId);
        Double averageRating = reviewService.getAverageRating(courseId);

        return ResponseEntity.ok(Map.of(
                "courseId", courseId,
                "averageRating", averageRating,
                "totalReviews", reviews.size(),
                "reviews", reviews
        ));
    }
}