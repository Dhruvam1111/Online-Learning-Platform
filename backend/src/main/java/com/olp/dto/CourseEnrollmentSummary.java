package com.olp.dto;

public class CourseEnrollmentSummary {

    private long totalEnrollments;
    private long currentMonthEnrollments;
    private long previousMonthEnrollments;
    private Double growthRatePercentage;
    private long recent7DaysEnrollments;
    private long recent30DaysEnrollments;

    public CourseEnrollmentSummary() {
    }

    public CourseEnrollmentSummary(
            long totalEnrollments,
            long currentMonthEnrollments,
            long previousMonthEnrollments,
            Double growthRatePercentage,
            long recent7DaysEnrollments,
            long recent30DaysEnrollments) {
        this.totalEnrollments = totalEnrollments;
        this.currentMonthEnrollments = currentMonthEnrollments;
        this.previousMonthEnrollments = previousMonthEnrollments;
        this.growthRatePercentage = growthRatePercentage;
        this.recent7DaysEnrollments = recent7DaysEnrollments;
        this.recent30DaysEnrollments = recent30DaysEnrollments;
    }

    public long getTotalEnrollments() {
        return totalEnrollments;
    }

    public void setTotalEnrollments(long totalEnrollments) {
        this.totalEnrollments = totalEnrollments;
    }

    public long getCurrentMonthEnrollments() {
        return currentMonthEnrollments;
    }

    public void setCurrentMonthEnrollments(long currentMonthEnrollments) {
        this.currentMonthEnrollments = currentMonthEnrollments;
    }

    public long getPreviousMonthEnrollments() {
        return previousMonthEnrollments;
    }

    public void setPreviousMonthEnrollments(long previousMonthEnrollments) {
        this.previousMonthEnrollments = previousMonthEnrollments;
    }

    public Double getGrowthRatePercentage() {
        return growthRatePercentage;
    }

    public void setGrowthRatePercentage(Double growthRatePercentage) {
        this.growthRatePercentage = growthRatePercentage;
    }

    public long getRecent7DaysEnrollments() {
        return recent7DaysEnrollments;
    }

    public void setRecent7DaysEnrollments(long recent7DaysEnrollments) {
        this.recent7DaysEnrollments = recent7DaysEnrollments;
    }

    public long getRecent30DaysEnrollments() {
        return recent30DaysEnrollments;
    }

    public void setRecent30DaysEnrollments(long recent30DaysEnrollments) {
        this.recent30DaysEnrollments = recent30DaysEnrollments;
    }
}
