package com.olp.dto;

import java.time.LocalDateTime;

public class EnrolledStudentResponse {

    private Integer studentId;
    private String name;
    private String email;
    private LocalDateTime enrolledAt;
    private Double progressPercentage;
    private int completedLessons;
    private int totalLessons;
    private LocalDateTime lastActivityAt;
    private boolean certificateIssued;
    private String certificateCode;
    private LocalDateTime certificateIssuedAt;

    public EnrolledStudentResponse() {
    }

    public EnrolledStudentResponse(
            Integer studentId,
            String name,
            String email,
            LocalDateTime enrolledAt,
            Double progressPercentage,
            int completedLessons,
            int totalLessons,
            LocalDateTime lastActivityAt,
            boolean certificateIssued,
            String certificateCode,
            LocalDateTime certificateIssuedAt) {
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.enrolledAt = enrolledAt;
        this.progressPercentage = progressPercentage;
        this.completedLessons = completedLessons;
        this.totalLessons = totalLessons;
        this.lastActivityAt = lastActivityAt;
        this.certificateIssued = certificateIssued;
        this.certificateCode = certificateCode;
        this.certificateIssuedAt = certificateIssuedAt;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public Double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public int getCompletedLessons() {
        return completedLessons;
    }

    public void setCompletedLessons(int completedLessons) {
        this.completedLessons = completedLessons;
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public void setTotalLessons(int totalLessons) {
        this.totalLessons = totalLessons;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public boolean isCertificateIssued() {
        return certificateIssued;
    }

    public void setCertificateIssued(boolean certificateIssued) {
        this.certificateIssued = certificateIssued;
    }

    public String getCertificateCode() {
        return certificateCode;
    }

    public void setCertificateCode(String certificateCode) {
        this.certificateCode = certificateCode;
    }

    public LocalDateTime getCertificateIssuedAt() {
        return certificateIssuedAt;
    }

    public void setCertificateIssuedAt(LocalDateTime certificateIssuedAt) {
        this.certificateIssuedAt = certificateIssuedAt;
    }
}
