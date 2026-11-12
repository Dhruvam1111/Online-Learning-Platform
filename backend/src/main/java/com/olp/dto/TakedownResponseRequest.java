package com.olp.dto;

import jakarta.validation.constraints.NotBlank;

public class TakedownResponseRequest {

    @NotBlank(message = "Response statement is required")
    private String response;

    public TakedownResponseRequest() {
    }

    public TakedownResponseRequest(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}

