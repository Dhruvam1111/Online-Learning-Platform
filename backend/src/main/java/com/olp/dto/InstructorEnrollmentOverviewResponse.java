package com.olp.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InstructorEnrollmentOverviewResponse {

    private int totalCourses;
    private long totalEnrollments;
    private long totalActiveStudents;
    private long totalDroppedStudents;
    private double overallCompletionRate;
    private String interval;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<EnrollmentDataPoint> overallTrend = new ArrayList<>();
    private List<CourseEnrollmentComparisonDto> courseBreakdown = new ArrayList<>();

    public InstructorEnrollmentOverviewResponse() {
    }

    public InstructorEnrollmentOverviewResponse(
            int totalCourses,
            long totalEnrollments,
            long totalActiveStudents,
            long totalDroppedStudents,
            double overallCompletionRate,
            String interval,
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<EnrollmentDataPoint> overallTrend,
            List<CourseEnrollmentComparisonDto> courseBreakdown) {
        this.totalCourses = totalCourses;
        this.totalEnrollments = totalEnrollments;
        this.totalActiveStudents = totalActiveStudents;
        this.totalDroppedStudents = totalDroppedStudents;
        this.overallCompletionRate = overallCompletionRate;
        this.interval = interval;
        this.startDate = startDate;
        this.endDate = endDate;
        this.overallTrend = overallTrend != null ? overallTrend : new ArrayList<>();
        this.courseBreakdown = courseBreakdown != null ? courseBreakdown : new ArrayList<>();
    }

    public int getTotalCourses() {
        return totalCourses;
    }

    public void setTotalCourses(int totalCourses) {
        this.totalCourses = totalCourses;
    }

    public long getTotalEnrollments() {
        return totalEnrollments;
    }

    public void setTotalEnrollments(long totalEnrollments) {
        this.totalEnrollments = totalEnrollments;
    }

    public long getTotalActiveStudents() {
        return totalActiveStudents;
    }

    public void setTotalActiveStudents(long totalActiveStudents) {
        this.totalActiveStudents = totalActiveStudents;
    }

    public long getTotalDroppedStudents() {
        return totalDroppedStudents;
    }

    public void setTotalDroppedStudents(long totalDroppedStudents) {
        this.totalDroppedStudents = totalDroppedStudents;
    }

    public double getOverallCompletionRate() {
        return overallCompletionRate;
    }

    public void setOverallCompletionRate(double overallCompletionRate) {
        this.overallCompletionRate = overallCompletionRate;
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

    public List<EnrollmentDataPoint> getOverallTrend() {
        return overallTrend;
    }

    public void setOverallTrend(List<EnrollmentDataPoint> overallTrend) {
        this.overallTrend = overallTrend != null ? overallTrend : new ArrayList<>();
    }

    public List<CourseEnrollmentComparisonDto> getCourseBreakdown() {
        return courseBreakdown;
    }

    public void setCourseBreakdown(List<CourseEnrollmentComparisonDto> courseBreakdown) {
        this.courseBreakdown = courseBreakdown != null ? courseBreakdown : new ArrayList<>();
    }
}
