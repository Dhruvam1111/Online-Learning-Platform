package com.olp.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentAnalyticsResponse {

    private Integer courseId;
    private String courseTitle;
    private String interval;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private CourseEnrollmentSummary summary;
    private ActiveVsDroppedStats retention;
    private List<EnrollmentDataPoint> trend = new ArrayList<>();

    public EnrollmentAnalyticsResponse() {
    }

    public EnrollmentAnalyticsResponse(
            Integer courseId,
            String courseTitle,
            String interval,
            LocalDateTime startDate,
            LocalDateTime endDate,
            CourseEnrollmentSummary summary,
            ActiveVsDroppedStats retention,
            List<EnrollmentDataPoint> trend) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.interval = interval;
        this.startDate = startDate;
        this.endDate = endDate;
        this.summary = summary;
        this.retention = retention;
        this.trend = trend != null ? trend : new ArrayList<>();
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

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public CourseEnrollmentSummary getSummary() {
        return summary;
    }

    public void setSummary(CourseEnrollmentSummary summary) {
        this.summary = summary;
    }

    public ActiveVsDroppedStats getRetention() {
        return retention;
    }

    public void setRetention(ActiveVsDroppedStats retention) {
        this.retention = retention;
    }

    public List<EnrollmentDataPoint> getTrend() {
        return trend;
    }

    public void setTrend(List<EnrollmentDataPoint> trend) {
        this.trend = trend != null ? trend : new ArrayList<>();
    }
}
