package com.olp.dto;

public class SendOtpResponse {

    private final String message;
    private final String maskedEmail;

    public SendOtpResponse(String message, String maskedEmail) {
        this.message = message;
        this.maskedEmail = maskedEmail;
    }

    public String getMessage() {
        return message;
    }

    public String getMaskedEmail() {
        return maskedEmail;
    }
}
