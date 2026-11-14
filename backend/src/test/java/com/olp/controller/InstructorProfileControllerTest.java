package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.InstructorMediaService;
import com.olp.service.InstructorProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorProfileControllerTest {

    @Mock
    private InstructorProfileService instructorProfileService;

    @Mock
    private InstructorMediaService instructorMediaService;

    @InjectMocks
    private InstructorProfileController controller;

    private User instructor;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        instructor = new User("Dr. Alan Turing", "turing@olp.com", "hash", "instructor", "active");
        instructor.setId(10);
        authentication = new UsernamePasswordAuthenticationToken(instructor, null);
    }

    // =========================================================================
    // 1. BASIC PROFILE TESTS (FR-I03)
    // =========================================================================

    @Test
    void testGetProfile_Success() {
        InstructorProfileResponse expected = new InstructorProfileResponse(
                10, "Dr. Alan Turing", "turing@olp.com", "instructor",
                "Ph.D.", "AI", "Bio", LocalDateTime.now(), LocalDateTime.now());

        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);
        when(instructorProfileService.getProfile(instructor)).thenReturn(expected);

        ResponseEntity<InstructorProfileResponse> response = controller.getProfile(authentication, null, null, null);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void testGetProfile_DifferentIdInQuery_ThrowsAccessDenied() {
        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);

        assertThrows(AccessDeniedException.class,
                () -> controller.getProfile(authentication, 999, null, null));
    }

    @Test
    void testUpdateProfile_Success() {
        UpdateInstructorProfileRequest request = new UpdateInstructorProfileRequest("Prof. Alan", "D.Sc.", "ML",
                "New Bio");
        InstructorProfileResponse expected = new InstructorProfileResponse(
                10, "Prof. Alan", "turing@olp.com", "instructor",
                "D.Sc.", "ML", "New Bio", LocalDateTime.now(), LocalDateTime.now());

        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);
        when(instructorProfileService.updateProfile(instructor, request)).thenReturn(expected);

        ResponseEntity<InstructorProfileResponse> response = controller.updateProfile(authentication, request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
        verify(instructorProfileService).updateProfile(instructor, request);
    }

    @Test
    void testGetInstructorSummary_PublicEndpoint_ReturnsSummary() {
        InstructorSummaryResponse expected = new InstructorSummaryResponse(
                10, "Dr. Alan Turing", "turing@olp.com",
                "Ph.D.", "AI", "Bio", LocalDateTime.now(), LocalDateTime.now());

        when(instructorProfileService.getInstructorSummary(10)).thenReturn(expected);

        ResponseEntity<InstructorSummaryResponse> response = controller.getInstructorSummary(10);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void testGetCurrentInstructorSummary_Authenticated_ReturnsSummary() {
        InstructorSummaryResponse expected = new InstructorSummaryResponse(
                10, "Dr. Alan Turing", "turing@olp.com",
                "Ph.D.", "AI", "Bio", LocalDateTime.now(), LocalDateTime.now());

        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);
        when(instructorProfileService.getInstructorSummary(10)).thenReturn(expected);

        ResponseEntity<InstructorSummaryResponse> response = controller.getCurrentInstructorSummary(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    // =========================================================================
    // 2. PROFILE PICTURE TESTS (FR-I04)
    // =========================================================================

    @Test
    void testUploadProfilePicture_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] { 1, 2, 3 });
        ProfilePictureResponse expected = new ProfilePictureResponse(
                10, "/api/instructors/10/picture", "avatar.png", "image/png", 3L, LocalDateTime.now(), "Uploaded");

        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);
        when(instructorMediaService.uploadProfilePicture(instructor, file)).thenReturn(expected);

        ResponseEntity<ProfilePictureResponse> response = controller.uploadProfilePicture(authentication, file);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void testGetOwnProfilePicture_Success() {
        Resource dummy = mock(Resource.class);
        when(dummy.getFilename()).thenReturn("avatar.png");

        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);
        when(instructorMediaService.getProfilePicture(10)).thenReturn(dummy);

        ResponseEntity<Resource> response = controller.getOwnProfilePicture(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertSame(dummy, response.getBody());
        assertNotNull(response.getHeaders().getContentType());
        assertEquals("image/png", Objects.requireNonNull(response.getHeaders().getContentType()).toString());
    }

    @Test
    void testGetInstructorProfilePicture_PublicEndpoint_Success() {
        Resource dummy = mock(Resource.class);
        when(dummy.getFilename()).thenReturn("avatar.jpg");

        when(instructorMediaService.getProfilePicture(10)).thenReturn(dummy);

        ResponseEntity<Resource> response = controller.getInstructorProfilePicture(10);

        assertEquals(200, response.getStatusCode().value());
        assertSame(dummy, response.getBody());
        assertNotNull(response.getHeaders().getContentType());
        assertEquals("image/jpeg", Objects.requireNonNull(response.getHeaders().getContentType()).toString());
    }

    @Test
    void testDeleteProfilePicture_Success() {
        ApiResponse expected = new ApiResponse(true, "Profile picture deleted successfully");

        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);
        when(instructorMediaService.deleteProfilePicture(instructor)).thenReturn(expected);

        ResponseEntity<ApiResponse> response = controller.deleteProfilePicture(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    // =========================================================================
    // 3. CREDENTIAL DOCUMENTS TESTS (FR-I04)
    // =========================================================================

    @Test
    void testUploadCredential_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "degree.pdf", "application/pdf", "%PDF".getBytes());
        InstructorCredentialResponse expected = new InstructorCredentialResponse(
                1, 10, "degree.pdf", "application/pdf", 100L, "DEGREE", "ACTIVE", "/download", LocalDateTime.now(),
                LocalDateTime.now());

        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);
        when(instructorMediaService.uploadCredential(instructor, file, "DEGREE")).thenReturn(expected);

        ResponseEntity<InstructorCredentialResponse> response = controller.uploadCredential(authentication, file,
                "DEGREE");

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void testGetCredentials_Success() {
        InstructorCredentialResponse item = new InstructorCredentialResponse(
                1, 10, "degree.pdf", "application/pdf", 100L, "DEGREE", "ACTIVE", "/download", LocalDateTime.now(),
                LocalDateTime.now());

        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);
        when(instructorMediaService.getCredentials(instructor)).thenReturn(List.of(item));

        ResponseEntity<List<InstructorCredentialResponse>> response = controller.getCredentials(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
    }

    @Test
    void testGetCredential_Success() {
        InstructorCredentialResponse expected = new InstructorCredentialResponse(
                1, 10, "degree.pdf", "application/pdf", 100L, "DEGREE", "ACTIVE", "/download", LocalDateTime.now(),
                LocalDateTime.now());

        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);
        when(instructorMediaService.getCredential(instructor, 1)).thenReturn(expected);

        ResponseEntity<InstructorCredentialResponse> response = controller.getCredential(authentication, 1);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void testDownloadCredential_Success() {
        InstructorCredentialResponse metadata = new InstructorCredentialResponse(
                1, 10, "degree.pdf", "application/pdf", 100L, "DEGREE", "ACTIVE", "/download", LocalDateTime.now(),
                LocalDateTime.now());
        Resource dummy = mock(Resource.class);

        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);
        when(instructorMediaService.getCredential(instructor, 1)).thenReturn(metadata);
        when(instructorMediaService.downloadCredential(instructor, 1)).thenReturn(dummy);

        ResponseEntity<Resource> response = controller.downloadCredential(authentication, 1);

        assertEquals(200, response.getStatusCode().value());
        assertSame(dummy, response.getBody());
        assertNotNull(response.getHeaders().getContentType());
        assertEquals("application/pdf", Objects.requireNonNull(response.getHeaders().getContentType()).toString());

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        assertTrue(disposition.contains("degree.pdf"));
    }

    @Test
    void testReplaceCredential_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "new.pdf", "application/pdf", "%PDF".getBytes());
        InstructorCredentialResponse expected = new InstructorCredentialResponse(
                1, 10, "new.pdf", "application/pdf", 100L, "DEGREE", "ACTIVE", "/download", LocalDateTime.now(),
                LocalDateTime.now());

        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);
        when(instructorMediaService.replaceCredential(instructor, 1, file, "DEGREE")).thenReturn(expected);

        ResponseEntity<InstructorCredentialResponse> response = controller.replaceCredential(authentication, 1, file,
                "DEGREE");

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void testDeleteCredential_Success() {
        ApiResponse expected = new ApiResponse(true, "Deleted");

        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);
        when(instructorMediaService.deleteCredential(instructor, 1)).thenReturn(expected);

        ResponseEntity<ApiResponse> response = controller.deleteCredential(authentication, 1);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void testGetUploadHistory_Success() {
        UploadHistoryResponse item = new UploadHistoryResponse(
                1, 10, "CREDENTIAL", "UPLOADED", "file.pdf", "application/pdf", 50L, "Details", LocalDateTime.now());

        when(instructorProfileService.resolveUser(authentication)).thenReturn(instructor);
        when(instructorMediaService.getUploadHistory(instructor)).thenReturn(List.of(item));

        ResponseEntity<List<UploadHistoryResponse>> response = controller.getUploadHistory(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
    }
}
