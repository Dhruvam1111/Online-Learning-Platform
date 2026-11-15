package com.olp.service;

import com.olp.dto.LectureMaterialResponse;
import com.olp.dto.LessonResponse;
import com.olp.dto.MaterialViewerResponse;
import com.olp.dto.ResourceAndMetadata;
import com.olp.entity.LectureBookmark;
import com.olp.entity.LectureMaterial;
import com.olp.entity.LectureProgress;
import com.olp.entity.Lesson;
import com.olp.repository.EnrollmentCheckRepository;
import com.olp.repository.LectureBookmarkRepository;
import com.olp.repository.LectureMaterialRepository;
import com.olp.repository.LectureProgressRepository;
import com.olp.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentLearningServiceTest {

    @Mock
    private LectureProgressRepository lectureProgressRepository;
    @Mock
    private LectureMaterialRepository lectureMaterialRepository;
    @Mock
    private LectureBookmarkRepository lectureBookmarkRepository;
    @Mock
    private EnrollmentCheckRepository enrollmentCheckRepository;
    @Mock
    private MaterialStorageService materialStorageService;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private LessonRepository lessonRepository;

    @InjectMocks
    private StudentLearningService studentLearningService;

    private final Integer studentId = 50;
    private final Integer courseId = 100;
    private final Integer lectureId = 10;
    private final Integer lessonId = 20;
    private final Integer moduleId = 30;

    private LectureMaterial downloadableMaterial;
    private LectureMaterial viewOnlyMaterial;

    @BeforeEach
    void setUp() {
        downloadableMaterial = new LectureMaterial(lessonId, null, null, 1, "Cheatsheet", "cheatsheet.pdf",
                "uploads/materials/cheatsheet.pdf", "pdf", "application/pdf", 2048L, true);
        downloadableMaterial.setId(101);

        viewOnlyMaterial = new LectureMaterial(lessonId, null, null, 1, "Lecture Notes", "notes.pdf",
                "uploads/materials/notes.pdf", "pdf", "application/pdf", 4096L, false);
        viewOnlyMaterial.setId(102);
    }

    private void mockLessonCourseLookup() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Integer>>any(), eq(lessonId)))
                .thenReturn(List.of(courseId));
    }

    @Test
    void testDownloadMaterial_Success_WhenDownloadableAndEnrolled() {
        mockLessonCourseLookup();
        when(lectureMaterialRepository.findById(101)).thenReturn(Optional.of(downloadableMaterial));
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);

        Resource mockResource = new ByteArrayResource("dummy content".getBytes(StandardCharsets.UTF_8));
        when(materialStorageService.loadMaterialAsResource("uploads/materials/cheatsheet.pdf")).thenReturn(mockResource);

        ResourceAndMetadata result = studentLearningService.downloadMaterial(studentId, 101);

        assertNotNull(result);
        assertEquals("cheatsheet.pdf", result.getFilename());
        assertEquals("application/pdf", result.getContentType());
        assertEquals(mockResource, result.getResource());
    }

    @Test
    void testDownloadMaterial_Fails_WhenViewOnly() {
        mockLessonCourseLookup();
        when(lectureMaterialRepository.findById(102)).thenReturn(Optional.of(viewOnlyMaterial));
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                studentLearningService.downloadMaterial(studentId, 102));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(Objects.requireNonNull(ex.getReason()).contains("view-only"));
        verify(materialStorageService, never()).loadMaterialAsResource(anyString());
    }

    @Test
    void testDownloadMaterial_Fails_WhenNotEnrolled() {
        mockLessonCourseLookup();
        when(lectureMaterialRepository.findById(101)).thenReturn(Optional.of(downloadableMaterial));
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                studentLearningService.downloadMaterial(studentId, 101));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(Objects.requireNonNull(ex.getReason()).contains("enrolled"));
        verify(materialStorageService, never()).loadMaterialAsResource(anyString());
    }

    @Test
    void testDownloadMaterial_Fails_NotFound() {
        when(lectureMaterialRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                studentLearningService.downloadMaterial(studentId, 999));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testViewMaterial_Success_EvenWhenViewOnly() {
        mockLessonCourseLookup();
        when(lectureMaterialRepository.findById(102)).thenReturn(Optional.of(viewOnlyMaterial));
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);

        Resource mockResource = new ByteArrayResource("notes content".getBytes(StandardCharsets.UTF_8));
        when(materialStorageService.loadMaterialAsResource("uploads/materials/notes.pdf")).thenReturn(mockResource);

        ResourceAndMetadata result = studentLearningService.viewMaterial(studentId, 102);

        assertNotNull(result);
        assertEquals("notes.pdf", result.getFilename());
        assertEquals("application/pdf", result.getContentType());
        assertEquals(mockResource, result.getResource());
    }

    @Test
    void testViewMaterial_Fails_WhenNotEnrolled() {
        mockLessonCourseLookup();
        when(lectureMaterialRepository.findById(102)).thenReturn(Optional.of(viewOnlyMaterial));
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                studentLearningService.viewMaterial(studentId, 102));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(materialStorageService, never()).loadMaterialAsResource(anyString());
    }

    @Test
    void testGetMaterialViewerInfo_ViewOnly_HidesDownloadUrl() {
        mockLessonCourseLookup();
        when(lectureMaterialRepository.findById(102)).thenReturn(Optional.of(viewOnlyMaterial));
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);

        MaterialViewerResponse viewer = studentLearningService.getMaterialViewerInfo(studentId, 102);

        assertNotNull(viewer);
        assertEquals(102, viewer.getId());
        assertFalse(viewer.getIsDownloadable());
        assertTrue(viewer.getViewOnly());
        assertNull(viewer.getDownloadUrl(), "Viewer response MUST NOT expose direct download URL for view-only material");
        assertEquals("/api/student/learning/materials/102/view", viewer.getViewUrl());
    }

    @Test
    void testGetMaterialViewerInfo_Downloadable_ExposesDownloadUrl() {
        mockLessonCourseLookup();
        when(lectureMaterialRepository.findById(101)).thenReturn(Optional.of(downloadableMaterial));
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);

        MaterialViewerResponse viewer = studentLearningService.getMaterialViewerInfo(studentId, 101);

        assertNotNull(viewer);
        assertEquals(101, viewer.getId());
        assertTrue(viewer.getIsDownloadable());
        assertFalse(viewer.getViewOnly());
        assertEquals("/api/student/learning/materials/101/download", viewer.getDownloadUrl());
    }

    @Test
    void testGetLectureMaterials_MasksDownloadUrl_WhenViewOnly() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Integer>>any(), eq(lectureId)))
                .thenReturn(List.of(courseId));
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);

        LectureMaterial mat = new LectureMaterial(lectureId, "Slide Deck", "slide.pdf", "pdf", false);
        mat.setId(201);
        when(lectureMaterialRepository.findByLectureId(lectureId)).thenReturn(List.of(mat));

        List<LectureMaterialResponse> list = studentLearningService.getLectureMaterials(studentId, lectureId);

        assertNotNull(list);
        assertEquals(1, list.size());
        LectureMaterialResponse resp = list.get(0);
        assertEquals(201L, resp.getId());
        assertFalse(resp.getIsDownloadable());
        assertTrue(resp.getViewOnly());
        assertNull(resp.getDownloadUrl(), "Download URL should be null for view-only materials");
        assertEquals("/api/student/learning/materials/201/view", resp.getViewUrl());
    }

    @Test
    void testGetLessonMaterials() {
        mockLessonCourseLookup();
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);
        when(lectureMaterialRepository.findByLessonId(lessonId)).thenReturn(List.of(downloadableMaterial, viewOnlyMaterial));

        List<LectureMaterialResponse> list = studentLearningService.getLessonMaterials(studentId, lessonId);

        assertEquals(2, list.size());
        assertTrue(list.get(0).getIsDownloadable());
        assertNotNull(list.get(0).getDownloadUrl());
        assertFalse(list.get(1).getIsDownloadable());
        assertNull(list.get(1).getDownloadUrl());
    }

    @Test
    void testGetModuleMaterials() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Integer>>any(), eq(moduleId)))
                .thenReturn(List.of(courseId));
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);

        LectureMaterial modMaterial = new LectureMaterial(null, moduleId, null, 1, "Syllabus", "syllabus.pdf",
                "uploads/materials/syllabus.pdf", "pdf", "application/pdf", 1024L, true);
        modMaterial.setId(301);
        when(lectureMaterialRepository.findByModuleId(moduleId)).thenReturn(List.of(modMaterial));

        List<LectureMaterialResponse> list = studentLearningService.getModuleMaterials(studentId, moduleId);

        assertEquals(1, list.size());
        assertEquals(301L, list.get(0).getId());
        assertTrue(list.get(0).getIsDownloadable());
    }

    @Test
    void testPlaybackPosition() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Integer>>any(), eq(lectureId)))
                .thenReturn(List.of(courseId));
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);

        LectureProgress progress = new LectureProgress(studentId, lectureId);
        progress.setPlaybackPositionSeconds(120);
        when(lectureProgressRepository.findByStudentIdAndLectureId(studentId, lectureId))
                .thenReturn(Optional.of(progress));

        Map<String, Object> getResult = studentLearningService.getPlaybackPosition(studentId, lectureId);
        assertEquals(120, getResult.get("playbackPositionSeconds"));

        Map<String, Object> updateResult = studentLearningService.updatePlaybackPosition(studentId, lectureId, 180);
        assertEquals(180, updateResult.get("playbackPositionSeconds"));
        verify(lectureProgressRepository, times(1)).save(any(LectureProgress.class));
    }

    @Test
    void testToggleBookmark() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Integer>>any(), eq(lectureId)))
                .thenReturn(List.of(courseId));
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);

        when(lectureBookmarkRepository.existsByStudentIdAndLectureId(studentId, lectureId)).thenReturn(false);

        Map<String, Object> addResult = studentLearningService.toggleLectureBookmark(studentId, lectureId);
        assertEquals(true, addResult.get("isBookmarked"));
        verify(lectureBookmarkRepository, times(1)).save(any(LectureBookmark.class));

        when(lectureBookmarkRepository.existsByStudentIdAndLectureId(studentId, lectureId)).thenReturn(true);
        Map<String, Object> removeResult = studentLearningService.toggleLectureBookmark(studentId, lectureId);
        assertEquals(false, removeResult.get("isBookmarked"));
        verify(lectureBookmarkRepository, times(1)).deleteByStudentIdAndLectureId(studentId, lectureId);
    }

    @Test
    void testGetLessonMaterials_excludesUnreleasedMaterials() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Integer>>any(), eq(lessonId)))
                .thenReturn(List.of(courseId));
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);

        Lesson parentLesson = new Lesson();
        parentLesson.setId(lessonId);
        parentLesson.setIsReleased(true);
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(parentLesson));

        LectureMaterial releasedMat = new LectureMaterial(lessonId, null, null, 1, "Released Notes", "rel.pdf",
                "uploads/rel.pdf", "pdf", "application/pdf", 1024L, true);
        releasedMat.setId(201);
        releasedMat.setIsReleased(true);

        LectureMaterial unreleasedMat = new LectureMaterial(lessonId, null, null, 2, "Unreleased Draft", "unrel.pdf",
                "uploads/unrel.pdf", "pdf", "application/pdf", 1024L, true);
        unreleasedMat.setId(202);
        unreleasedMat.setIsReleased(false);
        unreleasedMat.setReleaseAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusDays(5));

        when(lectureMaterialRepository.findByLessonId(lessonId)).thenReturn(List.of(releasedMat, unreleasedMat));

        List<LectureMaterialResponse> results = studentLearningService.getLessonMaterials(studentId, lessonId);

        assertEquals(1, results.size());
        assertEquals(201L, results.get(0).getId());
    }

    @Test
    void testDownloadMaterial_unreleasedMaterial_throwsForbidden() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Integer>>any(), eq(lessonId)))
                .thenReturn(List.of(courseId));
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);

        LectureMaterial unreleasedMat = new LectureMaterial(lessonId, null, null, 1, "Unreleased Draft", "unrel.pdf",
                "uploads/unrel.pdf", "pdf", "application/pdf", 1024L, true);
        unreleasedMat.setId(202);
        unreleasedMat.setIsReleased(false);
        unreleasedMat.setReleaseAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusDays(5));

        when(lectureMaterialRepository.findById(202)).thenReturn(Optional.of(unreleasedMat));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                studentLearningService.downloadMaterial(studentId, 202));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("scheduled for release"));
    }

    @Test
    void testViewMaterial_unreleasedParentLesson_throwsForbidden() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Integer>>any(), eq(lessonId)))
                .thenReturn(List.of(courseId));
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);

        LectureMaterial mat = new LectureMaterial(lessonId, null, null, 1, "Released Material", "mat.pdf",
                "uploads/mat.pdf", "pdf", "application/pdf", 1024L, true);
        mat.setId(203);
        mat.setIsReleased(true);

        Lesson unreleasedLesson = new Lesson();
        unreleasedLesson.setId(lessonId);
        unreleasedLesson.setIsReleased(false);
        unreleasedLesson.setReleaseAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusDays(2));

        when(lectureMaterialRepository.findById(203)).thenReturn(Optional.of(mat));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(unreleasedLesson));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                studentLearningService.viewMaterial(studentId, 203));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("parent lesson is scheduled for release"));
    }

    @Test
    void testGetLessonForStudent_releasedAndUnreleased() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Integer>>any(), eq(lessonId)))
                .thenReturn(List.of(courseId));
        when(enrollmentCheckRepository.isStudentEnrolled(studentId, courseId)).thenReturn(true);

        Lesson releasedLesson = new Lesson();
        releasedLesson.setId(lessonId);
        releasedLesson.setTitle("Introduction");
        releasedLesson.setIsReleased(true);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(releasedLesson));

        LessonResponse resp = studentLearningService.getLessonForStudent(studentId, lessonId);
        assertNotNull(resp);
        assertEquals(lessonId, resp.getId());
        assertEquals("Introduction", resp.getTitle());

        // Test unreleased
        Lesson unreleased = new Lesson();
        unreleased.setId(lessonId);
        unreleased.setIsReleased(false);
        unreleased.setReleaseAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusHours(10));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(unreleased));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                studentLearningService.getLessonForStudent(studentId, lessonId));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("scheduled for release"));
    }
}
