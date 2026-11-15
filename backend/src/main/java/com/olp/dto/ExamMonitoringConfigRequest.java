package com.olp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload for configuring exam monitoring and proctoring rules (FR-I26, Step 40).
 */
public class ExamMonitoringConfigRequest {

    private Boolean tabSwitchDetection = true;

    @Min(value = 0, message = "Tab switch limit cannot be negative")
    @Max(value = 50, message = "Tab switch limit cannot exceed 50")
    private Integer tabSwitchLimit = 3;

    private Boolean requireFullscreen = true;

    @Min(value = 0, message = "Fullscreen exit limit cannot be negative")
    @Max(value = 50, message = "Fullscreen exit limit cannot exceed 50")
    private Integer fullscreenExitLimit = 2;

    private Boolean requireScreenShare = false;

    private Boolean requireWebcam = false;

    private Boolean requireMicrophone = false;

    private Boolean requireStudentConsent = true;

    @Pattern(regexp = "^(WARN|BLOCK)$", message = "Unsupported browser policy must be either 'WARN' or 'BLOCK'")
    private String unsupportedBrowserPolicy = "WARN";

    private String consentText;

    public ExamMonitoringConfigRequest() {}

    public Boolean getTabSwitchDetection() {
        return tabSwitchDetection;
    }

    public void setTabSwitchDetection(Boolean tabSwitchDetection) {
        this.tabSwitchDetection = tabSwitchDetection;
    }

    public Integer getTabSwitchLimit() {
        return tabSwitchLimit;
    }

    public void setTabSwitchLimit(Integer tabSwitchLimit) {
        this.tabSwitchLimit = tabSwitchLimit;
    }

    public Boolean getRequireFullscreen() {
        return requireFullscreen;
    }

    public void setRequireFullscreen(Boolean requireFullscreen) {
        this.requireFullscreen = requireFullscreen;
    }

    public Integer getFullscreenExitLimit() {
        return fullscreenExitLimit;
    }

    public void setFullscreenExitLimit(Integer fullscreenExitLimit) {
        this.fullscreenExitLimit = fullscreenExitLimit;
    }

    public Boolean getRequireScreenShare() {
        return requireScreenShare;
    }

    public void setRequireScreenShare(Boolean requireScreenShare) {
        this.requireScreenShare = requireScreenShare;
    }

    public Boolean getRequireWebcam() {
        return requireWebcam;
    }

    public void setRequireWebcam(Boolean requireWebcam) {
        this.requireWebcam = requireWebcam;
    }

    public Boolean getRequireMicrophone() {
        return requireMicrophone;
    }

    public void setRequireMicrophone(Boolean requireMicrophone) {
        this.requireMicrophone = requireMicrophone;
    }

    public Boolean getRequireStudentConsent() {
        return requireStudentConsent;
    }

    public void setRequireStudentConsent(Boolean requireStudentConsent) {
        this.requireStudentConsent = requireStudentConsent;
    }

    public String getUnsupportedBrowserPolicy() {
        return unsupportedBrowserPolicy;
    }

    public void setUnsupportedBrowserPolicy(String unsupportedBrowserPolicy) {
        this.unsupportedBrowserPolicy = unsupportedBrowserPolicy;
    }

    public String getConsentText() {
        return consentText;
    }

    public void setConsentText(String consentText) {
        this.consentText = consentText;
    }
}
