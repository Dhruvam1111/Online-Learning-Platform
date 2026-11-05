package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an OTP token used for email verification.
 * Supports both Password Reset and Passwordless Login authentication.
 *
 * Developer B: Email OTP Verification & Authentication.
 */
@Entity
@Table(name = "two_factor_tokens")
public class TwoFactorToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user this OTP was generated for. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 6-digit OTP code (short-lived, 10 min window). */
    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    /** Timestamp when OTP was generated. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Expiration timestamp (created_at + 10 minutes). */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Flag indicating if the OTP has already been consumed. */
    @Column(name = "is_used", nullable = false)
    private boolean used;

    /** Number of failed attempts. Maximum 3 attempts before token invalidation. */
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    /** Purpose of the token: 'PASSWORD_RESET' or 'LOGIN'. */
    @Column(name = "purpose", nullable = false, length = 50)
    private String purpose;

    public TwoFactorToken() {}

    public TwoFactorToken(User user, String otpCode, LocalDateTime createdAt, LocalDateTime expiresAt, String purpose) {
        this.user = user;
        this.otpCode = otpCode;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.used = false;
        this.attemptCount = 0;
        this.purpose = purpose != null ? purpose : "PASSWORD_RESET";
    }

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public void incrementAttempt() { this.attemptCount++; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
