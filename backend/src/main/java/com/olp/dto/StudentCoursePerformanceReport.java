package com.olp.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StudentCoursePerformanceReport {

    private Integer courseId;
    private String courseTitle;
    private Integer studentId;
    private String studentName;
    private String studentEmail;
    private LocalDateTime enrolledAt;
    private Double overallAveragePercentage;
    private int quizzesAttempted;
    private int quizzesPassed;
    private int examsAttempted;
    private int examsPassed;
    private List<StudentAssessmentPerformanceDto> assessments = new ArrayList<>();

    public StudentCoursePerformanceReport() {
    }

    public StudentCoursePerformanceReport(
            Integer courseId,
            String courseTitle,
            Integer studentId,
            String studentName,
            String studentEmail,
            LocalDateTime enrolledAt,
            Double overallAveragePercentage,
            int quizzesAttempted,
            int quizzesPassed,
            int examsAttempted,
            int examsPassed,
            List<StudentAssessmentPerformanceDto> assessments) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.enrolledAt = enrolledAt;
        this.overallAveragePercentage = overallAveragePercentage;
        this.quizzesAttempted = quizzesAttempted;
        this.quizzesPassed = quizzesPassed;
        this.examsAttempted = examsAttempted;
        this.examsPassed = examsPassed;
        this.assessments = assessments != null ? assessments : new ArrayList<>();
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

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public Double getOverallAveragePercentage() {
        return overallAveragePercentage;
    }

    public void setOverallAveragePercentage(Double overallAveragePercentage) {
        this.overallAveragePercentage = overallAveragePercentage;
    }

    public int getQuizzesAttempted() {
        return quizzesAttempted;
    }

    public void setQuizzesAttempted(int quizzesAttempted) {
        this.quizzesAttempted = quizzesAttempted;
    }

    public int getQuizzesPassed() {
        return quizzesPassed;
    }

    public void setQuizzesPassed(int quizzesPassed) {
        this.quizzesPassed = quizzesPassed;
    }

    public int getExamsAttempted() {
        return examsAttempted;
    }

    public void setExamsAttempted(int examsAttempted) {
        this.examsAttempted = examsAttempted;
    }

    public int getExamsPassed() {
        return examsPassed;
    }

    public void setExamsPassed(int examsPassed) {
        this.examsPassed = examsPassed;
    }

    public List<StudentAssessmentPerformanceDto> getAssessments() {
        return assessments;
    }

    public void setAssessments(List<StudentAssessmentPerformanceDto> assessments) {
        this.assessments = assessments != null ? assessments : new ArrayList<>();
    }
}
