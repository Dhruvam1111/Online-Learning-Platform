package com.olp.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AiAskResponse {

    private Integer queryId;
    private Integer courseId;
    private String prompt;
    private String response;
    private String contextTitle;
    private List<String> suggestedFollowUps;
    private LocalDateTime createdAt;

    public AiAskResponse() {}

    public AiAskResponse(Integer queryId, Integer courseId, String prompt, String response,
                         String contextTitle, List<String> suggestedFollowUps, LocalDateTime createdAt) {
        this.queryId = queryId;
        this.courseId = courseId;
        this.prompt = prompt;
        this.response = response;
        this.contextTitle = contextTitle;
        this.suggestedFollowUps = suggestedFollowUps;
        this.createdAt = createdAt;
    }

    public Integer getQueryId() {
        return queryId;
    }

    public void setQueryId(Integer queryId) {
        this.queryId = queryId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getContextTitle() {
        return contextTitle;
    }

    public void setContextTitle(String contextTitle) {
        this.contextTitle = contextTitle;
    }

    public List<String> getSuggestedFollowUps() {
        return suggestedFollowUps;
    }

    public void setSuggestedFollowUps(List<String> suggestedFollowUps) {
        this.suggestedFollowUps = suggestedFollowUps;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
