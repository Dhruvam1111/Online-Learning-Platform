package com.olp.service;

import com.olp.dto.CourseProgressResponse;
import com.olp.entity.LectureProgress;
import com.olp.repository.EnrollmentCheckRepository;
import com.olp.repository.LectureProgressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseProgressServiceTest {

    private static final Integer STUDENT = 5;
    private static final Integer COURSE = 1;
    private static final Integer LECTURE = 100;

    @Mock
    private LectureProgressRepository progressRepository;
    @Mock
    private EnrollmentCheckRepository enrollmentCheckRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CourseProgressService service;

    private void stubLectureBelongsToCourse(Integer count) {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(LECTURE), eq(COURSE))).thenReturn(count);
    }

    private void stubTotalLectures(Integer total) {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(COURSE))).thenReturn(total);
    }

    private void stubCompletedIds(List<Integer> ids) {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Integer>>any(), eq(STUDENT), eq(COURSE)))
                .thenReturn(ids);
    }

    // ===== markLectureComplete =====

    @Test
    void testMarkLectureComplete_NotEnrolled_ThrowsForbidden() {
        when(enrollmentCheckRepository.isStudentEnrolled(STUDENT, COURSE)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.markLectureComplete(STUDENT, COURSE, LECTURE));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("enrolled"));
        verifyNoInteractions(jdbcTemplate, progressRepository);
    }

    @Test
    void testMarkLectureComplete_LectureNotInCourse_ThrowsNotFound() {
        when(enrollmentCheckRepository.isStudentEnrolled(STUDENT, COURSE)).thenReturn(true);
        stubLectureBelongsToCourse(0);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.markLectureComplete(STUDENT, COURSE, LECTURE));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Lecture not found"));
        verifyNoInteractions(progressRepository);
    }

    @Test
    void testMarkLectureComplete_NullLectureCount_ThrowsNotFound() {
        when(enrollmentCheckRepository.isStudentEnrolled(STUDENT, COURSE)).thenReturn(true);
        stubLectureBelongsToCourse(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.markLectureComplete(STUDENT, COURSE, LECTURE));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testMarkLectureComplete_FirstTime_SavesProgress() {
        when(enrollmentCheckRepository.isStudentEnrolled(STUDENT, COURSE)).thenReturn(true);
        stubLectureBelongsToCourse(1);
        when(progressRepository.existsByStudentIdAndLectureId(STUDENT, LECTURE)).thenReturn(false);

        service.markLectureComplete(STUDENT, COURSE, LECTURE);

        ArgumentCaptor<LectureProgress> saved = ArgumentCaptor.forClass(LectureProgress.class);
        verify(progressRepository, times(1)).save(saved.capture());
        assertEquals(STUDENT, saved.getValue().getStudentId());
        assertEquals(LECTURE, saved.getValue().getLectureId());
        assertNotNull(saved.getValue().getCompletedAt());
    }

    @Test
    void testMarkLectureComplete_AlreadyCompleted_IsIdempotentAndDoesNotSave() {
        when(enrollmentCheckRepository.isStudentEnrolled(STUDENT, COURSE)).thenReturn(true);
        stubLectureBelongsToCourse(1);
        when(progressRepository.existsByStudentIdAndLectureId(STUDENT, LECTURE)).thenReturn(true);

        service.markLectureComplete(STUDENT, COURSE, LECTURE);

        verify(progressRepository, never()).save(any(LectureProgress.class));
    }

    // ===== unmarkLectureComplete =====

    @Test
    void testUnmarkLectureComplete_NotEnrolled_ThrowsForbidden() {
        when(enrollmentCheckRepository.isStudentEnrolled(STUDENT, COURSE)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.unmarkLectureComplete(STUDENT, COURSE, LECTURE));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(progressRepository);
    }

    @Test
    void testUnmarkLectureComplete_Enrolled_DeletesProgressWithoutLectureLookup() {
        when(enrollmentCheckRepository.isStudentEnrolled(STUDENT, COURSE)).thenReturn(true);

        service.unmarkLectureComplete(STUDENT, COURSE, LECTURE);

        verify(progressRepository, times(1)).deleteByStudentIdAndLectureId(STUDENT, LECTURE);
        verifyNoInteractions(jdbcTemplate);
    }

    // ===== getCourseProgress =====

    @Test
    void testGetCourseProgress_OneOfThree_RoundsToThirtyThreePointThree() {
        stubTotalLectures(3);
        stubCompletedIds(List.of(10));

        CourseProgressResponse response = service.getCourseProgress(STUDENT, COURSE);

        assertEquals(COURSE, response.getCourseId());
        assertEquals(1, response.getCompletedCount());
        assertEquals(3, response.getTotalCount());
        assertEquals(33.3, response.getPercentage(), 0.0001);
        assertEquals(List.of(10), response.getCompletedLectureIds());
    }

    @Test
    void testGetCourseProgress_TwoOfThree_RoundsToSixtySixPointSeven() {
        stubTotalLectures(3);
        stubCompletedIds(List.of(10, 11));

        assertEquals(66.7, service.getCourseProgress(STUDENT, COURSE).getPercentage(), 0.0001);
    }

    @Test
    void testGetCourseProgress_AllComplete_IsOneHundredPercent() {
        stubTotalLectures(3);
        stubCompletedIds(List.of(10, 11, 12));

        assertEquals(100.0, service.getCourseProgress(STUDENT, COURSE).getPercentage(), 0.0001);
    }

    @Test
    void testGetCourseProgress_NoneComplete_IsZeroPercent() {
        stubTotalLectures(5);
        stubCompletedIds(List.of());

        CourseProgressResponse response = service.getCourseProgress(STUDENT, COURSE);

        assertEquals(0.0, response.getPercentage(), 0.0001);
        assertEquals(0, response.getCompletedCount());
        assertTrue(response.getCompletedLectureIds().isEmpty());
    }

    @Test
    void testGetCourseProgress_CourseWithNoLectures_IsZeroPercentWithoutDivisionByZero() {
        stubTotalLectures(0);
        stubCompletedIds(List.of());

        CourseProgressResponse response = service.getCourseProgress(STUDENT, COURSE);

        assertEquals(0, response.getTotalCount());
        assertEquals(0.0, response.getPercentage(), 0.0001);
    }

    @Test
    void testGetCourseProgress_NullTotalIsTreatedAsZero() {
        stubTotalLectures(null);
        stubCompletedIds(List.of());

        CourseProgressResponse response = service.getCourseProgress(STUDENT, COURSE);

        assertEquals(0, response.getTotalCount());
        assertEquals(0.0, response.getPercentage(), 0.0001);
    }

    @Test
    void testGetCourseProgress_RowMapperReadsLectureIdColumn() throws Exception {
        stubTotalLectures(4);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("lecture_id")).thenReturn(11, 12);
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Integer>>any(), eq(STUDENT), eq(COURSE)))
                .thenAnswer(invocation -> {
                    RowMapper<Integer> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0), mapper.mapRow(rs, 1));
                });

        CourseProgressResponse response = service.getCourseProgress(STUDENT, COURSE);

        assertEquals(List.of(11, 12), response.getCompletedLectureIds());
        assertEquals(2, response.getCompletedCount());
        assertEquals(50.0, response.getPercentage(), 0.0001);
    }

    @Test
    void testGetCourseProgress_DoesNotCheckEnrollment() {
        stubTotalLectures(1);
        stubCompletedIds(List.of());

        service.getCourseProgress(STUDENT, COURSE);

        verifyNoInteractions(enrollmentCheckRepository);
    }
}
