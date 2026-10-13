package com.olp.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class ReorderLecturesRequest {

    @NotEmpty(message = "Lecture IDs list cannot be empty")
    private List<Integer> lectureIds;

    public ReorderLecturesRequest() {}

    public ReorderLecturesRequest(List<Integer> lectureIds) {
        this.lectureIds = lectureIds;
    }

    public List<Integer> getLectureIds() {
        return lectureIds;
    }

    public void setLectureIds(List<Integer> lectureIds) {
        this.lectureIds = lectureIds;
    }
}
