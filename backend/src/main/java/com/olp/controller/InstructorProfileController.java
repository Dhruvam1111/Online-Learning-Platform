package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.InstructorMediaService;
import com.olp.service.InstructorProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLConnection;
import java.util.List;

@RestController
@RequestMapping({"/api/instructor", "/api/instructors"})
public class InstructorProfileController {

    private final InstructorProfileService instructorProfileService;
    private final InstructorMediaService instructorMediaService;

    public InstructorProfileController(InstructorProfileService instructorProfileService) {
        this(instructorProfileService, null);
    }

    @Autowired
    public InstructorProfileController(
            InstructorProfileService instructorProfileService,
            @Autowired(required = false) InstructorMediaService instructorMediaService) {
        this.instructorProfileService = instructorProfileService;
        this.instructorMediaService = instructorMediaService;
    }

    // =========================================================================
    // FR-I03: BASIC PROFILE MANAGEMENT
    // =========================================================================

    /**
     * GET /api/instructor/profile or /api/instructors/profile
     * Authenticated instructor retrieves their own profile.
     * Prevents selecting another instructor's profile by supplying a different user ID.
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('instructor')")
    public ResponseEntity<InstructorProfileResponse> getProfile(
            Authentication authentication,
            @RequestParam(name = "id", required = false) Integer queryId,
            @RequestParam(name = "instructorId", required = false) Integer queryInstructorId,
            @RequestParam(name = "userId", required = false) Integer queryUserId) {

        User currentUser = instructorProfileService.resolveUser(authentication);

        if ((queryId != null && !queryId.equals(currentUser.getId())) ||
            (queryInstructorId != null && !queryInstructorId.equals(currentUser.getId())) ||
            (queryUserId != null && !queryUserId.equals(currentUser.getId()))) {
            throw new AccessDeniedException("Cannot access another instructor's profile");
        }

        InstructorProfileResponse response = instructorProfileService.getProfile(currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/instructor/profile or /api/instructors/profile
     * Authenticated instructor updates their editable profile fields
     * (qualifications, expertise areas, bio, name).
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('instructor')")
    public ResponseEntity<InstructorProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateInstructorProfileRequest request) {

        User currentUser = instructorProfileService.resolveUser(authentication);
        InstructorProfileResponse response = instructorProfileService.updateProfile(currentUser, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructors/{instructorId}/summary or /api/instructor/{instructorId}/summary
     * Also GET /api/instructors/{instructorId}
     * FR-I03: Public reusable instructor summary API for instructor cards on course pages.
     */
    @GetMapping({"/{instructorId:[0-9]+}/summary", "/{instructorId:[0-9]+}"})
    public ResponseEntity<InstructorSummaryResponse> getInstructorSummary(
            @PathVariable Integer instructorId) {
        InstructorSummaryResponse response = instructorProfileService.getInstructorSummary(instructorId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/summary or /api/instructors/summary
     * Authenticated instructor retrieves their own summary card.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('instructor')")
    public ResponseEntity<InstructorSummaryResponse> getCurrentInstructorSummary(
            Authentication authentication) {
        User currentUser = instructorProfileService.resolveUser(authentication);
        InstructorSummaryResponse response = instructorProfileService.getInstructorSummary(currentUser.getId());
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // FR-I04: PROFILE PICTURE MANAGEMENT
    // =========================================================================

    /**
     * POST /api/instructor/profile/picture
     * Authenticated instructor uploads or replaces their profile picture.
     */
    @PostMapping(value = "/profile/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('instructor')")
    public ResponseEntity<ProfilePictureResponse> uploadProfilePicture(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        assertMediaServiceAvailable();
        User currentUser = instructorProfileService.resolveUser(authentication);
        ProfilePictureResponse response = instructorMediaService.uploadProfilePicture(currentUser, file);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/profile/picture
     * Authenticated instructor views/downloads their own profile picture.
     */
    @GetMapping("/profile/picture")
    @PreAuthorize("hasRole('instructor')")
    public ResponseEntity<Resource> getOwnProfilePicture(Authentication authentication) {
        assertMediaServiceAvailable();
        User currentUser = instructorProfileService.resolveUser(authentication);
        Resource resource = instructorMediaService.getProfilePicture(currentUser.getId());
        return serveImageResource(resource);
    }

    /**
     * GET /api/instructors/{instructorId}/picture or /api/instructor/{instructorId}/picture
     * Public endpoint to render an instructor's avatar on course pages and instructor cards.
     */
    @GetMapping({"/{instructorId:[0-9]+}/picture", "/{instructorId:[0-9]+}/avatar"})
    public ResponseEntity<Resource> getInstructorProfilePicture(@PathVariable Integer instructorId) {
        assertMediaServiceAvailable();
        Resource resource = instructorMediaService.getProfilePicture(instructorId);
        return serveImageResource(resource);
    }

    /**
     * DELETE /api/instructor/profile/picture
     * Authenticated instructor deletes their profile picture.
     */
    @DeleteMapping("/profile/picture")
    @PreAuthorize("hasRole('instructor')")
    public ResponseEntity<ApiResponse> deleteProfilePicture(Authentication authentication) {
        assertMediaServiceAvailable();
        User currentUser = instructorProfileService.resolveUser(authentication);
        ApiResponse response = instructorMediaService.deleteProfilePicture(currentUser);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // FR-I04: CREDENTIAL DOCUMENTS MANAGEMENT
    // =========================================================================

    /**
     * POST /api/instructor/credentials
     * Authenticated instructor uploads a credential document (PDF or image).
     */
    @PostMapping(value = "/credentials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('instructor')")
    public ResponseEntity<InstructorCredentialResponse> uploadCredential(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "documentType", required = false) String documentType) {
        assertMediaServiceAvailable();
        User currentUser = instructorProfileService.resolveUser(authentication);
        InstructorCredentialResponse response = instructorMediaService.uploadCredential(currentUser, file, documentType);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/credentials
     * Authenticated instructor lists all their active credentials.
     */
    @GetMapping("/credentials")
    @PreAuthorize("hasRole('instructor')")
    public ResponseEntity<List<InstructorCredentialResponse>> getCredentials(Authentication authentication) {
        assertMediaServiceAvailable();
        User currentUser = instructorProfileService.resolveUser(authentication);
        List<InstructorCredentialResponse> response = instructorMediaService.getCredentials(currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/credentials/{credentialId}
     * Authenticated instructor (or admin) views metadata of a specific credential.
     */
    @GetMapping("/credentials/{credentialId:[0-9]+}")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<InstructorCredentialResponse> getCredential(
            Authentication authentication,
            @PathVariable Integer credentialId) {
        assertMediaServiceAvailable();
        User currentUser = instructorProfileService.resolveUser(authentication);
        InstructorCredentialResponse response = instructorMediaService.getCredential(currentUser, credentialId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/credentials/{credentialId}/download
     * Secure download of private credential file. Only owning instructor or admin can download.
     */
    @GetMapping("/credentials/{credentialId:[0-9]+}/download")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<Resource> downloadCredential(
            Authentication authentication,
            @PathVariable Integer credentialId) {
        assertMediaServiceAvailable();
        User currentUser = instructorProfileService.resolveUser(authentication);
        InstructorCredentialResponse metadata = instructorMediaService.getCredential(currentUser, credentialId);
        Resource resource = instructorMediaService.downloadCredential(currentUser, credentialId);

        String contentType = metadata.getContentType() != null ? metadata.getContentType() : "application/octet-stream";
        String contentDisposition = "attachment; filename=\"" + metadata.getOriginalFileName() + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }

    /**
     * PUT /api/instructor/credentials/{credentialId}
     * Replace an existing credential document with a new file.
     */
    @PutMapping(value = "/credentials/{credentialId:[0-9]+}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('instructor')")
    public ResponseEntity<InstructorCredentialResponse> replaceCredential(
            Authentication authentication,
            @PathVariable Integer credentialId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "documentType", required = false) String documentType) {
        assertMediaServiceAvailable();
        User currentUser = instructorProfileService.resolveUser(authentication);
        InstructorCredentialResponse response = instructorMediaService.replaceCredential(currentUser, credentialId, file, documentType);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/instructor/credentials/{credentialId}
     * Authenticated instructor soft-deletes/revokes a credential document.
     */
    @DeleteMapping("/credentials/{credentialId:[0-9]+}")
    @PreAuthorize("hasRole('instructor')")
    public ResponseEntity<ApiResponse> deleteCredential(
            Authentication authentication,
            @PathVariable Integer credentialId) {
        assertMediaServiceAvailable();
        User currentUser = instructorProfileService.resolveUser(authentication);
        ApiResponse response = instructorMediaService.deleteCredential(currentUser, credentialId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // FR-I04: UPLOAD HISTORY AUDIT TRAIL
    // =========================================================================

    /**
     * GET /api/instructor/upload-history or /api/instructor/credentials/history
     * Returns the upload, replacement, and deletion audit history for the authenticated instructor.
     */
    @GetMapping({"/upload-history", "/credentials/history"})
    @PreAuthorize("hasRole('instructor')")
    public ResponseEntity<List<UploadHistoryResponse>> getUploadHistory(Authentication authentication) {
        assertMediaServiceAvailable();
        User currentUser = instructorProfileService.resolveUser(authentication);
        List<UploadHistoryResponse> response = instructorMediaService.getUploadHistory(currentUser);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void assertMediaServiceAvailable() {
        if (this.instructorMediaService == null) {
            throw new IllegalStateException("InstructorMediaService is not configured");
        }
    }

    private ResponseEntity<Resource> serveImageResource(Resource resource) {
        String filename = resource.getFilename();
        String contentType = URLConnection.guessContentTypeFromName(filename);
        if (contentType == null) {
            if (filename != null && filename.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (filename != null && filename.toLowerCase().endsWith(".webp")) {
                contentType = "image/webp";
            } else {
                contentType = "image/jpeg";
            }
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
