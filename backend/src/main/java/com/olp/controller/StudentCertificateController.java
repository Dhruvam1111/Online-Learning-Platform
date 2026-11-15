package com.olp.controller;

import com.olp.dto.CertificateResponse;
import com.olp.dto.CompletionEvaluationResult;
import com.olp.entity.User;
import com.olp.service.CourseCompletionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for student course completion verification, certificate claiming, and certificate records
 * (FR-S77, FR-S78, FR-S79, DR-02, DR-03, DR-ED01, DR-ED02).
 */
@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('student') or hasRole('instructor') or hasRole('admin')")
public class StudentCertificateController {

    private final CourseCompletionService courseCompletionService;

    public StudentCertificateController(CourseCompletionService courseCompletionService) {
        this.courseCompletionService = courseCompletionService;
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return user;
    }

    @GetMapping("/courses/{courseId}/completion-status")
    public ResponseEntity<CompletionEvaluationResult> getCompletionStatus(
            @PathVariable Integer courseId,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(courseCompletionService.getStudentCompletionStatus(user, courseId));
    }

    @PostMapping("/courses/{courseId}/certificate/claim")
    public ResponseEntity<CertificateResponse> claimCertificate(
            @PathVariable Integer courseId,
            Authentication authentication) {
        User user = resolveUser(authentication);
        CertificateResponse certificate = courseCompletionService.issueCertificate(user.getId(), courseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(certificate);
    }

    @GetMapping("/certificates")
    public ResponseEntity<List<CertificateResponse>> getMyCertificates(Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(courseCompletionService.getStudentCertificates(user));
    }

    @GetMapping("/certificates/{certificateId}")
    public ResponseEntity<CertificateResponse> getCertificateDetails(
            @PathVariable Integer certificateId,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(courseCompletionService.getStudentCertificate(user, certificateId));
    }
}
