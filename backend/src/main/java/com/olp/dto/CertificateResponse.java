package com.olp.dto;

import com.olp.entity.Certificate;
import com.olp.entity.StudentProfile;
import java.time.LocalDateTime;

/**
 * DTO representing an issued certificate for student and course records (FR-I12, FR-I57).
 * Includes learner information, course metadata, issue timestamps, and public verification URL (DR-02).
 */
public class CertificateResponse {

    private Integer id;
    private Integer certificateId;
    private Integer courseId;
    private String courseTitle;
    private String courseName;
    private Integer studentId;
    private Integer learnerId;
    private String studentName;
    private String learnerName;
    private String studentEmail;
    private String learnerEmail;
    private String educationLevel;
    private String certificateCode;
    private LocalDateTime issuedAt;
    private LocalDateTime issueDate;
    private String verificationUrl;
    private String publicVerificationUrl;
    private boolean valid = true;
    private String status = "VERIFIED";

    public CertificateResponse() {}

    public static CertificateResponse fromEntity(Certificate cert) {
        return fromEntity(cert, null);
    }

    public static CertificateResponse fromEntity(Certificate cert, StudentProfile profile) {
        if (cert == null) return null;
        CertificateResponse dto = new CertificateResponse();
        dto.setId(cert.getId());
        dto.setCertificateId(cert.getId());
        if (cert.getCourse() != null) {
            dto.setCourseId(cert.getCourse().getId());
            dto.setCourseTitle(cert.getCourse().getTitle());
            dto.setCourseName(cert.getCourse().getTitle());
        }
        if (cert.getStudent() != null) {
            dto.setStudentId(cert.getStudent().getId());
            dto.setLearnerId(cert.getStudent().getId());
            dto.setStudentName(cert.getStudent().getName());
            dto.setLearnerName(cert.getStudent().getName());
            dto.setStudentEmail(cert.getStudent().getEmail());
            dto.setLearnerEmail(cert.getStudent().getEmail());
        }
        if (profile != null) {
            dto.setEducationLevel(profile.getEducationLevel());
        }
        dto.setCertificateCode(cert.getCertificateCode());
        dto.setIssuedAt(cert.getIssuedAt());
        dto.setIssueDate(cert.getIssuedAt());
        if (cert.getCertificateCode() != null) {
            String vUrl = "/api/certificates/verify/" + cert.getCertificateCode();
            dto.setVerificationUrl(vUrl);
            dto.setPublicVerificationUrl(vUrl);
        }
        dto.setValid(true);
        dto.setStatus("VERIFIED");
        return dto;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
        this.certificateId = id;
    }

    public Integer getCertificateId() {
        return certificateId != null ? certificateId : id;
    }

    public void setCertificateId(Integer certificateId) {
        this.certificateId = certificateId;
        if (this.id == null) {
            this.id = certificateId;
        }
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
        if (this.courseName == null) {
            this.courseName = courseTitle;
        }
    }

    public String getCourseName() {
        return courseName != null ? courseName : courseTitle;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
        if (this.courseTitle == null) {
            this.courseTitle = courseName;
        }
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
        this.learnerId = studentId;
    }

    public Integer getLearnerId() {
        return learnerId != null ? learnerId : studentId;
    }

    public void setLearnerId(Integer learnerId) {
        this.learnerId = learnerId;
        if (this.studentId == null) {
            this.studentId = learnerId;
        }
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
        this.learnerName = studentName;
    }

    public String getLearnerName() {
        return learnerName != null ? learnerName : studentName;
    }

    public void setLearnerName(String learnerName) {
        this.learnerName = learnerName;
        if (this.studentName == null) {
            this.studentName = learnerName;
        }
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
        this.learnerEmail = studentEmail;
    }

    public String getLearnerEmail() {
        return learnerEmail != null ? learnerEmail : studentEmail;
    }

    public void setLearnerEmail(String learnerEmail) {
        this.learnerEmail = learnerEmail;
        if (this.studentEmail == null) {
            this.studentEmail = learnerEmail;
        }
    }

    public String getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(String educationLevel) {
        this.educationLevel = educationLevel;
    }

    public String getCertificateCode() {
        return certificateCode;
    }

    public void setCertificateCode(String certificateCode) {
        this.certificateCode = certificateCode;
        if (certificateCode != null && this.verificationUrl == null) {
            String vUrl = "/api/certificates/verify/" + certificateCode;
            this.verificationUrl = vUrl;
            this.publicVerificationUrl = vUrl;
        }
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
        this.issueDate = issuedAt;
    }

    public LocalDateTime getIssueDate() {
        return issueDate != null ? issueDate : issuedAt;
    }

    public void setIssueDate(LocalDateTime issueDate) {
        this.issueDate = issueDate;
        if (this.issuedAt == null) {
            this.issuedAt = issueDate;
        }
    }

    public String getVerificationUrl() {
        return verificationUrl;
    }

    public void setVerificationUrl(String verificationUrl) {
        this.verificationUrl = verificationUrl;
        if (this.publicVerificationUrl == null) {
            this.publicVerificationUrl = verificationUrl;
        }
    }

    public String getPublicVerificationUrl() {
        return publicVerificationUrl != null ? publicVerificationUrl : verificationUrl;
    }

    public void setPublicVerificationUrl(String publicVerificationUrl) {
        this.publicVerificationUrl = publicVerificationUrl;
        if (this.verificationUrl == null) {
            this.verificationUrl = publicVerificationUrl;
        }
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
