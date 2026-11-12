package com.olp.dto;

public class LectureMaterialResponse {
    private Long id;
    private Long lectureId;
    private String title;
    private String fileUrl;
    private String fileType;
    private Boolean isDownloadable;

    public LectureMaterialResponse(Long id, Long lectureId, String title, String fileUrl, String fileType, Boolean isDownloadable) {
        this.id = id;
        this.lectureId = lectureId;
        this.title = title;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.isDownloadable = isDownloadable;
    }

    public Long getId() { return id; }
    public Long getLectureId() { return lectureId; }
    public String getTitle() { return title; }
    public String getFileUrl() { return fileUrl; }
    public String getFileType() { return fileType; }
    public Boolean getIsDownloadable() { return isDownloadable; }
}