package com.olp.service;

import com.olp.entity.Lesson;
import com.olp.entity.VideoUpload;
import com.olp.entity.VideoUploadStatus;
import com.olp.event.VideoProcessingStatusEvent;
import com.olp.repository.LessonRepository;
import com.olp.repository.VideoUploadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

/**
 * Service managing video transcoding jobs producing HLS and DASH renditions (NFR-26, FR-I15).
 */
@Service
public class VideoTranscodeService {

    private static final Logger logger = LoggerFactory.getLogger(VideoTranscodeService.class);

    private final VideoUploadRepository videoUploadRepository;
    private final LessonRepository lessonRepository;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;

    public VideoTranscodeService(
            VideoUploadRepository videoUploadRepository,
            LessonRepository lessonRepository,
            EmailService emailService,
            ApplicationEventPublisher eventPublisher) {
        this.videoUploadRepository = videoUploadRepository;
        this.lessonRepository = lessonRepository;
        this.emailService = emailService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Enqueues a video transcoding job to produce adaptive bitrate streams (HLS/DASH).
     */
    @Async
    @Transactional
    public CompletableFuture<VideoUpload> enqueueTranscode(String uploadId) {
        logger.info("Starting transcoding job for upload: {}", uploadId);

        VideoUpload upload = videoUploadRepository.findById(uploadId).orElse(null);
        if (upload == null) {
            logger.warn("Video upload with id {} not found for transcoding", uploadId);
            return CompletableFuture.completedFuture(null);
        }

        VideoUploadStatus oldStatus = upload.getStatus();
        upload.setStatus(VideoUploadStatus.TRANSCODING);
        videoUploadRepository.save(upload);

        try {
            // Emulate transcoding pipeline producing HLS (.m3u8) and DASH (.mpd) manifests
            String hlsManifest = "/media/hls/" + uploadId + "/master.m3u8";
            String dashManifest = "/media/dash/" + uploadId + "/manifest.mpd";

            upload.setHlsUrl(hlsManifest);
            upload.setDashUrl(dashManifest);
            upload.setStatus(VideoUploadStatus.READY);

            if (upload.getLesson() != null) {
                Lesson lesson = upload.getLesson();
                lesson.setContentUrl(hlsManifest);
                lessonRepository.save(lesson);
            }

            logger.info("Transcoding complete for upload {}. HLS: {}, DASH: {}", uploadId, hlsManifest, dashManifest);
            notifyInstructor(upload, VideoUploadStatus.READY, null);
            eventPublisher.publishEvent(new VideoProcessingStatusEvent(this, upload, oldStatus, VideoUploadStatus.READY, null));
        } catch (Exception e) {
            logger.error("Transcoding failed for upload {}: {}", uploadId, e.getMessage());
            upload.setStatus(VideoUploadStatus.FAILED);
            upload.setFailureReason("Transcoding failed: " + e.getMessage());
            notifyInstructor(upload, VideoUploadStatus.FAILED, upload.getFailureReason());
            eventPublisher.publishEvent(new VideoProcessingStatusEvent(this, upload, oldStatus, VideoUploadStatus.FAILED, upload.getFailureReason()));
        }

        VideoUpload saved = videoUploadRepository.save(upload);
        return CompletableFuture.completedFuture(saved);
    }

    private void notifyInstructor(VideoUpload upload, VideoUploadStatus status, String reason) {
        if (upload.getUploader() != null && upload.getUploader().getEmail() != null) {
            emailService.sendVideoProcessingStatusEmail(
                    upload.getUploader().getEmail(),
                    upload.getUploader().getName(),
                    upload.getFilename(),
                    upload.getLesson() != null ? upload.getLesson().getTitle() : null,
                    status.name(),
                    reason
            );
        }
    }
}

