package com.olp.dto;

public class CourseSubmissionRequest {

    private String comments;

    public CourseSubmissionRequest() {}

    public CourseSubmissionRequest(String comments) {
        this.comments = comments;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}

