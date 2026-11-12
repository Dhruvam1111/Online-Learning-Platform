package com.olp.controller;

import com.olp.service.PaymentWebhookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller receiving asynchronous webhook callbacks from payment gateway (FR-PG14, FR-PG15).
 * Owned by Shubh (Webhook Engine).
 */
@RestController
@RequestMapping("/api/payments/webhook")
public class PaymentWebhookController {

    private final PaymentWebhookService webhookService;

    public PaymentWebhookController(PaymentWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * Webhook ingestion endpoint. Validates cryptographic HMAC signature and dispatches events.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signatureHeader) {

        boolean processed = webhookService.processWebhook(rawPayload, signatureHeader);

        if (processed) {
            return ResponseEntity.ok(Map.of("status", "success", "message", "Webhook event processed"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", "Invalid webhook signature or payload"));
        }
    }
}
