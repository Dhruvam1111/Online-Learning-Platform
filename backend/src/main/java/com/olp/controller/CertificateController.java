package com.olp.controller;

import com.olp.dto.CertificateResponse;
import com.olp.service.CourseCompletionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public controller for certificate verification (DR-02).
 * Enables learners, employers, and educational institutions to verify issued completion credentials publicly.
 */
@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    private final CourseCompletionService courseCompletionService;

    @Autowired
    public CertificateController(CourseCompletionService courseCompletionService) {
        this.courseCompletionService = courseCompletionService;
    }

    /**
     * GET /api/certificates/verify/{certificateCode}
     * DR-02: Public endpoint to verify an issued certificate by its unique code.
     */
    @GetMapping("/verify/{certificateCode}")
    public ResponseEntity<CertificateResponse> verifyCertificatePath(@PathVariable String certificateCode) {
        CertificateResponse response = courseCompletionService.verifyCertificate(certificateCode);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/certificates/{certificateCode}
     * Direct alias for certificate verification.
     */
    @GetMapping("/{certificateCode:[A-Za-z0-9\\-]+}")
    public ResponseEntity<CertificateResponse> getCertificateByCode(@PathVariable String certificateCode) {
        CertificateResponse response = courseCompletionService.verifyCertificate(certificateCode);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/certificates/verify?code=...
     * Query-parameter variant of public certificate verification.
     */
    @GetMapping("/verify")
    public ResponseEntity<CertificateResponse> verifyCertificateQuery(@RequestParam("code") String code) {
        CertificateResponse response = courseCompletionService.verifyCertificate(code);
        return ResponseEntity.ok(response);
    }
}
