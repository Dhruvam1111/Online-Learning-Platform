package com.olp.dto;

import jakarta.validation.constraints.NotNull;

public class ScreenShareConsentRequest {

    @NotNull(message = "Consent status is required")
    private Boolean consentGiven;

    public ScreenShareConsentRequest() {}

    public ScreenShareConsentRequest(Boolean consentGiven) {
        this.consentGiven = consentGiven;
    }

    public Boolean getConsentGiven() {
        return consentGiven;
    }

    public void setConsentGiven(Boolean consentGiven) {
        this.consentGiven = consentGiven;
    }
}
