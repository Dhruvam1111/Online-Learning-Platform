package com.olp.service;

import com.olp.dto.LectureBookmarkResponse;
import com.olp.entity.LectureBookmark;
import com.olp.repository.EnrollmentCheckRepository;
import com.olp.repository.LectureBookmarkRepository;
import com.olp.repository.LectureMaterialRepository;
import com.olp.repository.LectureProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LectureBookmarkServiceTest {

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
    private final Integer lectureId = 10;
    private final Integer courseId = 100;

    @BeforeEach
    void setUp() {
        lenient().when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(lectureId)))
                .thenReturn(List.of(courseId));
    }

    @Test
    void testToggleLectureBookmark_AddBookmark() {
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);
        when(lectureBookmarkRepository.existsByStudentIdAndLectureId(studentId, lectureId)).thenReturn(false);

        Map<String, Object> result = studentLearningService.toggleLectureBookmark(studentId, lectureId);

        assertNotNull(result);
        assertEquals(true, result.get("isBookmarked"));
        verify(lectureBookmarkRepository, times(1)).save(any(LectureBookmark.class));
    }

    @Test
    void testToggleLectureBookmark_RemoveBookmark() {
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);
        when(lectureBookmarkRepository.existsByStudentIdAndLectureId(studentId, lectureId)).thenReturn(true);

        Map<String, Object> result = studentLearningService.toggleLectureBookmark(studentId, lectureId);

        assertNotNull(result);
        assertEquals(false, result.get("isBookmarked"));
        verify(lectureBookmarkRepository, times(1)).deleteByStudentIdAndLectureId(studentId, lectureId);
    }

    @Test
    void testGetBookmarkedLectures_ReturnsList() {
        LectureBookmarkResponse response = new LectureBookmarkResponse(1L, 10L, "OOP Basics", 100L, "Java Core", LocalDateTime.now());
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(studentId)))
                .thenReturn(List.of(response));

        List<LectureBookmarkResponse> list = studentLearningService.getBookmarkedLectures(studentId);

        assertNotNull(list);
        assertEquals(1, list.size());
    }
}