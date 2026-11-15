package com.olp.dto;

import java.time.LocalDateTime;

public class ExamMonitoringEventResponse {

    private Integer id;
    private Integer attemptId;
    private String eventType;
    private LocalDateTime eventTimestamp;
    private Integer durationSeconds;
    private String severity;
    private String ruleTriggered;
    private String warningMessage;
    private Integer currentTabSwitches;
    private Integer maxTabSwitches;
    private Integer currentFullscreenExits;
    private Integer maxFullscreenExits;
    private Boolean screenShareActive;
    private Boolean requiresAction;

    public ExamMonitoringEventResponse() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getRuleTriggered() {
        return ruleTriggered;
    }

    public void setRuleTriggered(String ruleTriggered) {
        this.ruleTriggered = ruleTriggered;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }

    public Integer getCurrentTabSwitches() {
        return currentTabSwitches;
    }

    public void setCurrentTabSwitches(Integer currentTabSwitches) {
        this.currentTabSwitches = currentTabSwitches;
    }

    public Integer getMaxTabSwitches() {
        return maxTabSwitches;
    }

    public void setMaxTabSwitches(Integer maxTabSwitches) {
        this.maxTabSwitches = maxTabSwitches;
    }

    public Integer getCurrentFullscreenExits() {
        return currentFullscreenExits;
    }

    public void setCurrentFullscreenExits(Integer currentFullscreenExits) {
        this.currentFullscreenExits = currentFullscreenExits;
    }

    public Integer getMaxFullscreenExits() {
        return maxFullscreenExits;
    }

    public void setMaxFullscreenExits(Integer maxFullscreenExits) {
        this.maxFullscreenExits = maxFullscreenExits;
    }

    public Boolean getScreenShareActive() {
        return screenShareActive;
    }

    public void setScreenShareActive(Boolean screenShareActive) {
        this.screenShareActive = screenShareActive;
    }

    public Boolean getRequiresAction() {
        return requiresAction;
    }

    public void setRequiresAction(Boolean requiresAction) {
        this.requiresAction = requiresAction;
    }
}
