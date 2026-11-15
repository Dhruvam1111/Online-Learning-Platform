package com.olp.service;

import com.olp.dto.ApiResponse;
import com.olp.dto.InstructorCredentialResponse;
import com.olp.dto.ProfilePictureResponse;
import com.olp.dto.UploadHistoryResponse;
import com.olp.entity.InstructorCredential;
import com.olp.entity.InstructorProfile;
import com.olp.entity.InstructorUploadHistory;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.InstructorCredentialRepository;
import com.olp.repository.InstructorProfileRepository;
import com.olp.repository.InstructorUploadHistoryRepository;
import com.olp.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class InstructorMediaService {

    private final UserRepository userRepository;
    private final InstructorProfileRepository instructorProfileRepository;
    private final InstructorCredentialRepository instructorCredentialRepository;
    private final InstructorUploadHistoryRepository instructorUploadHistoryRepository;
    private final FileStorageService fileStorageService;

    public InstructorMediaService(
            UserRepository userRepository,
            InstructorProfileRepository instructorProfileRepository,
            InstructorCredentialRepository instructorCredentialRepository,
            InstructorUploadHistoryRepository instructorUploadHistoryRepository,
            FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.instructorProfileRepository = instructorProfileRepository;
        this.instructorCredentialRepository = instructorCredentialRepository;
        this.instructorUploadHistoryRepository = instructorUploadHistoryRepository;
        this.fileStorageService = fileStorageService;
    }

    // =========================================================================
    // 1. PROFILE PICTURE MANAGEMENT
    // =========================================================================

    /**
     * Upload or replace an instructor's profile picture.
     * Enforces file size (max 5 MB), image type (JPEG/PNG/WEBP), and stores to file storage.
     * Records upload/replacement history.
     */
    @Transactional
    public ProfilePictureResponse uploadProfilePicture(User currentUser, MultipartFile file) {
        validateInstructorUser(currentUser);

        User instructor = userRepository.findById(Objects.requireNonNull(currentUser.getId()))
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        InstructorProfile profile = instructorProfileRepository.findByInstructorId(Objects.requireNonNull(instructor.getId()))
                .orElseGet(() -> {
                    InstructorProfile newProfile = new InstructorProfile();
                    newProfile.setInstructor(instructor);
                    newProfile.setCreatedAt(LocalDateTime.now());
                    newProfile.setUpdatedAt(LocalDateTime.now());
                    return newProfile;
                });

        String previousPicturePath = profile.getProfilePictureUrl();
        boolean isReplacement = previousPicturePath != null && !previousPicturePath.isBlank();

        // 1. Store the new profile picture on disk
        String storedRelativePath = fileStorageService.storeProfilePicture(instructor.getId(), file);

        // 2. Delete old physical file if replacing
        if (isReplacement && previousPicturePath.startsWith("profile-pictures/")) {
            fileStorageService.deleteFile(previousPicturePath);
        }

        // 3. Update profile entity
        profile.setProfilePictureUrl(storedRelativePath);
        profile.setUpdatedAt(LocalDateTime.now());
        instructorProfileRepository.save(profile);

        // 4. Record upload/replacement in upload history
        String action = isReplacement ? "REPLACED" : "UPLOADED";
        String details = isReplacement
                ? "Replaced previous profile picture with " + file.getOriginalFilename()
                : "Uploaded initial profile picture";

        InstructorUploadHistory history = new InstructorUploadHistory(
                instructor,
                "PROFILE_PICTURE",
                action,
                file.getOriginalFilename(),
                storedRelativePath,
                file.getContentType(),
                file.getSize(),
                details
        );
        instructorUploadHistoryRepository.save(history);

        String publicUrl = "/api/instructors/" + instructor.getId() + "/picture";
        return new ProfilePictureResponse(
                instructor.getId(),
                publicUrl,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                LocalDateTime.now(),
                isReplacement ? "Profile picture replaced successfully" : "Profile picture uploaded successfully"
        );
    }

    /**
     * Load the profile picture resource for the given instructor ID.
     */
    @Transactional(readOnly = true)
    public Resource getProfilePicture(Integer instructorId) {
        if (instructorId == null) {
            throw new IllegalArgumentException("Instructor ID cannot be null");
        }

        InstructorProfile profile = instructorProfileRepository.findByInstructorId(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor profile not found for id: " + instructorId));

        if (profile.getProfilePictureUrl() == null || profile.getProfilePictureUrl().isBlank()) {
            throw new ResourceNotFoundException("No profile picture uploaded for instructor id: " + instructorId);
        }

        return fileStorageService.loadFileAsResource(profile.getProfilePictureUrl());
    }

    /**
     * Delete the instructor's profile picture, remove physical file, and record audit log.
     */
    @Transactional
    public ApiResponse deleteProfilePicture(User currentUser) {
        validateInstructorUser(currentUser);

        InstructorProfile profile = instructorProfileRepository.findByInstructorId(Objects.requireNonNull(currentUser.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Instructor profile not found"));

        if (profile.getProfilePictureUrl() == null || profile.getProfilePictureUrl().isBlank()) {
            return new ApiResponse(true, "No profile picture to delete");
        }

        String existingPath = profile.getProfilePictureUrl();

        // 1. Delete physical file
        if (existingPath.startsWith("profile-pictures/")) {
            fileStorageService.deleteFile(existingPath);
        }

        // 2. Clear profile picture URL
        profile.setProfilePictureUrl(null);
        profile.setUpdatedAt(LocalDateTime.now());
        instructorProfileRepository.save(profile);

        // 3. Record deletion in upload history
        InstructorUploadHistory history = new InstructorUploadHistory(
                currentUser,
                "PROFILE_PICTURE",
                "DELETED",
                existingPath.substring(existingPath.lastIndexOf("/") + 1),
                existingPath,
                "image/*",
                0L,
                "Profile picture deleted by instructor"
        );
        instructorUploadHistoryRepository.save(history);

        return new ApiResponse(true, "Profile picture deleted successfully");
    }

    // =========================================================================
    // 2. CREDENTIAL DOCUMENTS MANAGEMENT
    // =========================================================================

    /**
     * Upload a credential document (degrees, certificates, identity) supporting PDF and images.
     * Enforces file size (max 10 MB) and private secure storage.
     */
    @Transactional
    public InstructorCredentialResponse uploadCredential(User currentUser, MultipartFile file, String documentType) {
        validateInstructorUser(currentUser);

        User instructor = userRepository.findById(Objects.requireNonNull(currentUser.getId()))
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        String docType = (documentType != null && !documentType.isBlank())
                ? documentType.trim().toUpperCase()
                : "CREDENTIAL";

        // 1. Store document privately on disk
        String storedPath = fileStorageService.storeCredential(instructor.getId(), file);

        // 2. Create credential record
        InstructorCredential credential = new InstructorCredential(
                instructor,
                file.getOriginalFilename(),
                storedPath,
                file.getContentType(),
                file.getSize(),
                docType,
                "ACTIVE"
        );
        credential = instructorCredentialRepository.save(credential);

        // 3. Record upload history
        InstructorUploadHistory history = new InstructorUploadHistory(
                instructor,
                "CREDENTIAL",
                "UPLOADED",
                file.getOriginalFilename(),
                storedPath,
                file.getContentType(),
                file.getSize(),
                "Uploaded credential document: " + docType
        );
        instructorUploadHistoryRepository.save(history);

        return toCredentialResponse(credential);
    }

    /**
     * Get all active credentials uploaded by the authenticated instructor.
     */
    @Transactional(readOnly = true)
    public List<InstructorCredentialResponse> getCredentials(User currentUser) {
        validateInstructorUser(currentUser);

        List<InstructorCredential> credentials = instructorCredentialRepository
                .findByInstructorIdAndStatus(Objects.requireNonNull(currentUser.getId()), "ACTIVE");

        return credentials.stream()
                .map(this::toCredentialResponse)
                .toList();
    }

    /**
     * Get a specific credential document metadata by ID.
     * Authorized only for the owner instructor or an admin.
     */
    @Transactional(readOnly = true)
    public InstructorCredentialResponse getCredential(User currentUser, Integer credentialId) {
        if (credentialId == null) {
            throw new IllegalArgumentException("Credential ID cannot be null");
        }
        InstructorCredential credential = instructorCredentialRepository.findById(Objects.requireNonNull(credentialId))
                .orElseThrow(() -> new ResourceNotFoundException("Credential not found with id: " + credentialId));

        assertCanAccessCredential(currentUser, credential);
        return toCredentialResponse(credential);
    }

    /**
     * Download / stream a private credential document.
     * Secure access check: Only the owning instructor or authorized admin can access.
     */
    @Transactional(readOnly = true)
    public Resource downloadCredential(User currentUser, Integer credentialId) {
        if (credentialId == null) {
            throw new IllegalArgumentException("Credential ID cannot be null");
        }
        InstructorCredential credential = instructorCredentialRepository.findById(Objects.requireNonNull(credentialId))
                .orElseThrow(() -> new ResourceNotFoundException("Credential not found with id: " + credentialId));

        assertCanAccessCredential(currentUser, credential);
        return fileStorageService.loadFileAsResource(credential.getStoredFileName());
    }

    /**
     * Replace an existing credential document with an updated file.
     * Instructors can only replace their own credentials.
     */
    @Transactional
    public InstructorCredentialResponse replaceCredential(User currentUser, Integer credentialId,
                                                          MultipartFile file, String documentType) {
        validateInstructorUser(currentUser);
        if (credentialId == null) {
            throw new IllegalArgumentException("Credential ID cannot be null");
        }

        InstructorCredential credential = instructorCredentialRepository.findById(Objects.requireNonNull(credentialId))
                .orElseThrow(() -> new ResourceNotFoundException("Credential not found with id: " + credentialId));

        if (!credential.getInstructor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Cannot replace another instructor's credential");
        }

        String oldStoredPath = credential.getStoredFileName();
        String oldFileName = credential.getOriginalFileName();

        // 1. Store new file
        String newStoredPath = fileStorageService.storeCredential(currentUser.getId(), file);

        // 2. Delete old physical file
        fileStorageService.deleteFile(oldStoredPath);

        // 3. Update credential entity
        credential.setOriginalFileName(file.getOriginalFilename());
        credential.setStoredFileName(newStoredPath);
        credential.setContentType(file.getContentType());
        credential.setFileSize(file.getSize());
        if (documentType != null && !documentType.isBlank()) {
            credential.setDocumentType(documentType.trim().toUpperCase());
        }
        credential.setUpdatedAt(LocalDateTime.now());
        credential = instructorCredentialRepository.save(credential);

        // 4. Record replacement in upload history
        InstructorUploadHistory history = new InstructorUploadHistory(
                currentUser,
                "CREDENTIAL",
                "REPLACED",
                file.getOriginalFilename(),
                newStoredPath,
                file.getContentType(),
                file.getSize(),
                "Replaced previous document (" + oldFileName + ") with " + file.getOriginalFilename()
        );
        instructorUploadHistoryRepository.save(history);

        return toCredentialResponse(credential);
    }

    /**
     * Delete / revoke a credential document.
     * Instructors can only delete their own credentials.
     */
    @Transactional
    public ApiResponse deleteCredential(User currentUser, Integer credentialId) {
        validateInstructorUser(currentUser);
        if (credentialId == null) {
            throw new IllegalArgumentException("Credential ID cannot be null");
        }

        InstructorCredential credential = instructorCredentialRepository.findById(Objects.requireNonNull(credentialId))
                .orElseThrow(() -> new ResourceNotFoundException("Credential not found with id: " + credentialId));

        if (!credential.getInstructor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Cannot delete another instructor's credential");
        }

        // 1. Remove physical file
        fileStorageService.deleteFile(credential.getStoredFileName());

        // 2. Mark status as DELETED
        credential.setStatus("DELETED");
        credential.setUpdatedAt(LocalDateTime.now());
        instructorCredentialRepository.save(credential);

        // 3. Record deletion in upload history
        InstructorUploadHistory history = new InstructorUploadHistory(
                currentUser,
                "CREDENTIAL",
                "DELETED",
                credential.getOriginalFileName(),
                credential.getStoredFileName(),
                credential.getContentType(),
                credential.getFileSize(),
                "Credential document marked as DELETED"
        );
        instructorUploadHistoryRepository.save(history);

        return new ApiResponse(true, "Credential document deleted successfully");
    }

    // =========================================================================
    // 3. UPLOAD HISTORY MANAGEMENT
    // =========================================================================

    /**
     * Retrieve upload history for the authenticated instructor.
     */
    @Transactional(readOnly = true)
    public List<UploadHistoryResponse> getUploadHistory(User currentUser) {
        validateInstructorUser(currentUser);

        List<InstructorUploadHistory> historyList = instructorUploadHistoryRepository
                .findByInstructorIdOrderByCreatedAtDesc(Objects.requireNonNull(currentUser.getId()));

        return historyList.stream()
                .map(h -> new UploadHistoryResponse(
                        h.getId(),
                        h.getInstructor().getId(),
                        h.getCategory(),
                        h.getAction(),
                        h.getOriginalFileName(),
                        h.getContentType(),
                        h.getFileSize(),
                        h.getDetails(),
                        h.getCreatedAt()
                ))
                .toList();
    }

    // =========================================================================
    // 4. ADMIN ACCESS (Vetting & Verification)
    // =========================================================================

    /**
     * Admin lists credentials for an instructor to verify credentials.
     */
    @Transactional(readOnly = true)
    public List<InstructorCredentialResponse> getCredentialsForAdmin(Integer instructorId) {
        if (instructorId == null) {
            throw new IllegalArgumentException("Instructor ID cannot be null");
        }
        return instructorCredentialRepository.findByInstructorIdAndStatus(Objects.requireNonNull(instructorId), "ACTIVE")
                .stream()
                .map(this::toCredentialResponse)
                .toList();
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void validateInstructorUser(User user) {
        if (user == null || user.getId() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (!"instructor".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Access denied: only instructors can access this feature");
        }
    }

    private void assertCanAccessCredential(User user, InstructorCredential credential) {
        if (user == null || user.getId() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        boolean isOwner = credential.getInstructor() != null && Objects.equals(credential.getInstructor().getId(), user.getId());
        boolean isAdmin = "admin".equalsIgnoreCase(user.getRole());

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Access denied: credential document is private to the owner and administrators");
        }
    }

    public InstructorCredentialResponse toCredentialResponse(InstructorCredential credential) {
        String downloadUrl = "/api/instructor/credentials/" + credential.getId() + "/download";
        return new InstructorCredentialResponse(
                credential.getId(),
                credential.getInstructor().getId(),
                credential.getOriginalFileName(),
                credential.getContentType(),
                credential.getFileSize(),
                credential.getDocumentType(),
                credential.getStatus(),
                downloadUrl,
                credential.getUploadedAt(),
                credential.getUpdatedAt()
        );
    }
}

