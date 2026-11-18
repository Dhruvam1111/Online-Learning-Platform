package com.olp.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class AssessmentPerformanceSummaryDto {

    private Integer assessmentId;
    private String assessmentType; // "QUIZ" or "EXAM"
    private String title;
    private Integer courseId;
    private Double maxMarks;
    private Double passingMarks;
    private boolean resultsPublished;
    private long totalPublishedAttempts;
    private long uniqueStudents;
    private Double averageMarks;
    private Double averagePercentage;
    private Double highestMarks;
    private Double highestPercentage;
    private Double lowestMarks;
    private Double lowestPercentage;
    private long passedStudentsCount;
    private long failedStudentsCount;
    private Double passRatePercentage;
    private Map<String, Long> scoreDistribution = new LinkedHashMap<>();

    public AssessmentPerformanceSummaryDto() {
    }

    public AssessmentPerformanceSummaryDto(
            Integer assessmentId,
            String assessmentType,
            String title,
            Integer courseId,
            Double maxMarks,
            Double passingMarks,
            boolean resultsPublished,
            long totalPublishedAttempts,
            long uniqueStudents,
            Double averageMarks,
            Double averagePercentage,
            Double highestMarks,
            Double highestPercentage,
            Double lowestMarks,
            Double lowestPercentage,
            long passedStudentsCount,
            long failedStudentsCount,
            Double passRatePercentage,
            Map<String, Long> scoreDistribution) {
        this.assessmentId = assessmentId;
        this.assessmentType = assessmentType;
        this.title = title;
        this.courseId = courseId;
        this.maxMarks = maxMarks;
        this.passingMarks = passingMarks;
        this.resultsPublished = resultsPublished;
        this.totalPublishedAttempts = totalPublishedAttempts;
        this.uniqueStudents = uniqueStudents;
        this.averageMarks = averageMarks;
        this.averagePercentage = averagePercentage;
        this.highestMarks = highestMarks;
        this.highestPercentage = highestPercentage;
        this.lowestMarks = lowestMarks;
        this.lowestPercentage = lowestPercentage;
        this.passedStudentsCount = passedStudentsCount;
        this.failedStudentsCount = failedStudentsCount;
        this.passRatePercentage = passRatePercentage;
        this.scoreDistribution = scoreDistribution != null ? scoreDistribution : new LinkedHashMap<>();
    }

    public Integer getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(Integer assessmentId) {
        this.assessmentId = assessmentId;
    }

    public String getAssessmentType() {
        return assessmentType;
    }

    public void setAssessmentType(String assessmentType) {
        this.assessmentType = assessmentType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Double getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(Double maxMarks) {
        this.maxMarks = maxMarks;
    }

    public Double getPassingMarks() {
        return passingMarks;
    }

    public void setPassingMarks(Double passingMarks) {
        this.passingMarks = passingMarks;
    }

    public boolean isResultsPublished() {
        return resultsPublished;
    }

    public void setResultsPublished(boolean resultsPublished) {
        this.resultsPublished = resultsPublished;
    }

    public long getTotalPublishedAttempts() {
        return totalPublishedAttempts;
    }

    public void setTotalPublishedAttempts(long totalPublishedAttempts) {
        this.totalPublishedAttempts = totalPublishedAttempts;
    }

    public long getUniqueStudents() {
        return uniqueStudents;
    }

    public void setUniqueStudents(long uniqueStudents) {
        this.uniqueStudents = uniqueStudents;
    }

    public Double getAverageMarks() {
        return averageMarks;
    }

    public void setAverageMarks(Double averageMarks) {
        this.averageMarks = averageMarks;
    }

    public Double getAveragePercentage() {
        return averagePercentage;
    }

    public void setAveragePercentage(Double averagePercentage) {
        this.averagePercentage = averagePercentage;
    }

    public Double getHighestMarks() {
        return highestMarks;
    }

    public void setHighestMarks(Double highestMarks) {
        this.highestMarks = highestMarks;
    }

    public Double getHighestPercentage() {
        return highestPercentage;
    }

    public void setHighestPercentage(Double highestPercentage) {
        this.highestPercentage = highestPercentage;
    }

    public Double getLowestMarks() {
        return lowestMarks;
    }

    public void setLowestMarks(Double lowestMarks) {
        this.lowestMarks = lowestMarks;
    }

    public Double getLowestPercentage() {
        return lowestPercentage;
    }

    public void setLowestPercentage(Double lowestPercentage) {
        this.lowestPercentage = lowestPercentage;
    }

    public long getPassedStudentsCount() {
        return passedStudentsCount;
    }

    public void setPassedStudentsCount(long passedStudentsCount) {
        this.passedStudentsCount = passedStudentsCount;
    }

    public long getFailedStudentsCount() {
        return failedStudentsCount;
    }

    public void setFailedStudentsCount(long failedStudentsCount) {
        this.failedStudentsCount = failedStudentsCount;
    }

    public Double getPassRatePercentage() {
        return passRatePercentage;
    }

    public void setPassRatePercentage(Double passRatePercentage) {
        this.passRatePercentage = passRatePercentage;
    }

    public Map<String, Long> getScoreDistribution() {
        return scoreDistribution;
    }

    public void setScoreDistribution(Map<String, Long> scoreDistribution) {
        this.scoreDistribution = scoreDistribution != null ? scoreDistribution : new LinkedHashMap<>();
    }
}
