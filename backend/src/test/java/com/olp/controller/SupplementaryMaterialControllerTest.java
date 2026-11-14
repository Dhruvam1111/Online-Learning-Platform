package com.olp.controller;

import com.olp.dto.ContentRemovalResponse;
import com.olp.dto.ReorderStructureRequest;
import com.olp.dto.SupplementaryMaterialResponse;
import com.olp.dto.UpdateDownloadableRequest;
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
        resp.setOrderIndex(1);
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

        ResponseEntity<List<SupplementaryMaterialResponse>> response = controller.getMaterialsByLesson(400,
                authentication);

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

        ResponseEntity<List<SupplementaryMaterialResponse>> response = controller.getMaterialsByModule(200,
                authentication);

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
    void testDeleteMaterial_NoProgress_HardDelete() {
        ContentRemovalResponse removalResponse = new ContentRemovalResponse(
                4, true, false, "Content item successfully deleted", null, false);
        when(materialService.resolveUser(authentication)).thenReturn(instructor);
        when(materialService.removeMaterial(instructor, 4)).thenReturn(removalResponse);

        ResponseEntity<ContentRemovalResponse> response = controller.deleteMaterial(4, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isDeleted());
        assertFalse(response.getBody().isSoftDeleted());
        assertFalse(response.getBody().isCriteriaWarning());
        verify(materialService, times(1)).removeMaterial(instructor, 4);
    }

    @Test
    void testDeleteMaterial_WithProgress_SoftDelete() {
        ContentRemovalResponse removalResponse = new ContentRemovalResponse(
                5, true, true,
                "Content item has active student progress and was soft-deleted to preserve learner records",
                "Content item has active student progress and was soft-deleted to preserve learner records",
                false);
        when(materialService.resolveUser(authentication)).thenReturn(instructor);
        when(materialService.removeMaterial(instructor, 5)).thenReturn(removalResponse);

        ResponseEntity<ContentRemovalResponse> response = controller.deleteMaterial(5, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isDeleted());
        assertTrue(response.getBody().isSoftDeleted());
        assertNotNull(response.getBody().getWarning());
        verify(materialService, times(1)).removeMaterial(instructor, 5);
    }

    @Test
    void testReorderMaterialsInLesson() {
        ReorderStructureRequest request = new ReorderStructureRequest(List.of(3, 1, 2));
        SupplementaryMaterialResponse r1 = sampleResponse(3, 400, "Item 3");
        r1.setOrderIndex(1);
        SupplementaryMaterialResponse r2 = sampleResponse(1, 400, "Item 1");
        r2.setOrderIndex(2);
        SupplementaryMaterialResponse r3 = sampleResponse(2, 400, "Item 2");
        r3.setOrderIndex(3);

        when(materialService.resolveUser(authentication)).thenReturn(instructor);
        when(materialService.reorderMaterialsInLesson(instructor, 400, List.of(3, 1, 2)))
                .thenReturn(List.of(r1, r2, r3));

        ResponseEntity<List<SupplementaryMaterialResponse>> response = controller.reorderMaterialsInLesson(
                400, request, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size());
        assertEquals(3, response.getBody().get(0).getId());
        assertEquals(1, response.getBody().get(0).getOrderIndex());
        verify(materialService, times(1)).reorderMaterialsInLesson(instructor, 400, List.of(3, 1, 2));
    }

    @Test
    void testReplaceMaterial() {
        MockMultipartFile newFile = new MockMultipartFile("file", "replacement.pdf", "application/pdf", new byte[1024]);
        SupplementaryMaterialResponse replaced = sampleResponse(1, 400, "Lesson Slides");
        replaced.setOriginalFilename("replacement.pdf");
        replaced.setFileUrl("/uploads/materials/replacement.pdf");

        when(materialService.resolveUser(authentication)).thenReturn(instructor);
        when(materialService.replaceMaterialFile(instructor, 1, newFile)).thenReturn(replaced);

        ResponseEntity<SupplementaryMaterialResponse> response = controller.replaceMaterial(1, newFile, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("replacement.pdf", response.getBody().getOriginalFilename());
        verify(materialService, times(1)).replaceMaterialFile(instructor, 1, newFile);
    }

    @Test
    void testUpdateDownloadableStatus_QueryParam() {
        SupplementaryMaterialResponse resp = sampleResponse(3, 400, "Reference Sheet");
        resp.setIsDownloadable(true);

        when(materialService.resolveUser(authentication)).thenReturn(instructor);
        when(materialService.updateDownloadableStatus(instructor, 3, true)).thenReturn(resp);

        ResponseEntity<SupplementaryMaterialResponse> response = controller.updateDownloadableStatus(
                3, true, null, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getIsDownloadable());
        verify(materialService, times(1)).updateDownloadableStatus(instructor, 3, true);
    }

    @Test
    void testUpdateDownloadableStatus_RequestBody() {
        SupplementaryMaterialResponse resp = sampleResponse(3, 400, "Reference Sheet");
        resp.setIsDownloadable(false);

        when(materialService.resolveUser(authentication)).thenReturn(instructor);
        when(materialService.updateDownloadableStatus(instructor, 3, false)).thenReturn(resp);

        ResponseEntity<SupplementaryMaterialResponse> response = controller.updateDownloadableStatus(
                3, null, new UpdateDownloadableRequest(false), authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getIsDownloadable());
        verify(materialService, times(1)).updateDownloadableStatus(instructor, 3, false);
    }
}
