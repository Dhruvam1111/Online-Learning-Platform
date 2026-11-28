package com.olp.service;

import com.olp.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaterialStorageServiceTest {

    @TempDir
    Path tempDir;

    private MaterialStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new MaterialStorageService(tempDir.toString());
    }

    @Test
    void validateMaterial_acceptsAllowedDocumentsAndRejectsInvalidSizeExtensionOrMime() {
        MockMultipartFile validPdf = new MockMultipartFile(
                "file", "lecture-notes.pdf", "application/pdf", "pdf-bytes".getBytes(StandardCharsets.UTF_8));
        assertDoesNotThrow(() -> storageService.validateMaterial(validPdf));

        // Blank MIME with valid extension is allowed
        MockMultipartFile blankMimeTxt = new MockMultipartFile(
                "file", "readme.txt", "", "hello".getBytes(StandardCharsets.UTF_8));
        assertDoesNotThrow(() -> storageService.validateMaterial(blankMimeTxt));

        assertThrows(IllegalArgumentException.class, () -> storageService.validateMaterial(null));
        assertThrows(IllegalArgumentException.class, () -> storageService.validateMaterial(
                new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0])));
        assertThrows(IllegalArgumentException.class, () -> storageService.validateMaterial(
                new MockMultipartFile("file", "script.exe", "application/octet-stream", "bin".getBytes())));
        assertThrows(IllegalArgumentException.class, () -> storageService.validateMaterial(
                new MockMultipartFile("file", "page.pdf", "text/html", "<html>".getBytes())));
        assertThrows(IllegalArgumentException.class, () -> storageService.validateMaterial(
                new MockMultipartFile("file", "   ", "application/pdf", "data".getBytes())));

        MultipartFile oversized = mock(MultipartFile.class);
        when(oversized.isEmpty()).thenReturn(false);
        when(oversized.getSize()).thenReturn(MaterialStorageService.MAX_MATERIAL_FILE_SIZE + 1);
        assertThrows(IllegalArgumentException.class, () -> storageService.validateMaterial(oversized));
    }

    @Test
    void storeLoadAndDeleteMaterial_roundTripsAndBlocksPathTraversal() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "module 1 (draft).pdf", "application/pdf", "course material content".getBytes(StandardCharsets.UTF_8));

        String storedPath = storageService.storeMaterial(file);
        assertTrue(storedPath.contains("/materials/"));
        assertTrue(Files.exists(Path.of(storedPath)));

        Resource loaded = storageService.loadMaterialAsResource(storedPath);
        assertTrue(loaded.exists());
        try (var in = loaded.getInputStream()) {
            assertEquals("course material content", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }

        // Relative filename within materials dir also resolves cleanly
        String filenameOnly = Path.of(storedPath).getFileName().toString();
        assertTrue(storageService.loadMaterialAsResource(filenameOnly).exists());

        // Path traversal and external paths must be rejected
        assertThrows(IllegalArgumentException.class, () -> storageService.loadMaterialAsResource("../outside.pdf"));
        assertThrows(IllegalArgumentException.class, () -> storageService.loadMaterialAsResource(null));
        assertThrows(IllegalArgumentException.class, () -> storageService.loadMaterialAsResource("   "));
        assertThrows(ResourceNotFoundException.class, () -> storageService.loadMaterialAsResource("missing-file.pdf"));

        storageService.deleteMaterialFile(storedPath);
        assertFalse(Files.exists(Path.of(storedPath)));
        assertDoesNotThrow(() -> storageService.deleteMaterialFile(null));
    }
}
