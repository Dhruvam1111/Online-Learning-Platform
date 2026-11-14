package com.olp.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for submitting student consent prior to examination commencement (DR-06, FR-I26).
 */
public class ExamConsentRequest {

    @NotNull(message = "Consent status is required")
    private Boolean consentGiven;

    private Boolean browserSupportsFullscreen = true;

    public ExamConsentRequest() {}

    public ExamConsentRequest(Boolean consentGiven, Boolean browserSupportsFullscreen) {
        this.consentGiven = consentGiven;
        this.browserSupportsFullscreen = browserSupportsFullscreen != null ? browserSupportsFullscreen : true;
    }

    public Boolean getConsentGiven() {
        return consentGiven;
    }

    public void setConsentGiven(Boolean consentGiven) {
        this.consentGiven = consentGiven;
    }

    public Boolean getBrowserSupportsFullscreen() {
        return browserSupportsFullscreen;
    }

    public void setBrowserSupportsFullscreen(Boolean browserSupportsFullscreen) {
        this.browserSupportsFullscreen = browserSupportsFullscreen;
    }
}
