package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "content_declarations")
public class ContentDeclaration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @Column(name = "declaration_text", nullable = false, columnDefinition = "TEXT")
    private String declarationText;

    @Enumerated(EnumType.STRING)
    @Column(name = "license_type", nullable = false, length = 50)
    private LicenseType licenseType = LicenseType.ORIGINAL_CREATOR;

    @Column(name = "source_attribution", columnDefinition = "TEXT")
    private String sourceAttribution;

    @Column(name = "declared_at", nullable = false)
    private LocalDateTime declaredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "takedown_status", nullable = false, length = 30)
    private TakedownStatus takedownStatus = TakedownStatus.NONE;

    @Column(name = "takedown_reason", columnDefinition = "TEXT")
    private String takedownReason;

    @Column(name = "takedown_flagged_at")
    private LocalDateTime takedownFlaggedAt;

    @Column(name = "instructor_response", columnDefinition = "TEXT")
    private String instructorResponse;

    @Column(name = "instructor_responded_at")
    private LocalDateTime instructorRespondedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ContentDeclaration() {
    }

    public ContentDeclaration(Course course, Lesson lesson, User instructor, String declarationText,
                              LicenseType licenseType, String sourceAttribution) {
        this.course = course;
        this.lesson = lesson;
        this.instructor = instructor;
        this.declarationText = declarationText;
        this.licenseType = licenseType != null ? licenseType : LicenseType.ORIGINAL_CREATOR;
        this.sourceAttribution = sourceAttribution;
        this.declaredAt = LocalDateTime.now();
        this.takedownStatus = TakedownStatus.NONE;
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
        if (this.declaredAt == null) {
            this.declaredAt = now;
        }
        if (this.takedownStatus == null) {
            this.takedownStatus = TakedownStatus.NONE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public User getInstructor() {
        return instructor;
    }

    public void setInstructor(User instructor) {
        this.instructor = instructor;
    }

    public String getDeclarationText() {
        return declarationText;
    }

    public void setDeclarationText(String declarationText) {
        this.declarationText = declarationText;
    }

    public LicenseType getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(LicenseType licenseType) {
        this.licenseType = licenseType;
    }

    public String getSourceAttribution() {
        return sourceAttribution;
    }

    public void setSourceAttribution(String sourceAttribution) {
        this.sourceAttribution = sourceAttribution;
    }

    public LocalDateTime getDeclaredAt() {
        return declaredAt;
    }

    public void setDeclaredAt(LocalDateTime declaredAt) {
        this.declaredAt = declaredAt;
    }

    public TakedownStatus getTakedownStatus() {
        return takedownStatus;
    }

    public void setTakedownStatus(TakedownStatus takedownStatus) {
        this.takedownStatus = takedownStatus;
    }

    public String getTakedownReason() {
        return takedownReason;
    }

    public void setTakedownReason(String takedownReason) {
        this.takedownReason = takedownReason;
    }

    public LocalDateTime getTakedownFlaggedAt() {
        return takedownFlaggedAt;
    }

    public void setTakedownFlaggedAt(LocalDateTime takedownFlaggedAt) {
        this.takedownFlaggedAt = takedownFlaggedAt;
    }

    public String getInstructorResponse() {
        return instructorResponse;
    }

    public void setInstructorResponse(String instructorResponse) {
        this.instructorResponse = instructorResponse;
    }

    public LocalDateTime getInstructorRespondedAt() {
        return instructorRespondedAt;
    }

    public void setInstructorRespondedAt(LocalDateTime instructorRespondedAt) {
        this.instructorRespondedAt = instructorRespondedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
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

