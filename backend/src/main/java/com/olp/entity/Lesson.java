package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an individual lesson in the Course hierarchy (FR-I06).
 * Supports VIDEO, MATERIAL, QUIZ, ASSIGNMENT types and holds extensible metadata for SCORM/xAPI export (DR-14).
 */
@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private CourseSection section;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false, length = 50)
    private LessonType lessonType = LessonType.VIDEO;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_url", length = 500)
    private String contentUrl;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "release_at")
    private LocalDateTime releaseAt;

    @Column(name = "is_released", nullable = false)
    private Boolean isReleased = true;

    @Column(name = "time_zone", length = 50)
    private String timeZone = "Asia/Kolkata";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Lesson() {}

    public Lesson(CourseSection section, String title, LessonType lessonType,
                  String content, String contentUrl, Integer durationMinutes,
                  Integer orderIndex, String metadata) {
        this.section = section;
        this.title = title;
        this.lessonType = lessonType != null ? lessonType : LessonType.VIDEO;
        this.content = content;
        this.contentUrl = contentUrl;
        this.durationMinutes = durationMinutes;
        this.orderIndex = orderIndex != null ? orderIndex : 0;
        this.metadata = metadata;
    }

    public Lesson(CourseSection section, String title, LessonType lessonType, Integer orderIndex) {
        this(section, title, lessonType, null, null, null, orderIndex, null);
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
        if (this.orderIndex == null) {
            this.orderIndex = 0;
        }
        if (this.lessonType == null) {
            this.lessonType = LessonType.VIDEO;
        }
        if (this.isReleased == null) {
            this.isReleased = true;
        }
        if (this.timeZone == null || this.timeZone.isBlank()) {
            this.timeZone = "Asia/Kolkata";
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

    public CourseSection getSection() {
        return section;
    }

    public void setSection(CourseSection section) {
        this.section = section;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LessonType getLessonType() {
        return lessonType;
    }

    public void setLessonType(LessonType lessonType) {
        this.lessonType = lessonType != null ? lessonType : LessonType.VIDEO;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex != null ? orderIndex : 0;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
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

    public LocalDateTime getReleaseAt() {
        return releaseAt;
    }

    public void setReleaseAt(LocalDateTime releaseAt) {
        this.releaseAt = releaseAt;
    }

    public Boolean getIsReleased() {
        return isReleased;
    }

    public void setIsReleased(Boolean isReleased) {
        this.isReleased = isReleased;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public boolean isAvailableToStudent() {
        if (Boolean.TRUE.equals(this.isReleased)) {
            return true;
        }
        if (this.releaseAt == null) {
            return true;
        }
        return !this.releaseAt.isAfter(LocalDateTime.now(java.time.ZoneOffset.UTC));
    }
}

