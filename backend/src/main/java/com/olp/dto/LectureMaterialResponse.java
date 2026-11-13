package com.olp.dto;

public class LectureMaterialResponse {
    private Long id;
    private Long lectureId;
    private String title;
    private String fileUrl;
    private String fileType;
    private Boolean isDownloadable;
    private String downloadUrl;
    private String viewUrl;
    private Boolean viewOnly;

    public LectureMaterialResponse() {}

    public LectureMaterialResponse(Long id, Long lectureId, String title, String fileUrl, String fileType, Boolean isDownloadable) {
        this.id = id;
        this.lectureId = lectureId;
        this.title = title;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.isDownloadable = isDownloadable != null ? isDownloadable : false;
        this.viewOnly = !this.isDownloadable;
    }

    public LectureMaterialResponse(Long id, Long lectureId, String title, String fileUrl, String fileType,
                                   Boolean isDownloadable, String downloadUrl, String viewUrl, Boolean viewOnly) {
        this.id = id;
        this.lectureId = lectureId;
        this.title = title;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.isDownloadable = isDownloadable != null ? isDownloadable : false;
        this.downloadUrl = downloadUrl;
        this.viewUrl = viewUrl;
        this.viewOnly = viewOnly != null ? viewOnly : !this.isDownloadable;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLectureId() { return lectureId; }
    public void setLectureId(Long lectureId) { this.lectureId = lectureId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Boolean getIsDownloadable() { return isDownloadable; }
    public void setIsDownloadable(Boolean isDownloadable) {
        this.isDownloadable = isDownloadable;
        this.viewOnly = isDownloadable != null ? !isDownloadable : true;
    }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getViewUrl() { return viewUrl; }
    public void setViewUrl(String viewUrl) { this.viewUrl = viewUrl; }

    public Boolean getViewOnly() { return viewOnly; }
    public void setViewOnly(Boolean viewOnly) { this.viewOnly = viewOnly; }
}
