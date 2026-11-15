package com.olp.dto;

import jakarta.validation.constraints.NotBlank;

public class DiscussionReplyRequest {

    @NotBlank(message = "Content is required")
    private String content;

    public DiscussionReplyRequest() {}

    public DiscussionReplyRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
