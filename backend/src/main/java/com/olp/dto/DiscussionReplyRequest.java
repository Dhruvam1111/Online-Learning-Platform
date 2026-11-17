package com.olp.dto;

public class DiscussionReplyRequest {

    private String body;
    private boolean officialAnswer;

    public DiscussionReplyRequest() {
    }

    public DiscussionReplyRequest(String body, boolean officialAnswer) {
        this.body = body;
        this.officialAnswer = officialAnswer;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isOfficialAnswer() {
        return officialAnswer;
    }

    public void setOfficialAnswer(boolean officialAnswer) {
        this.officialAnswer = officialAnswer;
    }
}
