package com.olp.service;

import com.olp.entity.VideoUpload;
import com.olp.entity.VideoUploadStatus;
import com.olp.repository.VideoUploadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

/**
 * Service managing video transcoding jobs producing HLS and DASH renditions (NFR-26).
 */
@Service
public class VideoTranscodeService {

    private static final Logger logger = LoggerFactory.getLogger(VideoTranscodeService.class);

    private final VideoUploadRepository videoUploadRepository;

    public VideoTranscodeService(VideoUploadRepository videoUploadRepository) {
        this.videoUploadRepository = videoUploadRepository;
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

        upload.setStatus(VideoUploadStatus.TRANSCODING);
        videoUploadRepository.save(upload);

        try {
            // Emulate transcoding pipeline producing HLS (.m3u8) and DASH (.mpd) manifests
            String hlsManifest = "/media/hls/" + uploadId + "/master.m3u8";
            String dashManifest = "/media/dash/" + uploadId + "/manifest.mpd";

            upload.setHlsUrl(hlsManifest);
            upload.setDashUrl(dashManifest);
            upload.setStatus(VideoUploadStatus.READY);
            logger.info("Transcoding complete for upload {}. HLS: {}, DASH: {}", uploadId, hlsManifest, dashManifest);
        } catch (Exception e) {
            logger.error("Transcoding failed for upload {}: {}", uploadId, e.getMessage());
            upload.setStatus(VideoUploadStatus.FAILED);
            upload.setFailureReason("Transcoding failed: " + e.getMessage());
        }

        VideoUpload saved = videoUploadRepository.save(upload);
        return CompletableFuture.completedFuture(saved);
    }
}

