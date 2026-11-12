package com.olp.controller;

import com.olp.dto.ReconciliationReportResponse;
import com.olp.entity.User;
import com.olp.service.ReconciliationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing financial reconciliation reports and platform auditing (FR-PG12, FR-A23).
 * Owned by Dhruvam (Ledger & Reconciliation Engine).
 */
@RestController
@RequestMapping("/api/payments/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    /**
     * Generates real-time financial ledger and payment reconciliation report.
     */
    @GetMapping("/report")
    public ResponseEntity<ReconciliationReportResponse> getReconciliationReport(
            @AuthenticationPrincipal User currentUser) {
        ReconciliationReportResponse report = reconciliationService.generateReconciliationReport(currentUser);
        return ResponseEntity.ok(report);
    }
}
