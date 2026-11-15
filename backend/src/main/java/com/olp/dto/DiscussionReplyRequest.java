package com.olp.dto;

import jakarta.validation.constraints.NotBlank;

public class DiscussionReplyRequest {

    @NotBlank(message = "Reply content is required")
    private String content;

    private Boolean markAsAnswered;

    private Boolean officialAnswer;

    public DiscussionReplyRequest() {
    }

    public DiscussionReplyRequest(String content, Boolean markAsAnswered, Boolean officialAnswer) {
        this.content = content;
        this.markAsAnswered = markAsAnswered;
        this.officialAnswer = officialAnswer;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getMarkAsAnswered() {
        return markAsAnswered;
    }

    public void setMarkAsAnswered(Boolean markAsAnswered) {
        this.markAsAnswered = markAsAnswered;
    }

    public Boolean getOfficialAnswer() {
        return officialAnswer;
    }

    public void setOfficialAnswer(Boolean officialAnswer) {
        this.officialAnswer = officialAnswer;
    }
}

