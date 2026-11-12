package com.olp.service;

import com.olp.dto.ApiResponse;
import com.olp.dto.InstructorCredentialResponse;
import com.olp.dto.ProfilePictureResponse;
import com.olp.dto.UploadHistoryResponse;
import com.olp.entity.InstructorCredential;
import com.olp.entity.InstructorProfile;
import com.olp.entity.InstructorUploadHistory;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.InstructorCredentialRepository;
import com.olp.repository.InstructorProfileRepository;
import com.olp.repository.InstructorUploadHistoryRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorMediaServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private InstructorProfileRepository instructorProfileRepository;

    @Mock
    private InstructorCredentialRepository instructorCredentialRepository;

    @Mock
    private InstructorUploadHistoryRepository instructorUploadHistoryRepository;

    @Mock
    private FileStorageService fileStorageService;

    private InstructorMediaService service;

    private User instructor;
    private User otherInstructor;
    private User student;
    private User admin;
    private InstructorProfile existingProfile;

    @BeforeEach
    void setUp() {
        service = new InstructorMediaService(
                userRepository,
                instructorProfileRepository,
                instructorCredentialRepository,
                instructorUploadHistoryRepository,
                fileStorageService);

        instructor = new User("Dr. Alan Turing", "turing@olp.com", "hash", "instructor", "active");
        instructor.setId(10);

        otherInstructor = new User("Prof. Grace Hopper", "hopper@olp.com", "hash", "instructor", "active");
        otherInstructor.setId(20);

        student = new User("Ada Lovelace", "ada@olp.com", "hash", "student", "active");
        student.setId(30);

        admin = new User("Admin User", "admin@olp.com", "hash", "admin", "active");
        admin.setId(1);

        existingProfile = new InstructorProfile(instructor, "Ph.D", "AI", "Bio");
        existingProfile.setId(100);
    }

    // =========================================================================
    // 1. PROFILE PICTURE TESTS
    // =========================================================================

    @Test
    void testUploadProfilePicture_FirstTime_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] { 1, 2, 3 });
        String storedPath = "profile-pictures/10/uuid_avatar.png";

        when(userRepository.findById(10)).thenReturn(Optional.of(instructor));
        when(instructorProfileRepository.findByInstructorId(10)).thenReturn(Optional.of(existingProfile));
        when(fileStorageService.storeProfilePicture(10, file)).thenReturn(storedPath);

        ProfilePictureResponse response = service.uploadProfilePicture(instructor, file);

        assertNotNull(response);
        assertEquals(10, response.getInstructorId());
        assertEquals("/api/instructors/10/picture", response.getProfilePictureUrl());
        assertEquals("avatar.png", response.getOriginalFileName());
        assertEquals(storedPath, existingProfile.getProfilePictureUrl());

        verify(instructorProfileRepository).save(existingProfile);

        // Verify history record
        ArgumentCaptor<InstructorUploadHistory> historyCaptor = ArgumentCaptor.forClass(InstructorUploadHistory.class);
        verify(instructorUploadHistoryRepository).save(historyCaptor.capture());
        InstructorUploadHistory savedHistory = historyCaptor.getValue();
        assertEquals("PROFILE_PICTURE", savedHistory.getCategory());
        assertEquals("UPLOADED", savedHistory.getAction());
    }

    @Test
    void testUploadProfilePicture_Replacement_DeletesPreviousFileAndRecordsReplaced() {
        existingProfile.setProfilePictureUrl("profile-pictures/10/old_avatar.png");
        MockMultipartFile newFile = new MockMultipartFile("file", "new_avatar.jpg", "image/jpeg",
                new byte[] { 4, 5, 6 });
        String newStoredPath = "profile-pictures/10/uuid_new_avatar.jpg";

        when(userRepository.findById(10)).thenReturn(Optional.of(instructor));
        when(instructorProfileRepository.findByInstructorId(10)).thenReturn(Optional.of(existingProfile));
        when(fileStorageService.storeProfilePicture(10, newFile)).thenReturn(newStoredPath);

        ProfilePictureResponse response = service.uploadProfilePicture(instructor, newFile);

        assertNotNull(response);
        verify(fileStorageService).deleteFile("profile-pictures/10/old_avatar.png");
        assertEquals(newStoredPath, existingProfile.getProfilePictureUrl());

        ArgumentCaptor<InstructorUploadHistory> historyCaptor = ArgumentCaptor.forClass(InstructorUploadHistory.class);
        verify(instructorUploadHistoryRepository).save(historyCaptor.capture());
        assertEquals("REPLACED", historyCaptor.getValue().getAction());
    }

    @Test
    void testUploadProfilePicture_StudentRole_ThrowsAccessDenied() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] { 1, 2 });
        assertThrows(AccessDeniedException.class, () -> service.uploadProfilePicture(student, file));
    }

    @Test
    void testGetProfilePicture_Success() {
        existingProfile.setProfilePictureUrl("profile-pictures/10/avatar.png");
        when(instructorProfileRepository.findByInstructorId(10)).thenReturn(Optional.of(existingProfile));
        Resource dummy = new ByteArrayResource(new byte[] { 1, 2, 3 });
        when(fileStorageService.loadFileAsResource("profile-pictures/10/avatar.png")).thenReturn(dummy);

        Resource result = service.getProfilePicture(10);
        assertSame(dummy, result);
    }

    @Test
    void testGetProfilePicture_NotUploaded_ThrowsResourceNotFound() {
        existingProfile.setProfilePictureUrl(null);
        when(instructorProfileRepository.findByInstructorId(10)).thenReturn(Optional.of(existingProfile));

        assertThrows(ResourceNotFoundException.class, () -> service.getProfilePicture(10));
    }

    @Test
    void testDeleteProfilePicture_Success() {
        existingProfile.setProfilePictureUrl("profile-pictures/10/avatar.png");
        when(instructorProfileRepository.findByInstructorId(10)).thenReturn(Optional.of(existingProfile));

        ApiResponse res = service.deleteProfilePicture(instructor);
        assertTrue(res.isSuccess());
        assertNull(existingProfile.getProfilePictureUrl());
        verify(fileStorageService).deleteFile("profile-pictures/10/avatar.png");

        ArgumentCaptor<InstructorUploadHistory> historyCaptor = ArgumentCaptor.forClass(InstructorUploadHistory.class);
        verify(instructorUploadHistoryRepository).save(historyCaptor.capture());
        assertEquals("DELETED", historyCaptor.getValue().getAction());
    }

    // =========================================================================
    // 2. CREDENTIAL DOCUMENTS TESTS
    // =========================================================================

    @Test
    void testUploadCredential_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "degree.pdf", "application/pdf", "%PDF".getBytes());
        String storedPath = "credentials/10/uuid_degree.pdf";

        when(userRepository.findById(10)).thenReturn(Optional.of(instructor));
        when(fileStorageService.storeCredential(10, file)).thenReturn(storedPath);

        InstructorCredential savedEntity = new InstructorCredential(
                instructor, "degree.pdf", storedPath, "application/pdf", 100L, "DEGREE", "ACTIVE");
        savedEntity.setId(55);
        when(instructorCredentialRepository.save(any(InstructorCredential.class))).thenReturn(savedEntity);

        InstructorCredentialResponse response = service.uploadCredential(instructor, file, "DEGREE");

        assertNotNull(response);
        assertEquals(55, response.getId());
        assertEquals(10, response.getInstructorId());
        assertEquals("degree.pdf", response.getOriginalFileName());
        assertEquals("DEGREE", response.getDocumentType());
        assertEquals("ACTIVE", response.getStatus());

        verify(instructorUploadHistoryRepository).save(any(InstructorUploadHistory.class));
    }

    @Test
    void testGetCredentials_ReturnsOnlyActiveCredentialsOfCurrentInstructor() {
        InstructorCredential c1 = new InstructorCredential(instructor, "doc1.pdf", "path1", "application/pdf", 100L,
                "DEGREE", "ACTIVE");
        c1.setId(1);
        when(instructorCredentialRepository.findByInstructorIdAndStatus(10, "ACTIVE")).thenReturn(List.of(c1));

        List<InstructorCredentialResponse> list = service.getCredentials(instructor);
        assertEquals(1, list.size());
        assertEquals(1, list.get(0).getId());
    }

    @Test
    void testGetCredential_AnotherInstructor_ThrowsAccessDenied() {
        InstructorCredential c = new InstructorCredential(instructor, "doc.pdf", "path", "application/pdf", 100L,
                "DEGREE", "ACTIVE");
        c.setId(99);
        when(instructorCredentialRepository.findById(99)).thenReturn(Optional.of(c));

        assertThrows(AccessDeniedException.class, () -> service.getCredential(otherInstructor, 99));
    }

    @Test
    void testGetCredential_AdminAllowed() {
        InstructorCredential c = new InstructorCredential(instructor, "doc.pdf", "path", "application/pdf", 100L,
                "DEGREE", "ACTIVE");
        c.setId(99);
        when(instructorCredentialRepository.findById(99)).thenReturn(Optional.of(c));

        InstructorCredentialResponse res = service.getCredential(admin, 99);
        assertNotNull(res);
        assertEquals(99, res.getId());
    }

    @Test
    void testDownloadCredential_OwnerAllowed() {
        InstructorCredential c = new InstructorCredential(instructor, "doc.pdf", "credentials/10/uuid_doc.pdf",
                "application/pdf", 100L, "DEGREE", "ACTIVE");
        c.setId(99);
        when(instructorCredentialRepository.findById(99)).thenReturn(Optional.of(c));
        Resource dummy = new ByteArrayResource(new byte[] { 1, 2 });
        when(fileStorageService.loadFileAsResource("credentials/10/uuid_doc.pdf")).thenReturn(dummy);

        Resource res = service.downloadCredential(instructor, 99);
        assertSame(dummy, res);
    }

    @Test
    void testReplaceCredential_OwnerReplaces_Success() {
        InstructorCredential c = new InstructorCredential(instructor, "old.pdf", "credentials/10/old.pdf",
                "application/pdf", 100L, "DEGREE", "ACTIVE");
        c.setId(99);
        when(instructorCredentialRepository.findById(99)).thenReturn(Optional.of(c));

        MockMultipartFile newFile = new MockMultipartFile("file", "updated.pdf", "application/pdf", "%PDF".getBytes());
        when(fileStorageService.storeCredential(10, newFile)).thenReturn("credentials/10/updated.pdf");
        when(instructorCredentialRepository.save(any(InstructorCredential.class))).thenAnswer(i -> i.getArgument(0));

        InstructorCredentialResponse res = service.replaceCredential(instructor, 99, newFile, "CERTIFICATE");

        assertNotNull(res);
        assertEquals("updated.pdf", res.getOriginalFileName());
        assertEquals("CERTIFICATE", res.getDocumentType());
        verify(fileStorageService).deleteFile("credentials/10/old.pdf");
        verify(instructorUploadHistoryRepository).save(any(InstructorUploadHistory.class));
    }

    @Test
    void testDeleteCredential_SetsStatusDeleted() {
        InstructorCredential c = new InstructorCredential(instructor, "doc.pdf", "credentials/10/doc.pdf",
                "application/pdf", 100L, "DEGREE", "ACTIVE");
        c.setId(99);
        when(instructorCredentialRepository.findById(99)).thenReturn(Optional.of(c));

        ApiResponse res = service.deleteCredential(instructor, 99);
        assertTrue(res.isSuccess());
        assertEquals("DELETED", c.getStatus());
        verify(fileStorageService).deleteFile("credentials/10/doc.pdf");
        verify(instructorUploadHistoryRepository).save(any(InstructorUploadHistory.class));
    }

    // =========================================================================
    // 3. UPLOAD HISTORY TESTS
    // =========================================================================

    @Test
    void testGetUploadHistory_ReturnsHistoryList() {
        InstructorUploadHistory h = new InstructorUploadHistory(
                instructor, "CREDENTIAL", "UPLOADED", "file.pdf", "path", "application/pdf", 123L, "Details");
        h.setId(1);
        when(instructorUploadHistoryRepository.findByInstructorIdOrderByCreatedAtDesc(10)).thenReturn(List.of(h));

        List<UploadHistoryResponse> list = service.getUploadHistory(instructor);
        assertEquals(1, list.size());
        assertEquals("UPLOADED", list.get(0).getAction());
        assertEquals("file.pdf", list.get(0).getOriginalFileName());
    }
}
