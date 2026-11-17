package com.olp.dto;

public class DiscussionQuestionRequest {

    private String title;
    private String body;
    private Integer lectureId;

    public DiscussionQuestionRequest() {
    }

    public DiscussionQuestionRequest(String title, String body, Integer lectureId) {
        this.title = title;
        this.body = body;
        this.lectureId = lectureId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Integer getLectureId() {
        return lectureId;
    }

    public void setLectureId(Integer lectureId) {
        this.lectureId = lectureId;
    }
}
