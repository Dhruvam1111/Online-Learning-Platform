package com.olp.controller;

import com.olp.dto.CourseProgressResponse;
import com.olp.entity.User;
import com.olp.service.CourseProgressService;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseProgressControllerTest {

    @Mock
    private CourseProgressService progressService;

    @InjectMocks
    private CourseProgressController courseProgressController;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        User student = new User("Sam Student", "sam@example.com", "hash", "student");
        student.setId(7);
        authentication = new UsernamePasswordAuthenticationToken(student, null);
    }

    @Test
    void testCompleteLecture_ReturnsCompletedTrue() {
        ResponseEntity<Map<String, Object>> response = courseProgressController.completeLecture(3, 21, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Lecture marked as completed", response.getBody().get("message"));
        assertEquals(21, response.getBody().get("lectureId"));
        assertEquals(true, response.getBody().get("completed"));
        verify(progressService, times(1)).markLectureComplete(7, 3, 21);
    }

    @Test
    void testCompleteLecture_NotEnrolledPropagatesForbidden() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be enrolled in this course to track progress."))
                .when(progressService).markLectureComplete(7, 3, 21);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> courseProgressController.completeLecture(3, 21, authentication));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testUncompleteLecture_ReturnsCompletedFalse() {
        ResponseEntity<Map<String, Object>> response = courseProgressController.uncompleteLecture(3, 21, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Lecture unmarked as completed", response.getBody().get("message"));
        assertEquals(21, response.getBody().get("lectureId"));
        assertEquals(false, response.getBody().get("completed"));
        verify(progressService, times(1)).unmarkLectureComplete(7, 3, 21);
    }

    @Test
    void testGetProgress_ReturnsProgressForStudentAndCourse() {
        CourseProgressResponse progress = new CourseProgressResponse(3, 2, 4, 50.0, List.of(21, 22));
        when(progressService.getCourseProgress(7, 3)).thenReturn(progress);

        ResponseEntity<CourseProgressResponse> response = courseProgressController.getProgress(3, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, response.getBody().getCompletedCount());
        assertEquals(4, response.getBody().getTotalCount());
        assertEquals(50.0, response.getBody().getPercentage(), 0.0001);
        assertEquals(List.of(21, 22), response.getBody().getCompletedLectureIds());
    }

    @Test
    void testGetProgress_NonUserPrincipalThrowsClassCastException() {
        Authentication wrong = new UsernamePasswordAuthenticationToken("someone", null);

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> courseProgressController.getProgress(3, wrong));
        verifyNoInteractions(progressService);
    }
}
