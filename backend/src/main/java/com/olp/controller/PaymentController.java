package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller managing course purchase and payment transactions.
 * Exposes endpoints for order creation, QR payment confirmation, and audit logs.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Initializes a payment order for course purchase (FR-PG01, FR-PG03).
     * Returns order details used to generate the payment QR code.
     */
    @PostMapping("/create-order")
    public ResponseEntity<PaymentOrderResponse> createOrder(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreatePaymentOrderRequest request) {
        PaymentOrderResponse response = paymentService.createPaymentOrder(currentUser, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Confirms a successful payment and atomically enrolls the student (FR-PG04, NFR-12).
     * Called after the student's QR payment is received.
     */
    public ResponseEntity<PaymentTransactionResponse> confirmPayment(String orderId) {
        return confirmPayment(orderId, null, null);
    }

    @org.springframework.security.access.prepost.PreAuthorize("@paymentAccess.owns(authentication, #orderId)")
    @PostMapping("/confirm/{orderId}")
    public ResponseEntity<PaymentTransactionResponse> confirmPayment(
            @PathVariable String orderId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod) {
        String method = paymentMethod;
        if ((method == null || method.isBlank()) && body != null && body.containsKey("paymentMethod")) {
            method = String.valueOf(body.get("paymentMethod"));
        }
        PaymentTransactionResponse response = (method != null && !method.isBlank())
                ? paymentService.confirmPayment(orderId, method)
                : paymentService.confirmPayment(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Backward-compatible verification endpoint — delegates to confirmPayment.
     */
    @org.springframework.security.access.prepost.PreAuthorize("@paymentAccess.owns(authentication, #request['orderId'])")
    @PostMapping("/verify")
    public ResponseEntity<PaymentTransactionResponse> verifyPayment(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Object> request) {
        String orderId = (String) request.get("orderId");
        String method = (String) request.get("paymentMethod");
        PaymentTransactionResponse response = (method != null && !method.isBlank())
                ? paymentService.confirmPayment(orderId, method)
                : paymentService.confirmPayment(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Marks a payment as failed — no enrollment is created (FR-PG04).
     * System returns to original state: course remains unenrolled.
     */
    @org.springframework.security.access.prepost.PreAuthorize("@paymentAccess.owns(authentication, #orderId)")
    @PostMapping("/fail/{orderId}")
    public ResponseEntity<PaymentTransactionResponse> failPayment(@PathVariable String orderId) {
        PaymentTransactionResponse response = paymentService.failPayment(orderId);
        return ResponseEntity.ok(response);
    }

    @org.springframework.security.access.prepost.PreAuthorize("@paymentAccess.owns(authentication, #orderId)")
    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<PaymentTransactionResponse> cancelPayment(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.cancelPayment(orderId));
    }

    /**
     * Polling endpoint to check live payment status (FR-PG01).
     */
    @org.springframework.security.access.prepost.PreAuthorize("@paymentAccess.owns(authentication, #orderId)")
    @GetMapping("/status/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderStatus(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getOrderStatus(orderId));
    }

    /**
     * Viva Demo Utility: Simulates instant QR payment capture.
     * Equivalent to calling /confirm/{orderId}.
     */
    @org.springframework.security.access.prepost.PreAuthorize("@paymentAccess.owns(authentication, #orderId)")
    @PostMapping("/sandbox-simulate-pay/{orderId}")
    public ResponseEntity<PaymentTransactionResponse> simulatePaymentCapture(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.simulatePaymentSuccess(orderId));
    }

    /**
     * Student view: History of purchased courses and transactions.
     */
    @GetMapping("/my-transactions")
    public ResponseEntity<List<PaymentTransactionResponse>> getMyTransactions(
            @AuthenticationPrincipal User currentUser) {
        List<PaymentTransactionResponse> transactions = paymentService.getStudentTransactions(currentUser);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Admin view: Platform-wide financial audit log (FR-A23, NFR-33).
     */
    @GetMapping("/admin/all")
    public ResponseEntity<List<PaymentTransactionResponse>> getAllTransactions(
            @AuthenticationPrincipal User currentUser) {
        List<PaymentTransactionResponse> transactions = paymentService.getAllTransactions(currentUser);
        return ResponseEntity.ok(transactions);
    }
    @GetMapping("/admin/transactions") public Map<String,Object> page(@AuthenticationPrincipal User user,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){return paymentService.transactionsPage(user,page,size);}
}
