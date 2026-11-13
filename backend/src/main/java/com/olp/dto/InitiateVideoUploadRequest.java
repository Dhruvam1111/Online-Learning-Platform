package com.olp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class InitiateVideoUploadRequest {

    @NotNull(message = "Lesson ID is required")
    private Integer lessonId;

    @NotBlank(message = "Filename is required")
    private String filename;

    @NotBlank(message = "File type is required")
    private String fileType;

    @NotNull(message = "Total file size in bytes is required")
    @Min(value = 1, message = "Total file size must be at least 1 byte")
    private Long totalBytes;

    @NotNull(message = "Chunk size in bytes is required")
    @Min(value = 1024, message = "Chunk size must be at least 1 KB")
    private Long chunkSize = 5242880L; // default 5 MB

    public InitiateVideoUploadRequest() {}

    public Integer getLessonId() {
        return lessonId;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(Long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public Long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Long chunkSize) {
        this.chunkSize = chunkSize;
    }
}

