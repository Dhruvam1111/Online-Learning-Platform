package com.olp.event;

import com.olp.entity.VideoUpload;
import com.olp.entity.VideoUploadStatus;
import org.springframework.context.ApplicationEvent;

/**
 * Spring ApplicationEvent dispatched when a video upload status transitions (FR-I15).
 * Enables the in-app notification engine (FR-I70) and audit systems to react to READY and FAILED events.
 */
public class VideoProcessingStatusEvent extends ApplicationEvent {

    private final VideoUpload videoUpload;
    private final VideoUploadStatus oldStatus;
    private final VideoUploadStatus newStatus;
    private final String failureReason;

    public VideoProcessingStatusEvent(Object source, VideoUpload videoUpload, VideoUploadStatus oldStatus, VideoUploadStatus newStatus, String failureReason) {
        super(source);
        this.videoUpload = videoUpload;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.failureReason = failureReason;
    }

    public VideoUpload getVideoUpload() {
        return videoUpload;
    }

    public VideoUploadStatus getOldStatus() {
        return oldStatus;
    }

    public VideoUploadStatus getNewStatus() {
        return newStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
