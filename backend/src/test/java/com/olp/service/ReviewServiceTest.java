package com.olp.service;

import com.olp.dto.CreateReviewRequest;
import com.olp.dto.ReviewResponse;
import com.olp.entity.Review;
import com.olp.entity.User;
import com.olp.repository.EnrollmentCheckRepository;
import com.olp.repository.ReviewRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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

    private static final Integer COURSE = 1;
    private static final Integer STUDENT = 5;

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private EnrollmentCheckRepository enrollmentCheckRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService service;

    private User student(Integer id, String name) {
        User user = new User(name, name.toLowerCase() + "@olp.com", "hash", "student", "active");
        user.setId(id);
        return user;
    }

    private Review review(Integer id, Integer studentId, int rating, String comment) {
        Review r = new Review(studentId, COURSE, rating, comment);
        r.setId(id);
        r.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return r;
    }

    // ===== addReview =====

    @Test
    void testAddReview_NotEnrolled_ThrowsForbidden() {
        when(enrollmentCheckRepository.isStudentEnrolled(STUDENT, COURSE)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.addReview(COURSE, STUDENT, new CreateReviewRequest(5, "Great")));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("enrolled"));
        verifyNoInteractions(reviewRepository, userRepository);
    }

    @Test
    void testAddReview_AlreadyReviewed_ThrowsConflict() {
        when(enrollmentCheckRepository.isStudentEnrolled(STUDENT, COURSE)).thenReturn(true);
        when(reviewRepository.existsByCourseIdAndStudentId(COURSE, STUDENT)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.addReview(COURSE, STUDENT, new CreateReviewRequest(4, "Again")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("already reviewed"));
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void testAddReview_Success_SavesReviewAndReturnsResponseWithStudentName() {
        when(enrollmentCheckRepository.isStudentEnrolled(STUDENT, COURSE)).thenReturn(true);
        when(reviewRepository.existsByCourseIdAndStudentId(COURSE, STUDENT)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            saved.setId(77);
            return saved;
        });
        when(userRepository.findById(STUDENT)).thenReturn(Optional.of(student(STUDENT, "Sam")));

        ReviewResponse response = service.addReview(COURSE, STUDENT, new CreateReviewRequest(5, "Excellent"));

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertEquals(STUDENT, captor.getValue().getStudentId());
        assertEquals(COURSE, captor.getValue().getCourseId());
        assertEquals(5, captor.getValue().getRating());
        assertEquals("Excellent", captor.getValue().getComment());

        assertEquals(77, response.getId());
        assertEquals(COURSE, response.getCourseId());
        assertEquals(STUDENT, response.getStudentId());
        assertEquals("Sam", response.getStudentName());
        assertEquals(5, response.getRating());
        assertEquals("Excellent", response.getComment());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void testAddReview_StudentRecordMissing_FallsBackToGenericName() {
        when(enrollmentCheckRepository.isStudentEnrolled(STUDENT, COURSE)).thenReturn(true);
        when(reviewRepository.existsByCourseIdAndStudentId(COURSE, STUDENT)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(STUDENT)).thenReturn(Optional.empty());

        ReviewResponse response = service.addReview(COURSE, STUDENT, new CreateReviewRequest(3, null));

        assertEquals("Student", response.getStudentName());
        assertNull(response.getComment());
    }

    @Test
    void testAddReview_ServiceDoesNotValidateRatingRange() {
        when(enrollmentCheckRepository.isStudentEnrolled(STUDENT, COURSE)).thenReturn(true);
        when(reviewRepository.existsByCourseIdAndStudentId(COURSE, STUDENT)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(STUDENT)).thenReturn(Optional.of(student(STUDENT, "Sam")));

        ReviewResponse response = service.addReview(COURSE, STUDENT, new CreateReviewRequest(9, "out of range"));

        assertEquals(9, response.getRating());
    }

    // ===== getReviewsForCourse =====

    @Test
    void testGetReviewsForCourse_ReturnsResponsesInRepositoryOrderWithNames() {
        when(reviewRepository.findByCourseIdOrderByCreatedAtDesc(COURSE))
                .thenReturn(List.of(review(2, 6, 4, "Good"), review(1, 5, 5, "Great")));
        when(userRepository.findById(6)).thenReturn(Optional.of(student(6, "Ana")));
        when(userRepository.findById(5)).thenReturn(Optional.of(student(5, "Sam")));

        List<ReviewResponse> result = service.getReviewsForCourse(COURSE);

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getId());
        assertEquals("Ana", result.get(0).getStudentName());
        assertEquals(4, result.get(0).getRating());
        assertEquals(1, result.get(1).getId());
        assertEquals("Sam", result.get(1).getStudentName());
        assertEquals("Great", result.get(1).getComment());
        assertEquals(LocalDateTime.of(2026, 1, 1, 12, 0), result.get(1).getCreatedAt());
    }

    @Test
    void testGetReviewsForCourse_UnknownStudent_UsesFallbackName() {
        when(reviewRepository.findByCourseIdOrderByCreatedAtDesc(COURSE)).thenReturn(List.of(review(1, 99, 3, "ok")));
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        List<ReviewResponse> result = service.getReviewsForCourse(COURSE);

        assertEquals("Student", result.get(0).getStudentName());
    }

    @Test
    void testGetReviewsForCourse_NoReviews_ReturnsEmptyList() {
        when(reviewRepository.findByCourseIdOrderByCreatedAtDesc(COURSE)).thenReturn(List.of());

        assertTrue(service.getReviewsForCourse(COURSE).isEmpty());
        verifyNoInteractions(userRepository);
    }

    // ===== getAverageRating =====

    @Test
    void testGetAverageRating_NoReviews_ReturnsZero() {
        when(reviewRepository.findAverageRatingByCourseId(COURSE)).thenReturn(null);

        assertEquals(0.0, service.getAverageRating(COURSE), 0.0001);
    }

    @Test
    void testGetAverageRating_RoundsToOneDecimalPlace() {
        when(reviewRepository.findAverageRatingByCourseId(COURSE)).thenReturn(4.333);

        assertEquals(4.3, service.getAverageRating(COURSE), 0.0001);
    }

    @Test
    void testGetAverageRating_RoundsHalfUp() {
        when(reviewRepository.findAverageRatingByCourseId(COURSE)).thenReturn(4.25);

        assertEquals(4.3, service.getAverageRating(COURSE), 0.0001);
    }

    @Test
    void testGetAverageRating_WholeNumberIsUnchanged() {
        when(reviewRepository.findAverageRatingByCourseId(COURSE)).thenReturn(3.0);

        assertEquals(3.0, service.getAverageRating(COURSE), 0.0001);
    }
}
