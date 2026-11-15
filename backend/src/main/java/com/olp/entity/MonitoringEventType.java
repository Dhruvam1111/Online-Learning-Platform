package com.olp.entity;

/**
 * Proctoring event types captured during examination monitoring (FR-I29, Step 55).
 */
public enum MonitoringEventType {
    TAB_SWITCH,
    FOCUS_LOST,
    FOCUS_REGAINED,
    FULLSCREEN_EXIT,
    FULLSCREEN_ENTER,
    SCREEN_SHARE_STOPPED,
    SCREEN_SHARE_STARTED,
    BROWSER_WARNING,
    DEVICE_DISCONNECTED
}
