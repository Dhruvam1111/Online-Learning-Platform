package com.olp.dto;

import jakarta.validation.constraints.Size;

public class UpdateLectureRequest {

    @Size(max = 200, message = "Title must be 200 characters or fewer")
    private String title;

    private String youtubeUrl;

    private String videoUrl;

    private String description;

    private Integer orderIndex;

    private Integer position;

    public UpdateLectureRequest() {}

    public UpdateLectureRequest(String title, String youtubeUrl) {
        this.title = title;
        this.youtubeUrl = youtubeUrl;
    }

    public UpdateLectureRequest(String title, String youtubeUrl, Integer orderIndex) {
        this.title = title;
        this.youtubeUrl = youtubeUrl;
        this.orderIndex = orderIndex;
    }

    public UpdateLectureRequest(String title, String description, String youtubeUrl, Integer orderIndex) {
        this.title = title;
        this.description = description;
        this.youtubeUrl = youtubeUrl;
        this.orderIndex = orderIndex;
    }

    public String getEffectiveYoutubeUrl() {
        if (youtubeUrl != null && !youtubeUrl.isBlank()) {
            return youtubeUrl.trim();
        }
        if (videoUrl != null && !videoUrl.isBlank()) {
            return videoUrl.trim();
        }
        return null;
    }

    public Integer getEffectiveOrderIndex() {
        return orderIndex != null ? orderIndex : position;
    }

    // ===== Getters and Setters =====

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public void setYoutubeUrl(String youtubeUrl) {
        this.youtubeUrl = youtubeUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }
}
