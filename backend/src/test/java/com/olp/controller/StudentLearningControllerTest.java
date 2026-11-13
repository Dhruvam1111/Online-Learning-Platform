package com.olp.controller;

import com.olp.dto.LectureMaterialResponse;
import com.olp.dto.MaterialViewerResponse;
import com.olp.dto.ResourceAndMetadata;
import com.olp.entity.User;
import com.olp.service.StudentLearningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentLearningControllerTest {

    @Mock
    private StudentLearningService studentLearningService;

    @InjectMocks
    private StudentLearningController controller;

    private User student;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        student = new User("Alice", "alice@olp.com", "hash", "student");
        student.setId(50);
        authentication = new UsernamePasswordAuthenticationToken(student, null);
    }

    @Test
    void testDownloadMaterial_Success() {
        Resource resource = new ByteArrayResource("PDF-data".getBytes(StandardCharsets.UTF_8));
        ResourceAndMetadata data = new ResourceAndMetadata(resource, "slides.pdf", "application/pdf", 100L);

        when(studentLearningService.downloadMaterial(50, 101)).thenReturn(data);

        ResponseEntity<Resource> response = controller.downloadMaterial(101, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("attachment; filename=\"slides.pdf\""));
        assertEquals(resource, response.getBody());
        verify(studentLearningService, times(1)).downloadMaterial(50, 101);
    }

    @Test
    void testViewMaterial_Success() {
        Resource resource = new ByteArrayResource("Inline-PDF-data".getBytes(StandardCharsets.UTF_8));
        ResourceAndMetadata data = new ResourceAndMetadata(resource, "notes.pdf", "application/pdf", 200L);

        when(studentLearningService.viewMaterial(50, 102)).thenReturn(data);

        ResponseEntity<Resource> response = controller.viewMaterial(102, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("inline; filename=\"notes.pdf\""));
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
        assertFalse(response.getBody().getIsDownloadable());
        assertTrue(response.getBody().getViewOnly());
        assertNull(response.getBody().getDownloadUrl(), "No download URL should be exposed for view-only materials");
        assertEquals("/api/student/learning/materials/102/view", response.getBody().getViewUrl());
        verify(studentLearningService, times(1)).getMaterialViewerInfo(50, 102);
    }

    @Test
    void testGetLectureMaterials() {
        LectureMaterialResponse resp = new LectureMaterialResponse(1L, 10L, "Slides", null, "pdf", false);
        when(studentLearningService.getLectureMaterials(50, 10)).thenReturn(List.of(resp));

        ResponseEntity<List<LectureMaterialResponse>> response = controller.getLectureMaterials(10, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetLessonMaterials() {
        LectureMaterialResponse resp = new LectureMaterialResponse(2L, null, "Cheatsheet", null, "pdf", true);
        when(studentLearningService.getLessonMaterials(50, 20)).thenReturn(List.of(resp));

        ResponseEntity<List<LectureMaterialResponse>> response = controller.getLessonMaterials(20, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetModuleMaterials() {
        LectureMaterialResponse resp = new LectureMaterialResponse(3L, null, "Syllabus", null, "pdf", true);
        when(studentLearningService.getModuleMaterials(50, 30)).thenReturn(List.of(resp));

        ResponseEntity<List<LectureMaterialResponse>> response = controller.getModuleMaterials(30, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }
}
