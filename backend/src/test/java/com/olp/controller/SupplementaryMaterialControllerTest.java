package com.olp.controller;

import com.olp.dto.SupplementaryMaterialResponse;
import com.olp.entity.User;
import com.olp.service.SupplementaryMaterialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplementaryMaterialControllerTest {

    @Mock
    private SupplementaryMaterialService materialService;

    @InjectMocks
    private SupplementaryMaterialController controller;

    private User instructor;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        instructor = new User("Dr. Chen", "chen@olp.com", "hash", "instructor");
        instructor.setId(10);
        authentication = new UsernamePasswordAuthenticationToken(instructor, null);
    }

    private SupplementaryMaterialResponse sampleResponse(Integer id, Integer lessonId, String title) {
        SupplementaryMaterialResponse resp = new SupplementaryMaterialResponse();
        resp.setId(id);
        resp.setLessonId(lessonId);
        resp.setTitle(title);
        resp.setOriginalFilename("test.pdf");
        resp.setFileType("pdf");
        resp.setIsDownloadable(false);
        resp.setScanStatus("CLEAN");
        return resp;
    }

    @Test
    void testUploadMaterial() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[500]);
        SupplementaryMaterialResponse expected = sampleResponse(1, 400, "Lesson Slides");

        when(materialService.resolveUser(authentication)).thenReturn(instructor);
        when(materialService.uploadMaterial(instructor, 400, null, null, "Lesson Slides", file))
                .thenReturn(expected);

        ResponseEntity<SupplementaryMaterialResponse> response = controller.uploadMaterial(
                authentication, file, 400, null, null, "Lesson Slides");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getId());
        assertFalse(response.getBody().getIsDownloadable());
        verify(materialService, times(1)).uploadMaterial(instructor, 400, null, null, "Lesson Slides", file);
    }

    @Test
    void testGetMaterialsByLesson() {
        SupplementaryMaterialResponse resp = sampleResponse(1, 400, "Lesson Slides");
        when(materialService.resolveUser(authentication)).thenReturn(instructor);
        when(materialService.getMaterialsByLesson(instructor, 400)).thenReturn(List.of(resp));

        ResponseEntity<List<SupplementaryMaterialResponse>> response = controller.getMaterialsByLesson(400, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(1, response.getBody().get(0).getId());
    }

    @Test
    void testGetMaterialsByModule() {
        SupplementaryMaterialResponse resp = sampleResponse(2, null, "Module Overview");
        resp.setModuleId(200);

        when(materialService.resolveUser(authentication)).thenReturn(instructor);
        when(materialService.getMaterialsByModule(instructor, 200)).thenReturn(List.of(resp));

        ResponseEntity<List<SupplementaryMaterialResponse>> response = controller.getMaterialsByModule(200, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetMaterialById() {
        SupplementaryMaterialResponse resp = sampleResponse(3, 400, "Reference Sheet");
        when(materialService.resolveUser(authentication)).thenReturn(instructor);
        when(materialService.getMaterialById(instructor, 3)).thenReturn(resp);

        ResponseEntity<SupplementaryMaterialResponse> response = controller.getMaterialById(3, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().getId());
    }

    @Test
    void testDeleteMaterial() {
        when(materialService.resolveUser(authentication)).thenReturn(instructor);
        doNothing().when(materialService).deleteMaterial(instructor, 4);

        ResponseEntity<Void> response = controller.deleteMaterial(4, authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(materialService, times(1)).deleteMaterial(instructor, 4);
    }
}

