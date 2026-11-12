package com.olp.dto;

public class LectureSearchResult {
    private Long lectureId;
    private String title;
    private String videoUrl;
    private Integer orderIndex;
    private Long courseId;
    private String courseTitle;

    public LectureSearchResult(Long lectureId, String title, String videoUrl, Integer orderIndex, Long courseId, String courseTitle) {
        this.lectureId = lectureId;
        this.title = title;
        this.videoUrl = videoUrl;
        this.orderIndex = orderIndex;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
    }

    public Long getLectureId() { return lectureId; }
    public String getTitle() { return title; }
    public String getVideoUrl() { return videoUrl; }
    public Integer getOrderIndex() { return orderIndex; }
    public Long getCourseId() { return courseId; }
    public String getCourseTitle() { return courseTitle; }
}