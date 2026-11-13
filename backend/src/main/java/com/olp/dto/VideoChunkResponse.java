package com.olp.dto;

import com.olp.entity.VideoUploadStatus;

public class VideoChunkResponse {

    private String uploadId;
    private Integer chunkIndex;
    private Long receivedBytes;
    private Long totalBytes;
    private Double progressPercent;
    private VideoUploadStatus status;
    private boolean completed;
    private Integer nextExpectedChunkIndex;

    public VideoChunkResponse() {}

    public VideoChunkResponse(String uploadId, Integer chunkIndex, Long receivedBytes, Long totalBytes,
                              Double progressPercent, VideoUploadStatus status, boolean completed,
                              Integer nextExpectedChunkIndex) {
        this.uploadId = uploadId;
        this.chunkIndex = chunkIndex;
        this.receivedBytes = receivedBytes;
        this.totalBytes = totalBytes;
        this.progressPercent = progressPercent;
        this.status = status;
        this.completed = completed;
        this.nextExpectedChunkIndex = nextExpectedChunkIndex;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public Long getReceivedBytes() {
        return receivedBytes;
    }

    public void setReceivedBytes(Long receivedBytes) {
        this.receivedBytes = receivedBytes;
    }

    public Long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(Long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public Double getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Double progressPercent) {
        this.progressPercent = progressPercent;
    }

    public VideoUploadStatus getStatus() {
        return status;
    }

    public void setStatus(VideoUploadStatus status) {
        this.status = status;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Integer getNextExpectedChunkIndex() {
        return nextExpectedChunkIndex;
    }

    public void setNextExpectedChunkIndex(Integer nextExpectedChunkIndex) {
        this.nextExpectedChunkIndex = nextExpectedChunkIndex;
    }
}

