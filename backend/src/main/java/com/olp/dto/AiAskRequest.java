package com.olp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AiAskRequest {

    @NotNull(message = "Course ID is required")
    private Integer courseId;

    @NotBlank(message = "Prompt question is required")
    private String prompt;

    private String contextType = "GENERAL";

    private Integer contextId;

    public AiAskRequest() {}

    public AiAskRequest(Integer courseId, String prompt, String contextType, Integer contextId) {
        this.courseId = courseId;
        this.prompt = prompt;
        this.contextType = contextType != null ? contextType : "GENERAL";
        this.contextId = contextId;
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

    public String getContextType() {
        return contextType;
    }

    public void setContextType(String contextType) {
        this.contextType = contextType;
    }

    public Integer getContextId() {
        return contextId;
    }

    public void setContextId(Integer contextId) {
        this.contextId = contextId;
    }
}
