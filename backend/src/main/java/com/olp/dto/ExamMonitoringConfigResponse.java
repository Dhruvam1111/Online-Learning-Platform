package com.olp.dto;

/**
 * Response DTO returning the proctoring configuration for an examination (FR-I26, Step 40).
 * Consumed by the student exam client along with fallback flags for unsupported browser APIs (NFR-25).
 */
public class ExamMonitoringConfigResponse {

    private Integer id;
    private Integer examId;
    private String examTitle;
    private Boolean tabSwitchDetection;
    private Integer tabSwitchLimit;
    private Boolean requireFullscreen;
    private Integer fullscreenExitLimit;
    private Boolean requireScreenShare;
    private Boolean requireWebcam;
    private Boolean requireMicrophone;
    private Boolean requireStudentConsent;
    private String unsupportedBrowserPolicy;
    private String consentText;
    private String fullscreenSupportWarning;

    public ExamMonitoringConfigResponse() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getExamId() {
        return examId;
    }

    public void setExamId(Integer examId) {
        this.examId = examId;
    }

    public String getExamTitle() {
        return examTitle;
    }

    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }

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

    public String getFullscreenSupportWarning() {
        return fullscreenSupportWarning;
    }

    public void setFullscreenSupportWarning(String fullscreenSupportWarning) {
        this.fullscreenSupportWarning = fullscreenSupportWarning;
    }
}
