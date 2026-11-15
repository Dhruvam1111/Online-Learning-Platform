package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing an instructor pending grading alert or daily digest notification (FR-I33, Step 50).
 */
@Entity
@Table(name = "pending_grading_alerts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_grading_alert_digest", columnNames = {"instructor_id", "course_id", "alert_type", "digest_date"})
})
public class PendingGradingAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private GradingAlertType alertType;

    @Column(name = "pending_count", nullable = false)
    private Integer pendingCount = 0;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(name = "digest_date")
    private LocalDate digestDate;

    public PendingGradingAlert() {
    }

    public PendingGradingAlert(User instructor,
                               Course course,
                               GradingAlertType alertType,
                               Integer pendingCount,
                               String title,
                               String message,
                               LocalDate digestDate) {
        this.instructor = instructor;
        this.course = course;
        this.alertType = alertType;
        this.pendingCount = pendingCount != null ? pendingCount : 0;
        this.title = title;
        this.message = message;
        this.isRead = false;
        this.sentAt = LocalDateTime.now();
        this.digestDate = digestDate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getInstructor() {
        return instructor;
    }

    public void setInstructor(User instructor) {
        this.instructor = instructor;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public GradingAlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(GradingAlertType alertType) {
        this.alertType = alertType;
    }

    public Integer getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Integer pendingCount) {
        this.pendingCount = pendingCount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean read) {
        isRead = read;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDate getDigestDate() {
        return digestDate;
    }

    public void setDigestDate(LocalDate digestDate) {
        this.digestDate = digestDate;
    }
}
