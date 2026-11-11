package com.olp.controller;

import com.olp.dto.PaymentTransactionResponse;
import com.olp.entity.User;
import com.olp.repository.UserRepository;
import com.olp.service.StripePaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST controller for QR-based local payment sessions.
 * Replaced Stripe/Razorpay external gateway with a simple QR payment flow.
 */
@RestController
@RequestMapping("/api/payments/stripe")
public class StripePaymentController {

    private final StripePaymentService stripePaymentService;
    private final UserRepository userRepository;

    public StripePaymentController(StripePaymentService stripePaymentService,
                                   UserRepository userRepository) {
        this.stripePaymentService = stripePaymentService;
        this.userRepository = userRepository;
    }

    private User resolveUser(Authentication authentication, User principal) {
        if (principal != null) return principal;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object p = authentication.getPrincipal();
            if (p instanceof User u) return u;
            return userRepository.findByEmail(authentication.getName()).orElse(null);
        }
        return null;
    }

    private Integer resolveCourseId(Object rawCourseId) {
        if (rawCourseId == null) return 1;
        if (rawCourseId instanceof Number n) return n.intValue();
        String s = String.valueOf(rawCourseId).trim();
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /**
     * POST /api/payments/stripe/create-session
     * Creates a local QR payment order and returns order details.
     */
    @PostMapping("/create-session")
    public ResponseEntity<?> createSession(
            @AuthenticationPrincipal User currentUser,
            Authentication authentication,
            @RequestBody Map<String, Object> body) {

        User user = resolveUser(authentication, currentUser);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required."));
        }

        Integer courseId = resolveCourseId(body.get("courseId"));
        BigDecimal amount = body.get("amount") != null
                ? new BigDecimal(String.valueOf(body.get("amount")))
                : BigDecimal.valueOf(499);

        PaymentTransactionResponse response = stripePaymentService.createLocalPayment(user, courseId, amount);

        return ResponseEntity.ok(Map.of(
                "orderId", response.getOrderId(),
                "sessionId", "cs_test_" + java.util.UUID.randomUUID().toString().replace("-", ""),
                "sessionUrl", "/api/payments/confirm/" + response.getOrderId(),
                "status", "created",
                "amount", response.getAmount()
        ));
    }

    /**
     * POST /api/payments/stripe/verify-session
     * Kept for backward compatibility — redirects to QR confirm flow.
     * Clients should use POST /api/payments/confirm/{orderId} directly.
     */
    @PostMapping("/verify-session")
    public ResponseEntity<?> verifySession(
            @AuthenticationPrincipal User currentUser,
            Authentication authentication,
            @RequestBody Map<String, String> body) {

        String orderId = body.get("orderId");
        if (orderId == null || orderId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "orderId is required."));
        }

        // This endpoint is superseded by POST /api/payments/confirm/{orderId}
        return ResponseEntity.ok(Map.of(
                "message", "Use POST /api/payments/confirm/" + orderId + " to confirm this payment.",
                "confirmUrl", "/api/payments/confirm/" + orderId
        ));
    }
}
