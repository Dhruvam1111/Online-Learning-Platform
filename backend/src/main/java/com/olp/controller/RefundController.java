package com.olp.controller;

import com.olp.dto.RefundRequest;
import com.olp.dto.RefundResponse;
import com.olp.entity.User;
import com.olp.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller handling course purchase refunds and reversals (FR-PG05, FR-PG06, FR-A24).
 * Owned by Dhruvam (Refund Engine).
 */
@RestController
@RequestMapping("/api/payments/refunds")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    /**
     * Initiates a refund for a previously purchased course.
     */
    @PostMapping("/initiate")
    public ResponseEntity<RefundResponse> initiateRefund(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody RefundRequest request) {
        RefundResponse response = refundService.processRefund(currentUser, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin view of all refunds processed on the platform (FR-A24).
     */
    @GetMapping("/all")
    public ResponseEntity<List<RefundResponse>> getAllRefunds(
            @AuthenticationPrincipal User currentUser) {
        List<RefundResponse> refunds = refundService.getAllRefunds(currentUser);
        return ResponseEntity.ok(refunds);
    }
}
