package com.olp.dto;

import com.olp.entity.VideoUploadStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload delivered by the video transcoding engine via webhook (FR-I15).
 */
public class TranscoderWebhookRequest {

    @NotBlank(message = "uploadId is required")
    private String uploadId;

    @NotNull(message = "status is required")
    private VideoUploadStatus status;

    private String hlsUrl;
    private String dashUrl;
    private String failureReason;
    private String eventId;

    public TranscoderWebhookRequest() {}

    public TranscoderWebhookRequest(String uploadId, VideoUploadStatus status, String hlsUrl, String dashUrl, String failureReason) {
        this.uploadId = uploadId;
        this.status = status;
        this.hlsUrl = hlsUrl;
        this.dashUrl = dashUrl;
        this.failureReason = failureReason;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public VideoUploadStatus getStatus() {
        return status;
    }

    public void setStatus(VideoUploadStatus status) {
        this.status = status;
    }

    public String getHlsUrl() {
        return hlsUrl;
    }

    public void setHlsUrl(String hlsUrl) {
        this.hlsUrl = hlsUrl;
    }

    public String getDashUrl() {
        return dashUrl;
    }

    public void setDashUrl(String dashUrl) {
        this.dashUrl = dashUrl;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}
