package com.olp.dto;

import jakarta.validation.constraints.NotNull;

public class AiSummaryRequest {

    @NotNull(message = "Course ID is required")
    private Integer courseId;

    private Integer lectureId;

    private Integer materialId;

    public AiSummaryRequest() {}

    public AiSummaryRequest(Integer courseId, Integer lectureId, Integer materialId) {
        this.courseId = courseId;
        this.lectureId = lectureId;
        this.materialId = materialId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getLectureId() {
        return lectureId;
    }

    public void setLectureId(Integer lectureId) {
        this.lectureId = lectureId;
    }

    public Integer getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Integer materialId) {
        this.materialId = materialId;
    }
}
