package com.olp.service;

import com.olp.entity.LectureProgress;
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
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class PlaybackPositionServiceTest {

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
    void testUpdatePlaybackPosition_Success() {
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);
        when(lectureProgressRepository.findByStudentIdAndLectureId(studentId, lectureId))
                .thenReturn(Optional.empty());

        Map<String, Object> result = studentLearningService.updatePlaybackPosition(studentId, lectureId, 120);

        assertNotNull(result);
        assertEquals(120, result.get("playbackPositionSeconds"));
        assertEquals(lectureId, result.get("lectureId"));
        verify(lectureProgressRepository, times(1)).save(any(LectureProgress.class));
    }

    @Test
    void testUpdatePlaybackPosition_NotEnrolled_ThrowsForbidden() {
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                studentLearningService.updatePlaybackPosition(studentId, lectureId, 120));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(lectureProgressRepository, never()).save(any());
    }

    @Test
    void testGetPlaybackPosition_ReturnsStoredSeconds() {
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);
        LectureProgress progress = new LectureProgress(studentId, lectureId);
        progress.setPlaybackPositionSeconds(85);
        when(lectureProgressRepository.findByStudentIdAndLectureId(studentId, lectureId))
                .thenReturn(Optional.of(progress));

        Map<String, Object> result = studentLearningService.getPlaybackPosition(studentId, lectureId);

        assertNotNull(result);
        assertEquals(85, result.get("playbackPositionSeconds"));
    }

    @Test
    void testGetPlaybackPosition_NoHistory_ReturnsZero() {
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);
        when(lectureProgressRepository.findByStudentIdAndLectureId(studentId, lectureId))
                .thenReturn(Optional.empty());

        Map<String, Object> result = studentLearningService.getPlaybackPosition(studentId, lectureId);

        assertNotNull(result);
        assertEquals(0, result.get("playbackPositionSeconds"));
    }
}