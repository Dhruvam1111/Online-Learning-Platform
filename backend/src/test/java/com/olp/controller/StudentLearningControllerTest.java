package com.olp.controller;

import com.olp.dto.LectureBookmarkResponse;
import com.olp.dto.LectureMaterialResponse;
import com.olp.dto.LectureSearchResult;
import com.olp.dto.PlaybackPositionRequest;
import com.olp.entity.User;
import com.olp.service.StudentLearningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentLearningControllerTest {

    @Mock
    private StudentLearningService studentLearningService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private StudentLearningController studentLearningController;

    private User studentUser;

    @BeforeEach
    void setUp() {
        studentUser = new User();
        studentUser.setId(1);
        studentUser.setEmail("student@test.com");
        studentUser.setRole("student");
        when(authentication.getPrincipal()).thenReturn(studentUser);
    }

    @Test
    void testUpdatePlaybackPosition() {
        PlaybackPositionRequest req = new PlaybackPositionRequest();
        req.setPlaybackPositionSeconds(65);

        when(studentLearningService.updatePlaybackPosition(1, 10, 65))
                .thenReturn(Map.of("message", "Playback position updated"));

        ResponseEntity<?> res = studentLearningController.updatePlaybackPosition(10, req, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertTrue(Objects.requireNonNull(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("attachment; filename=\"slides.pdf\""));
        assertEquals(resource, response.getBody());
        verify(studentLearningService, times(1)).downloadMaterial(50, 101);
        assertEquals(200, res.getStatusCode().value());
        verify(studentLearningService, times(1)).updatePlaybackPosition(1, 10, 65);
    }

    @Test
    void testGetPlaybackPosition() {
        when(studentLearningService.getPlaybackPosition(1, 10))
                .thenReturn(Map.of("lectureId", 10, "playbackPositionSeconds", 65));

        ResponseEntity<?> res = studentLearningController.getPlaybackPosition(10, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertTrue(Objects.requireNonNull(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("inline; filename=\"notes.pdf\""));
        assertEquals(resource, response.getBody());
        verify(studentLearningService, times(1)).viewMaterial(50, 102);
    }

    @Test
    void testGetMaterialViewerInfo_Success() {
        MaterialViewerResponse viewer = new MaterialViewerResponse();
        viewer.setId(102);
        viewer.setTitle("Notes");
        viewer.setIsDownloadable(false);
        viewer.setViewOnly(true);
        viewer.setDownloadUrl(null);
        viewer.setViewUrl("/api/student/learning/materials/102/view");

        when(studentLearningService.getMaterialViewerInfo(50, 102)).thenReturn(viewer);

        ResponseEntity<MaterialViewerResponse> response = controller.getMaterialViewerInfo(102, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(Objects.requireNonNull(response.getBody()).getIsDownloadable());
        assertTrue(Objects.requireNonNull(response.getBody()).getViewOnly());
        assertNull(Objects.requireNonNull(response.getBody()).getDownloadUrl(), "No download URL should be exposed for view-only materials");
        assertEquals("/api/student/learning/materials/102/view", Objects.requireNonNull(response.getBody()).getViewUrl());
        verify(studentLearningService, times(1)).getMaterialViewerInfo(50, 102);
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void testGetLectureMaterials() {
        when(studentLearningService.getLectureMaterials(1, 10))
                .thenReturn(List.of(new LectureMaterialResponse(1L, 10L, "Notes", "url", "pdf", true)));

        ResponseEntity<List<LectureMaterialResponse>> res = studentLearningController.getLectureMaterials(10, authentication);

        assertEquals(200, res.getStatusCode().value());
        assertEquals(1, res.getBody().size());
    }

    @Test
    void testToggleBookmark() {
        when(studentLearningService.toggleLectureBookmark(1, 10))
                .thenReturn(Map.of("isBookmarked", true));

        ResponseEntity<?> res = studentLearningController.toggleLectureBookmark(10, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void testGetBookmarks() {
        when(studentLearningService.getBookmarkedLectures(1))
                .thenReturn(List.of(new LectureBookmarkResponse(1L, 10L, "Title", 100L, "Course", LocalDateTime.now())));

        ResponseEntity<List<LectureBookmarkResponse>> res = studentLearningController.getBookmarkedLectures(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
        assertEquals(200, res.getStatusCode().value());
        assertEquals(1, res.getBody().size());
    }

    @Test
    void testSearchCourseLectures() {
        when(studentLearningService.searchCourseLectures(1, 100, "spring"))
                .thenReturn(List.of(new LectureSearchResult(10L, "Spring Boot Intro", "url", 1, 100L, "Course")));

        ResponseEntity<List<LectureSearchResult>> res = studentLearningController.searchCourseLectures(100, "spring", authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
        assertEquals(200, res.getStatusCode().value());
        assertEquals(1, res.getBody().size());
    }
}
