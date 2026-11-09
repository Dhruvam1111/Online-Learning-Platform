package com.olp.controller;

import com.olp.dto.InstructorEarningsResponse;
import com.olp.entity.User;
import com.olp.service.InstructorPayoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller exposing instructor revenue analytics and administrative payout settlement (FR-PG07, FR-A26).
 * Owned by Dhruvam (Payout Controller).
 */
@RestController
@RequestMapping("/api/payments/instructor")
public class InstructorPayoutController {

    private final InstructorPayoutService payoutService;

    public InstructorPayoutController(InstructorPayoutService payoutService) {
        this.payoutService = payoutService;
    }

    /**
     * Returns instructor revenue breakdown, 80% net earnings, and sales history.
     */
    @GetMapping("/earnings")
    public ResponseEntity<InstructorEarningsResponse> getMyEarnings(
            @AuthenticationPrincipal User currentUser) {
        InstructorEarningsResponse response = payoutService.getEarningsReport(currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin endpoint: Marks an instructor payout as settled and transferred.
     */
    @PostMapping("/payouts/{earningId}/settle")
    public ResponseEntity<Map<String, String>> settlePayout(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Integer earningId) {
        payoutService.settleEarningPayout(currentUser, earningId);
        return ResponseEntity.ok(Map.of("message", "Payout settled successfully", "earningId", String.valueOf(earningId)));
    }

    /**
     * Admin endpoint: Halts an instructor payout due to disputes or policy reviews (US-A26).
     */
    @PostMapping("/payouts/{earningId}/halt")
    public ResponseEntity<Map<String, String>> haltPayout(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Integer earningId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Policy review hold";
        payoutService.haltEarningPayout(currentUser, earningId, reason);
        return ResponseEntity.ok(Map.of("message", "Payout halted successfully", "earningId", String.valueOf(earningId), "status", "halted"));
    }

    /**
     * Admin endpoint: Releases hold on an instructor payout (US-A26).
     */
    @PostMapping("/payouts/{earningId}/unhalt")
    public ResponseEntity<Map<String, String>> unhaltPayout(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Integer earningId) {
        payoutService.unhaltEarningPayout(currentUser, earningId);
        return ResponseEntity.ok(Map.of("message", "Payout released back to unpaid status", "earningId", String.valueOf(earningId), "status", "unpaid"));
    }

    /**
     * Admin endpoint: Lists all instructor earnings across platform for payout management.
     */
    @GetMapping("/admin/all")
    public ResponseEntity<java.util.List<InstructorEarningsResponse.SaleRecord>> getAllEarningsForAdmin(
            @AuthenticationPrincipal User currentUser) {
        java.util.List<InstructorEarningsResponse.SaleRecord> records = payoutService.getAllEarningsForAdmin(currentUser);
        return ResponseEntity.ok(records);
    }

    /**
     * Viva Demo Utility: Simulates instant direct bank transfer (IMPS/NEFT) payout to instructor.
     */
    @PostMapping("/payouts/withdraw-all")
    public ResponseEntity<Map<String, Object>> withdrawAllEarnings(
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> response = payoutService.withdrawAllEarningsForInstructor(currentUser);
        return ResponseEntity.ok(response);
    }
}
