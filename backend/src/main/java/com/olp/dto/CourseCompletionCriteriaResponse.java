package com.olp.dto;

import com.olp.entity.CourseCompletionCriteria;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response payload representing course certificate completion criteria (FR-I56).
 */
public class CourseCompletionCriteriaResponse {

    private Integer id;
    private Integer courseId;
    private String courseTitle;
    private BigDecimal minLessonCompletionPercentage;
    private BigDecimal minQuizPassingMarks;
    private BigDecimal minQuizPassingPercentage;
    private boolean requireAllQuizzesPassed;
    private List<Integer> mandatoryLessonIds = new ArrayList<>();
    private List<Integer> mandatoryQuizIds = new ArrayList<>();
    private boolean isLocked;
    private long certificatesIssuedCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CourseCompletionCriteriaResponse() {}

    public static CourseCompletionCriteriaResponse fromEntity(CourseCompletionCriteria criteria, long certificatesIssuedCount) {
        if (criteria == null) return null;
        CourseCompletionCriteriaResponse resp = new CourseCompletionCriteriaResponse();
        resp.setId(criteria.getId());
        if (criteria.getCourse() != null) {
            resp.setCourseId(criteria.getCourse().getId());
            resp.setCourseTitle(criteria.getCourse().getTitle());
        }
        resp.setMinLessonCompletionPercentage(criteria.getMinLessonCompletionPercentage());
        resp.setMinQuizPassingMarks(criteria.getMinQuizPassingMarks());
        resp.setMinQuizPassingPercentage(criteria.getMinQuizPassingPercentage());
        resp.setRequireAllQuizzesPassed(criteria.isRequireAllQuizzesPassed());
        resp.setMandatoryLessonIds(criteria.getMandatoryLessonIdList());
        resp.setMandatoryQuizIds(criteria.getMandatoryQuizIdList());
        resp.setLocked(criteria.isLocked() || certificatesIssuedCount > 0);
        resp.setCertificatesIssuedCount(certificatesIssuedCount);
        resp.setCreatedAt(criteria.getCreatedAt());
        resp.setUpdatedAt(criteria.getUpdatedAt());
        return resp;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public BigDecimal getMinLessonCompletionPercentage() {
        return minLessonCompletionPercentage;
    }

    public void setMinLessonCompletionPercentage(BigDecimal minLessonCompletionPercentage) {
        this.minLessonCompletionPercentage = minLessonCompletionPercentage;
    }

    public BigDecimal getMinQuizPassingMarks() {
        return minQuizPassingMarks;
    }

    public void setMinQuizPassingMarks(BigDecimal minQuizPassingMarks) {
        this.minQuizPassingMarks = minQuizPassingMarks;
    }

    public BigDecimal getMinQuizPassingPercentage() {
        return minQuizPassingPercentage;
    }

    public void setMinQuizPassingPercentage(BigDecimal minQuizPassingPercentage) {
        this.minQuizPassingPercentage = minQuizPassingPercentage;
    }

    public boolean isRequireAllQuizzesPassed() {
        return requireAllQuizzesPassed;
    }

    public void setRequireAllQuizzesPassed(boolean requireAllQuizzesPassed) {
        this.requireAllQuizzesPassed = requireAllQuizzesPassed;
    }

    public List<Integer> getMandatoryLessonIds() {
        return mandatoryLessonIds;
    }

    public void setMandatoryLessonIds(List<Integer> mandatoryLessonIds) {
        this.mandatoryLessonIds = mandatoryLessonIds != null ? mandatoryLessonIds : new ArrayList<>();
    }

    public List<Integer> getMandatoryQuizIds() {
        return mandatoryQuizIds;
    }

    public void setMandatoryQuizIds(List<Integer> mandatoryQuizIds) {
        this.mandatoryQuizIds = mandatoryQuizIds != null ? mandatoryQuizIds : new ArrayList<>();
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public long getCertificatesIssuedCount() {
        return certificatesIssuedCount;
    }

    public void setCertificatesIssuedCount(long certificatesIssuedCount) {
        this.certificatesIssuedCount = certificatesIssuedCount;
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
