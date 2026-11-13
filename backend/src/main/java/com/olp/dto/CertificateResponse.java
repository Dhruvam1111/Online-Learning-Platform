package com.olp.dto;

import com.olp.entity.Certificate;
import java.time.LocalDateTime;

/**
 * DTO representing an issued certificate for student and course records (FR-I12).
 */
public class CertificateResponse {

    private Integer id;
    private Integer courseId;
    private String courseTitle;
    private Integer studentId;
    private String studentName;
    private String certificateCode;
    private LocalDateTime issuedAt;

    public CertificateResponse() {}

    public static CertificateResponse fromEntity(Certificate cert) {
        if (cert == null) return null;
        CertificateResponse dto = new CertificateResponse();
        dto.setId(cert.getId());
        if (cert.getCourse() != null) {
            dto.setCourseId(cert.getCourse().getId());
            dto.setCourseTitle(cert.getCourse().getTitle());
        }
        if (cert.getStudent() != null) {
            dto.setStudentId(cert.getStudent().getId());
            dto.setStudentName(cert.getStudent().getName());
        }
        dto.setCertificateCode(cert.getCertificateCode());
        dto.setIssuedAt(cert.getIssuedAt());
        return dto;
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

    public String getCertificateCode() {
        return certificateCode;
    }

    public void setCertificateCode(String certificateCode) {
        this.certificateCode = certificateCode;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }
}

