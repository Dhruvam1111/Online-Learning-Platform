package com.olp.controller;

import com.olp.entity.User;
import com.olp.dto.*;
import com.olp.service.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.*;

/** Authenticated controls for the professor-approved simulator; no provider credentials required. */
@RestController
@RequestMapping("/api/payments")
public class PaymentSimulationController {
    private final SimulatedPaymentMethods methods;
    private final PaymentEventActions actions;
    private final PaymentWebhookService webhooks;
    private final PaymentWebhookAuthenticator signatures;
    public PaymentSimulationController(SimulatedPaymentMethods methods,PaymentEventActions actions,
            PaymentWebhookService webhooks,PaymentWebhookAuthenticator signatures) {
        this.methods=methods;this.actions=actions;this.webhooks=webhooks;this.signatures=signatures;
    }
    public record SavedMethodRequest(String method,boolean consent) {}
    public record TokenPaymentRequest(String orderId) {}
    @GetMapping("/methods") public List<Map<String,Object>> list(@AuthenticationPrincipal User user) { return methods.list(user); }
    @PostMapping("/methods") public Map<String,Object> create(@AuthenticationPrincipal User user,@RequestBody SavedMethodRequest request) {
        return methods.create(user,request.method(),request.consent());
    }
    @DeleteMapping("/methods/{token}") public Map<String,String> revoke(@AuthenticationPrincipal User user,@PathVariable String token) {
        methods.revoke(user,token);return Map.of("status","revoked");
    }
    @PostMapping("/methods/{token}/pay") public PaymentTransactionResponse pay(@AuthenticationPrincipal User user,@PathVariable String token,@RequestBody TokenPaymentRequest request) {
        return methods.pay(user,token,request.orderId());
    }
    @PostMapping("/refunds/request") public Map<String,Object> refund(@AuthenticationPrincipal User user,@RequestBody RefundRequest request) {
        return actions.requestRefund(user,request);
    }
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/simulator/events") public Map<String,Object> event(@RequestBody String payload) {
        if (!webhooks.processWebhook(payload,signatures.sign(payload))) throw new IllegalArgumentException("Invalid simulator event.");
        return Map.of("accepted",true);
    }
}
