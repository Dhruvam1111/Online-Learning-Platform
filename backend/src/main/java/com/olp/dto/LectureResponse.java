package com.olp.dto;

import com.olp.entity.Lecture;
import com.olp.util.YoutubeUtil;

import java.time.LocalDateTime;

public class LectureResponse {

    private Integer id;
    private Integer courseId;
    private String title;
    private String youtubeUrl;
    private String videoUrl;
    private String videoId;
    private String embedUrl;
    private String thumbnailUrl;
    private Integer orderIndex;
    private Integer position;
    private LocalDateTime createdAt;

    public LectureResponse() {}

    public LectureResponse(Integer id, Integer courseId, String title, String youtubeUrl,
                           String videoId, String embedUrl, String thumbnailUrl,
                           Integer orderIndex, LocalDateTime createdAt) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.youtubeUrl = youtubeUrl;
        this.videoUrl = youtubeUrl;
        this.videoId = videoId;
        this.embedUrl = embedUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.orderIndex = orderIndex;
        this.position = orderIndex;
        this.createdAt = createdAt;
    }

    public static LectureResponse fromEntity(Lecture lecture) {
        if (lecture == null) {
            return null;
        }
        String videoId = lecture.getVideoId();
        return new LectureResponse(
                lecture.getId(),
                lecture.getCourse() != null ? lecture.getCourse().getId() : null,
                lecture.getTitle(),
                lecture.getYoutubeUrl(),
                videoId,
                YoutubeUtil.buildEmbedUrl(videoId),
                YoutubeUtil.buildThumbnailUrl(videoId),
                lecture.getOrderIndex(),
                lecture.getCreatedAt()
        );
    }

    // ===== Getters and Setters =====

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

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
        this.videoUrl = youtubeUrl;
    }

    public String getVideoUrl() {
        return videoUrl != null ? videoUrl : youtubeUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
        this.youtubeUrl = videoUrl;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getEmbedUrl() {
        return embedUrl;
    }

    public void setEmbedUrl(String embedUrl) {
        this.embedUrl = embedUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
        this.position = orderIndex;
    }

    public Integer getPosition() {
        return position != null ? position : orderIndex;
    }

    public void setPosition(Integer position) {
        this.position = position;
        this.orderIndex = position;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
