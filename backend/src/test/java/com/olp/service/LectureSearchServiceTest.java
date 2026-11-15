package com.olp.service;

import com.olp.dto.LectureSearchResult;
import com.olp.repository.EnrollmentCheckRepository;
import com.olp.repository.LectureBookmarkRepository;
import com.olp.repository.LectureMaterialRepository;
import com.olp.repository.LectureProgressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LectureSearchServiceTest {

    @Mock
    private LectureProgressRepository lectureProgressRepository;
    @Mock
    private LectureMaterialRepository lectureMaterialRepository;
    @Mock
    private LectureBookmarkRepository lectureBookmarkRepository;
    @Mock
    private EnrollmentCheckRepository enrollmentCheckRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private StudentLearningService studentLearningService;

    private final Integer studentId = 1;
    private final Integer courseId = 100;

    @Test
    void testSearchCourseLectures_Enrolled_ReturnsMatchingLectures() {
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);
        LectureSearchResult item = new LectureSearchResult(10L, "Polymorphism", "http://vid", 1, 100L, "Java");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(courseId), eq("%poly%")))
                .thenReturn(List.of(item));

        List<LectureSearchResult> results = studentLearningService.searchCourseLectures(studentId, courseId, "poly");

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testSearchCourseLectures_NotEnrolled_ThrowsForbidden() {
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                studentLearningService.searchCourseLectures(studentId, courseId, "poly"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}