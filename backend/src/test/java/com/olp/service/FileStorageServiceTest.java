package com.olp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempUploadDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempUploadDir.toString());
    }

    private byte[] createTestImageBytes(String format) throws IOException {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 100, 100);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    // =========================================================================
    // 1. PROFILE PICTURE VALIDATION TESTS
    // =========================================================================

    @Test
    void testValidateProfilePicture_EmptyFile_ThrowsIllegalArgument() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.validateProfilePicture(emptyFile));
        assertTrue(ex.getMessage().contains("cannot be empty"));
    }

    @Test
    void testValidateProfilePicture_Exceeds5MB_ThrowsIllegalArgument() {
        byte[] oversized = new byte[(int) (FileStorageService.MAX_PROFILE_PICTURE_SIZE + 1024)];
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", oversized);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.validateProfilePicture(file));
        assertTrue(ex.getMessage().contains("exceeds maximum limit"));
    }

    @Test
    void testValidateProfilePicture_InvalidExtension_ThrowsIllegalArgument() {
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "image/png", new byte[]{1, 2, 3});
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.validateProfilePicture(file));
        assertTrue(ex.getMessage().contains("Invalid file format"));
    }

    @Test
    void testValidateProfilePicture_InvalidContentType_ThrowsIllegalArgument() {
        MockMultipartFile file = new MockMultipartFile("file", "picture.jpg", "application/pdf", new byte[]{1, 2, 3});
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.validateProfilePicture(file));
        assertTrue(ex.getMessage().contains("Invalid content type"));
    }

    @Test
    void testValidateProfilePicture_CorruptImageBytes_ThrowsIllegalArgument() {
        byte[] corruptBytes = "not-an-image-content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", corruptBytes);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.validateProfilePicture(file));
        assertTrue(ex.getMessage().contains("not a valid decodable image"));
    }

    @Test
    void testValidateProfilePicture_ValidPng_PassesValidation() throws IOException {
        byte[] imageBytes = createTestImageBytes("png");
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", imageBytes);

        assertDoesNotThrow(() -> fileStorageService.validateProfilePicture(file));
    }

    @Test
    void testValidateProfilePicture_ValidJpeg_PassesValidation() throws IOException {
        byte[] imageBytes = createTestImageBytes("jpeg");
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", imageBytes);

        assertDoesNotThrow(() -> fileStorageService.validateProfilePicture(file));
    }

    // =========================================================================
    // 2. CREDENTIAL DOCUMENT VALIDATION TESTS
    // =========================================================================

    @Test
    void testValidateCredentialFile_EmptyFile_ThrowsIllegalArgument() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "degree.pdf", "application/pdf", new byte[0]);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.validateCredentialFile(emptyFile));
        assertTrue(ex.getMessage().contains("cannot be empty"));
    }

    @Test
    void testValidateCredentialFile_Exceeds10MB_ThrowsIllegalArgument() {
        byte[] oversized = new byte[(int) (FileStorageService.MAX_CREDENTIAL_FILE_SIZE + 1024)];
        MockMultipartFile file = new MockMultipartFile("file", "degree.pdf", "application/pdf", oversized);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.validateCredentialFile(file));
        assertTrue(ex.getMessage().contains("exceeds maximum limit"));
    }

    @Test
    void testValidateCredentialFile_InvalidExtension_ThrowsIllegalArgument() {
        MockMultipartFile file = new MockMultipartFile("file", "script.exe", "application/pdf", new byte[]{1, 2, 3});
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.validateCredentialFile(file));
        assertTrue(ex.getMessage().contains("Invalid file format"));
    }

    @Test
    void testValidateCredentialFile_ValidPdf_PassesValidation() {
        MockMultipartFile file = new MockMultipartFile("file", "phd_certificate.pdf", "application/pdf", "%PDF-1.4 test".getBytes());
        assertDoesNotThrow(() -> fileStorageService.validateCredentialFile(file));
    }

    @Test
    void testValidateCredentialFile_ValidImageCredential_PassesValidation() throws IOException {
        byte[] imageBytes = createTestImageBytes("png");
        MockMultipartFile file = new MockMultipartFile("file", "degree_scan.png", "image/png", imageBytes);
        assertDoesNotThrow(() -> fileStorageService.validateCredentialFile(file));
    }

    // =========================================================================
    // 3. STORAGE & RETRIEVAL TESTS
    // =========================================================================

    @Test
    void testStoreProfilePicture_Success() throws IOException {
        byte[] imageBytes = createTestImageBytes("png");
        MockMultipartFile file = new MockMultipartFile("file", "my_avatar.png", "image/png", imageBytes);

        String storedPath = fileStorageService.storeProfilePicture(42, file);
        assertNotNull(storedPath);
        assertTrue(storedPath.startsWith("profile-pictures/42/"));
        assertTrue(storedPath.endsWith(".png"));

        Resource resource = fileStorageService.loadFileAsResource(storedPath);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    void testStoreCredential_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "cert.pdf", "application/pdf", "%PDF-1.4 sample content".getBytes());

        String storedPath = fileStorageService.storeCredential(10, file);
        assertNotNull(storedPath);
        assertTrue(storedPath.startsWith("credentials/10/"));
        assertTrue(storedPath.endsWith(".pdf"));

        Resource resource = fileStorageService.loadFileAsResource(storedPath);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    void testDeleteFile_RemovesPhysicalFile() {
        MockMultipartFile file = new MockMultipartFile("file", "cert.pdf", "application/pdf", "%PDF-1.4 sample content".getBytes());
        String storedPath = fileStorageService.storeCredential(10, file);

        Resource beforeDelete = fileStorageService.loadFileAsResource(storedPath);
        assertTrue(beforeDelete.exists());

        fileStorageService.deleteFile(storedPath);

        assertThrows(IllegalArgumentException.class, () -> fileStorageService.loadFileAsResource(storedPath));
    }
}

