package com.olp.dto;

import com.olp.entity.LectureMaterial;

/**
 * DTO representing viewer metadata for a material (FR-I17, Step 16).
 * When a material is marked view-only (isDownloadable == false),
 * downloadUrl is explicitly null, ensuring no direct download URL is exposed.
 */
public class MaterialViewerResponse {

    private Integer id;
    private Integer lessonId;
    private Integer moduleId;
    private Integer lectureId;
    private String title;
    private String originalFilename;
    private String fileType;
    private String mimeType;
    private Long fileSizeBytes;
    private Boolean isDownloadable;
    private String downloadUrl;
    private String viewUrl;
    private Boolean viewOnly;

    public MaterialViewerResponse() {}

    public static MaterialViewerResponse fromEntity(LectureMaterial material) {
        if (material == null) {
            return null;
        }
        MaterialViewerResponse resp = new MaterialViewerResponse();
        resp.setId(material.getId());
        resp.setLessonId(material.getLessonId());
        resp.setModuleId(material.getModuleId());
        resp.setLectureId(material.getLectureId());
        resp.setTitle(material.getTitle());
        resp.setOriginalFilename(material.getOriginalFilename());
        resp.setFileType(material.getFileType());
        resp.setMimeType(material.getMimeType());
        resp.setFileSizeBytes(material.getFileSizeBytes());

        boolean downloadable = Boolean.TRUE.equals(material.getIsDownloadable());
        resp.setIsDownloadable(downloadable);
        resp.setViewOnly(!downloadable);
        resp.setViewUrl("/api/student/learning/materials/" + material.getId() + "/view");
        resp.setDownloadUrl(downloadable ? "/api/student/learning/materials/" + material.getId() + "/download" : null);

        return resp;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getLessonId() { return lessonId; }
    public void setLessonId(Integer lessonId) { this.lessonId = lessonId; }

    public Integer getModuleId() { return moduleId; }
    public void setModuleId(Integer moduleId) { this.moduleId = moduleId; }

    public Integer getLectureId() { return lectureId; }
    public void setLectureId(Integer lectureId) { this.lectureId = lectureId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public Boolean getIsDownloadable() { return isDownloadable; }
    public void setIsDownloadable(Boolean downloadable) { isDownloadable = downloadable; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getViewUrl() { return viewUrl; }
    public void setViewUrl(String viewUrl) { this.viewUrl = viewUrl; }

    public Boolean getViewOnly() { return viewOnly; }
    public void setViewOnly(Boolean viewOnly) { this.viewOnly = viewOnly; }
}
