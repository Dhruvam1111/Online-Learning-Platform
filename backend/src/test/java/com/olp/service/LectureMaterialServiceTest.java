package com.olp.service;

import com.olp.dto.LectureMaterialResponse;
import com.olp.entity.LectureMaterial;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LectureMaterialServiceTest {

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
    void testGetLectureMaterials_Enrolled_ReturnsMaterialsList() {
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);

        LectureMaterial mat = new LectureMaterial();
        mat.setId(1);
        mat.setLectureId(lectureId);
        mat.setTitle("Course Notes PDF");
        mat.setFileUrl("https://cdn.example.com/notes.pdf");
        mat.setFileType("PDF");
        mat.setIsDownloadable(true);

        when(lectureMaterialRepository.findByLectureId(lectureId)).thenReturn(List.of(mat));

        List<LectureMaterialResponse> list = studentLearningService.getLectureMaterials(studentId, lectureId);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Course Notes PDF", list.get(0).getTitle());
    }

    @Test
    void testGetLectureMaterials_NotEnrolled_ThrowsForbidden() {
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                studentLearningService.getLectureMaterials(studentId, lectureId));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}