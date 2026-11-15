package com.olp.controller;

import com.olp.dto.InitiateVideoUploadRequest;
import com.olp.dto.VideoChunkResponse;
import com.olp.dto.VideoUploadResponse;
import com.olp.entity.User;
import com.olp.service.VideoUploadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for chunked, resumable video lecture uploads (FR-I13, Step 13).
 */
@RestController
@RequestMapping({"/api/instructor/videos", "/api/instructors/videos"})
@PreAuthorize("hasAnyRole('instructor', 'admin')")
public class VideoUploadController {

    private final VideoUploadService videoUploadService;

    @Autowired
    public VideoUploadController(VideoUploadService videoUploadService) {
        this.videoUploadService = videoUploadService;
    }

    /**
     * POST /api/instructor/videos/initiate
     * Start a new chunked upload session for a video lesson.
     */
    @PostMapping("/initiate")
    public ResponseEntity<VideoUploadResponse> initiateUpload(
            Authentication authentication,
            @Valid @RequestBody InitiateVideoUploadRequest request) {
        User user = videoUploadService.resolveUser(authentication);
        VideoUploadResponse response = videoUploadService.initiateUpload(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/instructor/videos/{uploadId}/chunks
     * Upload an individual chunk of the video payload.
     */
    @PostMapping(value = "/{uploadId}/chunks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoChunkResponse> uploadChunk(
            @PathVariable String uploadId,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        User user = videoUploadService.resolveUser(authentication);
        VideoChunkResponse response = videoUploadService.uploadChunk(user, uploadId, chunkIndex, file);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/videos/{uploadId}/resume
     * Inquire upload resumption state (which chunk index to transmit next).
     */
    @GetMapping("/{uploadId}/resume")
    public ResponseEntity<VideoChunkResponse> resumeUpload(
            @PathVariable String uploadId,
            Authentication authentication) {
        User user = videoUploadService.resolveUser(authentication);
        VideoChunkResponse response = videoUploadService.resumeUpload(user, uploadId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/videos/{uploadId}/status
     * Inspect current upload, assembly, and transcoding progress.
     */
    @GetMapping("/{uploadId}/status")
    public ResponseEntity<VideoUploadResponse> getUploadStatus(
            @PathVariable String uploadId,
            Authentication authentication) {
        User user = videoUploadService.resolveUser(authentication);
        VideoUploadResponse response = videoUploadService.getUploadStatus(user, uploadId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/videos/lesson/{lessonId}
     * GET /api/instructor/videos/lesson/{lessonId}/status
     * Retrieve active video upload details and processing status attached to a lesson.
     */
    @GetMapping({"/lesson/{lessonId}", "/lesson/{lessonId}/status"})
    public ResponseEntity<VideoUploadResponse> getVideoByLesson(
            @PathVariable Integer lessonId,
            Authentication authentication) {
        User user = videoUploadService.resolveUser(authentication);
        VideoUploadResponse response = videoUploadService.getVideoByLesson(user, lessonId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/instructor/videos/{uploadId}/retry
     * POST /api/instructor/videos/{uploadId}/reupload
     * Re-uploads a video after a failure, creating a fresh upload session job (FR-I15).
     */
    @PostMapping({"/{uploadId}/retry", "/{uploadId}/reupload"})
    public ResponseEntity<VideoUploadResponse> retryUploadAfterFailure(
            @PathVariable String uploadId,
            @RequestBody(required = false) InitiateVideoUploadRequest request,
            Authentication authentication) {
        User user = videoUploadService.resolveUser(authentication);
        VideoUploadResponse response = videoUploadService.retryUploadAfterFailure(user, uploadId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

