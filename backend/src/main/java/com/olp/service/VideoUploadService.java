package com.olp.service;

import com.olp.dto.InitiateVideoUploadRequest;
import com.olp.dto.VideoChunkResponse;
import com.olp.dto.VideoUploadResponse;
import com.olp.entity.*;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.LessonRepository;
import com.olp.repository.UserRepository;
import com.olp.repository.VideoUploadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service managing chunked, resumable video lecture uploads and lifecycle (FR-I13, Step 13).
 */
@Service
@Transactional
public class VideoUploadService {

    private static final Logger logger = LoggerFactory.getLogger(VideoUploadService.class);

    private final VideoUploadRepository videoUploadRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;
    private final VideoStorageService videoStorageService;
    private final VideoTranscodeService videoTranscodeService;
    private final ContentDeclarationService contentDeclarationService;

    @Autowired
    public VideoUploadService(
            VideoUploadRepository videoUploadRepository,
            LessonRepository lessonRepository,
            CourseRepository courseRepository,
            CourseInstructorRepository courseInstructorRepository,
            UserRepository userRepository,
            VideoStorageService videoStorageService,
            VideoTranscodeService videoTranscodeService,
            ContentDeclarationService contentDeclarationService) {
        this.videoUploadRepository = videoUploadRepository;
        this.lessonRepository = lessonRepository;
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.userRepository = userRepository;
        this.videoStorageService = videoStorageService;
        this.videoTranscodeService = videoTranscodeService;
        this.contentDeclarationService = contentDeclarationService;
    }

    /**
     * Resolves the authenticated User from the security context authentication.
     */
    public User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * Initiate a new chunked upload session for a video lesson.
     */
    public VideoUploadResponse initiateUpload(User instructor, InitiateVideoUploadRequest request) {
        validateInstructorRole(instructor);

        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + request.getLessonId()));

        if (lesson.getLessonType() != LessonType.VIDEO) {
            throw new IllegalArgumentException("Cannot upload video to lesson of type: " + lesson.getLessonType());
        }

        Course course = extractCourseFromLesson(lesson);
        assertInstructorAccess(course, instructor);

        // Gate: enforce content ownership declaration (Step 14 / FR-I64)
        contentDeclarationService.assertDeclared(lesson.getId());

        long chunkSize = request.getChunkSize();
        long totalBytes = request.getTotalBytes();
        int totalChunks = (int) Math.ceil((double) totalBytes / chunkSize);
        if (totalChunks <= 0) {
            totalChunks = 1;
        }

        String uploadId = UUID.randomUUID().toString();
        try {
            videoStorageService.initSession(uploadId);
        } catch (IOException e) {
            logger.error("Failed to initialize upload directory for {}: {}", uploadId, e.getMessage());
            throw new RuntimeException("Could not initialize video upload session", e);
        }

        VideoUpload videoUpload = new VideoUpload(
                uploadId,
                lesson,
                instructor,
                request.getFilename(),
                request.getFileType(),
                totalBytes,
                chunkSize,
                totalChunks
        );

        VideoUpload saved = videoUploadRepository.save(videoUpload);
        return VideoUploadResponse.fromEntity(saved);
    }

    /**
     * Upload an individual chunk and automatically assemble when all chunks arrive.
     */
    public VideoChunkResponse uploadChunk(User instructor, String uploadId, int chunkIndex, MultipartFile file) {
        validateInstructorRole(instructor);

        VideoUpload upload = videoUploadRepository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload session not found with id: " + uploadId));

        Course course = extractCourseFromLesson(upload.getLesson());
        assertInstructorAccess(course, instructor);

        if (upload.getStatus() == VideoUploadStatus.COMPLETED || upload.getStatus() == VideoUploadStatus.READY) {
            throw new IllegalStateException("Upload is already completed for session: " + uploadId);
        }

        if (chunkIndex < 0 || chunkIndex >= upload.getTotalChunks()) {
            throw new IllegalArgumentException(String.format("Invalid chunk index %d for total chunks %d", chunkIndex, upload.getTotalChunks()));
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Chunk payload cannot be empty");
        }

        try {
            videoStorageService.saveChunk(uploadId, chunkIndex, file.getInputStream());
        } catch (IOException e) {
            logger.error("Failed to write chunk {} for upload {}: {}", chunkIndex, uploadId, e.getMessage());
            throw new RuntimeException("Error writing video chunk to disk", e);
        }

        List<Integer> uploadedChunks = videoStorageService.getUploadedChunkIndices(uploadId);
        long receivedBytes = Math.min(upload.getTotalBytes(), (long) uploadedChunks.size() * upload.getChunkSize());
        upload.setReceivedBytes(receivedBytes);
        upload.setStatus(VideoUploadStatus.UPLOADING);

        boolean isCompleted = uploadedChunks.size() == upload.getTotalChunks();

        if (isCompleted) {
            try {
                String finalPath = videoStorageService.assembleChunks(uploadId, upload.getFilename(), upload.getTotalChunks());
                upload.setStoragePath(finalPath);
                upload.setReceivedBytes(upload.getTotalBytes());
                upload.setStatus(VideoUploadStatus.COMPLETED);

                // Update lesson's content URL
                Lesson lesson = upload.getLesson();
                lesson.setContentUrl(finalPath);
                lessonRepository.save(lesson);

                // Enqueue background transcoding job
                videoTranscodeService.enqueueTranscode(uploadId);
            } catch (IOException e) {
                logger.error("Failed to assemble chunks for upload {}: {}", uploadId, e.getMessage());
                upload.setStatus(VideoUploadStatus.FAILED);
                upload.setFailureReason("Failed to assemble uploaded chunks: " + e.getMessage());
            }
        }

        VideoUpload saved = videoUploadRepository.save(upload);

        int nextExpected = findNextMissingChunkIndex(uploadedChunks, upload.getTotalChunks());
        double progress = ((double) saved.getReceivedBytes() / saved.getTotalBytes()) * 100.0;

        return new VideoChunkResponse(
                uploadId,
                chunkIndex,
                saved.getReceivedBytes(),
                saved.getTotalBytes(),
                Math.min(100.0, Math.round(progress * 100.0) / 100.0),
                saved.getStatus(),
                isCompleted,
                nextExpected
        );
    }

    /**
     * Resume an interrupted upload, querying which chunk index is expected next.
     */
    @Transactional(readOnly = true)
    public VideoChunkResponse resumeUpload(User instructor, String uploadId) {
        validateInstructorRole(instructor);

        VideoUpload upload = videoUploadRepository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload session not found with id: " + uploadId));

        Course course = extractCourseFromLesson(upload.getLesson());
        assertInstructorAccess(course, instructor);

        List<Integer> uploadedChunks = videoStorageService.getUploadedChunkIndices(uploadId);
        int nextExpected = findNextMissingChunkIndex(uploadedChunks, upload.getTotalChunks());
        double progress = ((double) upload.getReceivedBytes() / upload.getTotalBytes()) * 100.0;
        boolean isCompleted = uploadedChunks.size() == upload.getTotalChunks();

        return new VideoChunkResponse(
                uploadId,
                nextExpected > 0 ? nextExpected - 1 : 0,
                upload.getReceivedBytes(),
                upload.getTotalBytes(),
                Math.min(100.0, Math.round(progress * 100.0) / 100.0),
                upload.getStatus(),
                isCompleted,
                nextExpected
        );
    }

    /**
     * Get the full status of an upload session.
     */
    @Transactional(readOnly = true)
    public VideoUploadResponse getUploadStatus(User instructor, String uploadId) {
        validateInstructorRole(instructor);

        VideoUpload upload = videoUploadRepository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload session not found with id: " + uploadId));

        Course course = extractCourseFromLesson(upload.getLesson());
        assertInstructorAccess(course, instructor);

        return VideoUploadResponse.fromEntity(upload);
    }

    /**
     * Retrieve current video upload for a lesson.
     */
    @Transactional(readOnly = true)
    public VideoUploadResponse getVideoByLesson(User instructor, Integer lessonId) {
        validateInstructorRole(instructor);

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));

        Course course = extractCourseFromLesson(lesson);
        assertInstructorAccess(course, instructor);

        VideoUpload upload = videoUploadRepository.findFirstByLessonIdOrderByCreatedAtDesc(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("No video upload found for lesson id: " + lessonId));

        return VideoUploadResponse.fromEntity(upload);
    }

    private void validateInstructorRole(User user) {
        if (user == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        if (!"instructor".equalsIgnoreCase(user.getRole()) && !"admin".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only instructors or administrators can manage video uploads");
        }
    }

    private void assertInstructorAccess(Course course, User instructor) {
        if (course == null) {
            throw new ResourceNotFoundException("Associated course not found");
        }
        if (instructor == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        if ("admin".equalsIgnoreCase(instructor.getRole())) {
            return;
        }
        if (course.getInstructor() != null && Objects.equals(course.getInstructor().getId(), instructor.getId())) {
            return;
        }
        if (courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), instructor.getId())) {
            return;
        }
        throw new AccessDeniedException("You are not authorized to upload videos for course: " + course.getTitle());
    }

    private Course extractCourseFromLesson(Lesson lesson) {
        if (lesson.getSection() != null &&
                lesson.getSection().getModule() != null &&
                lesson.getSection().getModule().getCourse() != null) {
            return lesson.getSection().getModule().getCourse();
        }
        throw new ResourceNotFoundException("Lesson hierarchy is missing course association");
    }

    private int findNextMissingChunkIndex(List<Integer> uploaded, int totalChunks) {
        for (int i = 0; i < totalChunks; i++) {
            if (!uploaded.contains(i)) {
                return i;
            }
        }
        return totalChunks;
    }
}

