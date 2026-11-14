package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing monitoring and proctoring configuration for an examination (FR-I26, Step 40).
 * Stores tab-switch limits, fullscreen requirements, device permissions, and consent policies.
 */
@Entity
@Table(name = "exam_monitoring_configs")
public class ExamMonitoringConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false, unique = true)
    private Exam exam;

    @Column(name = "tab_switch_detection", nullable = false)
    private Boolean tabSwitchDetection = true;

    @Column(name = "tab_switch_limit", nullable = false)
    private Integer tabSwitchLimit = 3;

    @Column(name = "require_fullscreen", nullable = false)
    private Boolean requireFullscreen = true;

    @Column(name = "fullscreen_exit_limit", nullable = false)
    private Integer fullscreenExitLimit = 2;

    @Column(name = "require_screen_share", nullable = false)
    private Boolean requireScreenShare = false;

    @Column(name = "require_webcam", nullable = false)
    private Boolean requireWebcam = false;

    @Column(name = "require_microphone", nullable = false)
    private Boolean requireMicrophone = false;

    @Column(name = "require_student_consent", nullable = false)
    private Boolean requireStudentConsent = true;

    @Column(name = "unsupported_browser_policy", length = 30, nullable = false)
    private String unsupportedBrowserPolicy = "WARN"; // 'WARN' or 'BLOCK' (NFR-25)

    @Column(name = "consent_text", columnDefinition = "TEXT")
    private String consentText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ExamMonitoringConfig() {}

    public ExamMonitoringConfig(Exam exam, Boolean tabSwitchDetection, Integer tabSwitchLimit,
                                Boolean requireFullscreen, Integer fullscreenExitLimit,
                                Boolean requireScreenShare, Boolean requireWebcam, Boolean requireMicrophone,
                                Boolean requireStudentConsent, String unsupportedBrowserPolicy, String consentText) {
        this.exam = exam;
        this.tabSwitchDetection = tabSwitchDetection != null ? tabSwitchDetection : true;
        this.tabSwitchLimit = tabSwitchLimit != null ? tabSwitchLimit : 3;
        this.requireFullscreen = requireFullscreen != null ? requireFullscreen : true;
        this.fullscreenExitLimit = fullscreenExitLimit != null ? fullscreenExitLimit : 2;
        this.requireScreenShare = requireScreenShare != null ? requireScreenShare : false;
        this.requireWebcam = requireWebcam != null ? requireWebcam : false;
        this.requireMicrophone = requireMicrophone != null ? requireMicrophone : false;
        this.requireStudentConsent = requireStudentConsent != null ? requireStudentConsent : true;
        this.unsupportedBrowserPolicy = unsupportedBrowserPolicy != null ? unsupportedBrowserPolicy.toUpperCase() : "WARN";
        this.consentText = consentText;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.tabSwitchDetection == null) {
            this.tabSwitchDetection = true;
        }
        if (this.tabSwitchLimit == null) {
            this.tabSwitchLimit = 3;
        }
        if (this.requireFullscreen == null) {
            this.requireFullscreen = true;
        }
        if (this.fullscreenExitLimit == null) {
            this.fullscreenExitLimit = 2;
        }
        if (this.requireScreenShare == null) {
            this.requireScreenShare = false;
        }
        if (this.requireWebcam == null) {
            this.requireWebcam = false;
        }
        if (this.requireMicrophone == null) {
            this.requireMicrophone = false;
        }
        if (this.requireStudentConsent == null) {
            this.requireStudentConsent = true;
        }
        if (this.unsupportedBrowserPolicy == null) {
            this.unsupportedBrowserPolicy = "WARN";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
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
        this.unsupportedBrowserPolicy = unsupportedBrowserPolicy != null ? unsupportedBrowserPolicy.toUpperCase() : "WARN";
    }

    public String getConsentText() {
        return consentText;
    }

    public void setConsentText(String consentText) {
        this.consentText = consentText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
